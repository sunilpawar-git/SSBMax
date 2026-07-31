package com.ssbmax.shared.presentation.premium

import com.ssbmax.shared.domain.model.BillingCycle
import com.ssbmax.shared.domain.model.SubscriptionTier
import com.ssbmax.shared.domain.usecase.auth.ObserveCurrentUserUseCase
import com.ssbmax.shared.domain.usecase.subscription.GetSubscriptionTierUseCase
import com.ssbmax.shared.domain.util.DomainLogger
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * KMP port of the Android `app/.../ui/premium/UpgradeViewModel.kt` (the LIVE
 * upgrade screen -- `com.ssbmax.ui.premium`, wired at `SSBMaxDestinations.UpgradeScreen`
 * / route "premium/upgrade"). NOT to be confused with the sibling Android
 * package `com.ssbmax.ui.upgrade` (route "upgrade") -- that package's
 * `UpgradeScreen`/`UpgradeViewModel` are dead code in the Android app itself:
 * `viewModelOf(::UpgradeUpgradeViewModel)` is bound in `ViewModelModule.kt`
 * but `UpgradeScreen` (the composable) has zero call sites anywhere in
 * `SharedNavGraph.kt` or any other nav graph -- confirmed by grep and by
 * `git log` showing the package untouched since the mechanical Phase 1/3
 * migration commits. Deliberately NOT ported this session (would be porting
 * unreachable code -- see this plan's own "no speculative features" rule).
 *
 * Uses a real `androidx.lifecycle.ViewModel` with `viewModelScope` (Phase 1 of
 * the KMP-convergence plan, see
 * [com.ssbmax.shared.presentation.oir.OIRTestViewModel]'s doc comment for the
 * precedent this mirrors). `android.util.Log` replaced with [DomainLogger].
 *
 * Behavior difference from the Android original (deliberate, not a port bug):
 * the Android `UpgradeViewModel` reads `userProfileRepository.getUserProfile(userId)`
 * directly and hand-maps `SubscriptionType` -> `SubscriptionTier` inline. This
 * port instead calls [GetSubscriptionTierUseCase] -- the exact same use case
 * the sibling (already-ported) `SubscriptionManagementViewModel` uses for the
 * identical lookup. Same SSOT result, avoids duplicating the
 * repository-to-tier mapping in two KMP ViewModels.
 *
 * Billing gap (explicitly NOT invented here): the Android original's
 * `upgradeToPlan()` is "visual only" -- it just flips `showComingSoonDialog`
 * to true; there is no Razorpay/Stripe/Play-Billing call anywhere in this
 * flow (see the Android file's own TODO comments). This port preserves that
 * exact placeholder behavior. `PlayBillingClient`/`StoreKitBillingClient`
 * (Phase 4 shims) are NOT wired here, matching the Android original which has
 * no working purchase flow either.
 */
class UpgradeViewModel(
    private val observeCurrentUser: ObserveCurrentUserUseCase,
    private val getSubscriptionTier: GetSubscriptionTierUseCase,
    private val logger: DomainLogger
) : ViewModel() {
    private val _uiState = MutableStateFlow(UpgradeUiState())
    val uiState: StateFlow<UpgradeUiState> = _uiState.asStateFlow()

    private companion object {
        const val TAG = "UpgradeViewModel"
    }

    init {
        loadCurrentSubscription()
        loadAvailablePlans()
    }

    private fun loadCurrentSubscription() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val currentUser = observeCurrentUser().first()
                if (currentUser == null) {
                    logger.w(TAG, "No user logged in, defaulting to FREE tier")
                    _uiState.update { it.copy(currentTier = SubscriptionTier.FREE, isLoading = false) }
                    return@launch
                }

                val tierResult = getSubscriptionTier(currentUser.id)
                val tier = tierResult.getOrElse {
                    logger.e(TAG, "Error loading subscription tier", it)
                    SubscriptionTier.FREE
                }

                _uiState.update { it.copy(currentTier = tier, isLoading = false) }
            } catch (e: Exception) {
                logger.e(TAG, "Error in loadCurrentSubscription", e)
                _uiState.update { it.copy(currentTier = SubscriptionTier.FREE, isLoading = false) }
            }
        }
    }

    private fun loadAvailablePlans() {
        // Prices sourced from SubscriptionTier domain model (single source of truth).
        val plans = listOf(
            SubscriptionPlan(
                tier = SubscriptionTier.FREE,
                name = "Basic",
                tagline = "Get Started with SSB Prep",
                priceMonthly = SubscriptionTier.FREE.monthlyPriceInt.toDouble(),
                priceQuarterly = SubscriptionTier.FREE.quarterlyPriceInt?.toDouble() ?: 0.0,
                priceAnnually = SubscriptionTier.FREE.yearlyPriceInt?.toDouble() ?: 0.0,
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
                priceMonthly = SubscriptionTier.PRO.monthlyPriceInt.toDouble(),
                priceQuarterly = SubscriptionTier.PRO.quarterlyPriceInt?.toDouble() ?: 0.0,
                priceAnnually = SubscriptionTier.PRO.yearlyPriceInt?.toDouble() ?: 0.0,
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
                tier = SubscriptionTier.PREMIUM,
                name = "Premium (AI)",
                tagline = "AI-Powered Excellence",
                priceMonthly = SubscriptionTier.PREMIUM.monthlyPriceInt.toDouble(),
                priceQuarterly = SubscriptionTier.PREMIUM.quarterlyPriceInt?.toDouble() ?: 0.0,
                priceAnnually = SubscriptionTier.PREMIUM.yearlyPriceInt?.toDouble() ?: 0.0,
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
                priceMonthly = SubscriptionTier.PREMIUM.monthlyPriceInt.toDouble(),
                priceQuarterly = SubscriptionTier.PREMIUM.quarterlyPriceInt?.toDouble() ?: 0.0,
                priceAnnually = SubscriptionTier.PREMIUM.yearlyPriceInt?.toDouble() ?: 0.0,
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

        _uiState.update { it.copy(availablePlans = plans) }
    }

    fun selectBillingCycle(cycle: BillingCycle) {
        _uiState.update { it.copy(selectedBillingCycle = cycle) }
    }

    fun upgradeToPlan(tier: SubscriptionTier) {
        // Visual only -- show coming soon dialog. No payment gateway wired
        // (matches the Android original; see class doc above).
        _uiState.update { it.copy(showComingSoonDialog = true, selectedPlanForUpgrade = tier) }
    }

    fun dismissComingSoonDialog() {
        _uiState.update { it.copy(showComingSoonDialog = false, selectedPlanForUpgrade = null) }
    }
}

/**
 * UI State for Upgrade Screen
 */
data class UpgradeUiState(
    val currentTier: SubscriptionTier = SubscriptionTier.FREE,
    val availablePlans: List<SubscriptionPlan> = emptyList(),
    val selectedBillingCycle: BillingCycle = BillingCycle.MONTHLY,
    val isLoading: Boolean = true,
    val showComingSoonDialog: Boolean = false,
    val selectedPlanForUpgrade: SubscriptionTier? = null
)

/**
 * Subscription plan details, as displayed on the upgrade screen.
 * (Distinct from `com.ssbmax.shared.domain.model.SubscriptionPlan`, the
 * simpler domain-layer plan shape used elsewhere -- this UI-only shape needs
 * per-billing-cycle pricing + gradient colors the domain model doesn't carry.)
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
