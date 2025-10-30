package com.ssbmax.ui.auth

import android.content.Intent
import app.cash.turbine.test
import com.ssbmax.core.data.repository.AuthRepositoryImpl
import com.ssbmax.core.domain.model.SSBMaxUser
import com.ssbmax.core.domain.model.UserRole
import com.ssbmax.core.domain.repository.AuthRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

/**
 * Tests for AuthViewModel with Google Sign-In
 * 
 * Note: Email/password authentication has been removed.
 * The app now exclusively uses Google Sign-In.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {
    
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: AuthViewModel
    private val mockRepository = mockk<AuthRepository>(relaxed = true)
    private val mockAuthRepositoryImpl = mockk<AuthRepositoryImpl>(relaxed = true)
    private val mockCurrentUserFlow = MutableStateFlow<SSBMaxUser?>(null)
    
    private val mockUser = SSBMaxUser(
        id = "test-user-123",
        email = "test@example.com",
        displayName = "Test User",
        photoUrl = null,
        role = UserRole.STUDENT,
        createdAt = System.currentTimeMillis(),
        lastLoginAt = System.currentTimeMillis()
    )
    
    @Before
    fun setup() {
        // CRITICAL: Set Main dispatcher BEFORE creating ViewModel
        Dispatchers.setMain(testDispatcher)
        
        // Mock currentUser as StateFlow
        every { mockRepository.currentUser } returns mockCurrentUserFlow
        
        // Create ViewModel AFTER setting test dispatcher
        viewModel = AuthViewModel(mockRepository, mockAuthRepositoryImpl)
    }
    
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
    
    // ==================== Google Sign-In Tests ====================
    
    @Test
    fun `getGoogleSignInIntent returns valid intent`() = runTest {
        // Given
        val mockIntent = mockk<Intent>(relaxed = true)
        every { mockAuthRepositoryImpl.getGoogleSignInIntent() } returns mockIntent
        
        // When
        val intent = viewModel.getGoogleSignInIntent()
        
        // Then
        assertNotNull("Google Sign-In intent should not be null", intent)
        verify { mockAuthRepositoryImpl.getGoogleSignInIntent() }
    }
    
    @Test
    fun `handleGoogleSignInResult with success shows success state`() = runTest {
        // Given
        val mockIntent = mockk<Intent>(relaxed = true)
        val currentTime = System.currentTimeMillis()
        val existingUser = mockUser.copy(
            role = UserRole.INSTRUCTOR, // Different role to avoid new user check
            createdAt = currentTime - 86400000, // Created 1 day ago
            lastLoginAt = currentTime
        )
        coEvery { mockAuthRepositoryImpl.handleGoogleSignInResult(mockIntent) } returns Result.success(existingUser)
        
        // When
        viewModel.handleGoogleSignInResult(mockIntent)
        testDispatcher.scheduler.advanceUntilIdle() // Wait for coroutines to complete
        
        // Then
        val state = viewModel.uiState.value
        assertTrue("Should be success state, got: $state", state is AuthUiState.Success)
        if (state is AuthUiState.Success) {
            assertEquals(existingUser.id, state.user.id)
            assertEquals(existingUser.email, state.user.email)
        }
    }
    
    @Test
    fun `handleGoogleSignInResult with new user shows needs role selection`() = runTest {
        // Given
        val mockIntent = mockk<Intent>(relaxed = true)
        val timestamp = System.currentTimeMillis()
        val newUser = mockUser.copy(
            role = UserRole.STUDENT, // Important: must be STUDENT
            createdAt = timestamp,
            lastLoginAt = timestamp // Same as createdAt (first login)
        )
        coEvery { mockAuthRepositoryImpl.handleGoogleSignInResult(mockIntent) } returns Result.success(newUser)
        
        // When
        viewModel.handleGoogleSignInResult(mockIntent)
        testDispatcher.scheduler.advanceUntilIdle() // Wait for coroutines to complete
        
        // Then
        val state = viewModel.uiState.value
        assertTrue("Should be needs role selection state, got: $state", state is AuthUiState.NeedsRoleSelection)
        if (state is AuthUiState.NeedsRoleSelection) {
            assertEquals(newUser.id, state.user.id)
        }
    }
    
    @Test
    fun `handleGoogleSignInResult with null intent shows error`() = runTest {
        // Given
        coEvery { mockAuthRepositoryImpl.handleGoogleSignInResult(null) } returns Result.failure(
            Exception("Google Sign-In failed")
        )
        
        // When
        viewModel.handleGoogleSignInResult(null)
        testDispatcher.scheduler.advanceUntilIdle() // Wait for coroutines to complete
        
        // Then
        val state = viewModel.uiState.value
        assertTrue("Should be error state, got: $state", state is AuthUiState.Error)
        if (state is AuthUiState.Error) {
            assertTrue(state.message.contains("Google Sign-In failed"))
        }
    }
    
    @Test
    fun `handleGoogleSignInResult with failure shows error state`() = runTest {
        // Given
        val mockIntent = mockk<Intent>(relaxed = true)
        coEvery { mockAuthRepositoryImpl.handleGoogleSignInResult(mockIntent) } returns Result.failure(
            Exception("Authentication error")
        )
        
        // When
        viewModel.handleGoogleSignInResult(mockIntent)
        testDispatcher.scheduler.advanceUntilIdle() // Wait for coroutines to complete
        
        // Then
        val state = viewModel.uiState.value
        assertTrue("Should be error state, got: $state", state is AuthUiState.Error)
        if (state is AuthUiState.Error) {
            assertTrue(state.message.contains("Authentication error"))
        }
    }
    
    // ==================== Role Selection Tests ====================
    
    @Test
    fun `setUserRole with student role updates user`() = runTest {
        // Given
        val updatedUser = mockUser.copy(role = UserRole.STUDENT)
        mockCurrentUserFlow.value = updatedUser // Set before calling function
        coEvery { mockAuthRepositoryImpl.updateUserRole(UserRole.STUDENT) } returns Result.success(Unit)
        
        // When
        viewModel.setUserRole(UserRole.STUDENT)
        testDispatcher.scheduler.advanceUntilIdle() // Wait for coroutines to complete
        
        // Then
        val state = viewModel.uiState.value
        assertTrue("Should be success state, got: $state", state is AuthUiState.Success)
        if (state is AuthUiState.Success) {
            assertEquals(UserRole.STUDENT, state.user.role)
        }
    }
    
    @Test
    fun `setUserRole with instructor role updates user`() = runTest {
        // Given
        val updatedUser = mockUser.copy(role = UserRole.INSTRUCTOR)
        mockCurrentUserFlow.value = updatedUser // Set before calling function
        coEvery { mockAuthRepositoryImpl.updateUserRole(UserRole.INSTRUCTOR) } returns Result.success(Unit)
        
        // When
        viewModel.setUserRole(UserRole.INSTRUCTOR)
        testDispatcher.scheduler.advanceUntilIdle() // Wait for coroutines to complete
        
        // Then
        val state = viewModel.uiState.value
        assertTrue("Should be success state, got: $state", state is AuthUiState.Success)
        if (state is AuthUiState.Success) {
            assertEquals(UserRole.INSTRUCTOR, state.user.role)
        }
    }
    
    @Test
    fun `setUserRole with failure shows error`() = runTest {
        // Given
        coEvery { mockAuthRepositoryImpl.updateUserRole(any()) } returns Result.failure(
            Exception("Failed to set role")
        )
        
        // When
        viewModel.setUserRole(UserRole.STUDENT)
        testDispatcher.scheduler.advanceUntilIdle() // Wait for coroutines to complete
        
        // Then
        val state = viewModel.uiState.value
        assertTrue("Should be error state, got: $state", state is AuthUiState.Error)
        if (state is AuthUiState.Error) {
            assertTrue(state.message.contains("Failed to set role"))
        }
    }
    
    // ==================== Sign Out Tests ====================
    
    @Test
    fun `signOut returns to initial state`() = runTest {
        // Given
        coEvery { mockRepository.signOut() } returns Result.success(Unit)
        
        // When
        viewModel.uiState.test {
            assertEquals(AuthUiState.Initial, awaitItem())
            
            viewModel.signOut()
            
            // Then - should stay at Initial state
            expectNoEvents() // No new events since already at Initial
        }
    }
    
    // ==================== Reset State Tests ====================
    
    @Test
    fun `resetState returns to initial`() = runTest {
        // Given - simulate an error state first
        val mockIntent = mockk<Intent>(relaxed = true)
        coEvery { mockAuthRepositoryImpl.handleGoogleSignInResult(mockIntent) } returns Result.failure(
            Exception("Error")
        )
        
        viewModel.handleGoogleSignInResult(mockIntent)
        testDispatcher.scheduler.advanceUntilIdle() // Wait for error state to set
        
        // Verify we're in error state
        val errorState = viewModel.uiState.value
        assertTrue("Should be in error state, got: $errorState", errorState is AuthUiState.Error)
        
        // When
        viewModel.resetState()
        
        // Then
        val finalState = viewModel.uiState.value
        assertEquals("Should return to initial state", AuthUiState.Initial, finalState)
    }
    
    @Test
    fun `initial state is Initial`() = runTest {
        // When/Then
        viewModel.uiState.test {
            assertEquals(AuthUiState.Initial, awaitItem())
        }
    }
}
