package com.ssbmax.ui.settings

import com.ssbmax.core.domain.model.SSBMaxUser
import com.ssbmax.core.domain.model.SubscriptionTier
import com.ssbmax.core.domain.model.UserRole
import com.ssbmax.core.domain.repository.UsageInfo
import com.ssbmax.core.domain.usecase.auth.ObserveCurrentUserUseCase
import com.ssbmax.core.domain.usecase.subscription.GetMonthlyUsageUseCase
import com.ssbmax.core.domain.usecase.subscription.GetSubscriptionTierUseCase
import com.ssbmax.testing.TestDispatcherRule
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*

/**
 * Tests for SubscriptionManagementViewModel
 * UPDATED: Now tests use cases instead of direct Firebase dependencies
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SubscriptionManagementViewModelTest {
    
    @get:Rule
    val dispatcherRule = TestDispatcherRule()
    
    private lateinit var viewModel: SubscriptionManagementViewModel
    private val mockObserveCurrentUser = mockk<ObserveCurrentUserUseCase>()
    private val mockGetSubscriptionTier = mockk<GetSubscriptionTierUseCase>()
    private val mockGetMonthlyUsage = mockk<GetMonthlyUsageUseCase>()
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
        // Mock Android Log
        mockkStatic(android.util.Log::class)
        every { android.util.Log.d(any(), any()) } returns 0
        every { android.util.Log.w(any(), any<String>(), any()) } returns 0
        every { android.util.Log.e(any(), any(), any()) } returns 0
        
        // Mock current user flow
        every { mockObserveCurrentUser() } returns mockCurrentUserFlow
        
        viewModel = SubscriptionManagementViewModel(
            mockObserveCurrentUser,
            mockGetSubscriptionTier,
            mockGetMonthlyUsage
        )
    }
    
    @Test
    fun `initial state is loading false`() {
        val state = viewModel.uiState.value
        assertFalse("Initial loading should be false", state.isLoading)
        assertEquals("Initial tier should be FREE", SubscriptionTierModel.FREE, state.currentTier)
        assertTrue("Initial usage should be empty", state.monthlyUsage.isEmpty())
        assertNull("Initial error should be null", state.error)
    }
    
    @Test
    fun `loadSubscriptionData with success updates state`() = runTest {
        // Given
        mockCurrentUserFlow.value = mockUser
        val mockUsage = mapOf(
            "OIR Tests" to UsageInfo(used = 1, limit = 5),
            "PPDT Tests" to UsageInfo(used = 2, limit = 5)
        )
        
        coEvery { mockGetSubscriptionTier(mockUser.id) } returns Result.success(SubscriptionTier.PRO)
        coEvery { mockGetMonthlyUsage(mockUser.id) } returns Result.success(mockUsage)
        
        // When
        viewModel.loadSubscriptionData()
        
        // Wait for async operation
        kotlinx.coroutines.delay(100)
        
        // Then
        val state = viewModel.uiState.value
        assertFalse("Loading should be false after completion", state.isLoading)
        assertEquals("Tier should be PRO", SubscriptionTierModel.PRO, state.currentTier)
        assertEquals("Usage map should have 2 entries", 2, state.monthlyUsage.size)
        assertNull("Error should be null on success", state.error)
    }
    
    @Test
    fun `loadSubscriptionData with no user shows error`() = runTest {
        // Given
        mockCurrentUserFlow.value = null
        
        // When
        viewModel.loadSubscriptionData()
        
        // Wait for async operation
        kotlinx.coroutines.delay(100)
        
        // Then
        val state = viewModel.uiState.value
        assertFalse("Loading should be false", state.isLoading)
        assertNotNull("Error should not be null", state.error)
        assertTrue("Error should mention authentication", state.error?.contains("User not authenticated") == true)
    }
    
    @Test
    fun `loadSubscriptionData with tier failure shows error`() = runTest {
        // Given
        mockCurrentUserFlow.value = mockUser
        coEvery { mockGetSubscriptionTier(mockUser.id) } returns Result.failure(
            Exception("Failed to load tier")
        )
        
        // When
        viewModel.loadSubscriptionData()
        
        // Wait for async operation
        kotlinx.coroutines.delay(100)
        
        // Then
        val state = viewModel.uiState.value
        assertFalse("Loading should be false", state.isLoading)
        assertNotNull("Error should not be null", state.error)
        assertTrue("Error should mention tier", state.error?.contains("Failed to load subscription tier") == true)
    }
}
