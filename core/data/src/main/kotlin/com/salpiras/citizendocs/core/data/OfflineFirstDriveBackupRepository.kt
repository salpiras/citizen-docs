package com.salpiras.citizendocs.core.data

import com.salpiras.citizendocs.core.common.CitizenDispatcher
import com.salpiras.citizendocs.core.common.Dispatcher
import com.salpiras.citizendocs.core.database.DocumentDao
import com.salpiras.citizendocs.core.database.DocumentEntity
import com.salpiras.citizendocs.core.database.DriveSyncDao
import com.salpiras.citizendocs.core.database.DriveSyncEntity
import com.salpiras.citizendocs.core.drive.DriveAuth
import com.salpiras.citizendocs.core.drive.DriveAuthorization
import com.salpiras.citizendocs.core.drive.DriveClient
import com.salpiras.citizendocs.core.drive.DriveContent
import com.salpiras.citizendocs.core.drive.DriveException
import com.salpiras.citizendocs.core.drive.DriveFile
import com.salpiras.citizendocs.core.drive.DriveUnauthorizedException
import com.salpiras.citizendocs.core.model.slugify
import com.salpiras.citizendocs.core.storage.DocumentFileStore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toJavaLocalDate
import java.net.HttpURLConnection
import javax.inject.Inject
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Moves documents between this device and a folder in the user's Drive.
 *
 * The folder is laid out the way the list is — `CitizenDocs/2026/01/Tax return 2025.pdf` —
 * and the Drive file's *name* is the user's real title rather than the slugified on-disk
 * one, both so the folder is worth opening and because the title will not fit in
 * `appProperties`, whose budget is 124 bytes for key and value together.
 */
