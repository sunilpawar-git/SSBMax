package com.ssbmax.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.ssbmax.core.data.BuildConfig
import com.ssbmax.core.data.di.ApplicationScope
import com.ssbmax.core.domain.model.SubscriptionType
import com.ssbmax.core.domain.repository.UserProfileRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Mock & Real Unified Billing Repository
 * Manages Google Play Billing in production and robust local test billing in debug.
 */
@Singleton
class BillingRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val billingClient: SSBMaxBillingClient,
    private val firebaseAuth: FirebaseAuth,
    private val userProfileRepository: UserProfileRepository,
    @ApplicationScope private val externalScope: CoroutineScope
) {
    private val TAG = "SSBMaxBilling"
    
    private val _premiumStatus = MutableStateFlow(false)
    val premiumStatus: StateFlow<Boolean> = _premiumStatus.asStateFlow()
    
    private val _products = MutableStateFlow<List<ProductInfo>>(emptyList())
    val products: StateFlow<List<ProductInfo>> = _products.asStateFlow()
    
    /**
     * Initialize billing (mock + real implementation)
     */
    fun initialize() {
        Log.d(TAG, "Initializing BillingRepository")
        
        // Load products
        _products.value = listOf(
            ProductInfo("premium_monthly", "Premium Monthly", "₹99/month"),
            ProductInfo("premium_yearly", "Premium Yearly", "₹999/year")
        )

        // Set listener on BillingClient to capture purchases
        billingClient.setListener { billingResult, purchases ->
            if (billingResult.responseCode == com.android.billingclient.api.BillingClient.BillingResponseCode.OK && purchases != null) {
                for (purchase in purchases) {
                    handlePurchase(purchase)
                }
            } else {
                Log.e(TAG, "Purchase callback failure or cancelled: ${billingResult.debugMessage}")
            }
        }

        billingClient.initialize()
    }
    
    private fun handlePurchase(purchase: com.android.billingclient.api.Purchase) {
        externalScope.launch {
            val result = billingClient.verifyPurchase(purchase.purchaseToken)
            if (result.isSuccess && result.getOrThrow()) {
                Log.i(TAG, "✅ Purchase verified successfully! Granting subscription tier...")
                
                // Identify target tier from product ID
                val isYearly = purchase.products.contains("premium_yearly")
                val subType = if (isYearly) SubscriptionType.PREMIUM else SubscriptionType.PRO
                
                // Save to Firestore User Profile
                val userId = firebaseAuth.currentUser?.uid
                if (userId != null) {
                    val profileResult = userProfileRepository.getUserProfile(userId).first()
                    val profile = profileResult.getOrNull()
                    if (profile != null) {
                        val updatedProfile = profile.copy(subscriptionType = subType)
                        userProfileRepository.updateUserProfile(updatedProfile)
                        Log.d(TAG, "Saved tier $subType in user profile Firestore document")
                    }
                }
                
                _premiumStatus.value = true
            } else {
                Log.e(TAG, "❌ Failed to verify purchase: ${result.exceptionOrNull()?.message}")
            }
        }
    }
    
    /**
     * Purchase a product (mock + real implementation)
     */
    fun purchaseProduct(activity: Activity, productId: String) {
        Log.d(TAG, "launching purchase flow for product: $productId")
        billingClient.launchPurchaseFlow(activity, productId)
    }
    
    /**
     * Check if user is premium
     */
    fun isPremium(): Boolean = _premiumStatus.value
}

/**
 * Product details DTO
 */
data class ProductInfo(
    val id: String,
    val name: String,
    val price: String
)
