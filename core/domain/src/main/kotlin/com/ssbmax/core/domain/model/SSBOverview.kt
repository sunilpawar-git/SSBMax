package com.ssbmax.core.domain.model

/**
 * Represents an informational card about SSB process
 * Used in Overview of SSB screen
 */
data class SSBInfoCard(
    val id: String,
    val title: String,
    val content: String,
    val icon: SSBInfoIcon,
    val isExpandable: Boolean = true,
    val videoUrl: String? = null,
    val order: Int
)

/**
 * Icons for different SSB information cards
 */
enum class SSBInfoIcon(val displayName: String) {
    INFORMATION("Information"),
    PROCESS("Process"),
    QUALITIES("Qualities"),
    PREPARATION("Preparation"),
    SUCCESS("Success Stories"),
    CALENDAR("Calendar"),
    MEDAL("Achievement"),
    BOOK("Learning");

    companion object {
        fun fromDisplayName(name: String): SSBInfoIcon? {
            return values().find { it.displayName.equals(name, ignoreCase = true) }
        }
    }
}

/**
 * Category for organizing SSB information
 */
enum class SSBInfoCategory(val displayName: String) {
    OVERVIEW("Overview"),
    SELECTION_PROCESS("Selection Process"),
    ASSESSMENT("Assessment Criteria"),
    PREPARATION("Preparation Tips"),
    SUCCESS_STORIES("Success Stories");

    companion object {
        fun fromDisplayName(name: String): SSBInfoCategory? {
            return values().find { it.displayName.equals(name, ignoreCase = true) }
        }
    }
}