internal class OfflineFirstDriveBackupRepository
@Inject
constructor(
    private val dao: DocumentDao,
    private val syncDao: DriveSyncDao,
    private val fileStore: DocumentFileStore,
    private val drive: DriveClient,
    private val auth: DriveAuth,
    private val clock: Clock,
    @param:Dispatcher(CitizenDispatcher.IO) private val io: CoroutineDispatcher,
) : DriveBackupRepository {

    override suspend fun connect(): DriveConnection = when (val result = auth.authorize()) {
        DriveAuthorization.Granted -> DriveConnection.Connected
        is DriveAuthorization.ConsentRequired -> DriveConnection.NeedsConsent(result.consent)
        is DriveAuthorization.Failed -> DriveConnection.Failed(result.cause)
    }

    override suspend fun backUp(): BackupReport = withContext(io) {
        translatingAuthFailure { performBackUp() }
    }

    override suspend fun restore(): RestoreReport = withContext(io) {
        translatingAuthFailure { performRestore() }
    }

    /**
     * `:core:drive`'s way of saying "not allowed in" becomes this module's way of saying it,
     * so nothing above here has to import a Drive type to tell that case apart.
     */
    private suspend fun <T> translatingAuthFailure(body: suspend () -> T): T = try {
        body()
    } catch (e: DriveUnauthorizedException) {
        throw DriveNotConnectedException().apply { initCause(e) }
    }

    private suspend fun performBackUp(): BackupReport {
        val documents = dao.all()
        if (documents.isEmpty()) return BackupReport(uploaded = 0, upToDate = 0)

        val root = drive.ensureFolder(ROOT_FOLDER)
        // Folders are resolved once per run, not once per document: a library of forty
        // documents in one month would otherwise ask Drive for the same two folders forty
        // times over.
        val folders = FolderCache(drive, root.id)

        var uploaded = 0
        var upToDate = 0

        documents.forEach { document ->
            val sync = syncDao.find(document.uuid)
            if (sync != null && document.updatedAt <= sync.uploadedAt) {
                upToDate++
                return@forEach
            }

            val parentId = folders.monthFolder(document.documentDate)
            val file = document.sendTo(parentId, existing = sync?.driveFileId)

            // Written only once Drive has confirmed. A run that dies half way leaves the
            // documents it never sent looking unsent, which is what makes the next run
            // resume instead of assuming.
            syncDao.upsert(
                DriveSyncEntity(
                    documentUuid = document.uuid,
                    driveFileId = file.id,
                    uploadedAt = clock.now(),
                ),
            )
            uploaded++
        }

        return BackupReport(uploaded = uploaded, upToDate = upToDate)
    }

    private suspend fun performRestore(): RestoreReport {
        var restored = 0
        var alreadyHeld = 0

        drive.listAllFiles().forEach { file ->
            // A file with no uuid is not something this app wrote in a shape it understands.
            // Skipping is right: inventing an identity for it would make the next restore
            // bring it down all over again.
            val uuid = file.appProperties[PROP_UUID] ?: return@forEach

            if (dao.findByUuid(uuid) != null) {
                alreadyHeld++
                return@forEach
            }

            restoreOne(file, uuid)
            restored++
        }

        return RestoreReport(restored = restored, alreadyHeld = alreadyHeld)
    }

    private suspend fun restoreOne(file: DriveFile, uuid: String) {
        val title = file.name.removeSuffix(PDF_SUFFIX)
        val fileName = fileStore.persistStream(slugify(title)) { out -> drive.download(file.id, out) }

        try {
            val createdAt = file.appProperties[PROP_CREATED_AT]
                ?.toLongOrNull()
                ?.let(Instant::fromEpochMilliseconds)
                ?: clock.now()

            dao.insert(
                DocumentEntity(
                    // The remote uuid, not a fresh one. Restoring must not change what a
                    // document *is*, or a second restore would bring it down again.
                    uuid = uuid,
                    title = title,
                    documentDate = file.appProperties[PROP_DATE]
                        ?.toLongOrNull()
                        ?.let(LocalDate.Companion::fromEpochDays)
                        ?: createdAt.toLocalDateOrToday(),
                    createdAt = createdAt,
                    // Equal to createdAt, so a restore does not immediately look like an
                    // edit and send every document straight back up again.
                    updatedAt = createdAt,
                    fileName = fileName,
                    pageCount = file.appProperties[PROP_PAGES]?.toIntOrNull() ?: 1,
                    sizeBytes = fileStore.sizeOf(fileName),
                ),
            )
        } catch (e: Throwable) {
            // Same rollback as adding a scan: no row, no file.
            fileStore.delete(fileName)
            throw e
        }

        syncDao.upsert(
            DriveSyncEntity(documentUuid = uuid, driveFileId = file.id, uploadedAt = clock.now()),
        )
    }

    /**
     * Uploads, or replaces the bytes of a file already up there.
     *
     * The 404 branch matters more than it looks: the user can delete a file from Drive
     * whenever they like, and without this every later backup would fail on it forever
     * while the document sat on the device looking backed up.
     */
    private suspend fun DocumentEntity.sendTo(parentId: String, existing: String?): DriveFile {
        val name = "$title$PDF_SUFFIX"
        val properties = mapOf(
            PROP_UUID to uuid,
            PROP_DATE to documentDate.toEpochDays().toString(),
            PROP_PAGES to pageCount.toString(),
            PROP_CREATED_AT to createdAt.toEpochMilliseconds().toString(),
        )
        val content = DriveContent { fileStore.openStream(fileName) }

        if (existing == null) return drive.upload(name, parentId, properties, content)

        return try {
            drive.update(existing, name, properties, content)
        } catch (e: DriveException) {
            if (e.statusCode != HttpURLConnection.HTTP_NOT_FOUND) throw e
            drive.upload(name, parentId, properties, content)
        }
    }

    private companion object {
        const val ROOT_FOLDER = "CitizenDocs"
        const val PDF_SUFFIX = ".pdf"

        // Single letters because the 124-byte budget covers the key as well as the value.
        const val PROP_UUID = "u"
        const val PROP_DATE = "d"
        const val PROP_PAGES = "p"
        const val PROP_CREATED_AT = "c"
    }
}

/** Remembers the year and month folders resolved during one backup run. */
private class FolderCache(private val drive: DriveClient, private val rootId: String) {
    private val resolved = mutableMapOf<String, String>()

    suspend fun monthFolder(date: LocalDate): String {
        val javaDate = date.toJavaLocalDate()
        val year = "%04d".format(javaDate.year)
        val month = "%02d".format(javaDate.monthValue)

        val yearId = resolved.getOrPut(year) { drive.ensureFolder(year, rootId).id }
        return resolved.getOrPut("$year/$month") { drive.ensureFolder(month, yearId).id }
    }
}

private fun Instant.toLocalDateOrToday(): LocalDate = LocalDate.fromEpochDays(toEpochMilliseconds() / MILLIS_PER_DAY)

private const val MILLIS_PER_DAY = 86_400_000L
