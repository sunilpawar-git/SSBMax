# Phase 7B: Firebase Integration - Code Templates

**Status:** Ready to implement AFTER Firebase Console setup  
**Prerequisite:** Complete `FIREBASE_SETUP_GUIDE.md` first

---

## 📁 Files We'll Create/Update

### Repository Layer (Core)
1. `core/data/repository/FirebaseAuthRepository.kt` - Authentication
2. `core/data/repository/FirebaseTestRepository.kt` - Test submissions
3. `core/data/repository/FirebaseUserRepository.kt` - User data

### Data Models (Domain)
4. `core/domain/model/FirestoreModels.kt` - Firestore-specific models

### DI (Dependency Injection)
5. `app/di/FirebaseModule.kt` - Provide Firebase instances

### ViewModels (Update existing)
6. Update `AuthViewModel.kt` - Connect to Firebase Auth
7. Update test ViewModels - Save submissions to Firestore

---

## 🔥 Template 1: Firebase Module (Dependency Injection)

**Create:** `app/src/main/kotlin/com/ssbmax/di/FirebaseModule.kt`

```kotlin
package com.ssbmax.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth {
        return FirebaseAuth.getInstance()
    }

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore {
        return FirebaseFirestore.getInstance().apply {
            // Enable offline persistence
            firestoreSettings = com.google.firebase.firestore.FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .setCacheSizeBytes(com.google.firebase.firestore.FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                .build()
        }
    }

    @Provides
    @Singleton
    fun provideFirebaseStorage(): FirebaseStorage {
        return FirebaseStorage.getInstance()
    }
}
```

**Why?** Hilt will provide these Firebase instances to all repositories.

---

## 🔥 Template 2: Firestore Data Models

**Create:** `core/domain/src/main/kotlin/com/ssbmax/core/domain/model/FirestoreModels.kt`

```kotlin
package com.ssbmax.core.domain.model

/**
 * Firestore document for user profile
 */
data class UserDocument(
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val photoUrl: String = "",
    val role: String = "student", // "student" or "instructor"
    val createdAt: Long = System.currentTimeMillis(),
    val batches: List<String> = emptyList(), // Batch IDs
    val totalTestsCompleted: Int = 0
)

/**
 * Firestore document for test submission
 */
data class SubmissionDocument(
    val id: String = "",
    val userId: String = "",
    val testId: String = "",
    val testType: String = "", // "OIR", "PPDT", "TAT", "WAT", "SRT"
    val submittedAt: Long = System.currentTimeMillis(),
    val status: String = "SUBMITTED_PENDING_REVIEW", // SubmissionStatus enum
    
    // Test-specific data (JSON serialized)
    val responses: String = "", // JSON of test responses
    
    // Scoring
    val aiPreliminaryScore: Float? = null,
    val instructorScore: Float? = null,
    val instructorId: String? = null,
    val instructorFeedback: String? = null,
    val gradedAt: Long? = null,
    
    // Metadata
    val batchId: String? = null,
    val metadata: Map<String, Any> = emptyMap()
)

/**
 * Firestore document for batch/class
 */
data class BatchDocument(
    val id: String = "",
    val name: String = "",
    val instructorId: String = "",
    val instructorName: String = "",
    val inviteCode: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val studentIds: List<String> = emptyList(),
    val isActive: Boolean = true
)

/**
 * Firestore document for test configuration
 */
data class TestConfigDocument(
    val id: String = "",
    val testType: String = "",
    val title: String = "",
    val description: String = "",
    val timeLimit: Int = 0, // seconds
    val questions: String = "", // JSON serialized
    val isActive: Boolean = true
)
```

**Why?** Firestore needs simple data classes (no enums, use strings).

---

## 🔥 Template 3: Firebase Auth Repository

**Create:** `core/data/src/main/kotlin/com/ssbmax/core/data/repository/FirebaseAuthRepository.kt`

