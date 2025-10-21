package com.ssbmax.core.domain.model

/**
 * Represents a user's profile information in the SSBMax app.
 * Used for personalization and display across the application.
 */
data class UserProfile(
    val userId: String,
    val fullName: String,
    val age: Int,
    val gender: Gender,
    val entryType: EntryType,
    val profilePictureUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    init {
        require(fullName.isNotBlank()) { "Full name cannot be blank" }
        require(age in 18..35) { "Age must be between 18 and 35" }
    }

    /**
     * Gets the user's initials for avatar display (e.g., "John Doe" -> "JD")
     */
    fun getInitials(): String {
        val names = fullName.trim().split(" ")
        return when {
            names.size >= 2 -> "${names.first().first()}${names.last().first()}"
            names.size == 1 -> names.first().take(2)
            else -> "U"
        }.uppercase()
    }
}

/**
 * Gender options for user profile
 */
enum class Gender(val displayName: String) {
    MALE("Male"),
    FEMALE("Female"),
    OTHER("Other");

    companion object {
        fun fromDisplayName(name: String): Gender? {
            return values().find { it.displayName.equals(name, ignoreCase = true) }
        }
    }
}

/**
 * Entry type for SSB examination
 */
enum class EntryType(val displayName: String) {
    ENTRY_10_PLUS_2("10+2 Entry"),
    GRADUATE("Graduate Entry"),
    SERVICE("Service Entry");

    companion object {
        fun fromDisplayName(name: String): EntryType? {
            return values().find { it.displayName.equals(name, ignoreCase = true) }
        }
    }
}

