package com.ssbmax.core.domain.model

/**
 * Study material categories aligned with SSB test types
 */
enum class StudyCategory {
    OIR_PREP,
    PPDT_TECH,
    PSYCHOLOGY,
    GTO_STRATEGY,
    INTERVIEW_PREP,
    GENERAL_SSB,
    CURRENT_AFFAIRS,
    PERSONALITY_DEV;
    
    val displayName: String
        get() = when (this) {
            OIR_PREP -> "OIR Preparation"
            PPDT_TECH -> "PPDT Techniques"
            PSYCHOLOGY -> "Psychology Tests"
            GTO_STRATEGY -> "GTO Strategy"
            INTERVIEW_PREP -> "Interview Preparation"
            GENERAL_SSB -> "General SSB Tips"
            CURRENT_AFFAIRS -> "Current Affairs"
            PERSONALITY_DEV -> "Personality Development"
        }
    
    val icon: String
        get() = when (this) {
            OIR_PREP -> "psychology"
            PPDT_TECH -> "image"
            PSYCHOLOGY -> "menu_book"
            GTO_STRATEGY -> "groups"
            INTERVIEW_PREP -> "record_voice_over"
            GENERAL_SSB -> "school"
            CURRENT_AFFAIRS -> "newspaper"
            PERSONALITY_DEV -> "emoji_people"
        }
}

/**
 * Study material model
 */
data class StudyMaterial(
    val id: String,
    val category: StudyCategory,
    val title: String,
    val description: String,
    val content: String, // Markdown or HTML content
    val author: String? = null,
    val estimatedReadTime: Int, // minutes
    val tags: List<String> = emptyList(),
    val isPremium: Boolean = false,
    val difficulty: DifficultyLevel = DifficultyLevel.BEGINNER,
    val viewCount: Int = 0,
    val likes: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val relatedMaterials: List<String> = emptyList(), // IDs of related materials
    val attachments: List<MaterialAttachment> = emptyList()
)

/**
 * Material attachments (PDFs, videos, etc.)
 */
data class MaterialAttachment(
    val id: String,
    val type: AttachmentType,
    val url: String,
    val title: String,
    val size: Long = 0, // bytes
    val duration: Int? = null // for videos, in seconds
)

enum class AttachmentType {
    PDF,
    VIDEO,
    AUDIO,
    IMAGE,
    DOCUMENT
}

/**
 * Difficulty levels for study materials
 */
enum class DifficultyLevel {
    BEGINNER,
    INTERMEDIATE,
    ADVANCED;
    
    val displayName: String
        get() = name.lowercase().replaceFirstChar { it.uppercase() }
}

/**
 * User's study progress for a material
 */
data class StudyProgress(
    val materialId: String,
    val userId: String,
    val progress: Float = 0f, // 0.0 to 100.0
    val lastReadAt: Long? = null,
    val timeSpent: Long = 0, // milliseconds
    val isBookmarked: Boolean = false,
    val isCompleted: Boolean = false,
    val notes: String? = null,
    val highlights: List<String> = emptyList()
)

/**
 * Study session tracking
 */
data class StudySession(
    val id: String,
    val userId: String,
    val materialId: String,
    val startedAt: Long,
    val endedAt: Long? = null,
    val duration: Long = 0, // milliseconds
    val progressIncrement: Float = 0f
) {
    val isActive: Boolean
        get() = endedAt == null
}

