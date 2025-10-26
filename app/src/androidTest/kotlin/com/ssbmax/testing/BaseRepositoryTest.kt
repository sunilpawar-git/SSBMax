package com.ssbmax.testing

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import org.junit.After
import org.junit.Before
import org.junit.Rule

/**
 * Base class for repository integration tests
 * Provides Firebase Emulator connection and test utilities
 * 
 * Usage:
 * ```
 * class MyRepositoryTest : BaseRepositoryTest() {
 *     @Test
 *     fun `test repository operation`() = runTest {
 *         // Test with real Firebase Emulator
 *     }
 * }
 * ```
 */
@OptIn(ExperimentalCoroutinesApi::class)
abstract class BaseRepositoryTest {
    
    @get:Rule
    val emulatorRule = FirebaseEmulatorExtension()
    
    protected lateinit var firestore: FirebaseFirestore
    protected lateinit var auth: FirebaseAuth
    protected val testDispatcher: TestDispatcher = StandardTestDispatcher()
    
    @Before
    open fun setUp() {
        firestore = emulatorRule.getFirestore()
        auth = emulatorRule.getAuth()
    }
    
    @After
    open fun tearDown() {
        // Sign out and clear any test data
        auth.signOut()
    }
    
    /**
     * Helper to create a test user in Firebase Auth Emulator
     */
    protected suspend fun createTestUser(
        email: String = "test@ssbmax.com",
        password: String = "testPassword123",
        displayName: String = "Test User"
    ): String {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        task.result.user?.updateProfile(
                            com.google.firebase.auth.UserProfileChangeRequest.Builder()
                                .setDisplayName(displayName)
                                .build()
                        )
                    }
                }
            
            result.await().user?.uid
                ?: throw IllegalStateException("Failed to create test user")
        } catch (e: Exception) {
            throw IllegalStateException("Failed to create test user: ${e.message}", e)
        }
    }
    
    /**
     * Helper to seed test data into Firestore Emulator
     */
    protected suspend fun seedFirestoreData(
        collection: String,
        documentId: String,
        data: Map<String, Any>
    ) {
        try {
            firestore.collection(collection)
                .document(documentId)
                .set(data)
                .await()
        } catch (e: Exception) {
            throw IllegalStateException("Failed to seed Firestore data: ${e.message}", e)
        }
    }
    
    /**
     * Helper to clear a Firestore collection
     */
    protected suspend fun clearFirestoreCollection(collection: String) {
        try {
            val snapshot = firestore.collection(collection).get().await()
            snapshot.documents.forEach { doc ->
                doc.reference.delete().await()
            }
        } catch (e: Exception) {
            throw IllegalStateException("Failed to clear Firestore collection: ${e.message}", e)
        }
    }
}

