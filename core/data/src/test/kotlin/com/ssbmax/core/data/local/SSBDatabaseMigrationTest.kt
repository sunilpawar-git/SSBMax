package com.ssbmax.core.data.local

import android.content.ContentValues
import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.ssbmax.core.data.local.DatabaseMigrations.MIGRATION_11_12
import com.ssbmax.core.data.local.DatabaseMigrations.MIGRATION_16_17
import com.ssbmax.core.data.local.DatabaseMigrations.MIGRATION_17_18
import com.ssbmax.core.data.local.DatabaseMigrations.MIGRATION_20_21
import com.ssbmax.core.data.local.DatabaseMigrations.MIGRATION_21_22
import com.ssbmax.core.data.local.DatabaseMigrations.MIGRATION_22_23
import com.ssbmax.core.data.local.DatabaseMigrations.MIGRATION_23_24
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class SSBDatabaseMigrationTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val dbName = "migration-test"

    @After
    fun tearDown() {
        context.deleteDatabase(dbName)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun migrate11To12_addsPiqAndSdUsageColumns() = runTest {
        // Create version 11 database with legacy schema
        val factory = FrameworkSQLiteOpenHelperFactory()
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(11) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS test_usage (
                            id TEXT NOT NULL PRIMARY KEY,
                            userId TEXT NOT NULL,
                            month TEXT NOT NULL,
                            oirTestsUsed INTEGER NOT NULL DEFAULT 0,
                            tatTestsUsed INTEGER NOT NULL DEFAULT 0,
                            watTestsUsed INTEGER NOT NULL DEFAULT 0,
                            srtTestsUsed INTEGER NOT NULL DEFAULT 0,
                            ppdtTestsUsed INTEGER NOT NULL DEFAULT 0,
                            gtoTestsUsed INTEGER NOT NULL DEFAULT 0,
                            interviewTestsUsed INTEGER NOT NULL DEFAULT 0,
                            lastUpdated INTEGER NOT NULL
                        )
                        """.trimIndent()
                    )
                    val values = ContentValues().apply {
                        put("id", "u1_2025-01")
                        put("userId", "u1")
                        put("month", "2025-01")
                        put("oirTestsUsed", 1)
                        put("tatTestsUsed", 2)
                        put("watTestsUsed", 0)
                        put("srtTestsUsed", 0)
                        put("ppdtTestsUsed", 0)
                        put("gtoTestsUsed", 0)
                        put("interviewTestsUsed", 0)
                        put("lastUpdated", 100L)
                    }
                    db.insert("test_usage", 0, values)
                }

                override fun onUpgrade(
                    db: SupportSQLiteDatabase,
                    oldVersion: Int,
                    newVersion: Int
                ) {
                    // not needed for setup
                }
            })
            .build()
        val openHelper = factory.create(config)
        openHelper.writableDatabase.apply {
            MIGRATION_11_12.migrate(this)
            // Manually bump version after migration
            version = 12
            query("SELECT piqTestsUsed, sdTestsUsed, tatTestsUsed FROM test_usage WHERE id = 'u1_2025-01'")
                .use { cursor ->
                    check(cursor.moveToFirst())
                    val piq = cursor.getInt(0)
                    val sd = cursor.getInt(1)
                    val tat = cursor.getInt(2)
                    assert(piq == 0)
                    assert(sd == 0)
                    assert(tat == 2)
                }
            close()
        }
        openHelper.close()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun migrate16To17_addsQuestionImageUrlColumn() = runTest {
        val factory = FrameworkSQLiteOpenHelperFactory()
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name("migration-test-16-17")
            .callback(object : SupportSQLiteOpenHelper.Callback(16) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS cached_oir_questions (
                            id TEXT PRIMARY KEY NOT NULL,
                            questionNumber INTEGER NOT NULL,
                            type TEXT NOT NULL,
                            subtype TEXT,
                            questionText TEXT NOT NULL,
                            optionsJson TEXT NOT NULL,
                            correctAnswerId TEXT NOT NULL,
                            explanation TEXT NOT NULL,
                            difficulty TEXT NOT NULL,
                            tags TEXT NOT NULL,
                            batchId TEXT NOT NULL,
                            cachedAt INTEGER NOT NULL,
                            lastUsed INTEGER,
                            usageCount INTEGER NOT NULL
                        )
                        """.trimIndent()
                    )
                }

                override fun onUpgrade(
                    db: SupportSQLiteDatabase,
                    oldVersion: Int,
                    newVersion: Int
                ) {}
            })
            .build()
        val openHelper = factory.create(config)
        openHelper.writableDatabase.apply {
            MIGRATION_16_17.migrate(this)
            version = 17

            query("PRAGMA table_info(cached_oir_questions)")
                .use { cursor ->
                    val columnNames = mutableSetOf<String>()
                    while (cursor.moveToNext()) {
                        columnNames.add(cursor.getString(1))
                    }
                    assert(columnNames.contains("questionImageUrl")) {
                        "Migration 16→17 failed: questionImageUrl column not found"
                    }
                }
            close()
        }
        openHelper.close()
        context.deleteDatabase("migration-test-16-17")
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun migrate17To18_createsCompositeIndexOnTypeAndLastUsed() = runTest {
        val factory = FrameworkSQLiteOpenHelperFactory()
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name("migration-test-17-18")
            .callback(object : SupportSQLiteOpenHelper.Callback(17) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS cached_oir_questions (
                            id TEXT PRIMARY KEY NOT NULL,
                            questionNumber INTEGER NOT NULL,
                            type TEXT NOT NULL,
                            subtype TEXT,
                            questionText TEXT NOT NULL,
                            optionsJson TEXT NOT NULL,
                            correctAnswerId TEXT NOT NULL,
                            explanation TEXT NOT NULL,
                            questionImageUrl TEXT,
                            difficulty TEXT NOT NULL,
                            tags TEXT NOT NULL,
                            batchId TEXT NOT NULL,
                            cachedAt INTEGER NOT NULL,
                            lastUsed INTEGER,
                            usageCount INTEGER NOT NULL
                        )
                        """.trimIndent()
                    )
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS index_cached_oir_questions_type ON cached_oir_questions(type)"
                    )
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS index_cached_oir_questions_batchId ON cached_oir_questions(batchId)"
                    )
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS index_cached_oir_questions_lastUsed ON cached_oir_questions(lastUsed)"
                    )
                }

                override fun onUpgrade(
                    db: SupportSQLiteDatabase,
                    oldVersion: Int,
                    newVersion: Int
                ) {}
            })
            .build()
        val openHelper = factory.create(config)
        openHelper.writableDatabase.apply {
            MIGRATION_17_18.migrate(this)
            version = 18

            query("PRAGMA index_info(index_oir_type_lastUsed)")
                .use { cursor ->
                    val columns = mutableListOf<String>()
                    while (cursor.moveToNext()) {
                        columns.add(cursor.getString(2))
                    }
                    assert(columns.size == 2) {
                        "Migration 17→18 failed: expected 2 columns in composite index, got ${columns.size}"
                    }
                    assert(columns[0] == "type") {
                        "Migration 17→18 failed: first column should be 'type', got '${columns[0]}'"
                    }
                    assert(columns[1] == "lastUsed") {
                        "Migration 17→18 failed: second column should be 'lastUsed', got '${columns[1]}'"
                    }
                }

            query("PRAGMA index_list(cached_oir_questions)")
                .use { cursor ->
                    val indexNames = mutableSetOf<String>()
                    while (cursor.moveToNext()) {
                        indexNames.add(cursor.getString(1))
                    }
                    assert(indexNames.contains("index_oir_type_lastUsed")) {
                        "Composite index index_oir_type_lastUsed not found"
                    }
                    assert(!indexNames.contains("index_cached_oir_questions_lastUsed")) {
                        "Old index index_cached_oir_questions_lastUsed should be dropped"
                    }
                }

            close()
        }
        openHelper.close()
        context.deleteDatabase("migration-test-17-18")
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun migrate19To20_createsSyncMetadataTableAndPreservesQuestions() = runTest {
        val factory = FrameworkSQLiteOpenHelperFactory()
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name("migration-test-19-20")
            .callback(object : SupportSQLiteOpenHelper.Callback(19) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS cached_oir_questions (
                            id TEXT PRIMARY KEY NOT NULL,
                            questionNumber INTEGER NOT NULL,
                            type TEXT NOT NULL,
                            subtype TEXT,
                            questionText TEXT NOT NULL,
                            optionsJson TEXT NOT NULL,
                            correctAnswerId TEXT NOT NULL,
                            explanation TEXT NOT NULL,
                            questionImageUrl TEXT,
                            difficulty TEXT NOT NULL,
                            tags TEXT NOT NULL,
                            batchId TEXT NOT NULL,
                            cachedAt INTEGER NOT NULL,
                            lastUsed INTEGER,
                            usageCount INTEGER NOT NULL
                        )
                        """.trimIndent()
                    )
                    // Seed a row to prove the migration preserves existing questions.
                    db.execSQL(
                        """
                        INSERT INTO cached_oir_questions
                        (id, questionNumber, type, questionText, optionsJson, correctAnswerId,
                         explanation, difficulty, tags, batchId, cachedAt, usageCount)
                        VALUES ('q1', 1, 'VERBAL_REASONING', 'Q?', '[]', 'opt_a', '', 'MEDIUM', '',
                                'batch_pdf_001', 0, 0)
                        """.trimIndent()
                    )
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()
        val openHelper = factory.create(config)
        openHelper.writableDatabase.apply {
            DatabaseMigrations.MIGRATION_19_20.migrate(this)
            version = 20

            // New single-row metadata table exists and is writable.
            query("SELECT name FROM sqlite_master WHERE type='table' AND name='oir_sync_metadata'")
                .use { cursor ->
                    assert(cursor.moveToFirst()) { "Migration 19→20 failed: oir_sync_metadata table not created" }
                }
            execSQL("INSERT INTO oir_sync_metadata (id, contentVersion, lastSyncAt) VALUES (0, 2, 123)")
            query("SELECT contentVersion FROM oir_sync_metadata WHERE id = 0").use { cursor ->
                assert(cursor.moveToFirst() && cursor.getInt(0) == 2) {
                    "Migration 19→20 failed: could not read back sync metadata"
                }
            }

            // Existing question row preserved.
            query("SELECT COUNT(*) FROM cached_oir_questions").use { cursor ->
                cursor.moveToFirst()
                assert(cursor.getInt(0) == 1) { "Migration 19→20 dropped existing questions" }
            }

            close()
        }
        openHelper.close()
        context.deleteDatabase("migration-test-19-20")
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun migrate20To21_addsCorrectAnswerIdsColumn() = runTest {
        val factory = FrameworkSQLiteOpenHelperFactory()
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name("migration-test-20-21")
            .callback(object : SupportSQLiteOpenHelper.Callback(20) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS cached_oir_questions (
                            id TEXT PRIMARY KEY NOT NULL,
                            questionNumber INTEGER NOT NULL,
                            type TEXT NOT NULL,
                            subtype TEXT,
                            questionText TEXT NOT NULL,
                            optionsJson TEXT NOT NULL,
                            correctAnswerId TEXT NOT NULL,
                            explanation TEXT NOT NULL,
                            questionImageUrl TEXT,
                            difficulty TEXT NOT NULL,
                            tags TEXT NOT NULL,
                            batchId TEXT NOT NULL,
                            cachedAt INTEGER NOT NULL,
                            lastUsed INTEGER,
                            usageCount INTEGER NOT NULL
                        )
                        """.trimIndent()
                    )
                    db.execSQL(
                        """
                        INSERT INTO cached_oir_questions
                        (id, questionNumber, type, questionText, optionsJson, correctAnswerId,
                         explanation, difficulty, tags, batchId, cachedAt, usageCount)
                        VALUES ('q1', 1, 'NON_VERBAL_REASONING', 'Which two belong to Class A?',
                                '[]', '', 'Answer: 2 and 3.', 'HARD', '', 'batch_001', 0, 0)
                        """.trimIndent()
                    )
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()
        val openHelper = factory.create(config)
        openHelper.writableDatabase.apply {
            MIGRATION_20_21.migrate(this)
            version = 21

            // Column must exist in schema
            query("PRAGMA table_info(cached_oir_questions)")
                .use { cursor ->
                    val columnNames = mutableSetOf<String>()
                    while (cursor.moveToNext()) {
                        columnNames.add(cursor.getString(1))
                    }
                    assert(columnNames.contains("correctAnswerIds")) {
                        "Migration 20→21 failed: correctAnswerIds column not found"
                    }
                }

            // Existing row preserved; new column is NULL for single-answer questions
            query("SELECT correctAnswerIds FROM cached_oir_questions WHERE id = 'q1'")
                .use { cursor ->
                    assert(cursor.moveToFirst()) { "Migration 20→21: existing row disappeared" }
                    assert(cursor.isNull(0)) {
                        "Migration 20→21: correctAnswerIds should be NULL for pre-migration rows"
                    }
                }

            close()
        }
        openHelper.close()
        context.deleteDatabase("migration-test-20-21")
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun migrate21To22_renamesContextToImageContextJsonAndAddsGenderTag() = runTest {
        // WHY: Phase 6 replaces `context: String` with `imageContextJson: String` + `genderTag: GenderTag`
        // on cached_ppdt_images. Existing rows must keep their data; genderTag defaults to MIXED so
        // old images remain visible to all users (no accidental gender lock-out).
        val factory = FrameworkSQLiteOpenHelperFactory()
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name("migration-test-21-22")
            .callback(object : SupportSQLiteOpenHelper.Callback(21) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS cached_ppdt_images (
                            id TEXT NOT NULL,
                            imageUrl TEXT NOT NULL,
                            localFilePath TEXT,
                            imageDescription TEXT NOT NULL,
                            context TEXT NOT NULL DEFAULT '',
                            viewingTimeSeconds INTEGER NOT NULL,
                            writingTimeMinutes INTEGER NOT NULL,
                            minCharacters INTEGER NOT NULL,
                            maxCharacters INTEGER NOT NULL,
                            category TEXT,
                            difficulty TEXT,
                            batchId TEXT NOT NULL,
                            cachedAt INTEGER NOT NULL,
                            lastUsed INTEGER,
                            usageCount INTEGER NOT NULL,
                            imageDownloaded INTEGER NOT NULL,
                            PRIMARY KEY (id)
                        )
                        """.trimIndent()
                    )
                    db.execSQL(
                        """
                        INSERT INTO cached_ppdt_images
                        (id, imageUrl, imageDescription, context, viewingTimeSeconds,
                         writingTimeMinutes, minCharacters, maxCharacters, batchId,
                         cachedAt, usageCount, imageDownloaded)
                        VALUES ('img_001', 'https://example.com/img.jpg', 'A scene', 'old context text',
                                30, 4, 200, 1000, 'batch_old', 0, 0, 0)
                        """.trimIndent()
                    )
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()
        val openHelper = factory.create(config)
        openHelper.writableDatabase.apply {
            MIGRATION_21_22.migrate(this)
            version = 22

            // imageContextJson column must exist; context column must be gone
            query("PRAGMA table_info(cached_ppdt_images)").use { cursor ->
                val columns = mutableSetOf<String>()
                while (cursor.moveToNext()) columns.add(cursor.getString(1))
                assert(columns.contains("imageContextJson")) {
                    "Migration 21→22: imageContextJson column not found"
                }
                assert(columns.contains("genderTag")) {
                    "Migration 21→22: genderTag column not found"
                }
                assert(!columns.contains("context")) {
                    "Migration 21→22: old 'context' column should be gone"
                }
            }

            // Existing row preserved; imageContextJson defaults to '{}'; genderTag defaults to 'MIXED'
            query("SELECT imageContextJson, genderTag FROM cached_ppdt_images WHERE id = 'img_001'")
                .use { cursor ->
                    assert(cursor.moveToFirst()) { "Migration 21→22: existing row disappeared" }
                    assertEquals("{}", cursor.getString(0))
                    assertEquals("MIXED", cursor.getString(1))
                }

            close()
        }
        openHelper.close()
        context.deleteDatabase("migration-test-21-22")
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun migrate22To23_addsLastStalenessCheckAtColumnWithDefaultZero() = runTest {
        // WHY: 24h TTL on the Firestore version check requires persisting when the last check ran.
        // The column defaults to 0 (epoch) so existing rows are treated as "never checked" — they
        // will trigger a Firestore read on the next launch, which is correct and safe.
        val factory = FrameworkSQLiteOpenHelperFactory()
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name("migration-test-22-23")
            .callback(object : SupportSQLiteOpenHelper.Callback(22) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS ppdt_batch_metadata (
                            batchId TEXT PRIMARY KEY NOT NULL,
                            downloadedAt INTEGER NOT NULL,
                            imageCount INTEGER NOT NULL,
                            version TEXT NOT NULL
                        )
                        """.trimIndent()
                    )
                    db.execSQL(
                        """
                        INSERT INTO ppdt_batch_metadata (batchId, downloadedAt, imageCount, version)
                        VALUES ('batch_001', 1000, 64, '2.0.0')
                        """.trimIndent()
                    )
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()
        val openHelper = factory.create(config)
        openHelper.writableDatabase.apply {
            MIGRATION_22_23.migrate(this)
            version = 23

            // Column must exist in schema
            query("PRAGMA table_info(ppdt_batch_metadata)").use { cursor ->
                val columns = mutableSetOf<String>()
                while (cursor.moveToNext()) columns.add(cursor.getString(1))
                assert(columns.contains("lastStalenessCheckAt")) {
                    "Migration 22→23 failed: lastStalenessCheckAt column not found in ppdt_batch_metadata"
                }
            }

            // Existing row preserved; new column defaults to 0
            query("SELECT lastStalenessCheckAt FROM ppdt_batch_metadata WHERE batchId = 'batch_001'")
                .use { cursor ->
                    assert(cursor.moveToFirst()) { "Migration 22→23: existing row disappeared" }
                    assertEquals(
                        "lastStalenessCheckAt must default to 0 for pre-migration rows",
                        0L, cursor.getLong(0)
                    )
                }

            close()
        }
        openHelper.close()
        context.deleteDatabase("migration-test-22-23")
    }

    @Test
    fun migration23to24_rebuildsTATImagesAndAddsLastStalenessCheckAt() {
        val factory = SupportSQLiteOpenHelper.Factory { config ->
            FrameworkSQLiteOpenHelperFactory().create(config)
        }
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name("migration-test-23-24")
            .callback(object : SupportSQLiteOpenHelper.Callback(23) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    // Schema at version 23: old cached_tat_images with sequenceNumber/prompt columns
                    db.execSQL("""
                        CREATE TABLE cached_tat_images (
                            id TEXT NOT NULL PRIMARY KEY,
                            imageUrl TEXT NOT NULL,
                            localFilePath TEXT,
                            sequenceNumber INTEGER NOT NULL,
                            prompt TEXT NOT NULL,
                            viewingTimeSeconds INTEGER NOT NULL,
                            writingTimeMinutes INTEGER NOT NULL,
                            minCharacters INTEGER NOT NULL,
                            maxCharacters INTEGER NOT NULL,
                            category TEXT,
                            difficulty TEXT,
                            batchId TEXT NOT NULL,
                            cachedAt INTEGER NOT NULL,
                            lastUsed INTEGER,
                            usageCount INTEGER NOT NULL,
                            imageDownloaded INTEGER NOT NULL
                        )
                    """.trimIndent())
                    db.execSQL("""
                        CREATE TABLE tat_batch_metadata (
                            batchId TEXT NOT NULL PRIMARY KEY,
                            downloadedAt INTEGER NOT NULL,
                            imageCount INTEGER NOT NULL,
                            version TEXT NOT NULL
                        )
                    """.trimIndent())
                    db.execSQL("""
                        INSERT INTO cached_tat_images
                        (id, imageUrl, localFilePath, sequenceNumber, prompt, viewingTimeSeconds,
                         writingTimeMinutes, minCharacters, maxCharacters, category, difficulty,
                         batchId, cachedAt, lastUsed, usageCount, imageDownloaded)
                        VALUES ('old_id', 'https://example.com/img.jpg', NULL, 1,
                                'Write a story', 30, 4, 50, 800, NULL, 'medium',
                                'batch_001', 1000, NULL, 0, 0)
                    """.trimIndent())
                    db.execSQL("""
                        INSERT INTO tat_batch_metadata (batchId, downloadedAt, imageCount, version)
                        VALUES ('batch_001', 1000, 167, '1.0.0')
                    """.trimIndent())
                }
                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()
        val openHelper = factory.create(config)
        openHelper.writableDatabase.apply {
            MIGRATION_23_24.migrate(this)
            version = 24

            // Old rows discarded — table rebuilt with Phase 2 schema
            query("SELECT COUNT(*) FROM cached_tat_images").use { cursor ->
                cursor.moveToFirst()
                assertEquals("Old rows must be dropped after schema rebuild", 0, cursor.getInt(0))
            }

            // New schema must have cardPosition, genderTag, imageContextJson columns
            query("PRAGMA table_info(cached_tat_images)").use { cursor ->
                val columns = mutableSetOf<String>()
                while (cursor.moveToNext()) columns.add(cursor.getString(1))
                assert(columns.contains("cardPosition")) { "Migration 23→24: cardPosition column missing" }
                assert(columns.contains("genderTag")) { "Migration 23→24: genderTag column missing" }
                assert(columns.contains("imageContextJson")) { "Migration 23→24: imageContextJson column missing" }
                assert(!columns.contains("sequenceNumber")) { "Migration 23→24: sequenceNumber should be gone" }
                assert(!columns.contains("prompt")) { "Migration 23→24: prompt should be gone" }
            }

            // tat_batch_metadata must have lastStalenessCheckAt with default 0
            query("PRAGMA table_info(tat_batch_metadata)").use { cursor ->
                val columns = mutableSetOf<String>()
                while (cursor.moveToNext()) columns.add(cursor.getString(1))
                assert(columns.contains("lastStalenessCheckAt")) {
                    "Migration 23→24: lastStalenessCheckAt missing in tat_batch_metadata"
                }
            }
            query("SELECT lastStalenessCheckAt FROM tat_batch_metadata WHERE batchId = 'batch_001'")
                .use { cursor ->
                    assert(cursor.moveToFirst()) { "Migration 23→24: existing batch row disappeared" }
                    assertEquals("lastStalenessCheckAt must default to 0", 0L, cursor.getLong(0))
                }

            close()
        }
        openHelper.close()
        context.deleteDatabase("migration-test-23-24")
    }
}













