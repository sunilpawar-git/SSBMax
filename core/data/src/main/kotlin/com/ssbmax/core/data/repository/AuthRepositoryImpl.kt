package com.ssbmax.core.data.repository

import android.content.Intent
import com.google.firebase.auth.FirebaseUser
import com.ssbmax.core.data.remote.FirebaseAuthService
import com.ssbmax.core.data.remote.FirestoreUserRepository
import com.ssbmax.core.domain.model.SSBMaxUser
import com.ssbmax.core.domain.model.StudentProfile
import com.ssbmax.core.domain.model.UserRole
import com.ssbmax.core.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firebase implementation of AuthRepository
 * Manages authentication with Firebase Auth and user profiles with Firestore
 */
@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuthService: FirebaseAuthService,
    private val firestoreUserRepository: FirestoreUserRepository
) : AuthRepository {

    private val _currentUser = MutableStateFlow<SSBMaxUser?>(null)
    override val currentUser: Flow<SSBMaxUser?> = _currentUser.asStateFlow()

    init {
        // Observe Firebase auth state and sync with Firestore user
        observeAuthState()
    }

    private fun observeAuthState() {
        // This will be initialized when the repository is created
        // We'll listen to Firebase auth changes and load user from Firestore
    }

    /**
     * Sign in with email and password
     * Note: We're primarily using Google Sign-In, but keeping this for compatibility
     */
    override suspend fun signIn(email: String, password: String): Result<SSBMaxUser> {
        // Firebase email/password auth can be added here if needed
        return Result.failure(Exception("Email/password sign-in not implemented. Please use Google Sign-In."))
    }

    /**
     * Sign up with email and password
     * Note: We're primarily using Google Sign-In, but keeping this for compatibility
     */
    override suspend fun signUp(email: String, password: String, displayName: String): Result<SSBMaxUser> {
        // Firebase email/password auth can be added here if needed
        return Result.failure(Exception("Email/password sign-up not implemented. Please use Google Sign-In."))
    }

    /**
     * Get Google Sign-In intent
     */
    fun getGoogleSignInIntent(): Intent {
        return firebaseAuthService.getSignInIntent()
    }

    /**
     * Handle Google Sign-In result
     */
    suspend fun handleGoogleSignInResult(data: Intent?): Result<SSBMaxUser> {
        return try {
            // Authenticate with Firebase using Google credentials
            val firebaseResult = firebaseAuthService.handleSignInResult(data)
            
            if (firebaseResult.isFailure) {
                return Result.failure(firebaseResult.exceptionOrNull() ?: Exception("Sign-in failed"))
            }
            
            val firebaseUser = firebaseResult.getOrNull()
                ?: return Result.failure(Exception("Firebase user is null"))
            
            // Load or create user profile in Firestore
            loadOrCreateUserProfile(firebaseUser)
        } catch (e: Exception) {
            Result.failure(Exception("Google Sign-In error: ${e.message}", e))
        }
    }

    /**
     * Load user profile from Firestore or create new one
     */
    private suspend fun loadOrCreateUserProfile(firebaseUser: FirebaseUser): Result<SSBMaxUser> {
        return try {
            // Try to load existing user from Firestore
            val userResult = firestoreUserRepository.getUser(firebaseUser.uid)
            
            if (userResult.isFailure) {
                return Result.failure(userResult.exceptionOrNull() ?: Exception("Failed to load user"))
            }
            
            val existingUser = userResult.getOrNull()
            
            val user = if (existingUser != null) {
                // Update last login time
                firestoreUserRepository.updateLastLogin(firebaseUser.uid)
                existingUser
            } else {
                // Create new user profile
                val newUser = SSBMaxUser(
                    id = firebaseUser.uid,
                    email = firebaseUser.email ?: "",
                    displayName = firebaseUser.displayName ?: "User",
                    photoUrl = firebaseUser.photoUrl?.toString(),
                    role = UserRole.STUDENT, // Default role, can be changed later
                    isPremium = false,
                    studentProfile = StudentProfile(userId = firebaseUser.uid),
                    instructorProfile = null,
                    createdAt = System.currentTimeMillis(),
                    lastLoginAt = System.currentTimeMillis()
                )
                
                // Save to Firestore
                firestoreUserRepository.saveUser(newUser)
                newUser
            }
            
            _currentUser.value = user
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to load/create user profile: ${e.message}", e))
        }
    }

    /**
     * Update user role
     */
    suspend fun updateUserRole(role: UserRole): Result<Unit> {
        return try {
            val userId = firebaseAuthService.getCurrentUserId()
                ?: return Result.failure(Exception("No user logged in"))
            
            val result = firestoreUserRepository.updateUserRole(userId, role)
            
            if (result.isSuccess) {
                // Reload user to update local state
                val userResult = firestoreUserRepository.getUser(userId)
                if (userResult.isSuccess) {
                    _currentUser.value = userResult.getOrNull()
                }
            }
            
            result
        } catch (e: Exception) {
            Result.failure(Exception("Failed to update role: ${e.message}", e))
        }
    }

    /**
     * Observe user from Firestore in real-time
     */
    fun observeCurrentUser(): Flow<SSBMaxUser?> {
        val userId = firebaseAuthService.getCurrentUserId() ?: return _currentUser
        return firestoreUserRepository.observeUser(userId)
    }
    
    /**
     * Sign out current user
     */
    override suspend fun signOut(): Result<Unit> {
        return try {
            val result = firebaseAuthService.signOut()
            if (result.isSuccess) {
                _currentUser.value = null
            }
            result
        } catch (e: Exception) {
            Result.failure(Exception("Sign out error: ${e.message}", e))
        }
    }
    
    /**
     * Check if user is authenticated
     */
    override suspend fun isAuthenticated(): Boolean {
        return firebaseAuthService.isAuthenticated()
    }

    /**
     * Delete user account
     */
    suspend fun deleteAccount(): Result<Unit> {
        return try {
            val userId = firebaseAuthService.getCurrentUserId()
                ?: return Result.failure(Exception("No user logged in"))
            
            // Delete from Firestore
            firestoreUserRepository.deleteUser(userId)
            
            // Delete from Firebase Auth
            val result = firebaseAuthService.deleteAccount()
            
            if (result.isSuccess) {
                _currentUser.value = null
            }
            
            result
        } catch (e: Exception) {
            Result.failure(Exception("Failed to delete account: ${e.message}", e))
        }
    }
}