```kotlin
package com.ssbmax.core.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.ssbmax.core.domain.model.UserDocument
import com.ssbmax.core.domain.model.UserRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseAuthRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
    private val _currentUser = MutableStateFlow<UserDocument?>(null)
    val currentUser: Flow<UserDocument?> = _currentUser.asStateFlow()

    init {
        // Listen to auth state changes
        firebaseAuth.addAuthStateListener { auth ->
            auth.currentUser?.let { firebaseUser ->
                // Load user document from Firestore
                loadUserDocument(firebaseUser.uid)
            } ?: run {
                _currentUser.value = null
            }
        }
    }

    /**
     * Get current Firebase user
     */
    fun getCurrentFirebaseUser(): FirebaseUser? {
        return firebaseAuth.currentUser
    }

    /**
     * Sign in with Google ID token
     */
    suspend fun signInWithGoogle(idToken: String): Result<UserDocument> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = firebaseAuth.signInWithCredential(credential).await()
            val firebaseUser = authResult.user ?: throw Exception("User is null")
            
            // Check if user document exists
            val userDoc = getUserDocument(firebaseUser.uid)
            
            if (userDoc == null) {
                // New user - needs role selection
                Result.success(
                    UserDocument(
                        uid = firebaseUser.uid,
                        email = firebaseUser.email ?: "",
                        displayName = firebaseUser.displayName ?: "",
                        photoUrl = firebaseUser.photoUrl?.toString() ?: "",
                        role = "" // Empty role means needs selection
                    )
                )
            } else {
                // Existing user
                _currentUser.value = userDoc
                Result.success(userDoc)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Create or update user document with role
     */
    suspend fun createUserWithRole(uid: String, role: UserRole): Result<UserDocument> {
        return try {
            val firebaseUser = firebaseAuth.currentUser ?: throw Exception("User not authenticated")
            
            val userDoc = UserDocument(
                uid = uid,
                email = firebaseUser.email ?: "",
                displayName = firebaseUser.displayName ?: "",
                photoUrl = firebaseUser.photoUrl?.toString() ?: "",
                role = role.name.lowercase(),
                createdAt = System.currentTimeMillis()
            )
            
            // Save to Firestore
            firestore.collection("users")
                .document(uid)
                .set(userDoc)
                .await()
            
            _currentUser.value = userDoc
            Result.success(userDoc)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get user document from Firestore
     */
    suspend fun getUserDocument(uid: String): UserDocument? {
        return try {
            val snapshot = firestore.collection("users")
                .document(uid)
                .get()
                .await()
            
            snapshot.toObject(UserDocument::class.java)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Load user document and update state
     */
    private fun loadUserDocument(uid: String) {
        firestore.collection("users")
            .document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _currentUser.value = null
                    return@addSnapshotListener
                }
                
                val userDoc = snapshot?.toObject(UserDocument::class.java)
                _currentUser.value = userDoc
            }
    }

    /**
     * Sign out
     */
    fun signOut() {
        firebaseAuth.signOut()
        _currentUser.value = null
    }
}
```

**Why?** Handles Google Sign-In and user profile management.

---

## 🔥 Template 4: Firebase Test Repository

**Create:** `core/data/src/main/kotlin/com/ssbmax/core/data/repository/FirebaseTestRepository.kt`

