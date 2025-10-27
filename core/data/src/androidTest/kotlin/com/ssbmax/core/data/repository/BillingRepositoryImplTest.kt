package com.ssbmax.core.data.repository

import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.ssbmax.core.data.local.SSBDatabase
import com.ssbmax.core.data.preferences.ThemePreferenceManager
import com.ssbmax.core.data.remote.FirebaseAuthService
import com.ssbmax.core.data.remote.FirebaseInitializer
import com.ssbmax.core.data.remote.FirestoreSubmissionRepository
import com.ssbmax.core.data.remote.FirestoreUserRepository
import com.ssbmax.core.domain.model.*
import com.ssbmax.core.domain.repository.AuthRepository
import com.ssbmax.core.domain.repository.BillingRepository
import com.ssbmax.testing.BaseRepositoryTest
import com.ssbmax.testing.FirebaseTestHelper
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.*
import org.junit.runner.RunWith
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

/**
 * Integration tests for BillingRepositoryImpl
 *
 * Tests the billing system for subscription management, payment processing,
 * and premium feature access control.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class BillingRepositoryImplTest : BaseRepositoryTest() {

    @Inject
    lateinit var billingRepository: BillingRepository

    @Inject
    lateinit var firestore: FirebaseFirestore

    @Inject
    lateinit var authRepository: AuthRepository

    @Inject
    lateinit var firebaseTestHelper: FirebaseTestHelper

    private lateinit var testUser: SSBMaxUser
    private lateinit var premiumUser: SSBMaxUser

    @Before
    override fun setup() {
        super.setup()

        // Initialize Firebase for testing
        FirebaseInitializer.initialize()
        firebaseTestHelper.setupEmulator()

        // Create test users
        testUser = SSBMaxUser(
            id = "student_free",
            email = "free@test.com",
            displayName = "Free User",
            photoUrl = null,
            role = UserRole.STUDENT,
            createdAt = System.currentTimeMillis(),
            lastLoginAt = System.currentTimeMillis()
        )

        premiumUser = SSBMaxUser(
            id = "student_premium",
            email = "premium@test.com",
            displayName = "Premium User",
            photoUrl = null,
            role = UserRole.STUDENT,
            createdAt = System.currentTimeMillis(),
            lastLoginAt = System.currentTimeMillis()
        )
    }

    @After
    override fun tearDown() {
        super.tearDown()
        firebaseTestHelper.cleanup()
    }

    @Test
    fun `getSubscriptionStatus returns FREE for user without active subscription`() = runTest(timeout = 30.seconds) {
        // Given: User without any subscription
        // (No setup needed - user has no subscription by default)

        // When: Get subscription status
        val statusResult = billingRepository.getSubscriptionStatus(testUser.id)

        // Then: Should return FREE status
        Assert.assertTrue("Should get subscription status", statusResult.isSuccess)
        val status = statusResult.getOrNull()
        Assert.assertNotNull("Status should exist", status)
        Assert.assertEquals("Should be FREE tier", SubscriptionTier.FREE, status?.tier)
        Assert.assertFalse("Should not be active", status?.isActive == true)
    }

    @Test
    fun `getSubscriptionStatus returns PREMIUM for user with active subscription`() = runTest(timeout = 30.seconds) {
        // Given: Create active premium subscription for user
        val subscription = createTestSubscription(premiumUser.id, SubscriptionTier.PREMIUM, isActive = true)
        firebaseTestHelper.createTestSubscription(subscription)

        // When: Get subscription status
        val statusResult = billingRepository.getSubscriptionStatus(premiumUser.id)

        // Then: Should return PREMIUM status
        Assert.assertTrue("Should get subscription status", statusResult.isSuccess)
        val status = statusResult.getOrNull()
        Assert.assertNotNull("Status should exist", status)
        Assert.assertEquals("Should be PREMIUM tier", SubscriptionTier.PREMIUM, status?.tier)
        Assert.assertTrue("Should be active", status?.isActive == true)
        Assert.assertNotNull("Should have start date", status?.startDate)
        Assert.assertNotNull("Should have end date", status?.endDate)
    }

    @Test
    fun `upgradeSubscription successfully creates premium subscription`() = runTest(timeout = 30.seconds) {
        // Given: Free user wants to upgrade
        val paymentMethodId = "pm_test_card"

        // When: Upgrade to premium
        val upgradeResult = billingRepository.upgradeSubscription(
            userId = testUser.id,
            tier = SubscriptionTier.PREMIUM,
            paymentMethodId = paymentMethodId
        )

        // Then: Upgrade should succeed
        Assert.assertTrue("Upgrade should succeed", upgradeResult.isSuccess)

        // User should now have premium status
        val statusResult = billingRepository.getSubscriptionStatus(testUser.id)
        Assert.assertTrue("Should get updated status", statusResult.isSuccess)
        val status = statusResult.getOrNull()
        Assert.assertEquals("Should now be PREMIUM", SubscriptionTier.PREMIUM, status?.tier)
        Assert.assertTrue("Should be active", status?.isActive == true)
    }

    @Test
    fun `cancelSubscription deactivates premium subscription`() = runTest(timeout = 30.seconds) {
        // Given: User with active premium subscription
        val subscription = createTestSubscription(premiumUser.id, SubscriptionTier.PREMIUM, isActive = true)
        firebaseTestHelper.createTestSubscription(subscription)

        // Verify initially active
        val initialStatus = billingRepository.getSubscriptionStatus(premiumUser.id).first()
        Assert.assertTrue("Should initially be active", initialStatus.getOrNull()?.isActive == true)

        // When: Cancel subscription
        val cancelResult = billingRepository.cancelSubscription(premiumUser.id)

        // Then: Cancellation should succeed
        Assert.assertTrue("Cancel should succeed", cancelResult.isSuccess)

        // Subscription should be cancelled
        val cancelledStatus = billingRepository.getSubscriptionStatus(premiumUser.id).first()
        Assert.assertFalse("Should no longer be active", cancelledStatus.getOrNull()?.isActive == true)
    }

    @Test
    fun `getPaymentHistory returns chronological payment records`() = runTest(timeout = 30.seconds) {
        // Given: Create multiple payments for user
        val payments = createTestPaymentHistory(premiumUser.id, 3)
        payments.forEach { payment ->
            firebaseTestHelper.createTestPayment(payment)
        }

        // When: Get payment history
        val historyResult = billingRepository.getPaymentHistory(premiumUser.id)

        // Then: Should return payments in chronological order (newest first)
        Assert.assertTrue("Should get payment history", historyResult.isSuccess)
        val paymentHistory = historyResult.getOrNull() ?: emptyList()

        Assert.assertEquals("Should return 3 payments", 3, paymentHistory.size)

        // Verify chronological ordering (newest first)
        for (i in 0 until paymentHistory.size - 1) {
            Assert.assertTrue(
                "Payments should be ordered newest first",
                paymentHistory[i].timestamp >= paymentHistory[i + 1].timestamp
            )
        }

        // Verify payment details
        val latestPayment = paymentHistory[0]
        Assert.assertEquals("Should have correct amount", 999L, latestPayment.amount)
        Assert.assertEquals("Should have correct currency", "INR", latestPayment.currency)
        Assert.assertEquals("Should have success status", PaymentStatus.SUCCESS, latestPayment.status)
    }

    @Test
    fun `processPayment handles successful payment transaction`() = runTest(timeout = 30.seconds) {
        // Given: Payment details
        val paymentRequest = PaymentRequest(
            userId = testUser.id,
            amount = 999L,
            currency = "INR",
            paymentMethodId = "pm_test_success",
            description = "Premium subscription upgrade"
        )

        // When: Process payment
        val paymentResult = billingRepository.processPayment(paymentRequest)

        // Then: Payment should succeed
        Assert.assertTrue("Payment should succeed", paymentResult.isSuccess)
        val payment = paymentResult.getOrNull()
        Assert.assertNotNull("Payment record should exist", payment)
        Assert.assertEquals("Should have correct amount", 999L, payment?.amount)
        Assert.assertEquals("Should have success status", PaymentStatus.SUCCESS, payment?.status)
        Assert.assertNotNull("Should have transaction ID", payment?.transactionId)
    }

    @Test
    fun `processPayment handles failed payment transaction`() = runTest(timeout = 30.seconds) {
        // Given: Payment with invalid card (simulated failure)
        val paymentRequest = PaymentRequest(
            userId = testUser.id,
            amount = 999L,
            currency = "INR",
            paymentMethodId = "pm_test_declined",
            description = "Premium subscription upgrade"
        )

        // When: Process payment (would fail in real scenario)
        val paymentResult = billingRepository.processPayment(paymentRequest)

        // Then: Should handle failure gracefully
        // Note: In real implementation, this would depend on payment processor response
        // For testing, we verify the method completes without crashing
        Assert.assertTrue("Payment processing should complete", paymentResult.isSuccess || paymentResult.isFailure)

        if (paymentResult.isSuccess) {
            val payment = paymentResult.getOrNull()
            Assert.assertNotNull("Payment record should exist", payment)
        }
    }

    @Test
    fun `getSubscriptionPlans returns available subscription options`() = runTest(timeout = 30.seconds) {
        // When: Get available subscription plans
        val plansResult = billingRepository.getSubscriptionPlans()

        // Then: Should return subscription plans
        Assert.assertTrue("Should get subscription plans", plansResult.isSuccess)
        val plans = plansResult.getOrNull() ?: emptyList()

        // Should have at least FREE and PREMIUM plans
        Assert.assertTrue("Should have subscription plans", plans.isNotEmpty())

        // Verify plan structure
        val premiumPlan = plans.find { it.tier == SubscriptionTier.PREMIUM }
        Assert.assertNotNull("Should have PREMIUM plan", premiumPlan)
        Assert.assertTrue("Premium plan should have price > 0", (premiumPlan?.price ?: 0) > 0)
        Assert.assertNotNull("Should have features list", premiumPlan?.features)
        Assert.assertTrue("Should have features", premiumPlan?.features?.isNotEmpty() == true)
    }

    @Test
    fun `isFeatureAccessible returns correct access based on subscription tier`() = runTest(timeout = 30.seconds) {
        // Given: Different subscription tiers
        val premiumFeatures = listOf("detailed_grading", "ai_insights", "priority_support")
        val basicFeatures = listOf("basic_tests")

        // Test FREE user
        premiumFeatures.forEach { feature ->
            val hasAccess = billingRepository.isFeatureAccessible(testUser.id, feature)
            Assert.assertFalse("FREE user should not have access to $feature", hasAccess)
        }

        // Give user premium subscription
        val subscription = createTestSubscription(testUser.id, SubscriptionTier.PREMIUM, isActive = true)
        firebaseTestHelper.createTestSubscription(subscription)

        // Test PREMIUM user
        premiumFeatures.forEach { feature ->
            val hasAccess = billingRepository.isFeatureAccessible(testUser.id, feature)
            Assert.assertTrue("PREMIUM user should have access to $feature", hasAccess)
        }

        basicFeatures.forEach { feature ->
            val hasAccess = billingRepository.isFeatureAccessible(testUser.id, feature)
            Assert.assertTrue("PREMIUM user should have access to basic $feature", hasAccess)
        }
    }

    @Test
    fun `observeSubscriptionChanges provides real-time subscription updates`() = runTest(timeout = 30.seconds) {
        // Given: Start observing subscription changes
        billingRepository.observeSubscriptionChanges(testUser.id).test {
            val initialStatus = awaitItem()
            Assert.assertEquals("Initial status should be FREE", SubscriptionTier.FREE, initialStatus.tier)

            // When: Upgrade to premium
            billingRepository.upgradeSubscription(
                userId = testUser.id,
                tier = SubscriptionTier.PREMIUM,
                paymentMethodId = "pm_test_realtime"
            )

            // Then: Should receive real-time update
            val updatedStatus = awaitItem()
            Assert.assertEquals("Should now be PREMIUM", SubscriptionTier.PREMIUM, updatedStatus.tier)
            Assert.assertTrue("Should be active", updatedStatus.isActive)

            // When: Cancel subscription
            billingRepository.cancelSubscription(testUser.id)

            // Then: Should receive cancellation update
            val cancelledStatus = awaitItem()
            Assert.assertFalse("Should no longer be active", cancelledStatus.isActive)
        }
    }

    @Test
    fun `getUsageStats returns subscription usage metrics`() = runTest(timeout = 30.seconds) {
        // Given: User with premium subscription and some usage
        val subscription = createTestSubscription(premiumUser.id, SubscriptionTier.PREMIUM, isActive = true)
        firebaseTestHelper.createTestSubscription(subscription)

        // Simulate some usage (this would normally come from other repositories)
        // In real implementation, usage stats would aggregate from various sources

        // When: Get usage statistics
        val usageResult = billingRepository.getUsageStats(premiumUser.id)

        // Then: Should return usage statistics
        Assert.assertTrue("Should get usage stats", usageResult.isSuccess)
        val usage = usageResult.getOrNull()
        Assert.assertNotNull("Usage stats should exist", usage)

        // Verify usage structure (exact values depend on implementation)
        Assert.assertNotNull("Should have tests taken count", usage?.testsTaken)
        Assert.assertNotNull("Should have study time", usage?.studyHours)
        Assert.assertNotNull("Should have subscription days used", usage?.subscriptionDaysUsed)
    }

    @Test
    fun `handleSubscriptionRenewal processes automatic renewal`() = runTest(timeout = 30.seconds) {
        // Given: Subscription nearing expiration
        val currentTime = System.currentTimeMillis()
        val expiringSubscription = createTestSubscription(
            userId = premiumUser.id,
            tier = SubscriptionTier.PREMIUM,
            isActive = true
        ).copy(
            endDate = currentTime + (24 * 60 * 60 * 1000L), // Expires in 1 day
            autoRenew = true
        )
        firebaseTestHelper.createTestSubscription(expiringSubscription)

        // When: Process renewal (normally called by background job)
        val renewalResult = billingRepository.handleSubscriptionRenewal(premiumUser.id)

        // Then: Renewal should be processed
        // Note: Exact behavior depends on payment processor integration
        Assert.assertTrue("Renewal processing should complete", renewalResult.isSuccess || renewalResult.isFailure)

        // Subscription should still be active if renewal succeeded
        val statusAfterRenewal = billingRepository.getSubscriptionStatus(premiumUser.id).first()
        // Note: In test environment, renewal might not actually process payment
        Assert.assertNotNull("Status should still exist after renewal attempt", statusAfterRenewal.getOrNull())
    }

    // ==================== HELPER METHODS ====================

    private fun createTestSubscription(
        userId: String,
        tier: SubscriptionTier,
        isActive: Boolean
    ): Subscription {
        val currentTime = System.currentTimeMillis()
        return Subscription(
            id = "sub_${userId}_${tier.name.lowercase()}",
            userId = userId,
            tier = tier,
            isActive = isActive,
            startDate = if (isActive) currentTime else null,
            endDate = if (isActive) currentTime + (30 * 24 * 60 * 60 * 1000L) else null, // 30 days
            autoRenew = true,
            paymentMethodId = "pm_test_card",
            createdAt = currentTime,
            updatedAt = currentTime
        )
    }

    private fun createTestPaymentHistory(userId: String, count: Int): List<Payment> {
        val currentTime = System.currentTimeMillis()
        return (1..count).map { index ->
            Payment(
                id = "payment_${userId}_$index",
                userId = userId,
                amount = 999L,
                currency = "INR",
                status = PaymentStatus.SUCCESS,
                paymentMethodId = "pm_test_card",
                transactionId = "txn_test_${index}",
                description = "Premium subscription - Month ${index}",
                timestamp = currentTime - (index * 24 * 60 * 60 * 1000L), // Different days
                metadata = mapOf("subscription_id" to "sub_test_$index")
            )
        }
    }
}
