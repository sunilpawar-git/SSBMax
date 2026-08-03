package com.ssbmax.core.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.ssbmax.core.data.local.dao.TATStoryAssessmentDao
import com.ssbmax.core.data.local.dao.TestUsageDao
import com.ssbmax.core.data.local.dao.UserPerformanceDao
import com.ssbmax.core.data.local.entity.TATStoryAssessmentEntity
import com.ssbmax.core.data.local.entity.TestUsageEntity
import com.ssbmax.core.data.local.entity.UserPerformanceEntity

/**
 * SSBMax Room Database
 * Single source of truth for local data
 */
@Database(
    entities = [
        TestUsageEntity::class,
        UserPerformanceEntity::class,
        TATStoryAssessmentEntity::class
    ],
    // v28 (Phase 9c): dropped the GPE-image, GTO-task, and interview-question
    // cache tables. GTOTaskCacheManager/FirestoreGTORepository were this
    // phase's own deletions (shared's GitLiveGTOTaskCacheManager/
    // GitLiveGTORepository are the sole implementation now); GPEImageCacheManager
    // and InterviewQuestionCacheManager turned out to already have zero real
    // callers (the "live callers" note on GPEImageCacheManager in the v27
    // migration comment was incorrect — grep-confirmed neither FirestoreGTORepository
    // nor DifficultyProgressionManager ever actually constructed it), so both
    // were swept in the same version bump rather than left as newly-discovered
    // dead code for a future phase. No explicit migration:
    // fallbackToDestructiveMigration() (below) recreates the DB, acceptable
    // since these are re-downloadable/re-derivable caches, not unrecoverable
    // user data, and there are no production users (KMP-convergence plan
    // decision).
    version = 28,
    exportSchema = true
)
@TypeConverters(RoomTypeConverters::class)
abstract class SSBDatabase : RoomDatabase() {

    /**
     * Test usage DAO
     */
    abstract fun testUsageDao(): TestUsageDao

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