```kotlin
package com.ssbmax.core.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.ssbmax.core.domain.model.SubmissionDocument
import com.ssbmax.core.domain.model.SubmissionStatus
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseTestRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    /**
     * Submit a test (create submission document)
     */
    suspend fun submitTest(submission: SubmissionDocument): Result<String> {
        return try {
            val docRef = if (submission.id.isEmpty()) {
                // Generate new ID
                firestore.collection("submissions").document()
            } else {
                // Use existing ID
                firestore.collection("submissions").document(submission.id)
            }
            
            val submissionWithId = submission.copy(id = docRef.id)
            docRef.set(submissionWithId).await()
            
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get a submission by ID
     */
    suspend fun getSubmission(submissionId: String): Result<SubmissionDocument?> {
        return try {
            val snapshot = firestore.collection("submissions")
                .document(submissionId)
                .get()
                .await()
            
            val submission = snapshot.toObject(SubmissionDocument::class.java)
            Result.success(submission)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get all submissions for a user
     */
    suspend fun getUserSubmissions(userId: String): Result<List<SubmissionDocument>> {
        return try {
            val snapshot = firestore.collection("submissions")
                .whereEqualTo("userId", userId)
                .orderBy("submittedAt", Query.Direction.DESCENDING)
                .get()
                .await()
            
            val submissions = snapshot.documents.mapNotNull { 
                it.toObject(SubmissionDocument::class.java) 
            }
            Result.success(submissions)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get submissions pending review (for instructors)
     */
    suspend fun getPendingSubmissions(instructorId: String? = null): Result<List<SubmissionDocument>> {
        return try {
            val query = firestore.collection("submissions")
                .whereEqualTo("status", SubmissionStatus.SUBMITTED_PENDING_REVIEW.name)
                .orderBy("submittedAt", Query.Direction.ASCENDING)
            
            val snapshot = query.get().await()
            
            val submissions = snapshot.documents.mapNotNull { 
                it.toObject(SubmissionDocument::class.java) 
            }
            Result.success(submissions)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Listen to submission changes in real-time
     */
    fun observeSubmission(submissionId: String): Flow<SubmissionDocument?> = callbackFlow {
        val listener = firestore.collection("submissions")
            .document(submissionId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val submission = snapshot?.toObject(SubmissionDocument::class.java)
                trySend(submission)
            }
        
        awaitClose { listener.remove() }
    }

    /**
     * Update submission with instructor grading
     */
    suspend fun gradeSubmission(
        submissionId: String,
        instructorId: String,
        score: Float,
        feedback: String
    ): Result<Unit> {
        return try {
            val updates = mapOf(
                "status" to SubmissionStatus.GRADED.name,
                "instructorId" to instructorId,
                "instructorScore" to score,
                "instructorFeedback" to feedback,
                "gradedAt" to System.currentTimeMillis()
            )
            
            firestore.collection("submissions")
                .document(submissionId)
                .update(updates)
                .await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

**Why?** Handles all test submission CRUD operations with Firestore.

---

## 🔥 Template 5: Update AuthViewModel

**Update:** `app/src/main/kotlin/com/ssbmax/ui/auth/AuthViewModel.kt`

Add these methods to connect to Firebase:

```kotlin
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val firebaseAuthRepository: FirebaseAuthRepository // INJECT THIS
) : ViewModel() {

    // ... existing code ...

    /**
     * Sign in with Google (called after getting ID token from Google Sign-In)
     */
    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            val result = firebaseAuthRepository.signInWithGoogle(idToken)
            
            result.fold(
                onSuccess = { userDoc ->
                    if (userDoc.role.isEmpty()) {
                        // New user - needs role selection
                        _pendingUser.value = userDoc
                        _authState.value = AuthState.NeedsRoleSelection
                    } else {
                        // Existing user - navigate to home
                        _authState.value = when (userDoc.role) {
                            "student" -> AuthState.StudentAuthenticated
                            "instructor" -> AuthState.InstructorAuthenticated
                            else -> AuthState.Unauthenticated
                        }
                    }
                    _uiState.update { it.copy(isLoading = false) }
                },
                onFailure = { error ->
                    _uiState.update { 
                        it.copy(
                            isLoading = false, 
                            error = error.message ?: "Sign in failed"
                        ) 
                    }
                    _authState.value = AuthState.Unauthenticated
                }
            )
        }
    }

    /**
     * Set user role after selection
     */
    fun setUserRole(role: UserRole) {
        viewModelScope.launch {
            val user = _pendingUser.value ?: return@launch
            
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            val result = firebaseAuthRepository.createUserWithRole(user.uid, role)
            
            result.fold(
                onSuccess = { userDoc ->
                    _authState.value = when (role) {
                        UserRole.STUDENT -> AuthState.StudentAuthenticated
                        UserRole.INSTRUCTOR -> AuthState.InstructorAuthenticated
                        else -> AuthState.Unauthenticated
                    }
                    _uiState.update { it.copy(isLoading = false) }
                },
                onFailure = { error ->
                    _uiState.update { 
                        it.copy(
                            isLoading = false, 
                            error = error.message ?: "Role selection failed"
                        ) 
                    }
                }
            )
        }
    }

    /**
     * Sign out
     */
    fun signOut() {
        firebaseAuthRepository.signOut()
        _authState.value = AuthState.Unauthenticated
    }
}
```

---

## 🔥 Template 6: Update Test ViewModels to Save to Firestore

**Example: Update PPDTTestViewModel**

```kotlin
@HiltViewModel
class PPDTTestViewModel @Inject constructor(
    private val firebaseTestRepository: FirebaseTestRepository, // INJECT THIS
    private val firebaseAuthRepository: FirebaseAuthRepository // INJECT THIS
) : ViewModel() {

    // ... existing code ...

    fun submitTest() {
        viewModelScope.launch {
            val userId = firebaseAuthRepository.getCurrentFirebaseUser()?.uid ?: return@launch
            
            // Create submission document
            val submission = SubmissionDocument(
                userId = userId,
                testId = _uiState.value.testConfig.testId,
                testType = "PPDT",
                submittedAt = System.currentTimeMillis(),
                status = SubmissionStatus.SUBMITTED_PENDING_REVIEW.name,
                responses = kotlinx.serialization.json.Json.encodeToString(
                    com.ssbmax.core.domain.model.PPDTSubmission.serializer(),
                    ppdtSubmission
                ),
                aiPreliminaryScore = aiScore.overallScore,
                metadata = mapOf(
                    "charactersCount" to story.length,
                    "writingTimeSeconds" to writingTimeTaken
                )
            )
            
            // Save to Firestore
            val result = firebaseTestRepository.submitTest(submission)
            
            result.fold(
                onSuccess = { submissionId ->
                    _uiState.update { 
                        it.copy(
                            currentPhase = PPDTPhase.SUBMITTED,
                            isSubmitted = true,
                            submissionId = submissionId
                        ) 
                    }
                },
                onFailure = { error ->
                    // Handle error (show toast, etc.)
                    println("Submission failed: ${error.message}")
                }
            )
        }
    }
}
```

Apply similar pattern to: `TATTestViewModel`, `WATTestViewModel`, `SRTTestViewModel`, `OIRTestViewModel`

---

## 📊 Firestore Collections Structure

```
/users/{userId}
  - uid: string
  - email: string
  - role: string ("student" | "instructor")
  - createdAt: timestamp

