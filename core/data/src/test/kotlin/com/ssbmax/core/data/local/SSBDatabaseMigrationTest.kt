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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
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
}













