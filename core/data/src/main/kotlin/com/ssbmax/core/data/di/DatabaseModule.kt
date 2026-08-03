package com.ssbmax.core.data.di

import androidx.room.Room
import com.google.gson.Gson
import com.ssbmax.core.data.local.DatabaseMigrations
import com.ssbmax.core.data.local.SSBDatabase
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * Koin module for the Room database and its DAOs, plus [Gson] (used by
 * several cache managers for JSON (de)serialization).
 *
 * Split out from repository bindings (see [repositoryModule]) to stay under
 * the 300-line file limit — was one `DataModule.kt` under Hilt.
 */
val databaseModule = module {
    single {
        Room.databaseBuilder(
            androidContext(),
            SSBDatabase::class.java,
            SSBDatabase.DATABASE_NAME
        )
            .addMigrations(
                DatabaseMigrations.MIGRATION_2_3,
                DatabaseMigrations.MIGRATION_3_4,
                DatabaseMigrations.MIGRATION_4_5,
                DatabaseMigrations.MIGRATION_5_6,
                DatabaseMigrations.MIGRATION_6_7,
                DatabaseMigrations.MIGRATION_7_8,
                DatabaseMigrations.MIGRATION_8_9,
                DatabaseMigrations.MIGRATION_9_10,
                DatabaseMigrations.MIGRATION_10_11,
                DatabaseMigrations.MIGRATION_11_12, // Add PIQ/SD usage columns
                DatabaseMigrations.MIGRATION_12_13, // Add GPE image cache tables
                DatabaseMigrations.MIGRATION_13_14, // Add GPE solution column
                DatabaseMigrations.MIGRATION_14_15, // Update TAT character limits
                DatabaseMigrations.MIGRATION_16_17, // Add OIR questionImageUrl column
                DatabaseMigrations.MIGRATION_17_18, // Composite (type, lastUsed) index for OIR hot query
                DatabaseMigrations.MIGRATION_18_19, // Restore type and batchId indices on cached_oir_questions
                DatabaseMigrations.MIGRATION_19_20, // Add oir_sync_metadata table for content-version reconciliation
                DatabaseMigrations.MIGRATION_20_21, // Add correctAnswerIds column for multi-answer OIR questions
                DatabaseMigrations.MIGRATION_21_22, // Phase 6: context→imageContextJson + genderTag on ppdt images
                DatabaseMigrations.MIGRATION_22_23, // Phase 6: 24h TTL staleness-check column on ppdt_batch_metadata
                DatabaseMigrations.MIGRATION_23_24, // Phase 2 TAT: pool-aware schema + tat_batch_metadata TTL
                DatabaseMigrations.MIGRATION_24_25, // Phase 3 TAT: tat_story_assessments table for per-story multimodal cache
                DatabaseMigrations.MIGRATION_25_26  // Add totalQuestionsAttempted, fixes accuracy >100% bug
            )
            .fallbackToDestructiveMigration() // If migration fails, recreate database
            .build()
    }

    single { get<SSBDatabase>().testUsageDao() }
    single { get<SSBDatabase>().gpeImageCacheDao() }
    single { get<SSBDatabase>().gtoTaskCacheDao() }
    single { get<SSBDatabase>().interviewQuestionCacheDao() }
    single { get<SSBDatabase>().userPerformanceDao() }
    single { get<SSBDatabase>().tatStoryAssessmentDao() }

    single { Gson() }
}
