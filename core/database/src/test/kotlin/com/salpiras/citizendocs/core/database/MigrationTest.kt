package com.salpiras.citizendocs.core.database

import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The migration runs against real SQLite, on the JVM via Robolectric, like the DAO test.
 *
 * Worth more than most tests in this project. A broken migration does not fail a build or
 * throw in review — it destroys documents on devices that already have them, and only those
 * devices, which is precisely the case a developer with a freshly installed debug build
 * never sees. The README notes that an earlier version of this app used
 * `fallbackToDestructiveMigration()`; this is the test that keeps that from happening again
 * by accident.
 *
 * `runMigrationsAndValidate(..., validateDroppedTables = true)` also checks the post-migration
 * schema against the committed `2.json`, so a migration that produces *almost* the right
 * shape fails here rather than at runtime on someone's phone.
 */
// Pinned like DocumentDaoTest: Robolectric does not ship an image for the project's
// targetSdk yet, and without this the class fails to configure before any test runs.
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        CitizenDocsDatabase::class.java,
    )

    @Test
    fun `migrating from 1 to 2 keeps every document and gives each one an identity`() {
        helper.createDatabase(TEST_DB, 1).use { db ->
            db.insertV1Document(title = "Tax return 2025", fileName = "Tax_return_2025.pdf", createdAt = 1_000L)
            db.insertV1Document(title = "Passport", fileName = "Passport.pdf", createdAt = 2_000L)
        }

        helper.runMigrationsAndValidate(TEST_DB, 2, true, MIGRATION_1_2).use { db ->
            db.query("SELECT title, uuid, created_at, updated_at FROM documents ORDER BY id").use { cursor ->
                assertThat(cursor.count).isEqualTo(2)

                val uuids = mutableListOf<String>()
                while (cursor.moveToNext()) {
                    val uuid = cursor.getString(1)
                    uuids += uuid

                    // 32 lowercase hex, the same shape newDocumentUuid() produces. A row
                    // left on the '' default would mean the backfill silently skipped it.
                    assertThat(uuid).matches("[0-9a-f]{32}")

                    // An untouched document has not been edited, so it must not claim to
                    // have changed at migration time — that would tell a future backup the
                    // whole library needs re-uploading.
                    assertThat(cursor.getLong(3)).isEqualTo(cursor.getLong(2))
                }

                assertThat(uuids).containsNoDuplicates()
                assertThat(uuids).hasSize(2)
            }
        }
    }

    @Test
    fun `migrating an empty database succeeds`() {
        helper.createDatabase(TEST_DB, 1).close()

        helper.runMigrationsAndValidate(TEST_DB, 2, true, MIGRATION_1_2).use { db ->
            db.query("SELECT COUNT(*) FROM documents").use { cursor ->
                cursor.moveToFirst()
                assertThat(cursor.getInt(0)).isEqualTo(0)
            }
        }
    }

    // Guards the ordering inside the migration: the unique index is created after the
    // backfill. Create it first and the second row, still holding the '' default, collides.
    @Test
    fun `the uuid index is unique after migrating several documents`() {
        helper.createDatabase(TEST_DB, 1).use { db ->
            repeat(DOCUMENT_COUNT) { index ->
                db.insertV1Document(title = "Document $index", fileName = "doc_$index.pdf", createdAt = index.toLong())
            }
        }

        helper.runMigrationsAndValidate(TEST_DB, 2, true, MIGRATION_1_2).use { db ->
            db.query("SELECT COUNT(DISTINCT uuid) FROM documents").use { cursor ->
                cursor.moveToFirst()
                assertThat(cursor.getInt(0)).isEqualTo(DOCUMENT_COUNT)
            }
        }
    }

    @Test
    fun `migrating from 2 to 3 adds an empty sync table`() {
        helper.createDatabase(TEST_DB, 2).close()

        helper.runMigrationsAndValidate(TEST_DB, 3, true, MIGRATION_2_3).use { db ->
            db.query("SELECT COUNT(*) FROM drive_sync").use { cursor ->
                cursor.moveToFirst()
                // Empty is the truth, not a starting guess: before this version nothing had
                // been uploaded, so claiming otherwise would make the first backup skip files.
                assertThat(cursor.getInt(0)).isEqualTo(0)
            }
        }
    }

    /**
     * The upgrade someone who installed the very first version will actually take. Running
     * the migrations one at a time proves each works; running them in sequence proves they
     * work after each other, which is the only path a real device follows.
     */
    @Test
    fun `migrating all the way from 1 to 3 keeps the documents`() {
        helper.createDatabase(TEST_DB, 1).use { db ->
            db.insertV1Document(title = "Tax return 2025", fileName = "Tax_return_2025.pdf", createdAt = 1_000L)
        }

        helper.runMigrationsAndValidate(TEST_DB, 3, true, MIGRATION_1_2, MIGRATION_2_3).use { db ->
            db.query("SELECT title, uuid FROM documents").use { cursor ->
                assertThat(cursor.count).isEqualTo(1)
                cursor.moveToFirst()
                assertThat(cursor.getString(0)).isEqualTo("Tax return 2025")
                assertThat(cursor.getString(1)).matches("[0-9a-f]{32}")
            }
        }
    }

    private companion object {
        const val TEST_DB = "migration-test.db"

        /** Enough rows that a backfill stamping one shared value would collide. */
        const val DOCUMENT_COUNT = 10
    }
}

/**
 * Inserts through raw SQL rather than through [DocumentEntity], because the entity is the
 * *current* schema. Using it here would write v2 rows into a v1 database and the test would
 * stop describing the upgrade it claims to cover.
 */
private fun androidx.sqlite.db.SupportSQLiteDatabase.insertV1Document(
    title: String,
    fileName: String,
    createdAt: Long,
) = execSQL(
    """
    INSERT INTO documents (title, document_date, created_at, file_name, page_count, size_bytes)
    VALUES (?, ?, ?, ?, ?, ?)
    """.trimIndent(),
    arrayOf<Any>(title, 20_000L, createdAt, fileName, 3, 248_000L),
)
