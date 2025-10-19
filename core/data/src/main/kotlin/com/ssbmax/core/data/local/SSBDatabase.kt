package com.ssbmax.core.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.ssbmax.core.data.local.dao.NotificationDao
import com.ssbmax.core.data.local.dao.TestResultDao
import com.ssbmax.core.data.local.entity.NotificationEntity
import com.ssbmax.core.data.local.entity.TestResultEntity

/**
 * SSBMax Room Database
 * Single source of truth for local data
 */
@Database(
    entities = [
        TestResultEntity::class,
        NotificationEntity::class
    ],
    version = 2, // Incremented for new NotificationEntity
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
    
    companion object {
        const val DATABASE_NAME = "ssbmax_database"
    }
}

