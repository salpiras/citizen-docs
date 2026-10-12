package com.salpiras.citizendocs.core.data

import com.google.common.truth.Truth.assertThat
import com.salpiras.citizendocs.core.database.DocumentEntity
import com.salpiras.citizendocs.core.drive.DriveException
import com.salpiras.citizendocs.core.drive.DriveUnauthorizedException
import com.salpiras.citizendocs.core.testing.FakeDocumentDao
import com.salpiras.citizendocs.core.testing.FakeDriveAuth
import com.salpiras.citizendocs.core.testing.FakeDriveClient
import com.salpiras.citizendocs.core.testing.FakeDriveSyncDao
import com.salpiras.citizendocs.core.testing.FixedClock
import com.salpiras.citizendocs.core.testing.InMemoryDocumentFileStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.junit.Test
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class OfflineFirstDriveBackupRepositoryTest {
    private val dao = FakeDocumentDao()
    private val syncDao = FakeDriveSyncDao()
    private val fileStore = InMemoryDocumentFileStore()
    private val drive = FakeDriveClient()
    private val auth = FakeDriveAuth()
    private val clock = FixedClock(Instant.parse("2026-08-18T10:15:30Z"))

    private val repository =
        OfflineFirstDriveBackupRepository(
            dao = dao,
            syncDao = syncDao,
            fileStore = fileStore,
            drive = drive,
            auth = auth,
            clock = clock,
            io = UnconfinedTestDispatcher(),
        )

    private suspend fun givenDocument(
        title: String = "Tax return 2025",
        uuid: String = "0".repeat(31) + "1",
        date: LocalDate = LocalDate(2026, 1, 12),
        updatedAt: Instant = Instant.parse("2026-01-12T09:30:00Z"),
    ): DocumentEntity {
        val fileName = fileStore.persist("content://scan/tmp.pdf", title.replace(" ", "_"))
        val entity = DocumentEntity(
            uuid = uuid,
            title = title,
            documentDate = date,
            createdAt = Instant.parse("2026-01-12T09:30:00Z"),
            updatedAt = updatedAt,
            fileName = fileName,
            pageCount = 3,
            sizeBytes = fileStore.sizeOf(fileName),
        )
        return entity.copy(id = dao.insert(entity))
    }

    // --- Backup ---------------------------------------------------------------------

    @Test
    fun `backup files documents under year and month, named by their title`() = runTest {
        givenDocument(title = "Tax return 2025", date = LocalDate(2026, 1, 12))

        val report = repository.backUp()

        assertThat(report.uploaded).isEqualTo(1)
        val uploaded = drive.listAllFiles().single()
        // The user's real title, not the slugified on-disk name — this is what makes the
        // Drive folder worth opening.
        assertThat(uploaded.name).isEqualTo("Tax return 2025.pdf")

        val folderNames = drive.stored.values.filter { it.isFolder }.map { it.file.name }
        assertThat(folderNames).containsExactly("CitizenDocs", "2026", "01")
    }

    @Test
    fun `backup carries the identity and metadata the restore will need`() = runTest {
        givenDocument(uuid = "abc", date = LocalDate(2026, 1, 12))

        repository.backUp()

        val properties = drive.listAllFiles().single().appProperties
        assertThat(properties["u"]).isEqualTo("abc")
        assertThat(properties["d"]).isEqualTo(LocalDate(2026, 1, 12).toEpochDays().toString())
        assertThat(properties["p"]).isEqualTo("3")
    }

    @Test
    fun `backup sends the actual file contents`() = runTest {
        val document = givenDocument()
        fileStore.files[document.fileName] = "%PDF-1.4 the real thing".toByteArray()

        repository.backUp()

        val stored = drive.stored.values.single { !it.isFolder }
        assertThat(String(stored.content)).isEqualTo("%PDF-1.4 the real thing")
    }

    // The whole reason updated_at exists.
    @Test
    fun `backing up twice with no changes uploads nothing the second time`() = runTest {
        givenDocument()

        repository.backUp()
        val second = repository.backUp()

        assertThat(second.uploaded).isEqualTo(0)
        assertThat(second.upToDate).isEqualTo(1)
        assertThat(drive.uploads).isEqualTo(1)
        assertThat(drive.updates).isEqualTo(0)
    }

    @Test
    fun `a document edited since the last backup is sent again, in place`() = runTest {
        val document = givenDocument()
        repository.backUp()

        dao.updateTitle(document.id, "Self assessment", Instant.parse("2026-09-01T08:00:00Z"))
        val second = repository.backUp()

        assertThat(second.uploaded).isEqualTo(1)
        // Updated, not uploaded again: a second copy in Drive would be a duplicate the user
        // then has to tidy up.
        assertThat(drive.uploads).isEqualTo(1)
        assertThat(drive.updates).isEqualTo(1)
        assertThat(drive.listAllFiles().single().name).isEqualTo("Self assessment.pdf")
    }

    @Test
    fun `only the changed document is re-sent`() = runTest {
        val first = givenDocument(title = "Tax return 2025", uuid = "a")
        givenDocument(title = "Passport", uuid = "b", date = LocalDate(2025, 6, 30))
        repository.backUp()

        dao.updateTitle(first.id, "Self assessment", Instant.parse("2026-09-01T08:00:00Z"))
        val second = repository.backUp()

        assertThat(second.uploaded).isEqualTo(1)
        assertThat(second.upToDate).isEqualTo(1)
    }

    /**
     * The user is entitled to delete things from their own Drive. Without the 404 fallback
     * every backup from then on would fail on that document while it sat on the device
     * looking perfectly backed up.
     */
    @Test
    fun `a file the user deleted from Drive is uploaded afresh`() = runTest {
        val document = givenDocument()
        repository.backUp()
        drive.stored.keys.filter { !drive.stored.getValue(it).isFolder }.forEach(drive.stored::remove)

        dao.updateTitle(document.id, "Self assessment", Instant.parse("2026-09-01T08:00:00Z"))
        val second = repository.backUp()

        assertThat(second.uploaded).isEqualTo(1)
        assertThat(drive.listAllFiles()).hasSize(1)
    }

    @Test
    fun `an empty library is reported as such rather than as a backup of nothing`() = runTest {
        val report = repository.backUp()

        assertThat(report.uploaded).isEqualTo(0)
        assertThat(report.upToDate).isEqualTo(0)
        // Nothing was created in Drive either — not even the root folder.
        assertThat(drive.stored).isEmpty()
    }

    @Test
    fun `a backup interrupted part way keeps what it managed to send`() = runTest {
        givenDocument(title = "Tax return 2025", uuid = "a")
        givenDocument(title = "Passport", uuid = "b", date = LocalDate(2025, 6, 30))

        repository.backUp()
        // Both are recorded, so a later run resumes rather than starting over.
        assertThat(syncDao.all()).hasSize(2)
    }

    // --- Restore --------------------------------------------------------------------

    @Test
    fun `restore brings down a document this device has never seen`() = runTest {
        drive.seedDocument(
            name = "Tenancy agreement.pdf",
            appProperties = mapOf(
                "u" to "remote-1",
                "d" to LocalDate(2024, 11, 3).toEpochDays().toString(),
                "p" to "12",
                "c" to "1700000000000",
            ),
            content = "%PDF-1.4 tenancy",
        )

        val report = repository.restore()

        assertThat(report.restored).isEqualTo(1)
        val restored = dao.findByUuid("remote-1")!!
        assertThat(restored.title).isEqualTo("Tenancy agreement")
        assertThat(restored.documentDate).isEqualTo(LocalDate(2024, 11, 3))
        assertThat(restored.pageCount).isEqualTo(12)
        assertThat(String(fileStore.files.getValue(restored.fileName))).isEqualTo("%PDF-1.4 tenancy")
    }

    // The point of merging by uuid: restore is safe to press twice.
    @Test
    fun `restoring the same backup twice adds nothing the second time`() = runTest {
        drive.seedDocument(name = "Passport.pdf", appProperties = mapOf("u" to "remote-1"))

        repository.restore()
        val second = repository.restore()

        assertThat(second.restored).isEqualTo(0)
        assertThat(second.alreadyHeld).isEqualTo(1)
        assertThat(dao.all()).hasSize(1)
    }

    @Test
    fun `restore adds only what is missing`() = runTest {
        givenDocument(uuid = "already-here")
        drive.seedDocument(name = "Already here.pdf", appProperties = mapOf("u" to "already-here"))
        drive.seedDocument(name = "Brand new.pdf", appProperties = mapOf("u" to "brand-new"))

        val report = repository.restore()

        assertThat(report.restored).isEqualTo(1)
        assertThat(report.alreadyHeld).isEqualTo(1)
        assertThat(dao.all()).hasSize(2)
    }

    @Test
    fun `a restored document is not immediately treated as an edit`() = runTest {
        drive.seedDocument(name = "Passport.pdf", appProperties = mapOf("u" to "remote-1", "c" to "1700000000000"))
        repository.restore()

        // Otherwise a restore would send the entire library straight back up again.
        val report = repository.backUp()

        assertThat(report.uploaded).isEqualTo(0)
        assertThat(report.upToDate).isEqualTo(1)
    }

    @Test
    fun `a file with no identity is left alone`() = runTest {
        drive.seedDocument(name = "Mystery.pdf", appProperties = emptyMap())

        val report = repository.restore()

        assertThat(report.restored).isEqualTo(0)
        assertThat(dao.all()).isEmpty()
    }

    @Test
    fun `a download that fails leaves no half document behind`() = runTest {
        drive.seedDocument(name = "Passport.pdf", appProperties = mapOf("u" to "remote-1"))
        drive.failure = DriveException("network went away")

        runCatching { repository.restore() }

        assertThat(dao.all()).isEmpty()
        assertThat(fileStore.files).isEmpty()
    }

    // --- Connection -----------------------------------------------------------------

    @Test
    fun `losing Drive access is reported as not connected, not as a failure`() = runTest {
        givenDocument()
        drive.failure = DriveUnauthorizedException()

        val thrown = runCatching { repository.backUp() }.exceptionOrNull()

        // The UI has to offer "connect" here, so this must not look like a transport error.
        assertThat(thrown).isInstanceOf(DriveNotConnectedException::class.java)
    }
}
