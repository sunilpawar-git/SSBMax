package com.ssbmax.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import com.ssbmax.core.data.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * SSBMax Billing Client
 *
 * Manages Google Play Billing integration for subscription management.
 * In debug mode (USE_CLOUD_AI = false), acts as a robust mock play billing client.
 * In production mode (USE_CLOUD_AI = true), interacts with actual Google Play Services.
 */
@Singleton
class SSBMaxBillingClient @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val TAG = "SSBMaxBilling"
    internal var billingClient: BillingClient? = null
    private var purchasesUpdatedListener: PurchasesUpdatedListener? = null

    /**
     * Set the listener for purchase updates
     */
    fun setListener(listener: PurchasesUpdatedListener) {
        this.purchasesUpdatedListener = listener
    }

    /**
     * Initializes the billing client connection.
     */
    fun initialize() {
        if (!BuildConfig.USE_CLOUD_AI) {
            Log.d(TAG, "🛠️ Running in DEBUG mode - BillingClient initialized with Local Mock Manager")
            return
        }

        Log.d(TAG, "🚀 Running in PRODUCTION mode - Initializing Google Play BillingClient")
        val listener = PurchasesUpdatedListener { billingResult, purchases ->
            purchasesUpdatedListener?.onPurchasesUpdated(billingResult, purchases)
        }

        billingClient = BillingClient.newBuilder(context)
            .setListener(listener)
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder()
                    .enableOneTimeProducts()
                    .build()
            )
            .build()

        connectToGooglePlay()
    }

    private fun connectToGooglePlay() {
        billingClient?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.i(TAG, "✅ Billing Client successfully connected to Google Play!")
                } else {
                    Log.e(TAG, "❌ Billing Client setup failed: ${billingResult.debugMessage}")
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.w(TAG, "⚠️ Billing Service disconnected. Reconnecting...")
                // Retry connection in real scenarios
            }
        })
    }

    /**
     * Queries available subscription products.
     */
    suspend fun querySubscriptions(): Result<List<String>> = withContext(Dispatchers.IO) {
        if (!BuildConfig.USE_CLOUD_AI) {
            Log.d(TAG, "🛠️ Debug: Querying available mock subscriptions")
            return@withContext Result.success(listOf("premium_monthly", "premium_yearly"))
        }

        val client = billingClient
        if (client == null || !client.isReady) {
            return@withContext Result.failure(IllegalStateException("Billing client is not connected"))
        }

        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId("premium_monthly")
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId("premium_yearly")
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        suspendCancellableCoroutine { continuation ->
            client.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    val ids = productDetailsList.map { it.productId }
                    continuation.resume(Result.success(ids))
                } else {
                    continuation.resume(Result.failure(Exception("Query subscriptions failed: ${billingResult.debugMessage}")))
                }
            }
        }
    }

    /**
     * Launches purchase flow for a subscription.
     */
    fun launchPurchaseFlow(activity: Activity, productId: String) {
        if (!BuildConfig.USE_CLOUD_AI) {
            Log.d(TAG, "🛠️ Debug: Simulating purchase flow for $productId")
            // Instantly notify listener of a successful mock purchase
            val mockPurchase = Purchase(
                "{\"orderId\":\"mock_order_${System.currentTimeMillis()}\",\"packageName\":\"com.ssbmax\",\"productId\":\"$productId\",\"purchaseTime\":${System.currentTimeMillis()},\"purchaseState\":1,\"purchaseToken\":\"mock_token_${System.currentTimeMillis()}\",\"acknowledged\":false}",
                "mock_signature"
            )
            val successResult = BillingResult.newBuilder()
                .setResponseCode(BillingClient.BillingResponseCode.OK)
                .setDebugMessage("Mock purchase success")
                .build()
            
            purchasesUpdatedListener?.onPurchasesUpdated(successResult, listOf(mockPurchase))
            return
        }

        val client = billingClient
        if (client == null || !client.isReady) {
            Log.e(TAG, "❌ launchPurchaseFlow: BillingClient not ready")
            return
        }

        // Query product details first to get reference for launch
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(productId)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        client.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && productDetailsList.isNotEmpty()) {
                val productDetails = productDetailsList.first()
                val offerToken = productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken ?: ""
                
                val productDetailsParamsList = listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(productDetails)
                        .setOfferToken(offerToken)
                        .build()
                )

                val flowParams = BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(productDetailsParamsList)
                    .build()

                client.launchBillingFlow(activity, flowParams)
            } else {
                Log.e(TAG, "❌ Failed to fetch product details for purchase: ${billingResult.debugMessage}")
            }
        }
    }

    /**
     * Verifies an existing purchase.
     */
    suspend fun verifyPurchase(purchaseToken: String): Result<Boolean> = withContext(Dispatchers.IO) {
        if (!BuildConfig.USE_CLOUD_AI) {
            Log.d(TAG, "🛠️ Debug: Verifying mock purchase token")
            return@withContext Result.success(purchaseToken.startsWith("mock_token"))
        }

        val client = billingClient
        if (client == null || !client.isReady) {
            return@withContext Result.failure(IllegalStateException("Billing client is not connected"))
        }

        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        suspendCancellableCoroutine { continuation ->
            client.queryPurchasesAsync(params) { billingResult, purchases ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    val hasPurchase = purchases.any { it.purchaseToken == purchaseToken && it.purchaseState == Purchase.PurchaseState.PURCHASED }
                    continuation.resume(Result.success(hasPurchase))
                } else {
                    continuation.resume(Result.success(false))
                }
            }
        }
    }

    /**
     * Disconnects from billing client.
     */
    fun disconnect() {
        billingClient?.endConnection()
        billingClient = null
    }
}
