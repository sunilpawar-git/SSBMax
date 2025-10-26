package com.ssbmax.ui.premium

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssbmax.core.domain.model.BillingCycle
import com.ssbmax.core.domain.model.SSBMaxUser
import com.ssbmax.core.domain.model.SubscriptionTier
import com.ssbmax.core.domain.repository.UserProfileRepository
import com.ssbmax.core.domain.usecase.auth.ObserveCurrentUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Upgrade Screen
 * Manages subscription tier comparison and upgrade flow
 * 
 * Note: Payment gateway integration pending
 * TODO: Integrate Razorpay/Stripe for actual subscription purchases
 * TODO: Create SubscriptionRepository for subscription management
 * TODO: Add subscription renewal and cancellation flows
 */
@HiltViewModel
class UpgradeViewModel @Inject constructor(
    private val observeCurrentUser: ObserveCurrentUserUseCase,
    private val userProfileRepository: UserProfileRepository
    // Payment gateway not yet integrated
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(UpgradeUiState())
    val uiState: StateFlow<UpgradeUiState> = _uiState.asStateFlow()
    
    init {
        loadCurrentSubscription()
        loadAvailablePlans()
    }
    
    private fun loadCurrentSubscription() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            try {
                Log.d("Upgrade", "Loading current subscription")
                
                // Get current user
                val currentUser = observeCurrentUser().first()
                if (currentUser == null) {
                    Log.w("Upgrade", "No user logged in, defaulting to BASIC tier")
                    _uiState.update {
                        it.copy(
                            currentTier = SubscriptionTier.BASIC,
                            isLoading = false
                        )
                    }
                    return@launch
                }
                
                // Load user profile to get subscription info
                userProfileRepository.getUserProfile(currentUser.id)
                    .catch { error ->
                        Log.e("Upgrade", "Error loading user profile", error)
                        // Default to BASIC on error
                        _uiState.update {
                            it.copy(
                                currentTier = SubscriptionTier.BASIC,
                                isLoading = false
                            )
                        }
                    }
                    .collect { result ->
                        val profile = result.getOrNull()
                        
                        // Map SubscriptionType to SubscriptionTier
                        val tier = when (profile?.subscriptionType) {
                            com.ssbmax.core.domain.model.SubscriptionType.FREE -> SubscriptionTier.BASIC
                            com.ssbmax.core.domain.model.SubscriptionType.PREMIUM_ASSESSOR -> SubscriptionTier.PRO
                            com.ssbmax.core.domain.model.SubscriptionType.PREMIUM_AI -> SubscriptionTier.PREMIUM_AI
                            null -> SubscriptionTier.BASIC
                        }
                        
                        Log.d("Upgrade", "Current subscription tier: $tier (from type: ${profile?.subscriptionType})")
                        
                        _uiState.update {
                            it.copy(
                                currentTier = tier,
                                isLoading = false
                            )
                        }
                    }
            } catch (e: Exception) {
                Log.e("Upgrade", "Error in loadCurrentSubscription", e)
                _uiState.update {
                    it.copy(
                        currentTier = SubscriptionTier.BASIC,
                        isLoading = false
                    )
                }
            }
        }
    }
    
    private fun loadAvailablePlans() {
        val plans = listOf(
            SubscriptionPlan(
                tier = SubscriptionTier.BASIC,
                name = "Basic",
                tagline = "Get Started with SSB Prep",
                priceMonthly = 0.0,
                priceQuarterly = 0.0,
                priceAnnually = 0.0,
                features = listOf(
                    PlanFeature("Overview of SSB process", true),
                    PlanFeature("Full access to all study materials", true),
                    PlanFeature("Basic progress tracking", true),
                    PlanFeature("Community access", true),
                    PlanFeature("Practice tests", false),
                    PlanFeature("AI-powered assessment", false),
                    PlanFeature("SSB Marketplace access", false)
                ),
                isRecommended = false,
                gradient = listOf("#6366f1", "#8b5cf6")
            ),
            SubscriptionPlan(
                tier = SubscriptionTier.PRO,
                name = "Pro",
                tagline = "Accelerate Your Preparation",
                priceMonthly = 499.0,
                priceQuarterly = 1299.0,
                priceAnnually = 4499.0,
                features = listOf(
                    PlanFeature("Everything in Basic", true),
                    PlanFeature("Unlimited practice tests", true),
                    PlanFeature("Advanced analytics", true),
                    PlanFeature("Test history & comparisons", true),
                    PlanFeature("Priority support", true),
                    PlanFeature("AI-powered assessment", false),
                    PlanFeature("SSB Marketplace access", false)
                ),
                isRecommended = true,
                gradient = listOf("#8b5cf6", "#a855f7")
            ),
            SubscriptionPlan(
                tier = SubscriptionTier.PREMIUM_AI,
                name = "Premium (AI)",
                tagline = "AI-Powered Excellence",
                priceMonthly = 999.0,
                priceQuarterly = 2699.0,
                priceAnnually = 8999.0,
                features = listOf(
                    PlanFeature("Everything in Pro", true),
                    PlanFeature("AI-based test result analysis", true),
                    PlanFeature("Personalized feedback & tips", true),
                    PlanFeature("Predictive success scoring", true),
                    PlanFeature("Custom study plans", true),
                    PlanFeature("24/7 AI mentor support", true),
                    PlanFeature("SSB Marketplace access", false)
                ),
                isRecommended = false,
                gradient = listOf("#a855f7", "#c026d3")
            ),
            SubscriptionPlan(
                tier = SubscriptionTier.PREMIUM,
                name = "Premium",
                tagline = "Complete SSB Solution",
                priceMonthly = 1499.0,
                priceQuarterly = 3999.0,
                priceAnnually = 13999.0,
                features = listOf(
                    PlanFeature("Everything in Pro", true),
                    PlanFeature("SSB Marketplace access", true),
                    PlanFeature("Connect with verified assessors", true),
                    PlanFeature("Online 1-on-1 sessions", true),
                    PlanFeature("Enroll in physical classes", true),
                    PlanFeature("Exclusive webinars & workshops", true),
                    PlanFeature("Mock interview sessions", true)
                ),
                isRecommended = false,
                gradient = listOf("#c026d3", "#db2777")
            )
        )
        
        _uiState.update {
            it.copy(availablePlans = plans)
        }
    }
    
    fun selectBillingCycle(cycle: BillingCycle) {
        _uiState.update {
            it.copy(selectedBillingCycle = cycle)
        }
    }
    
    fun upgradeToPlan(tier: SubscriptionTier) {
        // Visual only - show coming soon dialog
        _uiState.update {
            it.copy(showComingSoonDialog = true, selectedPlanForUpgrade = tier)
        }
    }
    
    fun dismissComingSoonDialog() {
        _uiState.update {
            it.copy(showComingSoonDialog = false, selectedPlanForUpgrade = null)
        }
    }
}

