package com.ssbmax.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Tracks a user's progress on each of the 20 OIR practice sets.
 * Primary key is "{userId}_{setNumber}" for efficient per-user queries.
 */
@Entity(tableName = "oir_set_progress")
data class OIRSetProgressEntity(
    @PrimaryKey val id: String, // "{userId}_{setNumber}"
    val userId: String,
    val setNumber: Int,
    val isCompleted: Boolean,
    val score: Int?,
    val totalQuestions: Int,
    val completedAt: Long?
)
