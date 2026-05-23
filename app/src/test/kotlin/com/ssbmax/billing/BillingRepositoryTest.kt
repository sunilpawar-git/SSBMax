package com.ssbmax.billing

import android.app.Activity
import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.ssbmax.core.domain.model.SubscriptionType
import com.ssbmax.core.domain.model.UserProfile
import com.ssbmax.core.domain.repository.UserProfileRepository
import io.mockk.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BillingRepositoryTest {

    private lateinit var context: Context
    private lateinit var billingClient: SSBMaxBillingClient
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var userProfileRepository: UserProfileRepository
    private lateinit var externalScope: CoroutineScope
    
    private lateinit var repository: BillingRepository
    
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Before
    fun setup() {
        context = mockk(relaxed = true)
        billingClient = mockk(relaxed = true)
        firebaseAuth = mockk(relaxed = true)
        userProfileRepository = mockk(relaxed = true)
        externalScope = CoroutineScope(UnconfinedTestDispatcher())
        
        repository = BillingRepository(
            context = context,
            billingClient = billingClient,
            firebaseAuth = firebaseAuth,
            userProfileRepository = userProfileRepository,
            externalScope = externalScope
        )
    }

    @Test
    fun initialize_populatesMockProducts() {
        assertTrue(repository.products.value.isEmpty())

        repository.initialize()

        val products = repository.products.value
        assertEquals(2, products.size)
        assertEquals("premium_monthly", products[0].id)
        assertEquals("premium_yearly", products[1].id)
        verify { billingClient.setListener(any()) }
        verify { billingClient.initialize() }
    }

    @Test
    fun purchaseProduct_launchesBillingFlow() {
        repository.initialize()

        val activity = mockk<Activity>(relaxed = true)
        repository.purchaseProduct(activity = activity, productId = "premium_monthly")

        verify { billingClient.launchPurchaseFlow(activity, "premium_monthly") }
    }

    @Test
    fun handlePurchase_verifiesAndUpdatesProfile() = runTest {
        val listenerSlot = slot<com.android.billingclient.api.PurchasesUpdatedListener>()
        every { billingClient.setListener(capture(listenerSlot)) } just Runs
        
        val mockUser = mockk<FirebaseUser>()
        every { mockUser.uid } returns "test_user_id"
        every { firebaseAuth.currentUser } returns mockUser
        
        val mockProfile = UserProfile(
            userId = "test_user_id",
            fullName = "Test User",
            age = 22,
            gender = com.ssbmax.core.domain.model.Gender.MALE,
            entryType = com.ssbmax.core.domain.model.EntryType.GRADUATE,
            subscriptionType = SubscriptionType.FREE
        )
        every { userProfileRepository.getUserProfile("test_user_id") } returns flowOf(Result.success(mockProfile))
        coEvery { userProfileRepository.updateUserProfile(any()) } returns Result.success(Unit)
        coEvery { billingClient.verifyPurchase(any()) } returns Result.success(true)

        repository.initialize()

        val mockPurchase = mockk<com.android.billingclient.api.Purchase>(relaxed = true)
        every { mockPurchase.purchaseToken } returns "mock_token_123"
        every { mockPurchase.products } returns listOf("premium_yearly")
        
        val billingResult = com.android.billingclient.api.BillingResult.newBuilder()
            .setResponseCode(com.android.billingclient.api.BillingClient.BillingResponseCode.OK)
            .build()

        listenerSlot.captured.onPurchasesUpdated(billingResult, listOf(mockPurchase))

        coVerify(timeout = 3000) { userProfileRepository.updateUserProfile(match { 
            it.subscriptionType == SubscriptionType.PREMIUM
        }) }
        assertTrue(repository.premiumStatus.value)
    }
}
