package com.ssbmax.billing

import android.content.Context
import com.android.billingclient.api.*
import com.ssbmax.core.data.BuildConfig
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SSBMaxBillingClientTest {

    private lateinit var context: Context
    private lateinit var client: SSBMaxBillingClient
    private var mockBillingClient: BillingClient? = null

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        client = SSBMaxBillingClient(context)

        if (BuildConfig.USE_CLOUD_AI) {
            val billing = mockk<BillingClient>(relaxed = true)
            every { billing.isReady } returns true
            mockBillingClient = billing
            client.billingClient = billing
        }
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun querySubscriptions_returnsSuccess() = runTest {
        mockBillingClient?.let { billing ->
            val callbackSlot = slot<ProductDetailsResponseListener>()
            every { billing.queryProductDetailsAsync(any(), capture(callbackSlot)) } answers {
                val billingResult = BillingResult.newBuilder()
                    .setResponseCode(BillingClient.BillingResponseCode.OK)
                    .build()
                val details = mockk<ProductDetails> {
                    every { productId } returns "premium_monthly"
                }
                callbackSlot.captured.onProductDetailsResponse(billingResult, listOf(details))
            }
        }

        val result = client.querySubscriptions()
        assertTrue(result.isSuccess)
    }

    @Test
    fun verifyPurchase_returnsSuccess() = runTest {
        mockBillingClient?.let { billing ->
            val callbackSlot = slot<PurchasesResponseListener>()
            every { billing.queryPurchasesAsync(any<QueryPurchasesParams>(), capture(callbackSlot)) } answers {
                val billingResult = BillingResult.newBuilder()
                    .setResponseCode(BillingClient.BillingResponseCode.OK)
                    .build()
                val purchase = mockk<Purchase> {
                    every { purchaseToken } returns "mock_token_123"
                    every { purchaseState } returns Purchase.PurchaseState.PURCHASED
                }
                callbackSlot.captured.onQueryPurchasesResponse(billingResult, listOf(purchase))
            }
        }

        val result = client.verifyPurchase("mock_token_123")
        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow())
    }
}
