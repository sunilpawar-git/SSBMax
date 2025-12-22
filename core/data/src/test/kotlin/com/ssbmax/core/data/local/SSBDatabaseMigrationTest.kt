package com.ssbmax.core.data.local

import android.content.ContentValues
import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.ssbmax.core.data.local.DatabaseMigrations.MIGRATION_11_12
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
}













