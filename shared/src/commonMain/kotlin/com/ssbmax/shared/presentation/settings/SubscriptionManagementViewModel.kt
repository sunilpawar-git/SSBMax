package com.ssbmax.shared.presentation.settings

import com.ssbmax.shared.domain.model.SubscriptionTier
import com.ssbmax.shared.domain.usecase.auth.ObserveCurrentUserUseCase
import com.ssbmax.shared.domain.usecase.subscription.GetMonthlyUsageUseCase
import com.ssbmax.shared.domain.usecase.subscription.GetSubscriptionTierUseCase
import com.ssbmax.shared.domain.util.DomainLogger
import com.ssbmax.shared.ui.util.formatFullDate
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

/**
 * KMP port of the Android `app/.../ui/settings/SubscriptionManagementViewModel.kt`.
 *
 * Phase 1 of the KMP-convergence plan: a real `androidx.lifecycle.ViewModel`
 * using `viewModelScope`, converged with `app`'s existing ViewModel idiom and
 * this module's own DI (`viewModelOf`) / screen (`koinViewModel()`)
 * conventions — no more manual `CoroutineScope` + `close()`. `android.util.Log`/`ErrorLogger`
 * calls replaced with [DomainLogger].
 *
 * Kotlin/Native gotcha fixes (this port, not behavior changes):
 * - `java.util.Calendar`/`java.util.Locale` (JVM-only) replaced with
 *   `kotlinx.datetime.Clock` + [formatFullDate] (the same KMP-safe date
 *   formatter [com.ssbmax.shared.presentation.gto.common.GTOSubmissionCoordinator]'s
 *   sibling verticals already use) -- the "add one month" `Calendar.add`
 *   call becomes a flat +30-day offset in epoch millis, which matches the
 *   Android original's own doc comment marking this expiry date as "mock for
 *   now" (not a real billing-cycle calculation either way).
 */
class SubscriptionManagementViewModel(
    private val observeCurrentUser: ObserveCurrentUserUseCase,
    private val getSubscriptionTier: GetSubscriptionTierUseCase,
    private val getMonthlyUsage: GetMonthlyUsageUseCase,
    private val logger: DomainLogger
) : ViewModel() {
    private val _uiState = MutableStateFlow(SubscriptionManagementUiState())
    val uiState: StateFlow<SubscriptionManagementUiState> = _uiState.asStateFlow()

    private companion object {
        const val TAG = "SubscriptionMgmtViewModel"
        const val THIRTY_DAYS_MILLIS = 30L * 24 * 60 * 60 * 1000
    }

    fun loadSubscriptionData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                val user = observeCurrentUser().first()
                val userId = user?.id ?: throw IllegalStateException("User not authenticated")

                val tierResult = getSubscriptionTier(userId)
                val tier = tierResult.getOrElse {
                    throw IllegalStateException("Failed to load subscription tier: ${it.message}")
                }

                val usageResult = getMonthlyUsage(userId)
                val domainUsage = usageResult.getOrElse {
                    logger.e(TAG, "Failed to load usage, using empty map", it)
                    emptyMap()
                }

                val uiTier = SubscriptionTierModel.from(tier)
                val uiUsage = domainUsage.mapValues { (_, value) -> UsageInfo.from(value) }

                val expiresAt = if (tier != SubscriptionTier.FREE) {
                    formatFullDate(Clock.System.now().toEpochMilliseconds() + THIRTY_DAYS_MILLIS)
                } else {
                    null
                }

                _uiState.value = SubscriptionManagementUiState(
                    isLoading = false,
                    currentTier = uiTier,
                    monthlyUsage = uiUsage,
                    subscriptionExpiresAt = expiresAt
                )
            } catch (e: Exception) {
                logger.e(TAG, "Failed to load subscription data", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to load subscription data: ${e.message}"
                    )
                }
            }
        }
    }
}

/**
 * UI State for Subscription Management
 */
data class SubscriptionManagementUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val currentTier: SubscriptionTierModel = SubscriptionTierModel.FREE,
    val monthlyUsage: Map<String, UsageInfo> = emptyMap(),
    val subscriptionExpiresAt: String? = null
)

/**
 * UI model for subscription tier with display properties.
 * Maps from domain [SubscriptionTier] to UI-specific display model.
 */
enum class SubscriptionTierModel(
    val displayName: String,
    val price: String,
    val oirTestLimit: Int,
    val tatTestLimit: Int,
    val watTestLimit: Int,
    val srtTestLimit: Int,
    val ppdtTestLimit: Int,
    val piqTestLimit: Int,
    val sdTestLimit: Int,
    val gtoTestLimit: Int,
    val interviewTestLimit: Int,
    val features: List<String>
) {
    FREE(
        displayName = "Free",
        price = "₹0/month",
        oirTestLimit = 1,
        tatTestLimit = 0,
        watTestLimit = 0,
        srtTestLimit = 0,
        ppdtTestLimit = 1,
        piqTestLimit = 1,
        sdTestLimit = 0,
        gtoTestLimit = 0,
        interviewTestLimit = 0,
        features = listOf(
            "1 OIR test per month",
            "1 PPDT test per month",
            "1 PIQ form (required)",
            "Access to all study materials",
            "Basic progress tracking",
            "Community support"
        )
    ),
    PRO(
        displayName = "Pro",
        price = "₹99/month",
        oirTestLimit = 5,
        tatTestLimit = 3,
        watTestLimit = 3,
        srtTestLimit = 3,
        ppdtTestLimit = 5,
        piqTestLimit = -1,
        sdTestLimit = 3,
        gtoTestLimit = 3,
        interviewTestLimit = 1,
        features = listOf(
            "5 OIR tests per month",
            "5 PPDT tests per month",
            "3 TAT, WAT, SRT, SD tests each",
            "3 attempts per GTO test (8 tests)",
            "1 Interview practice",
            "Unlimited PIQ updates",
            "Advanced analytics",
            "Priority support",
            "Download study materials"
        )
    ),
    PREMIUM(
        displayName = "Premium",
        price = "₹999/month",
        oirTestLimit = -1,
        tatTestLimit = -1,
        watTestLimit = -1,
        srtTestLimit = -1,
        ppdtTestLimit = -1,
        piqTestLimit = -1,
        sdTestLimit = -1,
        gtoTestLimit = -1,
        interviewTestLimit = -1,
        features = listOf(
            "Unlimited all tests",
            "AI-powered feedback",
            "Personalized study plans",
            "Expert mentor sessions",
            "SSB Marketplace access",
            "Premium content library",
            "Certificate of completion",
            "Lifetime access to materials"
        )
    );

    companion object {
        fun from(tier: SubscriptionTier): SubscriptionTierModel = when (tier) {
            SubscriptionTier.FREE -> FREE
            SubscriptionTier.PRO -> PRO
            SubscriptionTier.PREMIUM -> PREMIUM
        }
    }
}

/**
 * Usage information for display.
 */
data class UsageInfo(
    val used: Int,
    val limit: Int
) {
    companion object {
        fun from(domain: com.ssbmax.shared.domain.repository.UsageInfo): UsageInfo = UsageInfo(
            used = domain.used,
            limit = domain.limit
        )
    }
}