/submissions/{submissionId}
  - id: string
  - userId: string
  - testId: string
  - testType: string ("OIR", "PPDT", "TAT", etc.)
  - responses: JSON string
  - status: string ("SUBMITTED_PENDING_REVIEW", "GRADED")
  - aiPreliminaryScore: number
  - instructorScore: number (optional)
  - instructorFeedback: string (optional)
  - submittedAt: timestamp
  - gradedAt: timestamp (optional)

/batches/{batchId}
  - id: string
  - name: string
  - instructorId: string
  - inviteCode: string
  - studentIds: array<string>
  - createdAt: timestamp

/test_configs/{testId}
  - id: string
  - testType: string
  - title: string
  - questions: JSON string
```

---

## ✅ Implementation Checklist

- [ ] Create `FirebaseModule.kt` for DI
- [ ] Create `FirestoreModels.kt` for data models
- [ ] Create `FirebaseAuthRepository.kt`
- [ ] Create `FirebaseTestRepository.kt`
- [ ] Update `AuthViewModel.kt` to use Firebase Auth
- [ ] Update test ViewModels (PPDT, TAT, WAT, SRT, OIR) to save submissions
- [ ] Test Google Sign-In flow
- [ ] Test submission creation in Firestore
- [ ] Test real-time listeners
- [ ] Verify Firestore security rules

---

## 🚀 Testing Steps

1. **Test Authentication:**
   - Run app → Sign in with Google
   - Check Firebase Console → Authentication → Users (should see your account)
   - Check Firestore → users collection (should see your user document)

2. **Test Submission:**
   - Take a test (e.g., PPDT)
   - Submit test
   - Check Firestore → submissions collection (should see new document)
   - Verify AI score is saved

3. **Test Real-time Updates:**
   - Open submission result screen
   - In Firebase Console, manually update the submission (add instructor score)
   - Result screen should update automatically

---

## 🎯 Next: Implementation

Once you've completed the Firebase Console setup (Part 1-5 of FIREBASE_SETUP_GUIDE.md), implement these templates in order:

1. Template 1: Firebase Module ✅
2. Template 2: Firestore Models ✅
3. Template 3: Auth Repository ✅
4. Template 4: Test Repository ✅
5. Template 5: Update AuthViewModel ✅
6. Template 6: Update Test ViewModels ✅

Let me know when you're ready, and I'll help you implement each step! 🔥

