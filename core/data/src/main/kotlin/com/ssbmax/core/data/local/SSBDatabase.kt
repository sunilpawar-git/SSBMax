package com.ssbmax.core.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.ssbmax.core.data.local.dao.NotificationDao
import com.ssbmax.core.data.local.dao.OIRQuestionCacheDao
import com.ssbmax.core.data.local.dao.SRTSituationCacheDao
import com.ssbmax.core.data.local.dao.TestResultDao
import com.ssbmax.core.data.local.dao.TestUsageDao
import com.ssbmax.core.data.local.dao.WATWordCacheDao
import com.ssbmax.core.data.local.entity.CachedOIRQuestionEntity
import com.ssbmax.core.data.local.entity.CachedSRTSituationEntity
import com.ssbmax.core.data.local.entity.CachedWATWordEntity
import com.ssbmax.core.data.local.entity.NotificationEntity
import com.ssbmax.core.data.local.entity.OIRBatchMetadataEntity
import com.ssbmax.core.data.local.entity.SRTBatchMetadataEntity
import com.ssbmax.core.data.local.entity.TestResultEntity
import com.ssbmax.core.data.local.entity.TestUsageEntity
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
        SRTBatchMetadataEntity::class
    ],
    version = 6, // Incremented for SRT situation caching
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
    
    companion object {
        const val DATABASE_NAME = "ssbmax_database"
    }
}

