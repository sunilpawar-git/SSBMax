package com.ssbmax.core.domain.model

/**
 * Legacy user model - kept for backward compatibility.
 *
 * @deprecated Use [SSBMaxUser] instead for full feature support
 *
 * **Migration Timeline:**
 * - Deprecated: Phase 3 (2024-Q3)
 * - Removal Target: Phase 6 (2025-Q2)
 *
 * **Migration Guide:**
 * ```kotlin
 * // OLD (deprecated)
 * val user = User(
 *     id = "123",
 *     email = "user@example.com",
 *     displayName = "John Doe",
 *     isPremium = true
 * )
 *
 * // NEW (recommended)
 * val user = SSBMaxUser(
 *     id = "123",
 *     email = "user@example.com",
 *     displayName = "John Doe",
 *     subscriptionType = SubscriptionType.PRO,
 *     userRole = UserRole.CANDIDATE
 * )
 * ```
 *
 * **Breaking Changes:**
 * - `isPremium` replaced with `subscriptionType` (FREE/PRO/PREMIUM)
 * - Added `userRole` (CANDIDATE/COACH/ADMIN)
 * - Added profile fields (photoUrl, phone, registrationDate)
 *
 * @see SSBMaxUser
 */
@Deprecated("Use SSBMaxUser instead", ReplaceWith("SSBMaxUser"))
data class User(
    val id: String,
    val email: String,
    val displayName: String,
    val isPremium: Boolean = false
)

/**
 * Authentication state
 */
sealed class AuthState {
    data object Unauthenticated : AuthState()
    data object Loading : AuthState()
    data class Authenticated(val user: SSBMaxUser) : AuthState()
    data class Error(val message: String) : AuthState()
    data object RequiresRoleSelection : AuthState()
}

/**
 * Authentication result
 */
sealed class AuthResult {
    data class Success(val user: SSBMaxUser) : AuthResult()
    data class NeedsRoleSelection(val userId: String, val email: String, val displayName: String) : AuthResult()
    data class Error(val message: String) : AuthResult()
}

