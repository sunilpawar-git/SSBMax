package com.ssbmax.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for caching GTO (Group Testing Officer) tasks locally
 * Follows progressive caching strategy
 * 
 * GTO Tasks include:
 * - Group Discussion (GD)
 * - Group Planning Exercise (GPE)
 * - Progressive Group Tasks (PGT)
 * - Half Group Tasks (HGT)
 * - Individual Obstacles
 * - Command Tasks
 * - Final Group Task (FGT)
 * - Lecturette
 */
@Entity(tableName = "cached_gto_tasks")
data class CachedGTOTaskEntity(
    @PrimaryKey val id: String,
    val taskType: String, // GD, GPE, PGT, HGT, OBSTACLE, COMMAND, FGT, LECTURETTE
    val title: String,
    val description: String,
    val instructions: String,
    val timeAllowedMinutes: Int,
    val difficultyLevel: String? = null, // easy, medium, hard
    val category: String? = null, // leadership, planning, problem_solving, communication
    val scenario: String? = null, // For scenario-based tasks
    val resources: String? = null, // JSON list of available resources
    val objectives: String? = null, // JSON list of objectives
    val evaluationCriteria: String? = null, // JSON list of criteria
    val batchId: String,
    val cachedAt: Long,
    val lastUsed: Long?,
    val usageCount: Int = 0
)

