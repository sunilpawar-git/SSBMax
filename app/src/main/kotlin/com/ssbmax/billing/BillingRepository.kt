package com.ssbmax.billing

import android.app.Activity
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Mock Billing Repository
 * TODO: Replace with real Google Play Billing when ready for production
 */
@Singleton
class BillingRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private val _premiumStatus = MutableStateFlow(false)
    val premiumStatus: StateFlow<Boolean> = _premiumStatus.asStateFlow()
    
    private val _products = MutableStateFlow<List<ProductInfo>>(emptyList())
    val products: StateFlow<List<ProductInfo>> = _products.asStateFlow()
    
    /**
     * Initialize billing (mock implementation)
     */
    fun initialize() {
        // Mock products
        _products.value = listOf(
            ProductInfo("premium_monthly", "Premium Monthly", "$9.99/month"),
            ProductInfo("premium_yearly", "Premium Yearly", "$79.99/year")
        )
    }
    
    /**
     * Purchase a product (mock implementation)
     */
    fun purchaseProduct(activity: Activity, productId: String) {
        // Mock purchase - immediately grant premium
        _premiumStatus.value = true
    }
    
    /**
     * Check if user is premium
     */
    fun isPremium(): Boolean = _premiumStatus.value
}

/**
 * Mock product info
 * TODO: Replace with real ProductDetails from Google Play Billing
 */
data class ProductInfo(
    val id: String,
    val name: String,
    val price: String
)
