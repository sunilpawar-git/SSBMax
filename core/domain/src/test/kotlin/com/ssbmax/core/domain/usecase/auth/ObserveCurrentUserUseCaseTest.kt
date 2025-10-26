package com.ssbmax.core.domain.usecase.auth

import app.cash.turbine.test
import com.ssbmax.core.domain.model.SSBMaxUser
import com.ssbmax.core.domain.model.SubscriptionTier
import com.ssbmax.core.domain.model.UserRole
import com.ssbmax.core.domain.repository.AuthRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for ObserveCurrentUserUseCase
 * Tests real-time user observation logic
 */
class ObserveCurrentUserUseCaseTest {
    
    private lateinit var authRepository: AuthRepository
    private lateinit var useCase: ObserveCurrentUserUseCase
    
    @Before
    fun setUp() {
        authRepository = mockk()
        useCase = ObserveCurrentUserUseCase(authRepository)
    }
    
    @Test
    fun `invoke returns flow from repository`() = runTest {
        // Given
        val mockUser = SSBMaxUser(
            id = "user123",
            email = "test@ssbmax.com",
            displayName = "Test User",
            role = UserRole.STUDENT,
            subscriptionTier = SubscriptionTier.BASIC,
            subscription = null
        )
        every { authRepository.currentUser } returns flowOf(mockUser)
        
        // When
        useCase().test {
            // Then
            val emittedUser = awaitItem()
            assertEquals(mockUser, emittedUser)
            awaitComplete()
        }
    }
    
    @Test
    fun `invoke emits null when no user is signed in`() = runTest {
        // Given
        every { authRepository.currentUser } returns flowOf(null)
        
        // When
        useCase().test {
            // Then
            val emittedUser = awaitItem()
            assertNull(emittedUser)
            awaitComplete()
        }
    }
    
    @Test
    fun `invoke emits multiple user states`() = runTest {
        // Given
        val user1 = SSBMaxUser(
            id = "user1",
            email = "user1@ssbmax.com",
            displayName = "User One",
            role = UserRole.STUDENT,
            subscriptionTier = SubscriptionTier.BASIC,
            subscription = null
        )
        val user2 = SSBMaxUser(
            id = "user2",
            email = "user2@ssbmax.com",
            displayName = "User Two",
            role = UserRole.INSTRUCTOR,
            subscriptionTier = SubscriptionTier.PRO,
            subscription = null
        )
        every { authRepository.currentUser } returns flowOf(user1, null, user2)
        
        // When
        useCase().test {
            // Then
            assertEquals(user1, awaitItem())
            assertNull(awaitItem())
            assertEquals(user2, awaitItem())
            awaitComplete()
        }
    }
    
    @Test
    fun `invoke preserves user properties correctly`() = runTest {
        // Given
        val mockUser = SSBMaxUser(
            id = "user-premium",
            email = "premium@ssbmax.com",
            displayName = "Premium User",
            role = UserRole.STUDENT,
            subscriptionTier = SubscriptionTier.PRO,
            subscription = null
        )
        every { authRepository.currentUser } returns flowOf(mockUser)
        
        // When
        useCase().test {
            // Then
            val emittedUser = awaitItem()
            assertNotNull(emittedUser)
            assertEquals("user-premium", emittedUser?.id)
            assertEquals("premium@ssbmax.com", emittedUser?.email)
            assertEquals("Premium User", emittedUser?.displayName)
            assertEquals(UserRole.STUDENT, emittedUser?.role)
            assertEquals(SubscriptionTier.PRO, emittedUser?.subscriptionTier)
            awaitComplete()
        }
    }
}

