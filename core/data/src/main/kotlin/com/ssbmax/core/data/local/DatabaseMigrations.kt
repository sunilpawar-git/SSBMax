package com.ssbmax.core.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Room database migrations for SSBMax
 */
object DatabaseMigrations {
    
    /**
     * Migration from version 2 to 3
     * Adds OIR question caching tables
     */
    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Create cached_oir_questions table
            database.execSQL("""
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
                    usageCount INTEGER NOT NULL DEFAULT 0
                )
            """.trimIndent())
            
            // Create oir_batch_metadata table
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS oir_batch_metadata (
                    batchId TEXT PRIMARY KEY NOT NULL,
                    downloadedAt INTEGER NOT NULL,
                    questionCount INTEGER NOT NULL,
                    version TEXT NOT NULL
                )
            """.trimIndent())
            
            // Create indices for better query performance
            database.execSQL("""
                CREATE INDEX IF NOT EXISTS index_cached_oir_questions_type 
                ON cached_oir_questions(type)
            """.trimIndent())
            
            database.execSQL("""
                CREATE INDEX IF NOT EXISTS index_cached_oir_questions_batchId 
                ON cached_oir_questions(batchId)
            """.trimIndent())
            
            database.execSQL("""
                CREATE INDEX IF NOT EXISTS index_cached_oir_questions_lastUsed 
                ON cached_oir_questions(lastUsed)
            """.trimIndent())
        }
    }
    
    /**
     * Migration from version 3 to 4
     * Adds test usage tracking table
     */
    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Create test_usage table
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS test_usage (
                    id TEXT PRIMARY KEY NOT NULL,
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
            """.trimIndent())
            
            // Create index for better query performance
            database.execSQL("""
                CREATE INDEX IF NOT EXISTS index_test_usage_userId_month 
                ON test_usage(userId, month)
            """.trimIndent())
        }
    }
}

