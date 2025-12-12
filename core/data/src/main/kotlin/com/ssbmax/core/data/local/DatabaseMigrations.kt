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
    
    /**
     * Migration from version 6 to 7
     * Adds TAT image caching tables
     */
    val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Create cached_tat_images table
            // NOTE: No DEFAULT values in SQL - Room handles defaults via Kotlin data class
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS cached_tat_images (
                    id TEXT PRIMARY KEY NOT NULL,
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
            
            // Create tat_batch_metadata table
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS tat_batch_metadata (
                    batchId TEXT PRIMARY KEY NOT NULL,
                    downloadedAt INTEGER NOT NULL,
                    imageCount INTEGER NOT NULL,
                    version TEXT NOT NULL
                )
            """.trimIndent())
            
            // Create indices for better query performance
            database.execSQL("""
                CREATE INDEX IF NOT EXISTS index_cached_tat_images_category 
                ON cached_tat_images(category)
            """.trimIndent())
            
            database.execSQL("""
                CREATE INDEX IF NOT EXISTS index_cached_tat_images_difficulty 
                ON cached_tat_images(difficulty)
            """.trimIndent())
            
            database.execSQL("""
                CREATE INDEX IF NOT EXISTS index_cached_tat_images_batchId 
                ON cached_tat_images(batchId)
            """.trimIndent())
            
            database.execSQL("""
                CREATE INDEX IF NOT EXISTS index_cached_tat_images_usageCount 
                ON cached_tat_images(usageCount)
            """.trimIndent())
            
            database.execSQL("""
                CREATE INDEX IF NOT EXISTS index_cached_tat_images_imageDownloaded 
                ON cached_tat_images(imageDownloaded)
            """.trimIndent())
        }
    }
    
    /**
     * Migration from version 7 to 8
     * Adds PPDT image caching tables
     */
    val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Create cached_ppdt_images table
            // NOTE: No DEFAULT values in SQL - Room handles defaults via Kotlin data class
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS cached_ppdt_images (
                    id TEXT PRIMARY KEY NOT NULL,
                    imageUrl TEXT NOT NULL,
                    localFilePath TEXT,
                    imageDescription TEXT NOT NULL,
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
            
            // Create ppdt_batch_metadata table
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS ppdt_batch_metadata (
                    batchId TEXT PRIMARY KEY NOT NULL,
                    downloadedAt INTEGER NOT NULL,
                    imageCount INTEGER NOT NULL,
                    version TEXT NOT NULL
                )
            """.trimIndent())
            
            // Create indices for better query performance
            database.execSQL("""
                CREATE INDEX IF NOT EXISTS index_cached_ppdt_images_category 
                ON cached_ppdt_images(category)
            """.trimIndent())
            
            database.execSQL("""
                CREATE INDEX IF NOT EXISTS index_cached_ppdt_images_difficulty 
                ON cached_ppdt_images(difficulty)
            """.trimIndent())
            
            database.execSQL("""
                CREATE INDEX IF NOT EXISTS index_cached_ppdt_images_batchId 
                ON cached_ppdt_images(batchId)
            """.trimIndent())
            
            database.execSQL("""
                CREATE INDEX IF NOT EXISTS index_cached_ppdt_images_usageCount 
                ON cached_ppdt_images(usageCount)
            """.trimIndent())
            
            database.execSQL("""
                CREATE INDEX IF NOT EXISTS index_cached_ppdt_images_imageDownloaded 
                ON cached_ppdt_images(imageDownloaded)
            """.trimIndent())
        }
    }
    
    /**
     * Migration from version 8 to 9
     * Adds GTO task caching tables
     */
    val MIGRATION_8_9 = object : Migration(8, 9) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Create cached_gto_tasks table
            // NOTE: No DEFAULT values in SQL - Room handles defaults via Kotlin data class
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS cached_gto_tasks (
                    id TEXT PRIMARY KEY NOT NULL,
                    taskType TEXT NOT NULL,
                    title TEXT NOT NULL,
                    description TEXT NOT NULL,
                    instructions TEXT NOT NULL,
                    timeAllowedMinutes INTEGER NOT NULL,
                    difficultyLevel TEXT,
                    category TEXT,
                    scenario TEXT,
                    resources TEXT,
                    objectives TEXT,
                    evaluationCriteria TEXT,
                    batchId TEXT NOT NULL,
                    cachedAt INTEGER NOT NULL,
                    lastUsed INTEGER,
                    usageCount INTEGER NOT NULL
                )
            """.trimIndent())
            
            // Create gto_batch_metadata table
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS gto_batch_metadata (
                    batchId TEXT PRIMARY KEY NOT NULL,
                    downloadedAt INTEGER NOT NULL,
                    taskCount INTEGER NOT NULL,
                    version TEXT NOT NULL
                )
            """.trimIndent())
            
            // Create indices for better query performance
            database.execSQL("""
                CREATE INDEX IF NOT EXISTS index_cached_gto_tasks_taskType 
                ON cached_gto_tasks(taskType)
            """.trimIndent())
            
            database.execSQL("""
                CREATE INDEX IF NOT EXISTS index_cached_gto_tasks_category 
                ON cached_gto_tasks(category)
            """.trimIndent())
            
            database.execSQL("""
                CREATE INDEX IF NOT EXISTS index_cached_gto_tasks_difficulty 
                ON cached_gto_tasks(difficultyLevel)
            """.trimIndent())
            
            database.execSQL("""
                CREATE INDEX IF NOT EXISTS index_cached_gto_tasks_batchId 
                ON cached_gto_tasks(batchId)
            """.trimIndent())
            
            database.execSQL("""
                CREATE INDEX IF NOT EXISTS index_cached_gto_tasks_usageCount 
                ON cached_gto_tasks(usageCount)
            """.trimIndent())
        }
    }
    
    /**
     * Migration from version 9 to 10 - FINAL MIGRATION!
     * Adds Interview question caching tables
     * This completes the progressive caching system for ALL 7 SSB tests!
     */
    val MIGRATION_9_10 = object : Migration(9, 10) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Create cached_interview_questions table
            // NOTE: No DEFAULT values in SQL - Room handles defaults via Kotlin data class
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS cached_interview_questions (
                    id TEXT PRIMARY KEY NOT NULL,
                    question TEXT NOT NULL,
                    category TEXT NOT NULL,
                    difficulty TEXT,
                    suggestedAnswer TEXT,
                    keyPoints TEXT,
                    commonMistakes TEXT,
                    followUpQuestions TEXT,
                    batchId TEXT NOT NULL,
                    cachedAt INTEGER NOT NULL,
                    lastUsed INTEGER,
                    usageCount INTEGER NOT NULL
                )
            """.trimIndent())
            
            // Create interview_batch_metadata table
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS interview_batch_metadata (
                    batchId TEXT PRIMARY KEY NOT NULL,
                    downloadedAt INTEGER NOT NULL,
                    questionCount INTEGER NOT NULL,
                    version TEXT NOT NULL
                )
            """.trimIndent())
            
            // Create indices for better query performance
            database.execSQL("""
                CREATE INDEX IF NOT EXISTS index_cached_interview_questions_category 
                ON cached_interview_questions(category)
            """.trimIndent())
            
            database.execSQL("""
                CREATE INDEX IF NOT EXISTS index_cached_interview_questions_difficulty 
                ON cached_interview_questions(difficulty)
            """.trimIndent())
            
            database.execSQL("""
                CREATE INDEX IF NOT EXISTS index_cached_interview_questions_batchId 
                ON cached_interview_questions(batchId)
            """.trimIndent())
            
            database.execSQL("""
                CREATE INDEX IF NOT EXISTS index_cached_interview_questions_usageCount 
                ON cached_interview_questions(usageCount)
            """.trimIndent())
        }
    }
    
    /**
     * Migration from version 10 to 11
     * Adds user performance tracking for adaptive difficulty progression
     */
    val MIGRATION_10_11 = object : Migration(10, 11) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Drop the table if it exists (fresh start for this migration)
            database.execSQL("DROP TABLE IF EXISTS user_performance")
            
            // Create user_performance table with correct column order matching Entity
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS user_performance (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    testType TEXT NOT NULL,
                    difficulty TEXT NOT NULL,
                    totalAttempts INTEGER NOT NULL DEFAULT 0,
                    correctAnswers INTEGER NOT NULL DEFAULT 0,
                    incorrectAnswers INTEGER NOT NULL DEFAULT 0,
                    averageScore REAL NOT NULL DEFAULT 0.0,
                    averageTimeSeconds REAL NOT NULL DEFAULT 0.0,
                    currentLevel TEXT NOT NULL,
                    readyForNextLevel INTEGER NOT NULL DEFAULT 0,
                    lastAttemptAt INTEGER NOT NULL,
                    firstAttemptAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL
                )
            """.trimIndent())
            
            // Create indices for better query performance
            database.execSQL("""
                CREATE INDEX IF NOT EXISTS index_user_performance_testType 
                ON user_performance(testType)
            """.trimIndent())
            
            database.execSQL("""
                CREATE INDEX IF NOT EXISTS index_user_performance_difficulty 
                ON user_performance(difficulty)
            """.trimIndent())
            
            database.execSQL("""
                CREATE UNIQUE INDEX IF NOT EXISTS index_user_performance_testType_difficulty 
                ON user_performance(testType, difficulty)
            """.trimIndent())
        }
    }

    /**
     * Migration from version 11 to 12
     * Adds PIQ and SD monthly usage tracking to test_usage
     */
    val MIGRATION_11_12 = object : Migration(11, 12) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE test_usage ADD COLUMN piqTestsUsed INTEGER NOT NULL DEFAULT 0")
            database.execSQL("ALTER TABLE test_usage ADD COLUMN sdTestsUsed INTEGER NOT NULL DEFAULT 0")
        }
    }

    /**
     * Migration from version 12 to 13
     * Adds GPE (Group Planning Exercise) image caching tables
     */
    val MIGRATION_12_13 = object : Migration(12, 13) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Create cached_gpe_images table
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS cached_gpe_images (
                    id TEXT PRIMARY KEY NOT NULL,
                    imageUrl TEXT NOT NULL,
                    localFilePath TEXT,
                    scenario TEXT NOT NULL,
                    imageDescription TEXT NOT NULL,
                    resources TEXT,
                    viewingTimeSeconds INTEGER NOT NULL DEFAULT 60,
                    planningTimeSeconds INTEGER NOT NULL DEFAULT 1740,
                    minCharacters INTEGER NOT NULL DEFAULT 500,
                    maxCharacters INTEGER NOT NULL DEFAULT 2000,
                    category TEXT,
                    difficulty TEXT,
                    batchId TEXT NOT NULL,
                    cachedAt INTEGER NOT NULL,
                    lastUsed INTEGER,
                    usageCount INTEGER NOT NULL DEFAULT 0,
                    imageDownloaded INTEGER NOT NULL DEFAULT 0
                )
            """.trimIndent())

            // Create gpe_batch_metadata table
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS gpe_batch_metadata (
                    batchId TEXT PRIMARY KEY NOT NULL,
                    downloadedAt INTEGER NOT NULL,
                    imageCount INTEGER NOT NULL,
                    version TEXT NOT NULL
                )
            """.trimIndent())

            // Create indices for better query performance
            database.execSQL("""
                CREATE INDEX IF NOT EXISTS index_cached_gpe_images_batchId
                ON cached_gpe_images(batchId)
            """.trimIndent())

            database.execSQL("""
                CREATE INDEX IF NOT EXISTS index_cached_gpe_images_category
                ON cached_gpe_images(category)
            """.trimIndent())

            database.execSQL("""
                CREATE INDEX IF NOT EXISTS index_cached_gpe_images_difficulty
                ON cached_gpe_images(difficulty)
            """.trimIndent())

            database.execSQL("""
                CREATE INDEX IF NOT EXISTS index_cached_gpe_images_lastUsed
                ON cached_gpe_images(lastUsed)
            """.trimIndent())

            database.execSQL("""
                CREATE INDEX IF NOT EXISTS index_cached_gpe_images_usageCount
                ON cached_gpe_images(usageCount)
            """.trimIndent())
        }
    }
}

