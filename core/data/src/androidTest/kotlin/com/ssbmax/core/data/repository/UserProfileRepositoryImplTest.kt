package com.ssbmax.core.data.repository

import app.cash.turbine.test
import com.google.firebase.firestore.FirebaseFirestore
import com.ssbmax.core.data.FirebaseTestHelper
import com.ssbmax.core.domain.model.EntryType
import com.ssbmax.core.domain.model.Gender
import com.ssbmax.core.domain.model.SubscriptionType
import com.ssbmax.core.domain.model.UserProfile
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

/**
 * Integration tests for UserProfileRepositoryImpl with Firebase Emulator
 * 
 * Prerequisites:
 * - Firebase Emulator must be running: `firebase emulators:start`
 * - Tests use real Firestore operations against emulator
 * - Tests validate actual data persistence and real-time updates
 */
class UserProfileRepositoryImplTest {
    
    private lateinit var firestore: FirebaseFirestore
    private lateinit var repository: UserProfileRepositoryImpl
    
    private val testUserId = "test-user-${System.currentTimeMillis()}"
    
    @Before
    fun setUp() {
        // Get Firestore instance configured for emulator
        firestore = FirebaseTestHelper.getEmulatorFirestore()
        repository = UserProfileRepositoryImpl(firestore)
    }
    
    @After
    fun tearDown() = runTest {
        // Clean up test data
        try {
            firestore.collection("users")
                .document(testUserId)
                .collection("data")
                .document("profile")
                .delete()
                .await()
        } catch (e: Exception) {
            // Ignore cleanup errors
        }
    }
    
    @Test
    fun saveUserProfile_successfully_persists_to_firestore() = runTest {
        // Given
        val profile = createTestProfile()
        
        // When
        val result = repository.saveUserProfile(profile)
        
        // Then
        assertTrue("Save should succeed", result.isSuccess)
        
        // Verify data persisted
        val doc = firestore.collection("users")
            .document(testUserId)
            .collection("data")
            .document("profile")
            .get()
            .await()
        
        assertTrue("Document should exist", doc.exists())
        assertEquals(profile.fullName, doc.getString("fullName"))
        assertEquals(profile.age.toLong(), doc.getLong("age"))
    }
    
    @Test
    fun getUserProfile_returns_null_when_profile_does_not_exist() = runTest {
        // When/Then
        repository.getUserProfile("non-existent-user").test(timeout = 30.seconds) {
            val result = awaitItem()
            assertTrue("Should return success", result.isSuccess)
            assertNull("Profile should be null", result.getOrNull())
            cancelAndIgnoreRemainingEvents()
        }
    }
    
    @Test
    fun getUserProfile_returns_saved_profile() = runTest {
        // Given
        val profile = createTestProfile()
        repository.saveUserProfile(profile)
        
        // When/Then
        repository.getUserProfile(testUserId).test(timeout = 30.seconds) {
            val result = awaitItem()
            assertTrue("Should return success", result.isSuccess)
            
            val loadedProfile = result.getOrNull()
            assertNotNull("Profile should not be null", loadedProfile)
            assertEquals(profile.fullName, loadedProfile?.fullName)
            assertEquals(profile.age, loadedProfile?.age)
            assertEquals(profile.gender, loadedProfile?.gender)
            assertEquals(profile.entryType, loadedProfile?.entryType)
            cancelAndIgnoreRemainingEvents()
        }
    }
    
    @Test
    fun getUserProfile_emits_realtime_updates() = runTest {
        // Given
        val initialProfile = createTestProfile()
        repository.saveUserProfile(initialProfile)
        
        // When - Start observing
        repository.getUserProfile(testUserId).test(timeout = 10.seconds) {
            // Then - Verify initial value
            val initial = awaitItem()
            assertEquals("Test User", initial.getOrNull()?.fullName)
            
            // When - Update profile
            val updatedProfile = initialProfile.copy(fullName = "Updated Name")
            repository.updateUserProfile(updatedProfile)
            
            // Then - Verify update received
            val updated = awaitItem()
            assertEquals("Updated Name", updated.getOrNull()?.fullName)
            
            cancelAndIgnoreRemainingEvents()
        }
    }
    
    @Test
    fun updateUserProfile_updates_existing_profile() = runTest {
        // Given
        val originalProfile = createTestProfile()
        repository.saveUserProfile(originalProfile)
        
        // When
        val updatedProfile = originalProfile.copy(
            fullName = "Updated User",
            age = 25
        )
        val result = repository.updateUserProfile(updatedProfile)
        
        // Then
        assertTrue("Update should succeed", result.isSuccess)
        
        // Verify updated data
        val doc = firestore.collection("users")
            .document(testUserId)
            .collection("data")
            .document("profile")
            .get()
            .await()
        
        assertEquals("Updated User", doc.getString("fullName"))
        assertEquals(25L, doc.getLong("age"))
    }
    
    @Test
    fun updateUserProfile_updates_timestamp() = runTest {
        // Given
        val originalProfile = createTestProfile()
        repository.saveUserProfile(originalProfile)
        
        Thread.sleep(100) // Ensure timestamp difference
        
        // When
        val updatedProfile = originalProfile.copy(fullName = "New Name")
        repository.updateUserProfile(updatedProfile)
        
        // Then
        val doc = firestore.collection("users")
            .document(testUserId)
            .collection("data")
            .document("profile")
            .get()
            .await()
        
        val updatedAt = doc.getLong("updatedAt") ?: 0
        assertTrue("updatedAt should be greater than createdAt", 
            updatedAt > originalProfile.createdAt)
    }
    
