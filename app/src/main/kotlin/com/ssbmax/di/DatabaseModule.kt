package com.ssbmax.di

import androidx.room.Room
import com.ssbmax.data.local.DatabaseMigrations
import com.ssbmax.data.local.SSBDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * Koin module for the Room database (KMP-convergence Phase 9f moved this out
 * of the deleted `core:data` module — see [SSBDatabase]'s class doc) and the
 * application-scoped [CoroutineScope] (used by
 * [com.ssbmax.workers.WorkManagerSubmissionAnalysisTrigger] for long-lived
 * work that outlives an individual WorkManager job; SupervisorJob so one
 * child failure doesn't cancel others).
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
                DatabaseMigrations.MIGRATION_11_12,
                DatabaseMigrations.MIGRATION_12_13,
                DatabaseMigrations.MIGRATION_13_14,
                DatabaseMigrations.MIGRATION_14_15,
                DatabaseMigrations.MIGRATION_16_17,
                DatabaseMigrations.MIGRATION_17_18,
                DatabaseMigrations.MIGRATION_18_19,
                DatabaseMigrations.MIGRATION_19_20,
                DatabaseMigrations.MIGRATION_20_21,
                DatabaseMigrations.MIGRATION_21_22,
                DatabaseMigrations.MIGRATION_22_23,
                DatabaseMigrations.MIGRATION_23_24,
                DatabaseMigrations.MIGRATION_24_25,
                DatabaseMigrations.MIGRATION_25_26
            )
            .fallbackToDestructiveMigration() // If migration fails, recreate database
            .build()
    }

    single { get<SSBDatabase>().tatStoryAssessmentDao() }

    single<CoroutineScope> { CoroutineScope(SupervisorJob() + Dispatchers.Default) }
}
