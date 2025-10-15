package com.ssbmax.core.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.ssbmax.core.data.local.dao.TestResultDao
import com.ssbmax.core.data.local.entity.TestResultEntity

/**
 * SSBMax Room Database
 * Single source of truth for local data
 */
@Database(
    entities = [
        TestResultEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class SSBDatabase : RoomDatabase() {
    
    /**
     * Test results DAO
     */
    abstract fun testResultDao(): TestResultDao
    
    companion object {
        const val DATABASE_NAME = "ssbmax_database"
    }
}

