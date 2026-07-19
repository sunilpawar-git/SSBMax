package com.ssbmax.shared.data.repository

import com.ssbmax.shared.domain.model.GoogleSignInData
import com.ssbmax.shared.domain.model.SSBMaxUser
import com.ssbmax.shared.domain.model.StudentProfile
import com.ssbmax.shared.domain.model.SubscriptionTier
import com.ssbmax.shared.domain.model.UserRole
import com.ssbmax.shared.domain.repository.AuthRepository
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.FirebaseUser
import dev.gitlive.firebase.auth.GoogleAuthProvider
import dev.gitlive.firebase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.Clock

/**
 * GitLive-Firebase-backed implementation, ported from the Android
 * AuthRepositoryImpl + FirestoreUserRepository pair (Phase 2).
 *
 * handleGoogleSignInResult now does the real load-or-create-profile merge via
 * GitLiveUserRepository (users/{userId}) — closing the gap the Phase 0 report
 * flagged ("that's real business logic worth porting in Phase 2"). currentUser
 * is still a plain in-memory StateFlow rather than the Android original's
 * callbackFlow-over-Firebase-authState + Firestore join; that reactive-auth-state
 * wiring (observing role/profile changes pushed from another device) remains
 * out of scope for this slice and is not silently dropped — same "trimmed but
 * real" pattern Phase 0/2 used elsewhere.
 *
 * A real Google ID token exchange (Android Credential Manager / iOS
 * GoogleSignIn SDK) is NOT wired here — GoogleSignInData carries an
 * (idToken, accessToken) pair by convention for this spike; obtaining that
 * pair from a native picker is platform UI work out of Phase 0 scope.
 */
class GitLiveAuthRepository(
    private val userRepository: GitLiveUserRepository = GitLiveUserRepository()
) : AuthRepository {

    private val _currentUser = MutableStateFlow<SSBMaxUser?>(null)
    override val currentUser: StateFlow<SSBMaxUser?> = _currentUser.asStateFlow()

    // Email/password auth is not implemented in the Android app either (always
    // Result.failure there) — Google Sign-In is the only real flow. Matched here
    // rather than left unimplemented, since GitLiveAuthRepository must satisfy the
    // full real AuthRepository interface after the Phase 1 domain move.
    override suspend fun signIn(email: String, password: String): Result<SSBMaxUser> {
        return Result.failure(UnsupportedOperationException("Email/password sign-in is not supported"))
    }

    override suspend fun signUp(email: String, password: String, displayName: String): Result<SSBMaxUser> {
        return Result.failure(UnsupportedOperationException("Email/password sign-up is not supported"))
    }

    // Real Google ID token exchange (Android Credential Manager / iOS GoogleSignIn SDK)
    // is out of Phase 0/1 scope (see class doc comment) — this placeholder LaunchData
    // is not wired to a real platform picker yet.
    override fun getGoogleSignInIntent(): GoogleSignInData.LaunchData {
        return GoogleSignInData.LaunchData(Unit)
    }

    override suspend fun updateUserRole(role: UserRole): Result<Unit> {
        val current = _currentUser.value
            ?: return Result.failure(IllegalStateException("No authenticated user"))
        val userId = current.id
        val result = userRepository.updateUserRole(userId, role)
        if (result.isSuccess) {
            _currentUser.value = current.copy(role = role)
        }
        return result
    }

    override suspend fun handleGoogleSignInResult(data: GoogleSignInData): Result<SSBMaxUser> {
        return try {
            val tokens = data as? GoogleSignInData.ResultData
                ?: return Result.failure(Exception("Expected GoogleSignInData.ResultData"))

            val tokenPair = @Suppress("UNCHECKED_CAST") (tokens.platformData as? Pair<String, String?>)
                ?: return Result.failure(Exception("Missing Google ID token"))
            val (idToken, accessToken) = tokenPair

            val credential = GoogleAuthProvider.credential(idToken, accessToken)
            val authResult = Firebase.auth.signInWithCredential(credential)
            val firebaseUser = authResult.user
                ?: return Result.failure(Exception("Firebase user is null after sign-in"))

            val userResult = loadOrCreateUserProfile(firebaseUser)
            userResult.onSuccess { _currentUser.value = it }
            userResult
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Ported from AuthRepositoryImpl.loadOrCreateUserProfile: an existing Firestore
     * user wins (with lastLoginAt refreshed), otherwise a fresh FREE-tier student
     * profile is created and saved — same fields, same defaults as the Android impl.
     */
    private suspend fun loadOrCreateUserProfile(firebaseUser: FirebaseUser): Result<SSBMaxUser> {
        val existingResult = userRepository.getUser(firebaseUser.uid)
        if (existingResult.isFailure) {
            return Result.failure(existingResult.exceptionOrNull() ?: Exception("Failed to load user"))
        }

        val existingUser = existingResult.getOrNull()
        if (existingUser != null) {
            userRepository.updateLastLogin(firebaseUser.uid)
            return Result.success(existingUser)
        }

        val newUser = firebaseUser.toDomainUser()
        val saveResult = userRepository.saveUser(newUser)
        if (saveResult.isFailure) {
            return Result.failure(saveResult.exceptionOrNull() ?: Exception("Failed to save new user"))
        }
        return Result.success(newUser)
    }

    override suspend fun signOut(): Result<Unit> {
        return try {
            Firebase.auth.signOut()
            _currentUser.value = null
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun isAuthenticated(): Boolean {
        return Firebase.auth.currentUser != null
    }

    private fun FirebaseUser.toDomainUser(): SSBMaxUser = SSBMaxUser(
        id = uid,
        email = email ?: "",
        displayName = displayName ?: "User",
        photoUrl = photoURL,
        role = UserRole.STUDENT,
        subscriptionTier = SubscriptionTier.FREE,
        studentProfile = StudentProfile(userId = uid),
        createdAt = Clock.System.now().toEpochMilliseconds(),
        lastLoginAt = Clock.System.now().toEpochMilliseconds()
    )
}
