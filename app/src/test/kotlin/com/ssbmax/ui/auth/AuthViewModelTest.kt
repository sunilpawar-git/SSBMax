package com.ssbmax.ui.auth

import app.cash.turbine.test
import com.ssbmax.core.data.repository.AuthRepositoryImpl
import com.ssbmax.core.domain.model.SSBMaxUser
import com.ssbmax.core.domain.model.UserRole
import com.ssbmax.core.domain.repository.AuthRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

/**
 * Tests for AuthViewModel
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {
    
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: AuthViewModel
    private val mockRepository = mockk<AuthRepository>(relaxed = true)
    private val mockAuthRepositoryImpl = mockk<AuthRepositoryImpl>(relaxed = true)
    
    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = AuthViewModel(mockRepository, mockAuthRepositoryImpl)
    }
    
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
    
    @Test
    fun `signIn with valid credentials shows error for deprecated method`() = runTest {
        // When
        viewModel.uiState.test {
            assertEquals(AuthUiState.Initial, awaitItem())
            
            viewModel.signIn("test@example.com", "password123")
            
            assertEquals(AuthUiState.Loading, awaitItem())
            val errorState = awaitItem() as AuthUiState.Error
            assertTrue(errorState.message.contains("not yet implemented"))
        }
    }
    
    @Test
    fun `signIn with invalid credentials shows validation error`() = runTest {
        // When
        viewModel.uiState.test {
            assertEquals(AuthUiState.Initial, awaitItem())
            
            viewModel.signIn("test@example.com", "12345")
            
            val errorState = awaitItem() as AuthUiState.Error
            assertTrue(errorState.message.contains("valid"))
        }
    }
    
    @Test
    fun `signIn with invalid email shows validation error`() = runTest {
        // When
        viewModel.uiState.test {
            assertEquals(AuthUiState.Initial, awaitItem())
            
            viewModel.signIn("invalidemail", "password123")
            
            val errorState = awaitItem() as AuthUiState.Error
            assertTrue(errorState.message.contains("valid email"))
        }
    }
    
    @Test
    fun `signIn with short password shows validation error`() = runTest {
        // When
        viewModel.uiState.test {
            assertEquals(AuthUiState.Initial, awaitItem())
            
            viewModel.signIn("test@example.com", "12345")
            
            val errorState = awaitItem() as AuthUiState.Error
            assertTrue(errorState.message.contains("valid"))
        }
    }
    
    @Test
    fun `signUp with valid data shows error for deprecated method`() = runTest {
        // When
        viewModel.uiState.test {
            assertEquals(AuthUiState.Initial, awaitItem())
            
            viewModel.signUp("new@example.com", "password123", "New User")
            
            assertEquals(AuthUiState.Loading, awaitItem())
            val errorState = awaitItem() as AuthUiState.Error
            assertTrue(errorState.message.contains("not yet implemented"))
        }
    }
    
    @Test
    fun `signUp with blank display name shows error`() = runTest {
        // When
        viewModel.uiState.test {
            assertEquals(AuthUiState.Initial, awaitItem())
            
            viewModel.signUp("test@example.com", "password123", "")
            
            val errorState = awaitItem() as AuthUiState.Error
            assertTrue(errorState.message.contains("fill all fields"))
        }
    }
    
    @Test
    fun `resetState returns to initial`() = runTest {
        // Given
        coEvery { mockRepository.signIn(any(), any()) } returns Result.failure(Exception("Error"))
        
        viewModel.uiState.test {
            awaitItem() // Initial
            
            viewModel.signIn("test@example.com", "password")
            awaitItem() // Loading
            awaitItem() // Error
            
            // When
            viewModel.resetState()
            
            // Then
            assertEquals(AuthUiState.Initial, awaitItem())
        }
    }
}

