package com.ssbmax.core.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.ssbmax.core.data.local.dao.GPEImageCacheDao
import com.ssbmax.core.data.local.dao.GTOTaskCacheDao
import com.ssbmax.core.data.local.dao.InterviewQuestionCacheDao
import com.ssbmax.core.data.local.dao.TATStoryAssessmentDao
import com.ssbmax.core.data.local.dao.TestUsageDao
import com.ssbmax.core.data.local.dao.UserPerformanceDao
import com.ssbmax.core.data.local.entity.CachedGPEImageEntity
import com.ssbmax.core.data.local.entity.CachedGTOTaskEntity
import com.ssbmax.core.data.local.entity.CachedInterviewQuestionEntity
import com.ssbmax.core.data.local.entity.GPEBatchMetadataEntity
import com.ssbmax.core.data.local.entity.GTOBatchMetadataEntity
import com.ssbmax.core.data.local.entity.InterviewBatchMetadataEntity
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
        CachedGPEImageEntity::class,
        GPEBatchMetadataEntity::class,
        CachedGTOTaskEntity::class,
        GTOBatchMetadataEntity::class,
        CachedInterviewQuestionEntity::class,
        InterviewBatchMetadataEntity::class,
        UserPerformanceEntity::class,
        TATStoryAssessmentEntity::class
    ],
    // v27 (Phase 9a/9b): dropped OIR/WAT/SRT/PPDT/TAT question-cache tables,
    // test_results, and notifications — their repositories
    // (TestContentRepositoryImpl/FirestoreQuestionCacheRepository/
    // TestRepositoryImpl/NotificationRepositoryImpl) and cache managers are
    // deleted; shared's SQLDelight-backed GitLive* equivalents are now the
    // sole implementation on both platforms. UserPerformanceEntity stays —
    // DifficultyProgressionManager (core:data) still reads/writes it. No
    // explicit migration: fallbackToDestructiveMigration() (below) recreates
    // the DB, acceptable since these are re-downloadable/re-derivable caches,
    // not unrecoverable user data, and there are no production users (KMP-
    // convergence plan decision).
    version = 27,
    exportSchema = true
)
@TypeConverters(RoomTypeConverters::class)
abstract class SSBDatabase : RoomDatabase() {

    /**
     * Test usage DAO
     */
    abstract fun testUsageDao(): TestUsageDao

    /**
     * GPE image cache DAO
     */
    abstract fun gpeImageCacheDao(): GPEImageCacheDao

    /**
     * GTO task cache DAO
     */
    abstract fun gtoTaskCacheDao(): GTOTaskCacheDao
    
    /**
     * Interview question cache DAO
     */
    abstract fun interviewQuestionCacheDao(): InterviewQuestionCacheDao
    
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

