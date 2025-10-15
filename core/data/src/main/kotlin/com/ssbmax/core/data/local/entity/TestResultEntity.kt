package com.ssbmax.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for test results
 * Stores test completion data locally with sync status for offline-first architecture
 */
@Entity(tableName = "test_results")
data class TestResultEntity(
    @PrimaryKey val id: String,
    val testId: String,
    val userId: String,
    val score: Float,
    val maxScore: Float,
    val completedAt: Long,
    val timeSpentMinutes: Long,
    val syncStatus: SyncStatus = SyncStatus.PENDING
)

/**
 * Sync status for offline-first architecture
 */
enum class SyncStatus {
    PENDING,    // Not yet synced to cloud
    SYNCED,     // Successfully synced
    FAILED      // Sync failed, will retry
}

