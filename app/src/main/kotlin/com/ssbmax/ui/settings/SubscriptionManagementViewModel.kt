package com.ssbmax.ui.settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssbmax.core.domain.model.SubscriptionTier
import com.ssbmax.core.domain.usecase.auth.ObserveCurrentUserUseCase
import com.ssbmax.core.domain.usecase.subscription.GetMonthlyUsageUseCase
import com.ssbmax.core.domain.usecase.subscription.GetSubscriptionTierUseCase
import com.ssbmax.utils.ErrorLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
/**
 * ViewModel for Subscription Management Screen
 * REFACTORED: Now uses use cases instead of direct Firebase dependencies
 */
@HiltViewModel
class SubscriptionManagementViewModel @Inject constructor(
    private val observeCurrentUser: ObserveCurrentUserUseCase,
    private val getSubscriptionTier: GetSubscriptionTierUseCase,
    private val getMonthlyUsage: GetMonthlyUsageUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SubscriptionManagementUiState())
    val uiState: StateFlow<SubscriptionManagementUiState> = _uiState.asStateFlow()
    
    companion object {
        private const val TAG = "SubscriptionVM"
    }
    
    fun loadSubscriptionData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            try {
                // Get current user ID
                val user = observeCurrentUser().first()
                val userId = user?.id ?: throw Exception("User not authenticated")
                
                // Load subscription tier
                val tierResult = getSubscriptionTier(userId)
                val tier = tierResult.getOrElse {
                    throw Exception("Failed to load subscription tier: ${it.message}")
                }
                
                // Load monthly usage
                val usageResult = getMonthlyUsage(userId)
                val domainUsage = usageResult.getOrElse {
                    ErrorLogger.log(it, "Failed to load usage, using empty map")
                    emptyMap()
                }
                
                // Map domain models to UI models
                val uiTier = SubscriptionTierModel.from(tier)
                val uiUsage = domainUsage.mapValues { (_, value) ->
                    UsageInfo.from(value)
                }
                
                // Calculate expiry date (mock for now)
                val expiresAt = if (tier != SubscriptionTier.FREE) {
                    val calendar = Calendar.getInstance()
                    calendar.add(Calendar.MONTH, 1)
                    SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(calendar.time)
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
                ErrorLogger.log(e, "Failed to load subscription data")
                _uiState.update { it.copy(
                    isLoading = false,
                    error = "Failed to load subscription data: ${e.message}"
                ) }
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
 * UI model for subscription tier with display properties
 * Maps from domain SubscriptionTier to UI-specific display model
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
        fun from(tier: SubscriptionTier): SubscriptionTierModel {
            return when (tier) {
                SubscriptionTier.FREE -> FREE
                SubscriptionTier.PRO -> PRO
                SubscriptionTier.PREMIUM -> PREMIUM
            }
        }
    }
}

/**
 * Usage information for display
 */
data class UsageInfo(
    val used: Int,
    val limit: Int
) {
    companion object {
        fun from(domain: com.ssbmax.core.domain.repository.UsageInfo): UsageInfo {
            return UsageInfo(
                used = domain.used,
                limit = domain.limit
            )
        }
    }
}
