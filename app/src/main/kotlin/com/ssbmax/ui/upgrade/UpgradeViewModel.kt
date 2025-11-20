package com.ssbmax.ui.upgrade

import androidx.lifecycle.ViewModel
import com.ssbmax.core.domain.model.BillingPeriod
import com.ssbmax.core.domain.model.SubscriptionPlan
import com.ssbmax.core.domain.model.SubscriptionTier
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/**
 * ViewModel for Upgrade Screen
 * Manages subscription plans and selection
 */
@HiltViewModel
class UpgradeViewModel @Inject constructor() : ViewModel() {
    
    private val _uiState = MutableStateFlow(UpgradeUiState())
    val uiState: StateFlow<UpgradeUiState> = _uiState.asStateFlow()
    
    init {
        loadPlans()
    }
    
    private fun loadPlans() {
        // Note: Prices sourced from SubscriptionTier domain model (single source of truth)
        // TODO: Migrate to Google Play Billing Library for production
        val plans = listOf(
            SubscriptionPlan(
                tier = SubscriptionTier.PRO,
                name = "Pro",
                price = SubscriptionTier.PRO.monthlyPriceInt,
                billingPeriod = BillingPeriod.MONTHLY,
                features = listOf(
                    "All study materials",
                    "Some practice tests",
                    "Progress tracking",
                    "Email support"
                )
            ),
            SubscriptionPlan(
                tier = SubscriptionTier.PREMIUM,
                name = "AI Premium",
                price = SubscriptionTier.PREMIUM.monthlyPriceInt,
                billingPeriod = BillingPeriod.MONTHLY,
                features = listOf(
                    "Everything in Pro",
                    "AI-powered test analysis",
                    "Detailed feedback reports",
                    "Personalized recommendations",
                    "Priority support"
                ),
                isPopular = true
            ),
            SubscriptionPlan(
                tier = SubscriptionTier.PREMIUM,
                name = "Premium",
                price = SubscriptionTier.PREMIUM.monthlyPriceInt,
                billingPeriod = BillingPeriod.MONTHLY,
                features = listOf(
                    "Everything in AI Premium",
                    "SSB Marketplace access",
                    "Professional assessor reviews",
                    "Coaching institute connections",
                    "Dedicated support"
                )
            )
        )
        _uiState.update { it.copy(plans = plans) }
    }
    
    fun selectPlan(plan: SubscriptionPlan) {
        _uiState.update { it.copy(selectedPlan = plan) }
    }
}

/**
 * UI state for Upgrade screen
 */
data class UpgradeUiState(
    val plans: List<SubscriptionPlan> = emptyList(),
    val selectedPlan: SubscriptionPlan? = null,
    val currentTier: SubscriptionTier = SubscriptionTier.FREE
)

