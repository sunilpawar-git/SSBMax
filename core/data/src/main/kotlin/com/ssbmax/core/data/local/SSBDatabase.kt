package com.ssbmax.core.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.ssbmax.core.data.local.dao.NotificationDao
import com.ssbmax.core.data.local.dao.OIRQuestionCacheDao
import com.ssbmax.core.data.local.dao.TestResultDao
import com.ssbmax.core.data.local.dao.TestUsageDao
import com.ssbmax.core.data.local.entity.CachedOIRQuestionEntity
import com.ssbmax.core.data.local.entity.NotificationEntity
import com.ssbmax.core.data.local.entity.OIRBatchMetadataEntity
import com.ssbmax.core.data.local.entity.TestResultEntity
import com.ssbmax.core.data.local.entity.TestUsageEntity

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
        TestUsageEntity::class
    ],
    version = 4, // Incremented for test usage tracking
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
    
    companion object {
        const val DATABASE_NAME = "ssbmax_database"
    }
}

