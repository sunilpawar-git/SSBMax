package com.ssbmax.ui.profile

import app.cash.turbine.test
import com.ssbmax.core.domain.model.EntryType
import com.ssbmax.core.domain.model.Gender
import com.ssbmax.core.domain.model.SSBMaxUser
import com.ssbmax.core.domain.model.UserProfile
import com.ssbmax.core.domain.model.UserRole
import com.ssbmax.core.domain.repository.AuthRepository
import com.ssbmax.core.domain.repository.UserProfileRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for UserProfileViewModel
 * Tests profile loading, saving, validation, and state management
 */
@OptIn(ExperimentalCoroutinesApi::class)
class UserProfileViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: UserProfileViewModel
    private val mockUserProfileRepository = mockk<UserProfileRepository>(relaxed = true)
    private val mockAuthRepository = mockk<AuthRepository>(relaxed = true)
    private val mockCurrentUserFlow = MutableStateFlow<SSBMaxUser?>(null)

    private val testUser = SSBMaxUser(
        id = "test-user-123",
        email = "test@example.com",
        role = UserRole.STUDENT,
        displayName = "Test User"
    )

    private val testProfile = UserProfile(
        userId = "test-user-123",
        fullName = "John Doe",
        age = 25,
        gender = Gender.MALE,
        entryType = EntryType.ENTRY_10_PLUS_2,
        profilePictureUrl = null
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockkStatic(android.util.Log::class)
        every { android.util.Log.d(any<String>(), any<String>()) } returns 0
        every { android.util.Log.w(any<String>(), any<String>()) } returns 0
        every { android.util.Log.e(any<String>(), any<String>()) } returns 0
        every { android.util.Log.e(any<String>(), any<String>(), any()) } returns 0
        every { mockAuthRepository.currentUser } returns mockCurrentUserFlow
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkStatic(android.util.Log::class)
    }

    // ==================== loadProfile() Tests ====================

    @Test
    fun `loadProfile with authenticated user loads profile successfully`() = runTest {
        // Given
        mockCurrentUserFlow.value = testUser
        coEvery { mockUserProfileRepository.getUserProfile(testUser.id) } returns 
            flowOf(Result.success(testProfile))

        // When
        viewModel = UserProfileViewModel(mockUserProfileRepository, mockAuthRepository)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertEquals(testProfile, state.profile)
        assertFalse(state.isLoading)
        assertNull(state.error)
        // Note: fullName, age, etc are separate editable fields in UI state, not synced from profile
    }

    @Test
    fun `loadProfile with no user shows authentication error`() = runTest {
        // Given
        mockCurrentUserFlow.value = null
        
        // When
        viewModel = UserProfileViewModel(mockUserProfileRepository, mockAuthRepository)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Please sign in to view your profile", state.error)
        assertNull(state.profile)
    }

    @Test
    fun `loadProfile with repository error shows error message`() = runTest {
        // Given
        mockCurrentUserFlow.value = testUser
        val errorMessage = "Network error"
        coEvery { mockUserProfileRepository.getUserProfile(testUser.id) } returns 
            flowOf(Result.failure(Exception(errorMessage)))

        // When
        viewModel = UserProfileViewModel(mockUserProfileRepository, mockAuthRepository)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(errorMessage, state.error)
        assertNull(state.profile)
    }

    @Test
    fun `loadProfile sets loading state initially`() = runTest {
        // Given
        mockCurrentUserFlow.value = testUser
        coEvery { mockUserProfileRepository.getUserProfile(testUser.id) } returns 
            flowOf(Result.success(testProfile))

        // When
        viewModel = UserProfileViewModel(mockUserProfileRepository, mockAuthRepository)
        
        // Then - The ViewModel loads in init, so loading happens immediately
        // After advanceUntilIdle, it should be done loading
        advanceUntilIdle()
        
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)  // Should be done loading now
        assertNotNull(state.profile)  // And should have loaded the profile
    }

    // ==================== Update Field Tests ====================

    @Test
    fun `updateFullName updates state correctly`() = runTest {
        // Given
        mockCurrentUserFlow.value = testUser
        coEvery { mockUserProfileRepository.getUserProfile(testUser.id) } returns 
            flowOf(Result.success(testProfile))
        viewModel = UserProfileViewModel(mockUserProfileRepository, mockAuthRepository)
        advanceUntilIdle()

        // When
        val newName = "Jane Doe"
        viewModel.updateFullName(newName)

        // Then
        val state = viewModel.uiState.value
        assertEquals(newName, state.fullName)
        assertNull(state.error) // Error should be cleared
    }

    @Test
    fun `updateAge with valid number updates state`() = runTest {
        // Given
        mockCurrentUserFlow.value = testUser
        coEvery { mockUserProfileRepository.getUserProfile(testUser.id) } returns 
            flowOf(Result.success(testProfile))
        viewModel = UserProfileViewModel(mockUserProfileRepository, mockAuthRepository)
        advanceUntilIdle()

        // When
        viewModel.updateAge("22")

        // Then
        assertEquals(22, viewModel.uiState.value.age)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `updateAge with invalid input sets null`() = runTest {
        // Given
        mockCurrentUserFlow.value = testUser
        coEvery { mockUserProfileRepository.getUserProfile(testUser.id) } returns 
            flowOf(Result.success(testProfile))
        viewModel = UserProfileViewModel(mockUserProfileRepository, mockAuthRepository)
        advanceUntilIdle()

        // When
        viewModel.updateAge("invalid")

        // Then
        assertNull(viewModel.uiState.value.age)
    }

    @Test
    fun `updateGender updates state correctly`() = runTest {
        // Given
        mockCurrentUserFlow.value = testUser
        coEvery { mockUserProfileRepository.getUserProfile(testUser.id) } returns 
            flowOf(Result.success(testProfile))
        viewModel = UserProfileViewModel(mockUserProfileRepository, mockAuthRepository)
        advanceUntilIdle()

        // When
        viewModel.updateGender(Gender.FEMALE)

        // Then
        assertEquals(Gender.FEMALE, viewModel.uiState.value.gender)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `updateEntryType updates state correctly`() = runTest {
        // Given
        mockCurrentUserFlow.value = testUser
        coEvery { mockUserProfileRepository.getUserProfile(testUser.id) } returns 
            flowOf(Result.success(testProfile))
        viewModel = UserProfileViewModel(mockUserProfileRepository, mockAuthRepository)
        advanceUntilIdle()

        // When
        viewModel.updateEntryType(EntryType.GRADUATE)

        // Then
        assertEquals(EntryType.GRADUATE, viewModel.uiState.value.entryType)
        assertNull(viewModel.uiState.value.error)
    }

    // ==================== saveProfile() Validation Tests ====================

    @Test
    fun `saveProfile with valid data saves successfully`() = runTest {
        // Given
        mockCurrentUserFlow.value = testUser
        coEvery { mockUserProfileRepository.getUserProfile(testUser.id) } returns 
            flowOf(Result.success(null))
        coEvery { mockUserProfileRepository.saveUserProfile(any()) } returns 
            Result.success(Unit)
        
        viewModel = UserProfileViewModel(mockUserProfileRepository, mockAuthRepository)
        advanceUntilIdle()

        // Set valid profile data
        viewModel.updateFullName("John Doe")
        viewModel.updateAge("25")
        viewModel.updateGender(Gender.MALE)
        viewModel.updateEntryType(EntryType.ENTRY_10_PLUS_2)

        // When
        viewModel.saveProfile()
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertTrue(state.isSaved)
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertNotNull(state.profile)
        
        coVerify { mockUserProfileRepository.saveUserProfile(any()) }
    }

    @Test
    fun `saveProfile with blank name shows validation error`() = runTest {
        // Given
        mockCurrentUserFlow.value = testUser
        coEvery { mockUserProfileRepository.getUserProfile(testUser.id) } returns 
            flowOf(Result.success(null))
        viewModel = UserProfileViewModel(mockUserProfileRepository, mockAuthRepository)
        advanceUntilIdle()

        viewModel.updateFullName("")  // Blank name
        viewModel.updateAge("25")
        viewModel.updateGender(Gender.MALE)
        viewModel.updateEntryType(EntryType.ENTRY_10_PLUS_2)

        // When
        viewModel.saveProfile()
        advanceUntilIdle()

        // Then
        assertEquals("Full name is required", viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isSaved)
        
        coVerify(exactly = 0) { mockUserProfileRepository.saveUserProfile(any()) }
    }

    @Test
    fun `saveProfile with null age shows validation error`() = runTest {
        // Given
        mockCurrentUserFlow.value = testUser
        coEvery { mockUserProfileRepository.getUserProfile(testUser.id) } returns 
            flowOf(Result.success(null))
        viewModel = UserProfileViewModel(mockUserProfileRepository, mockAuthRepository)
        advanceUntilIdle()

        viewModel.updateFullName("John Doe")
        // Don't set age (remains null)
        viewModel.updateGender(Gender.MALE)
        viewModel.updateEntryType(EntryType.ENTRY_10_PLUS_2)

        // When
        viewModel.saveProfile()
        advanceUntilIdle()

        // Then
        assertEquals("Age is required", viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isSaved)
    }

    @Test
    fun `saveProfile with age below 18 shows validation error`() = runTest {
        // Given
        mockCurrentUserFlow.value = testUser
        coEvery { mockUserProfileRepository.getUserProfile(testUser.id) } returns 
            flowOf(Result.success(null))
        viewModel = UserProfileViewModel(mockUserProfileRepository, mockAuthRepository)
        advanceUntilIdle()

        viewModel.updateFullName("John Doe")
        viewModel.updateAge("17")  // Below minimum
        viewModel.updateGender(Gender.MALE)
        viewModel.updateEntryType(EntryType.ENTRY_10_PLUS_2)

        // When
        viewModel.saveProfile()
        advanceUntilIdle()

        // Then
        assertEquals("Age must be between 18 and 35", viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isSaved)
    }

    @Test
    fun `saveProfile with age above 35 shows validation error`() = runTest {
        // Given
        mockCurrentUserFlow.value = testUser
        coEvery { mockUserProfileRepository.getUserProfile(testUser.id) } returns 
            flowOf(Result.success(null))
        viewModel = UserProfileViewModel(mockUserProfileRepository, mockAuthRepository)
        advanceUntilIdle()

        viewModel.updateFullName("John Doe")
        viewModel.updateAge("36")  // Above maximum
        viewModel.updateGender(Gender.MALE)
        viewModel.updateEntryType(EntryType.ENTRY_10_PLUS_2)

        // When
        viewModel.saveProfile()
        advanceUntilIdle()

        // Then
        assertEquals("Age must be between 18 and 35", viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isSaved)
    }

    @Test
    fun `saveProfile with null gender shows validation error`() = runTest {
        // Given
        mockCurrentUserFlow.value = testUser
        coEvery { mockUserProfileRepository.getUserProfile(testUser.id) } returns 
            flowOf(Result.success(null))
        viewModel = UserProfileViewModel(mockUserProfileRepository, mockAuthRepository)
        advanceUntilIdle()

        viewModel.updateFullName("John Doe")
        viewModel.updateAge("25")
        // Don't set gender (remains null)
        viewModel.updateEntryType(EntryType.ENTRY_10_PLUS_2)

        // When
        viewModel.saveProfile()
        advanceUntilIdle()

        // Then
        assertEquals("Gender is required", viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isSaved)
    }

    @Test
    fun `saveProfile with null entryType shows validation error`() = runTest {
        // Given
        mockCurrentUserFlow.value = testUser
        coEvery { mockUserProfileRepository.getUserProfile(testUser.id) } returns 
            flowOf(Result.success(null))
        viewModel = UserProfileViewModel(mockUserProfileRepository, mockAuthRepository)
        advanceUntilIdle()

        viewModel.updateFullName("John Doe")
        viewModel.updateAge("25")
        viewModel.updateGender(Gender.MALE)
        // Don't set entryType (remains null)

        // When
        viewModel.saveProfile()
        advanceUntilIdle()

        // Then
        assertEquals("Entry type is required", viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isSaved)
    }

    // ==================== saveProfile() Repository Tests ====================

    @Test
    fun `saveProfile calls updateUserProfile when profile exists`() = runTest {
        // Given
        mockCurrentUserFlow.value = testUser
        coEvery { mockUserProfileRepository.getUserProfile(testUser.id) } returns 
            flowOf(Result.success(testProfile))  // Existing profile
        coEvery { mockUserProfileRepository.updateUserProfile(any()) } returns 
            Result.success(Unit)
        
        viewModel = UserProfileViewModel(mockUserProfileRepository, mockAuthRepository)
        advanceUntilIdle()

        // Update all required fields (since they're not auto-populated from loaded profile)
        viewModel.updateFullName("Updated Name")
        viewModel.updateAge("26")
        viewModel.updateGender(Gender.MALE)
        viewModel.updateEntryType(EntryType.GRADUATE)

        // When
        viewModel.saveProfile()
        advanceUntilIdle()

        // Then
        coVerify { mockUserProfileRepository.updateUserProfile(any()) }
        coVerify(exactly = 0) { mockUserProfileRepository.saveUserProfile(any()) }
        assertTrue(viewModel.uiState.value.isSaved)
    }

    @Test
    fun `saveProfile calls saveUserProfile when profile does not exist`() = runTest {
        // Given
        mockCurrentUserFlow.value = testUser
        coEvery { mockUserProfileRepository.getUserProfile(testUser.id) } returns 
            flowOf(Result.success(null))  // No existing profile
        coEvery { mockUserProfileRepository.saveUserProfile(any()) } returns 
            Result.success(Unit)
        
        viewModel = UserProfileViewModel(mockUserProfileRepository, mockAuthRepository)
        advanceUntilIdle()

        viewModel.updateFullName("John Doe")
        viewModel.updateAge("25")
        viewModel.updateGender(Gender.MALE)
        viewModel.updateEntryType(EntryType.ENTRY_10_PLUS_2)

        // When
        viewModel.saveProfile()
        advanceUntilIdle()

        // Then
        coVerify { mockUserProfileRepository.saveUserProfile(any()) }
        coVerify(exactly = 0) { mockUserProfileRepository.updateUserProfile(any()) }
        assertTrue(viewModel.uiState.value.isSaved)
    }

    @Test
    fun `saveProfile with repository error shows error message`() = runTest {
        // Given
        mockCurrentUserFlow.value = testUser
        coEvery { mockUserProfileRepository.getUserProfile(testUser.id) } returns 
            flowOf(Result.success(null))
        val errorMessage = "Network error"
        coEvery { mockUserProfileRepository.saveUserProfile(any()) } returns 
            Result.failure(Exception(errorMessage))
        
        viewModel = UserProfileViewModel(mockUserProfileRepository, mockAuthRepository)
        advanceUntilIdle()

        viewModel.updateFullName("John Doe")
        viewModel.updateAge("25")
        viewModel.updateGender(Gender.MALE)
        viewModel.updateEntryType(EntryType.ENTRY_10_PLUS_2)

        // When
        viewModel.saveProfile()
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(errorMessage, state.error)
        assertFalse(state.isSaved)
    }

    @Test
    fun `saveProfile without authenticated user shows error`() = runTest {
        // Given
        mockCurrentUserFlow.value = null  // Not authenticated
        viewModel = UserProfileViewModel(mockUserProfileRepository, mockAuthRepository)
        advanceUntilIdle()

        viewModel.updateFullName("John Doe")
        viewModel.updateAge("25")
        viewModel.updateGender(Gender.MALE)
        viewModel.updateEntryType(EntryType.ENTRY_10_PLUS_2)

        // When
        viewModel.saveProfile()
        advanceUntilIdle()

        // Then
        assertEquals("Please sign in to save your profile", viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isSaved)
        
        coVerify(exactly = 0) { mockUserProfileRepository.saveUserProfile(any()) }
    }

    // ==================== resetSavedState() Test ====================

    @Test
    fun `resetSavedState clears isSaved flag`() = runTest {
        // Given
        mockCurrentUserFlow.value = testUser
        coEvery { mockUserProfileRepository.getUserProfile(testUser.id) } returns
            flowOf(Result.success(null))
        coEvery { mockUserProfileRepository.saveUserProfile(any()) } returns
            Result.success(Unit)

        viewModel = UserProfileViewModel(mockUserProfileRepository, mockAuthRepository)
        advanceUntilIdle()

        viewModel.updateFullName("John Doe")
        viewModel.updateAge("25")
        viewModel.updateGender(Gender.MALE)
        viewModel.updateEntryType(EntryType.ENTRY_10_PLUS_2)
        viewModel.saveProfile()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isSaved)  // Verify it's saved

        // When
        viewModel.resetSavedState()

        // Then
        assertFalse(viewModel.uiState.value.isSaved)
    }

    // ==================== Reactive Auth State Observation Tests ====================

    @Test
    fun `reactive auth state - profile loads when user signs in after ViewModel creation`() = runTest {
        // Given - Start with no user signed in
        mockCurrentUserFlow.value = null
        coEvery { mockUserProfileRepository.getUserProfile(testUser.id) } returns
            flowOf(Result.success(testProfile))

        // When - Create ViewModel (starts observing auth state)
        viewModel = UserProfileViewModel(mockUserProfileRepository, mockAuthRepository)
        advanceUntilIdle()

        // Initially no profile loaded
        assertNull(viewModel.uiState.value.profile)

        // User signs in (auth state changes)
        mockCurrentUserFlow.value = testUser
        advanceUntilIdle()

        // Then - Profile should be loaded automatically
        val state = viewModel.uiState.value
        assertEquals(testProfile, state.profile)
        assertFalse(state.isLoading)
        assertNull(state.error)
    }

    @Test
    fun `reactive auth state - profile clears when user signs out`() = runTest {
        // Given - Start with user signed in and profile loaded
        mockCurrentUserFlow.value = testUser
        coEvery { mockUserProfileRepository.getUserProfile(testUser.id) } returns
            flowOf(Result.success(testProfile))

        viewModel = UserProfileViewModel(mockUserProfileRepository, mockAuthRepository)
        advanceUntilIdle()

        // Verify profile is loaded
        assertEquals(testProfile, viewModel.uiState.value.profile)

        // When - User signs out (auth state changes to null)
        mockCurrentUserFlow.value = null
        advanceUntilIdle()

        // Then - Profile should be cleared
        val state = viewModel.uiState.value
        assertNull(state.profile)
        assertFalse(state.isLoading)
        assertEquals("Please sign in to view your profile", state.error)
    }

    @Test
    fun `reactive auth state - handles multiple auth state changes correctly`() = runTest {
        // Given - Start with no user
        mockCurrentUserFlow.value = null
        coEvery { mockUserProfileRepository.getUserProfile(testUser.id) } returns
            flowOf(Result.success(testProfile))

        viewModel = UserProfileViewModel(mockUserProfileRepository, mockAuthRepository)
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.profile)

        // When - User signs in
        mockCurrentUserFlow.value = testUser
        advanceUntilIdle()
        assertEquals(testProfile, viewModel.uiState.value.profile)

        // User signs out
        mockCurrentUserFlow.value = null
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.profile)

        // User signs in again (different user)
        val differentUser = testUser.copy(id = "different-user-456")
        coEvery { mockUserProfileRepository.getUserProfile(differentUser.id) } returns
            flowOf(Result.success(testProfile.copy(userId = differentUser.id, fullName = "Different User")))

        mockCurrentUserFlow.value = differentUser
        advanceUntilIdle()

        // Then - New profile should be loaded
        val state = viewModel.uiState.value
        assertEquals("Different User", state.profile?.fullName)
        assertEquals(differentUser.id, state.profile?.userId)
    }

    @Test
    fun `reactive auth state - handles profile loading errors gracefully`() = runTest {
        // Given - Start with no user
        mockCurrentUserFlow.value = null
        val errorMessage = "Network error"
        coEvery { mockUserProfileRepository.getUserProfile(testUser.id) } returns
            flowOf(Result.failure(Exception(errorMessage)))

        viewModel = UserProfileViewModel(mockUserProfileRepository, mockAuthRepository)
        advanceUntilIdle()

        // User signs in
        mockCurrentUserFlow.value = testUser
        advanceUntilIdle()

        // Then - Error should be handled gracefully
        val state = viewModel.uiState.value
        assertNull(state.profile)
        assertFalse(state.isLoading)
        assertEquals(errorMessage, state.error)
    }

    @Test
    fun `reactive auth state - profile data flows correctly to UI state`() = runTest {
        // Given - Complete profile with all fields
        val completeProfile = UserProfile(
            userId = testUser.id,
            fullName = "John Doe",
            age = 25,
            gender = Gender.MALE,
            entryType = EntryType.ENTRY_10_PLUS_2,
            subscriptionType = com.ssbmax.core.domain.model.SubscriptionType.PREMIUM
        )

        mockCurrentUserFlow.value = testUser
        coEvery { mockUserProfileRepository.getUserProfile(testUser.id) } returns
            flowOf(Result.success(completeProfile))

        // When - ViewModel observes auth state and loads profile
        viewModel = UserProfileViewModel(mockUserProfileRepository, mockAuthRepository)
        advanceUntilIdle()

        // Then - Profile data should be available in UI state
        val state = viewModel.uiState.value
        assertEquals(completeProfile, state.profile)
        assertEquals("John Doe", state.profile?.fullName)
        assertEquals(25, state.profile?.age)
        assertEquals(Gender.MALE, state.profile?.gender)
        assertEquals(EntryType.ENTRY_10_PLUS_2, state.profile?.entryType)
        assertEquals(com.ssbmax.core.domain.model.SubscriptionType.PREMIUM, state.profile?.subscriptionType)
    }

    @Test
    fun `reactive auth state - prevents race condition from synchronous access`() = runTest {
        // Given - Auth repository returns null initially, then user later
        mockCurrentUserFlow.value = null
        coEvery { mockUserProfileRepository.getUserProfile(testUser.id) } returns
            flowOf(Result.success(testProfile))

        // When - Create ViewModel (starts observing) before auth state is ready
        viewModel = UserProfileViewModel(mockUserProfileRepository, mockAuthRepository)
        advanceUntilIdle()

        // Verify no profile loaded yet (reactive observation waiting for auth state)
        assertNull(viewModel.uiState.value.profile)

        // Auth state becomes available later (simulating real app startup timing)
        mockCurrentUserFlow.value = testUser
        advanceUntilIdle()

        // Then - Profile should load automatically without manual refresh
        val state = viewModel.uiState.value
        assertEquals(testProfile, state.profile)
        assertFalse(state.isLoading)
    }

    @Test
    fun `backwards compatibility - loadProfile method still works when called manually`() = runTest {
        // Given - User is signed in
        mockCurrentUserFlow.value = testUser
        coEvery { mockUserProfileRepository.getUserProfile(testUser.id) } returns
            flowOf(Result.success(testProfile))

        viewModel = UserProfileViewModel(mockUserProfileRepository, mockAuthRepository)
        advanceUntilIdle()

        // Verify profile is loaded initially
        assertEquals(testProfile, viewModel.uiState.value.profile)

        // When - Manually call loadProfile (backwards compatibility)
        val updatedProfile = testProfile.copy(fullName = "Updated Name")
        coEvery { mockUserProfileRepository.getUserProfile(testUser.id) } returns
            flowOf(Result.success(updatedProfile))

        viewModel.loadProfile()
        advanceUntilIdle()

        // Then - Profile should be refreshed
        assertEquals("Updated Name", viewModel.uiState.value.profile?.fullName)
    }
}

