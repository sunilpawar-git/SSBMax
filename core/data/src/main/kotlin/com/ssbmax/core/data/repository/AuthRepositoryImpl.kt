package com.ssbmax.core.data.repository

import android.content.Intent
import android.util.Log
import com.google.firebase.auth.FirebaseUser
import com.ssbmax.core.data.di.ApplicationScope
import com.ssbmax.core.data.remote.FirebaseAuthService
import com.ssbmax.core.data.remote.FirestoreUserRepository
import com.ssbmax.core.data.util.MemoryLeakTracker
import com.ssbmax.core.domain.model.SSBMaxUser
import com.ssbmax.core.domain.model.StudentProfile
import com.ssbmax.core.domain.model.SubscriptionTier
import com.ssbmax.core.domain.model.UserRole
import com.ssbmax.core.domain.repository.AuthRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firebase implementation of AuthRepository
 * Manages authentication with Firebase Auth and user profiles with Firestore
 * 
 * Uses reactive StateFlow with WhileSubscribed to automatically manage lifecycle:
 * - Starts collecting when first subscriber attaches
 * - Stops collecting 5 seconds after last subscriber detaches
 * - Eliminates memory leak from unbound CoroutineScope
 */
@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuthService: FirebaseAuthService,
    private val firestoreUserRepository: FirestoreUserRepository,
    @ApplicationScope private val applicationScope: CoroutineScope
) : AuthRepository {

    init {
        Log.d("AuthRepositoryImpl", "🔐 AuthRepository singleton initialized")
        MemoryLeakTracker.logMemoryDump("AuthRepository-Init")
    }

    /**
     * Reactive auth state that automatically starts/stops based on active collectors.
     * When Firebase auth state changes, loads or creates user profile from Firestore.
     */
    override val currentUser: StateFlow<SSBMaxUser?> = callbackFlow {
        Log.d("AuthRepositoryImpl", "🔄 Starting Firebase auth state collection")
        firebaseAuthService.authState.collect { firebaseUser ->
            if (firebaseUser != null) {
                Log.d("AuthRepositoryImpl", "👤 User authenticated: ${firebaseUser.email}")
                // User is signed in, load their profile from Firestore
                val result = loadOrCreateUserProfile(firebaseUser)
                trySend(result.getOrNull())
            } else {
                Log.d("AuthRepositoryImpl", "🚪 User signed out")
                // User is signed out
                trySend(null)
            }
        }
        awaitClose {
            Log.d("AuthRepositoryImpl", "🔄 Auth state collection closed")
        }
    }.stateIn(
        scope = applicationScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        initialValue = null
    )

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
    override fun getGoogleSignInIntent(): Intent {
        return firebaseAuthService.getSignInIntent()
    }

    /**
     * Handle Google Sign-In result
     */
    override suspend fun handleGoogleSignInResult(data: Intent?): Result<SSBMaxUser> {
        return try {
            android.util.Log.d("AuthRepositoryImpl", "handleGoogleSignInResult: Starting authentication")
            // Authenticate with Firebase using Google credentials
            val firebaseResult = firebaseAuthService.handleSignInResult(data)
            
            if (firebaseResult.isFailure) {
                val error = firebaseResult.exceptionOrNull() ?: Exception("Sign-in failed")
                android.util.Log.e("AuthRepositoryImpl", "Firebase sign-in failed: ${error.message}", error)
                return Result.failure(error)
            }
            
            val firebaseUser = firebaseResult.getOrNull()
            if (firebaseUser == null) {
                android.util.Log.e("AuthRepositoryImpl", "Firebase user is null after sign-in")
                return Result.failure(Exception("Firebase user is null"))
            }
            
            android.util.Log.d("AuthRepositoryImpl", "Firebase authentication successful: ${firebaseUser.email}")
            // Load or create user profile in Firestore
            loadOrCreateUserProfile(firebaseUser)
        } catch (e: Exception) {
            android.util.Log.e("AuthRepositoryImpl", "Google Sign-In error: ${e.message}", e)
            Result.failure(Exception("Google Sign-In error: ${e.message}", e))
        }
    }

    /**
     * Load user profile from Firestore or create new one
     */
    private suspend fun loadOrCreateUserProfile(firebaseUser: FirebaseUser): Result<SSBMaxUser> {
        return try {
            android.util.Log.d("AuthRepositoryImpl", "Loading/creating profile for user: ${firebaseUser.uid}")
            // Try to load existing user from Firestore
            val userResult = firestoreUserRepository.getUser(firebaseUser.uid)
            
            if (userResult.isFailure) {
                val error = userResult.exceptionOrNull() ?: Exception("Failed to load user")
                android.util.Log.e("AuthRepositoryImpl", "Failed to load user: ${error.message}", error)
                return Result.failure(error)
            }
            
            val existingUser = userResult.getOrNull()
            
            val user = if (existingUser != null) {
                android.util.Log.d("AuthRepositoryImpl", "Existing user found: ${existingUser.email}")
                // Update last login time
                firestoreUserRepository.updateLastLogin(firebaseUser.uid)
                existingUser
            } else {
                android.util.Log.d("AuthRepositoryImpl", "Creating new user profile")
                // Create new user profile
                val newUser = SSBMaxUser(
                    id = firebaseUser.uid,
                    email = firebaseUser.email ?: "",
                    displayName = firebaseUser.displayName ?: "User",
                    photoUrl = firebaseUser.photoUrl?.toString(),
                    role = UserRole.STUDENT, // Default role, can be changed later
                    subscriptionTier = SubscriptionTier.FREE,
                    subscription = null,
                    studentProfile = StudentProfile(userId = firebaseUser.uid),
                    instructorProfile = null,
                    createdAt = System.currentTimeMillis(),
                    lastLoginAt = System.currentTimeMillis()
                )
                
                // Save to Firestore
                firestoreUserRepository.saveUser(newUser)
                android.util.Log.d("AuthRepositoryImpl", "New user profile created and saved")
                newUser
            }
            
            android.util.Log.d("AuthRepositoryImpl", "User profile loaded successfully: ${user.email}")
            Result.success(user)
        } catch (e: Exception) {
            android.util.Log.e("AuthRepositoryImpl", "Failed to load/create user profile: ${e.message}", e)
            Result.failure(Exception("Failed to load/create user profile: ${e.message}", e))
        }
    }

    /**
     * Update user role
     * Note: The reactive Flow will automatically emit updated user from Firestore
     */
    override suspend fun updateUserRole(role: UserRole): Result<Unit> {
        return try {
            val userId = firebaseAuthService.getCurrentUserId()
                ?: return Result.failure(Exception("No user logged in"))
            
            firestoreUserRepository.updateUserRole(userId, role)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to update role: ${e.message}", e))
        }
    }

    /**
     * Observe user from Firestore in real-time
     */
    fun observeCurrentUser(): StateFlow<SSBMaxUser?> {
        val userId = firebaseAuthService.getCurrentUserId()
        if (userId == null) {
            Log.w("AuthRepositoryImpl", "⚠️ observeCurrentUser called but no authenticated user")
            return currentUser
        }

        Log.d("AuthRepositoryImpl", "👀 Starting real-time user observation for: $userId")
        return firestoreUserRepository.observeUser(userId)
            .stateIn(
                scope = applicationScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = currentUser.value
            )
    }
    
    /**
     * Sign out current user
     * Note: The reactive Flow will automatically emit null when Firebase auth state changes
     */
    override suspend fun signOut(): Result<Unit> {
        return try {
            firebaseAuthService.signOut()
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
     * Note: The reactive Flow will automatically emit null when Firebase auth state changes
     */
    suspend fun deleteAccount(): Result<Unit> {
        return try {
            val userId = firebaseAuthService.getCurrentUserId()
                ?: return Result.failure(Exception("No user logged in"))
            
            // Delete from Firestore
            firestoreUserRepository.deleteUser(userId)
            
            // Delete from Firebase Auth (this will trigger auth state change to null)
            firebaseAuthService.deleteAccount()
        } catch (e: Exception) {
            Result.failure(Exception("Failed to delete account: ${e.message}", e))
        }
    }
}

