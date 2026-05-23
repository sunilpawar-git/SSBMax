package com.ssbmax.ui.premium

import android.app.Activity
import com.ssbmax.billing.BillingRepository
import com.ssbmax.core.domain.model.*
import com.ssbmax.core.domain.repository.UserProfileRepository
import com.ssbmax.core.domain.usecase.auth.ObserveCurrentUserUseCase
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class UpgradeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    
    private lateinit var observeCurrentUser: ObserveCurrentUserUseCase
    private lateinit var userProfileRepository: UserProfileRepository
    private lateinit var billingRepository: BillingRepository
    
    private lateinit var viewModel: UpgradeViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        observeCurrentUser = mockk(relaxed = true)
        userProfileRepository = mockk(relaxed = true)
        billingRepository = mockk(relaxed = true)
        
        val mockUser = SSBMaxUser(
            id = "user_123",
            email = "user@example.com",
            displayName = "User Name",
            role = UserRole.STUDENT
        )
        every { observeCurrentUser.invoke() } returns flowOf(mockUser)
        
        val mockProfile = UserProfile(
            userId = "user_123",
            fullName = "User Name",
            age = 25,
            gender = Gender.MALE,
            entryType = EntryType.GRADUATE,
            subscriptionType = SubscriptionType.FREE
        )
        every { userProfileRepository.getUserProfile("user_123") } returns flowOf(Result.success(mockProfile))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun initViewModel() {
        viewModel = UpgradeViewModel(
            observeCurrentUser = observeCurrentUser,
            userProfileRepository = userProfileRepository,
            billingRepository = billingRepository
        )
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @Test
    fun init_loadsCurrentSubscriptionAndPlans() {
        initViewModel()

        val state = viewModel.uiState.value
        assertEquals(SubscriptionTier.FREE, state.currentTier)
        assertEquals(4, state.availablePlans.size)
        assertFalse(state.isLoading)
    }

    @Test
    fun selectBillingCycle_updatesUiState() {
        initViewModel()

        viewModel.selectBillingCycle(BillingCycle.ANNUALLY)

        val state = viewModel.uiState.value
        assertEquals(BillingCycle.ANNUALLY, state.selectedBillingCycle)
    }

    @Test
    fun upgradeToPlan_withTier_showsComingSoonDialog() {
        initViewModel()

        viewModel.upgradeToPlan(SubscriptionTier.PREMIUM)

        val state = viewModel.uiState.value
        assertTrue(state.showComingSoonDialog)
        assertEquals(SubscriptionTier.PREMIUM, state.selectedPlanForUpgrade)

        viewModel.dismissComingSoonDialog()
        val dismissedState = viewModel.uiState.value
        assertFalse(dismissedState.showComingSoonDialog)
    }

    @Test
    fun upgradeToPlan_withActivity_triggersBillingRepositoryPurchase() {
        initViewModel()

        val mockActivity = mockk<Activity>(relaxed = true)
        viewModel.upgradeToPlan(mockActivity, SubscriptionTier.PREMIUM)

        verify { billingRepository.purchaseProduct(mockActivity, "premium_monthly") }
    }
}
