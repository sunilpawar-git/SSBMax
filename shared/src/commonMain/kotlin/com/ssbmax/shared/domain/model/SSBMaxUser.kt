package com.ssbmax.shared.domain.model

import kotlinx.datetime.Clock

/**
 * Ported (near-verbatim) from core/domain/model/UserRole.kt as part of the
 * Phase 0 KMP spike. `System.currentTimeMillis()` (JVM-only) is replaced with
 * `Clock.System.now().toEpochMilliseconds()` (kotlinx-datetime) since this file
 * now needs to compile for iosMain too. Instructor profile / legacy `isPremium`
 * trimmed — out of scope for the login + OIR-result vertical slice.
 */
enum class UserRole {
    STUDENT,
    INSTRUCTOR,
    BOTH;

    val isStudent: Boolean
        get() = this == STUDENT || this == BOTH

    val isInstructor: Boolean
        get() = this == INSTRUCTOR || this == BOTH
}

data class SSBMaxUser(
    val id: String,
    val email: String,
    val displayName: String,
    val photoUrl: String? = null,
    val role: UserRole,
    val subscriptionTier: SubscriptionTier = SubscriptionTier.FREE,
    val studentProfile: StudentProfile? = null,
    val createdAt: Long = Clock.System.now().toEpochMilliseconds(),
    val lastLoginAt: Long = Clock.System.now().toEpochMilliseconds()
)

data class StudentProfile(
    val userId: String,
    val totalTestsAttempted: Int = 0,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0
)

enum class SubscriptionTier {
    FREE,
    PRO,
    PREMIUM;

    val displayName: String
        get() = when (this) {
            FREE -> "Free"
            PRO -> "Pro"
            PREMIUM -> "Premium"
        }
}
