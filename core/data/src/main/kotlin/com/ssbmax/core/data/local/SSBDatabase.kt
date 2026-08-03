package com.ssbmax.core.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.ssbmax.core.data.local.dao.TATStoryAssessmentDao
import com.ssbmax.core.data.local.dao.UserPerformanceDao
import com.ssbmax.core.data.local.entity.TATStoryAssessmentEntity
import com.ssbmax.core.data.local.entity.UserPerformanceEntity

/**
 * SSBMax Room Database
 * Single source of truth for local data
 */
@Database(
    entities = [
        UserPerformanceEntity::class,
        TATStoryAssessmentEntity::class
    ],
    // v29 (Phase 9d): dropped the test-usage mirror table. TestUsageDao was
    // written by SubscriptionManager.canTakeTest/recordTestUsage, both now
    // deleted (shared's CheckTestEligibilityUseCase/GitLiveTestUsageRecorder
    // are the sole implementation now, Firestore-only, no local mirror) — and
    // grep-confirmed to have had zero readers even before deletion, since
    // canTakeTest's only production callers had already migrated away in an
    // earlier phase and its own getTotalTestsUsedThisMonth() had zero callers
    // of its own. No explicit migration: fallbackToDestructiveMigration()
    // (below) recreates the DB, same precedent as v27/v28 — re-downloadable
    // cache data, no production users (KMP-convergence plan decision).
    version = 29,
    exportSchema = true
)
@TypeConverters(RoomTypeConverters::class)
abstract class SSBDatabase : RoomDatabase() {

    /**
     * User performance DAO for adaptive difficulty
     */
    abstract fun userPerformanceDao(): UserPerformanceDao

    /**
     * TAT per-story assessment cache DAO
     */
    abstract fun tatStoryAssessmentDao(): TATStoryAssessmentDao

    companion object {
        const val DATABASE_NAME = "ssbmax_database"
    }
}

