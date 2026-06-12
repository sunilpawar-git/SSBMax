package com.ssbmax.core.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for caching OIR questions locally
 *
 * This enables offline testing and reduces Firestore reads
 */
@Entity(
    tableName = "cached_oir_questions",
    indices = [
        Index(value = ["type", "lastUsed"], name = "index_oir_type_lastUsed"),
        Index(value = ["type"], name = "index_cached_oir_questions_type"),
        Index(value = ["batchId"], name = "index_cached_oir_questions_batchId")
    ]
)
data class CachedOIRQuestionEntity(
    @PrimaryKey 
    val id: String,
    
    val questionNumber: Int,
    
    val type: String, // VERBAL_REASONING, NON_VERBAL_REASONING, etc.
    
    val subtype: String?, // SYNONYMS, ANALOGIES, CODING, etc.
    
    val questionText: String,
    
    val optionsJson: String, // JSON serialized list of options
    
    val correctAnswerId: String,
    
    val explanation: String,

    val questionImageUrl: String? = null, // Figure for non-verbal questions (nullable for verbal)

    val difficulty: String, // EASY, MEDIUM, HARD
    
    val tags: String, // Comma-separated tags for analytics
    
    val batchId: String, // batch_001, batch_002, etc.
    
    val cachedAt: Long, // Timestamp when cached
    
    val lastUsed: Long?, // Last time this question was used in a test
    
    val usageCount: Int = 0, // How many times used in tests

    @ColumnInfo(name = "correctAnswerIds")
    val correctAnswerIds: String? = null // JSON e.g. "[\"opt_b\",\"opt_c\"]"; null → single-answer
)