/**
 * UI State for Upgrade Screen
 */
data class UpgradeUiState(
    val currentTier: SubscriptionTier = SubscriptionTier.BASIC,
    val availablePlans: List<SubscriptionPlan> = emptyList(),
    val selectedBillingCycle: BillingCycle = BillingCycle.MONTHLY,
    val isLoading: Boolean = true,
    val showComingSoonDialog: Boolean = false,
    val selectedPlanForUpgrade: SubscriptionTier? = null
)

/**
 * Subscription plan details
 */
data class SubscriptionPlan(
    val tier: SubscriptionTier,
    val name: String,
    val tagline: String,
    val priceMonthly: Double,
    val priceQuarterly: Double,
    val priceAnnually: Double,
    val features: List<PlanFeature>,
    val isRecommended: Boolean,
    val gradient: List<String>
) {
    fun getPriceForCycle(cycle: BillingCycle): Double {
        return when (cycle) {
            BillingCycle.MONTHLY -> priceMonthly
            BillingCycle.QUARTERLY -> priceQuarterly
            BillingCycle.ANNUALLY -> priceAnnually
        }
    }
    
    fun getSavingsForCycle(cycle: BillingCycle): String? {
        return when (cycle) {
            BillingCycle.MONTHLY -> null
            BillingCycle.QUARTERLY -> {
                val savings = (priceMonthly * 3) - priceQuarterly
                if (savings > 0) "Save ₹${savings.toInt()}" else null
            }
            BillingCycle.ANNUALLY -> {
                val savings = (priceMonthly * 12) - priceAnnually
                if (savings > 0) "Save ₹${savings.toInt()}" else null
            }
        }
    }
}

/**
 * Individual feature in a plan
 */
data class PlanFeature(
    val description: String,
    val isIncluded: Boolean
)

