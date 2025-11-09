package com.ssbmax.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for tracking test usage per month
 */
@Entity(tableName = "test_usage")
data class TestUsageEntity(
    @PrimaryKey val id: String, // Format: "userId_2025-10"
    val userId: String,
    val month: String, // Format: "2025-10"
    val oirTestsUsed: Int = 0,
    val tatTestsUsed: Int = 0,
    val watTestsUsed: Int = 0,
    val srtTestsUsed: Int = 0,
    val ppdtTestsUsed: Int = 0,
    val piqTestsUsed: Int = 0,
    val gtoTestsUsed: Int = 0,
    val interviewTestsUsed: Int = 0,
    val sdTestsUsed: Int = 0,
    val lastUpdated: Long = System.currentTimeMillis()
)

