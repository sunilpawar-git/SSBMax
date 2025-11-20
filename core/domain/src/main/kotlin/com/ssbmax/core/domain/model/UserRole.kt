package com.ssbmax.core.domain.model

/**
 * User roles in the SSBMax platform
 */
enum class UserRole {
    STUDENT,
    INSTRUCTOR,
    BOTH; // User can switch between roles
    
    val isStudent: Boolean
        get() = this == STUDENT || this == BOTH
    
    val isInstructor: Boolean
        get() = this == INSTRUCTOR || this == BOTH
}

/**
 * Extended user model with role-based information
 */
data class SSBMaxUser(
    val id: String,
    val email: String,
    val displayName: String,
    val photoUrl: String? = null,
    val role: UserRole,
    val subscriptionTier: SubscriptionTier = SubscriptionTier.FREE,
    val subscription: UserSubscription? = null,
    val studentProfile: StudentProfile? = null,
    val instructorProfile: InstructorProfile? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val lastLoginAt: Long = System.currentTimeMillis()
) {
    /**
     * Legacy property for backward compatibility.
     *
     * @deprecated Use [subscriptionTier] instead for granular tier checking
     *
     * **Migration Timeline:**
     * - Deprecated: Phase 3 (2024-Q3)
     * - Removal Target: Phase 6 (2025-Q2)
     *
     * **Migration Guide:**
     * ```kotlin
     * // OLD (deprecated - boolean check)
     * if (user.isPremium) { /* ... */ }
     *
     * // NEW (recommended - tier-specific checks)
     * when (user.subscriptionTier) {
     *     SubscriptionTier.FREE -> { /* Free tier */ }
     *     SubscriptionTier.PRO -> { /* Pro features */ }
     *     SubscriptionTier.PREMIUM -> { /* Premium features */ }
     * }
     *
     * // Or for simple paid check:
     * if (user.subscriptionTier != SubscriptionTier.FREE) { /* Paid tier */ }
     * ```
     *
     * **Breaking Changes:**
     * - Binary check (free/premium) replaced with three-tier system (FREE/PRO/PREMIUM)
     * - Use `subscriptionTier` for more precise feature gating
     *
     * @see subscriptionTier
     */
    @Deprecated("Use subscriptionTier instead", ReplaceWith("subscriptionTier != SubscriptionTier.FREE"))
    val isPremium: Boolean
        get() = subscriptionTier != SubscriptionTier.FREE
}

/**
 * Student-specific profile information
 */
data class StudentProfile(
    val userId: String,
    val currentBatchIds: List<String> = emptyList(),
    val phase1Progress: PhaseProgress? = null,
    val phase2Progress: PhaseProgress? = null,
    val totalTestsAttempted: Int = 0,
    val totalStudyHours: Float = 0f,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val achievements: List<String> = emptyList()
)

/**
 * Instructor-specific profile information
 */
data class InstructorProfile(
    val userId: String,
    val specialization: List<String> = emptyList(), // e.g., ["Psychology", "GTO"]
    val batchesCreated: List<String> = emptyList(),
    val totalStudents: Int = 0,
    val totalTestsGraded: Int = 0,
    val averageGradingTime: Long = 0, // in milliseconds
    val rating: Float = 0f,
    val bio: String? = null,
    val certifications: List<String> = emptyList()
)

