package com.ssbmax.shared.data.repository

import com.ssbmax.shared.domain.model.GoogleSignInData
import com.ssbmax.shared.domain.model.SSBMaxUser
import com.ssbmax.shared.domain.model.StudentProfile
import com.ssbmax.shared.domain.model.UserRole
import com.ssbmax.shared.domain.repository.AuthRepository
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.FirebaseUser
import dev.gitlive.firebase.auth.GoogleAuthProvider
import dev.gitlive.firebase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * GitLive-Firebase-backed implementation for the Phase 0 KMP spike.
 *
 * Deliberately simplified vs. the Android AuthRepositoryImpl: no
 * FirestoreUserRepository-backed "load or create profile" merge (that's real
 * business logic worth porting in Phase 2, not needed to validate the
 * dependency stack in Phase 0) — this always constructs a fresh profile.
 * currentUser is a plain in-memory StateFlow rather than the original's
 * callbackFlow-over-Firebase-authState + FirestoreUserRepository join,
 * again to keep the spike's surface area to "does GitLive auth work at all."
 *
 * A real Google ID token exchange (Android Credential Manager / iOS
 * GoogleSignIn SDK) is NOT wired here — GoogleSignInData carries an
 * (idToken, accessToken) pair by convention for this spike; obtaining that
 * pair from a native picker is platform UI work out of Phase 0 scope.
 */
class GitLiveAuthRepository : AuthRepository {

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
        _currentUser.value = current.copy(role = role)
        return Result.success(Unit)
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

            val user = firebaseUser.toDomainUser()
            _currentUser.value = user
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
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
        studentProfile = StudentProfile(userId = uid)
    )
}
