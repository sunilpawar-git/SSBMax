package com.ssbmax.billing

import com.ssbmax.shared.platform.billing.BillingCancelledException
import com.ssbmax.shared.platform.billing.BillingClient
import com.ssbmax.shared.platform.billing.BillingProduct
import com.ssbmax.shared.platform.billing.PurchaseResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * BillingRepositoryTest - Unit tests for BillingRepository against a fake
 * [BillingClient] (real Play Billing/StoreKit actuals live in `shared` and
 * require a live store connection, out of scope for a plain unit test --
 * this verifies BillingRepository's own orchestration logic: does it call
 * connect before query, does purchase success/failure/cancellation update
 * premiumStatus correctly).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BillingRepositoryTest {

    private class FakeBillingClient(
        private val products: List<BillingProduct> = emptyList(),
        private val existingPurchases: List<PurchaseResult> = emptyList(),
        private val purchaseResult: Result<PurchaseResult>? = null
    ) : BillingClient {
        var connectCalled = false
        var disconnectCalled = false

        override suspend fun connect(): Result<Unit> {
            connectCalled = true
            return Result.success(Unit)
        }

        override suspend fun queryProducts(productIds: List<String>): Result<List<BillingProduct>> =
            Result.success(products)

        override suspend fun purchase(productId: String): Result<PurchaseResult> =
            purchaseResult ?: Result.failure(IllegalStateException("no purchaseResult configured"))

        override suspend fun restorePurchases(): Result<List<PurchaseResult>> =
            Result.success(existingPurchases)

        override fun disconnect() {
            disconnectCalled = true
        }
    }

    @Test
    fun `initialize connects then loads real products from the billing client`() = runTest {
        val fake = FakeBillingClient(
            products = listOf(
                BillingProduct("ssbmax_premium_monthly_PLACEHOLDER", "Premium Monthly", "desc", "$9.99")
            )
        )
        val repository = BillingRepository(fake, this)

        assertTrue(repository.products.value.isEmpty())
        repository.initialize()
        advanceUntilIdle()

        assertTrue("initialize() must connect before querying products", fake.connectCalled)
        assertEquals(1, repository.products.value.size)
        assertEquals("ssbmax_premium_monthly_PLACEHOLDER", repository.products.value[0].id)
        assertEquals("$9.99", repository.products.value[0].price)
    }

    @Test
    fun `initialize reflects an already-active purchase as premium`() = runTest {
        val fake = FakeBillingClient(
            existingPurchases = listOf(PurchaseResult("ssbmax_premium_monthly_PLACEHOLDER", "token", true))
        )
        val repository = BillingRepository(fake, this)

        repository.initialize()
        advanceUntilIdle()

        assertTrue("an existing restored purchase should mark the user premium", repository.premiumStatus.value)
    }

    @Test
    fun `purchaseProduct grants premium only on success`() = runTest {
        val fake = FakeBillingClient(
            purchaseResult = Result.success(PurchaseResult("ssbmax_premium_monthly_PLACEHOLDER", "token", true))
        )
        val repository = BillingRepository(fake, this)

        assertFalse(repository.premiumStatus.value)
        repository.purchaseProduct("ssbmax_premium_monthly_PLACEHOLDER")
        advanceUntilIdle()

        assertTrue(repository.premiumStatus.value)
    }

    @Test
    fun `purchaseProduct does not grant premium when the user cancels`() = runTest {
        val fake = FakeBillingClient(
            purchaseResult = Result.failure(BillingCancelledException())
        )
        val repository = BillingRepository(fake, this)

        repository.purchaseProduct("ssbmax_premium_monthly_PLACEHOLDER")
        advanceUntilIdle()

        assertFalse(
            "a cancelled purchase must not silently grant premium (the pre-shim mock's" +
                " purchaseProduct() granted premium unconditionally -- this guards against that regressing)",
            repository.premiumStatus.value
        )
    }

    @Test
    fun `purchaseProduct does not grant premium on a real failure`() = runTest {
        val fake = FakeBillingClient(
            purchaseResult = Result.failure(IllegalStateException("store error"))
        )
        val repository = BillingRepository(fake, this)

        repository.purchaseProduct("ssbmax_premium_monthly_PLACEHOLDER")
        advanceUntilIdle()

        assertFalse(repository.premiumStatus.value)
    }

    @Test
    fun `disconnect delegates to the billing client`() {
        val fake = FakeBillingClient()
        val repository = BillingRepository(fake, TestScope(StandardTestDispatcher()))

        repository.disconnect()

        assertTrue(fake.disconnectCalled)
    }
}
