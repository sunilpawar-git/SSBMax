package com.ssbmax.core.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.ssbmax.core.data.local.dao.GTOTaskCacheDao
import com.ssbmax.core.data.local.dao.InterviewQuestionCacheDao
import com.ssbmax.core.data.local.dao.NotificationDao
import com.ssbmax.core.data.local.dao.OIRQuestionCacheDao
import com.ssbmax.core.data.local.dao.PPDTImageCacheDao
import com.ssbmax.core.data.local.dao.SRTSituationCacheDao
import com.ssbmax.core.data.local.dao.TATImageCacheDao
import com.ssbmax.core.data.local.dao.TestResultDao
import com.ssbmax.core.data.local.dao.TestUsageDao
import com.ssbmax.core.data.local.dao.UserPerformanceDao
import com.ssbmax.core.data.local.dao.WATWordCacheDao
import com.ssbmax.core.data.local.entity.CachedGTOTaskEntity
import com.ssbmax.core.data.local.entity.CachedInterviewQuestionEntity
import com.ssbmax.core.data.local.entity.CachedOIRQuestionEntity
import com.ssbmax.core.data.local.entity.CachedPPDTImageEntity
import com.ssbmax.core.data.local.entity.CachedSRTSituationEntity
import com.ssbmax.core.data.local.entity.CachedTATImageEntity
import com.ssbmax.core.data.local.entity.CachedWATWordEntity
import com.ssbmax.core.data.local.entity.GTOBatchMetadataEntity
import com.ssbmax.core.data.local.entity.InterviewBatchMetadataEntity
import com.ssbmax.core.data.local.entity.NotificationEntity
import com.ssbmax.core.data.local.entity.OIRBatchMetadataEntity
import com.ssbmax.core.data.local.entity.PPDTBatchMetadataEntity
import com.ssbmax.core.data.local.entity.SRTBatchMetadataEntity
import com.ssbmax.core.data.local.entity.TATBatchMetadataEntity
import com.ssbmax.core.data.local.entity.TestResultEntity
import com.ssbmax.core.data.local.entity.TestUsageEntity
import com.ssbmax.core.data.local.entity.UserPerformanceEntity
import com.ssbmax.core.data.local.entity.WATBatchMetadataEntity

/**
 * SSBMax Room Database
 * Single source of truth for local data
 */
@Database(
    entities = [
        TestResultEntity::class,
        NotificationEntity::class,
        CachedOIRQuestionEntity::class,
        OIRBatchMetadataEntity::class,
        TestUsageEntity::class,
        CachedWATWordEntity::class,
        WATBatchMetadataEntity::class,
        CachedSRTSituationEntity::class,
        SRTBatchMetadataEntity::class,
        CachedTATImageEntity::class,
        TATBatchMetadataEntity::class,
        CachedPPDTImageEntity::class,
        PPDTBatchMetadataEntity::class,
        CachedGTOTaskEntity::class,
        GTOBatchMetadataEntity::class,
        CachedInterviewQuestionEntity::class,
        InterviewBatchMetadataEntity::class,
        UserPerformanceEntity::class
    ],
    version = 11, // Added adaptive difficulty progression
    exportSchema = true
)
abstract class SSBDatabase : RoomDatabase() {
    
    /**
     * Test results DAO
     */
    abstract fun testResultDao(): TestResultDao
    
    /**
     * Notifications DAO
     */
    abstract fun notificationDao(): NotificationDao
    
    /**
     * OIR question cache DAO
     */
    abstract fun oirQuestionCacheDao(): OIRQuestionCacheDao
    
    /**
     * Test usage DAO
     */
    abstract fun testUsageDao(): TestUsageDao
    
    /**
     * WAT word cache DAO
     */
    abstract fun watWordCacheDao(): WATWordCacheDao
    
    /**
     * SRT situation cache DAO
     */
    abstract fun srtSituationCacheDao(): SRTSituationCacheDao
    
    /**
     * TAT image cache DAO
     */
    abstract fun tatImageCacheDao(): TATImageCacheDao
    
    /**
     * PPDT image cache DAO
     */
    abstract fun ppdtImageCacheDao(): PPDTImageCacheDao
    
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
    
    companion object {
        const val DATABASE_NAME = "ssbmax_database"
    }
}

