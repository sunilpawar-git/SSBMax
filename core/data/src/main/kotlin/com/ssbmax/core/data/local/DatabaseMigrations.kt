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
            // NOTE: No DEFAULT values in SQL - Room handles defaults via Kotlin data class
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS test_usage (
                    id TEXT PRIMARY KEY NOT NULL,
                    userId TEXT NOT NULL,
                    month TEXT NOT NULL,
                    oirTestsUsed INTEGER NOT NULL,
                    tatTestsUsed INTEGER NOT NULL,
                    watTestsUsed INTEGER NOT NULL,
                    srtTestsUsed INTEGER NOT NULL,
                    ppdtTestsUsed INTEGER NOT NULL,
                    gtoTestsUsed INTEGER NOT NULL,
                    interviewTestsUsed INTEGER NOT NULL,
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
    
    /**
     * Migration from version 4 to 5
     * Adds WAT word caching tables
     */
    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Create cached_wat_words table
            // NOTE: No DEFAULT values in SQL - Room handles defaults via Kotlin data class
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS cached_wat_words (
                    id TEXT PRIMARY KEY NOT NULL,
                    word TEXT NOT NULL,
                    sequenceNumber INTEGER NOT NULL,
                    timeAllowedSeconds INTEGER NOT NULL,
                    category TEXT,
                    difficulty TEXT,
                    batchId TEXT NOT NULL,
                    cachedAt INTEGER NOT NULL,
                    lastUsed INTEGER,
                    usageCount INTEGER NOT NULL
                )
            """.trimIndent())
            
            // Create wat_batch_metadata table
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS wat_batch_metadata (
                    batchId TEXT PRIMARY KEY NOT NULL,
                    downloadedAt INTEGER NOT NULL,
                    wordCount INTEGER NOT NULL,
                    version TEXT NOT NULL
                )
            """.trimIndent())
            
            // Create indices for better query performance
            database.execSQL("""
                CREATE INDEX IF NOT EXISTS index_cached_wat_words_category 
                ON cached_wat_words(category)
            """.trimIndent())
            
            database.execSQL("""
                CREATE INDEX IF NOT EXISTS index_cached_wat_words_difficulty 
                ON cached_wat_words(difficulty)
            """.trimIndent())
            
            database.execSQL("""
                CREATE INDEX IF NOT EXISTS index_cached_wat_words_batchId 
                ON cached_wat_words(batchId)
            """.trimIndent())
            
            database.execSQL("""
                CREATE INDEX IF NOT EXISTS index_cached_wat_words_usageCount 
                ON cached_wat_words(usageCount)
            """.trimIndent())
        }
    }
    
    /**
     * Migration from version 5 to 6
     * Adds SRT situation caching tables
     */
    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Create cached_srt_situations table
            // NOTE: No DEFAULT values in SQL - Room handles defaults via Kotlin data class
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS cached_srt_situations (
                    id TEXT PRIMARY KEY NOT NULL,
                    situation TEXT NOT NULL,
                    sequenceNumber INTEGER NOT NULL,
                    category TEXT NOT NULL,
                    timeAllowedSeconds INTEGER NOT NULL,
                    difficulty TEXT,
                    batchId TEXT NOT NULL,
                    cachedAt INTEGER NOT NULL,
                    lastUsed INTEGER,
                    usageCount INTEGER NOT NULL
                )
            """.trimIndent())
            
            // Create srt_batch_metadata table
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS srt_batch_metadata (
                    batchId TEXT PRIMARY KEY NOT NULL,
                    downloadedAt INTEGER NOT NULL,
                    situationCount INTEGER NOT NULL,
                    version TEXT NOT NULL
                )
            """.trimIndent())
            
            // Create indices for better query performance
            database.execSQL("""
                CREATE INDEX IF NOT EXISTS index_cached_srt_situations_category 
                ON cached_srt_situations(category)
            """.trimIndent())
            
            database.execSQL("""
                CREATE INDEX IF NOT EXISTS index_cached_srt_situations_difficulty 
                ON cached_srt_situations(difficulty)
            """.trimIndent())
            
            database.execSQL("""
                CREATE INDEX IF NOT EXISTS index_cached_srt_situations_batchId 
                ON cached_srt_situations(batchId)
            """.trimIndent())
            
            database.execSQL("""
                CREATE INDEX IF NOT EXISTS index_cached_srt_situations_usageCount 
                ON cached_srt_situations(usageCount)
            """.trimIndent())
        }
    }
}

