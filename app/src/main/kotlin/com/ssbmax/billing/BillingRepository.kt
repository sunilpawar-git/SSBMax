package com.ssbmax.billing

import com.ssbmax.shared.platform.billing.BillingClient
import com.ssbmax.shared.platform.billing.SSBMaxProductIds
import com.ssbmax.utils.ErrorLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * App-layer wrapper around the shared-module [BillingClient] (Phase 4
 * platform shim -- real Play Billing on Android, real StoreKit 1 on iOS,
 * see those actuals' class docs). Replaces the previous mock implementation
 * (`initialize()` populated hardcoded fake products; `purchaseProduct()`
 * unconditionally granted premium) and the redundant parallel
 * `SSBMaxBillingClient` stub (deleted -- same responsibility, never wired
 * to anything real, exactly the "two parallel implementations" anti-pattern
 * this phase is supposed to avoid manufacturing).
 *
 * Does NOT touch [com.ssbmax.core.data.repository.SubscriptionManager] (the
 * SSOT for subscription *limits*) -- wiring a confirmed purchase into that
 * SSOT is a separate, real product-integration task (needs server-side
 * receipt validation before trusting a client-reported purchase), out of
 * this shim's scope. This class only handles the purchase transaction
 * itself and locally tracks premium status for UI purposes.
 */
class BillingRepository(
    private val billingClient: BillingClient,
    private val scope: CoroutineScope
) {

    private val _premiumStatus = MutableStateFlow(false)
    val premiumStatus: StateFlow<Boolean> = _premiumStatus.asStateFlow()

    private val _products = MutableStateFlow<List<ProductInfo>>(emptyList())
    val products: StateFlow<List<ProductInfo>> = _products.asStateFlow()

    /** Connects to the store and loads product + current-purchase state. */
    fun initialize() {
        scope.launch {
            billingClient.connect()
                .onFailure { ErrorLogger.log(it, "Billing connect failed") }
                .onSuccess {
                    billingClient.queryProducts(SSBMaxProductIds.ALL)
                        .onSuccess { products ->
                            _products.value = products.map { ProductInfo(it.id, it.title, it.formattedPrice) }
                        }
                        .onFailure { ErrorLogger.log(it, "Billing queryProducts failed") }
                    refreshPurchaseStatus()
                }
        }
    }

    /** Re-checks the store for active purchases (e.g. on app resume). */
    fun refreshPurchaseStatus() {
        scope.launch {
            billingClient.restorePurchases()
                .onSuccess { purchases -> _premiumStatus.value = purchases.isNotEmpty() }
                .onFailure { ErrorLogger.log(it, "Billing restorePurchases failed") }
        }
    }

    /**
     * Launches the platform purchase flow for [productId]. On Android, the
     * shared client resolves the foreground Activity itself (see
     * PlayBillingClient's class doc) -- no Activity parameter needed here.
     */
    fun purchaseProduct(productId: String) {
        scope.launch {
            billingClient.purchase(productId)
                .onSuccess { _premiumStatus.value = true }
                .onFailure { error ->
                    // User cancellation is expected, routine flow -- not an error to log.
                    if (error !is com.ssbmax.shared.platform.billing.BillingCancelledException) {
                        ErrorLogger.log(error, "Billing purchase failed")
                    }
                }
        }
    }

    /** Check if user is premium (based on the last successful store check). */
    fun isPremium(): Boolean = _premiumStatus.value

    fun disconnect() {
        billingClient.disconnect()
    }
}

data class ProductInfo(
    val id: String,
    val name: String,
    val price: String
)