    @Test
    fun hasCompletedProfile_returns_false_for_non_existent_profile() = runTest {
        // When/Then
        repository.hasCompletedProfile("non-existent-user").test(timeout = 30.seconds) {
            val hasCompleted = awaitItem()
            assertFalse("Should return false for non-existent profile", hasCompleted)
            cancelAndIgnoreRemainingEvents()
        }
    }
    
    @Test
    fun hasCompletedProfile_returns_true_for_complete_profile() = runTest {
        // Given
        val completeProfile = createTestProfile()
        repository.saveUserProfile(completeProfile)
        
        // When/Then
        repository.hasCompletedProfile(testUserId).test(timeout = 30.seconds) {
            val hasCompleted = awaitItem()
            assertTrue("Should return true for complete profile", hasCompleted)
            cancelAndIgnoreRemainingEvents()
        }
    }
    
    @Test
    fun hasCompletedProfile_returns_false_for_incomplete_profile() = runTest {
        // Given - Profile with blank name
        val incompleteProfile = createTestProfile().copy(fullName = "")
        repository.saveUserProfile(incompleteProfile)
        
        // When/Then
        repository.hasCompletedProfile(testUserId).test(timeout = 30.seconds) {
            val hasCompleted = awaitItem()
            assertFalse("Should return false for incomplete profile", hasCompleted)
            cancelAndIgnoreRemainingEvents()
        }
    }
    
    @Test
    fun profile_supports_all_gender_types() = runTest {
        val genders = listOf(Gender.MALE, Gender.FEMALE, Gender.OTHER)
        
        genders.forEach { gender ->
            val userId = "test-user-gender-${gender.name}-${System.currentTimeMillis()}"
            val profile = createTestProfile(userId).copy(gender = gender)
            
            // When
            repository.saveUserProfile(profile)
            
            // Then
            repository.getUserProfile(userId).test(timeout = 30.seconds) {
                val result = awaitItem()
                assertEquals(gender, result.getOrNull()?.gender)
                cancelAndIgnoreRemainingEvents()
            }
            
            // Cleanup
            firestore.collection("users")
                .document(userId)
                .collection("data")
                .document("profile")
                .delete()
                .await()
        }
    }
    
    @Test
    fun profile_supports_all_entry_types() = runTest {
        val entryTypes = listOf(
            EntryType.ENTRY_10_PLUS_2,
            EntryType.GRADUATE,
            EntryType.SERVICE
        )
        
        entryTypes.forEach { entryType ->
            val userId = "test-user-entry-${entryType.name}-${System.currentTimeMillis()}"
            val profile = createTestProfile(userId).copy(entryType = entryType)
            
            // When
            repository.saveUserProfile(profile)
            
            // Then
            repository.getUserProfile(userId).test(timeout = 30.seconds) {
                val result = awaitItem()
                assertEquals(entryType, result.getOrNull()?.entryType)
                cancelAndIgnoreRemainingEvents()
            }
            
            // Cleanup
            firestore.collection("users")
                .document(userId)
                .collection("data")
                .document("profile")
                .delete()
                .await()
        }
    }
    
    @Test
    fun profile_supports_all_subscription_types() = runTest {
        val subscriptionTypes = listOf(
            SubscriptionType.FREE,
            SubscriptionType.PREMIUM_ASSESSOR,
            SubscriptionType.PREMIUM_AI
        )
        
        subscriptionTypes.forEach { subType ->
            val userId = "test-user-sub-${subType.name}-${System.currentTimeMillis()}"
            val profile = createTestProfile(userId).copy(subscriptionType = subType)
            
            // When
            repository.saveUserProfile(profile)
            
            // Then
            repository.getUserProfile(userId).test(timeout = 30.seconds) {
                val result = awaitItem()
                assertEquals(subType, result.getOrNull()?.subscriptionType)
                cancelAndIgnoreRemainingEvents()
            }
            
            // Cleanup
            firestore.collection("users")
                .document(userId)
                .collection("data")
                .document("profile")
                .delete()
                .await()
        }
    }
    
    @Test
    fun profile_with_picture_url_persists_correctly() = runTest {
        // Given
        val profileWithPicture = createTestProfile().copy(
            profilePictureUrl = "https://example.com/profile.jpg"
        )
        
        // When
        repository.saveUserProfile(profileWithPicture)
        
        // Then
        repository.getUserProfile(testUserId).test(timeout = 30.seconds) {
            val result = awaitItem()
            assertEquals(
                "https://example.com/profile.jpg",
                result.getOrNull()?.profilePictureUrl
            )
            cancelAndIgnoreRemainingEvents()
        }
    }
    
    // Helper methods
    private fun createTestProfile(userId: String = testUserId) = UserProfile(
        userId = userId,
        fullName = "Test User",
        age = 22,
        gender = Gender.MALE,
        entryType = EntryType.GRADUATE,
        profilePictureUrl = null,
        subscriptionType = SubscriptionType.FREE,
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis()
    )
}

