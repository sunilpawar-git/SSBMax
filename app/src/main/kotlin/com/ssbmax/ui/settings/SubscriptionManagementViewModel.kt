package com.ssbmax.ui.settings

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

/**
 * ViewModel for Subscription Management Screen
 */
@HiltViewModel
class SubscriptionManagementViewModel @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SubscriptionManagementUiState())
    val uiState: StateFlow<SubscriptionManagementUiState> = _uiState.asStateFlow()
    
    companion object {
        private const val TAG = "SubscriptionVM"
        private val monthFormat = SimpleDateFormat("yyyy-MM", Locale.US)
    }
    
    fun loadSubscriptionData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val userId = firebaseAuth.currentUser?.uid
                    ?: throw Exception("User not authenticated")
                
                // Load subscription tier
                val tier = loadSubscriptionTier(userId)
                
                // Load monthly usage
                val usage = loadMonthlyUsage(userId, tier)
                
                // Calculate expiry date (mock for now)
                val expiresAt = if (tier != SubscriptionTierModel.FREE) {
                    val calendar = Calendar.getInstance()
                    calendar.add(Calendar.MONTH, 1)
                    SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(calendar.time)
                } else {
                    null
                }
                
                _uiState.value = SubscriptionManagementUiState(
                    isLoading = false,
                    currentTier = tier,
                    monthlyUsage = usage,
                    subscriptionExpiresAt = expiresAt
                )
                
                Log.d(TAG, "Loaded subscription data: tier=$tier, usage=${usage.size} tests")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load subscription data", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load subscription data: ${e.message}"
                )
            }
        }
    }
    
    private suspend fun loadSubscriptionTier(userId: String): SubscriptionTierModel {
        return try {
            val doc = firestore.collection("users")
                .document(userId)
                .collection("data")
                .document("subscription")
                .get()
                .await()
            
            val tierString = doc.getString("tier") ?: "FREE"
            when (tierString.uppercase()) {
                "PRO" -> SubscriptionTierModel.PRO
                "PREMIUM" -> SubscriptionTierModel.PREMIUM
                else -> SubscriptionTierModel.FREE
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load tier, defaulting to FREE", e)
            SubscriptionTierModel.FREE
        }
    }
    
    private suspend fun loadMonthlyUsage(
        userId: String,
        tier: SubscriptionTierModel
    ): Map<String, UsageInfo> {
        return try {
            val currentMonth = monthFormat.format(Date())
            val doc = firestore.collection("users")
                .document(userId)
                .collection("test_usage")
                .document(currentMonth)
                .get()
                .await()
            
            mapOf(
                "OIR Tests" to UsageInfo(
                    used = doc.getLong("oirTestsUsed")?.toInt() ?: 0,
                    limit = tier.oirTestLimit
                ),
                "TAT Tests" to UsageInfo(
                    used = doc.getLong("tatTestsUsed")?.toInt() ?: 0,
                    limit = tier.tatTestLimit
                ),
                "WAT Tests" to UsageInfo(
                    used = doc.getLong("watTestsUsed")?.toInt() ?: 0,
                    limit = tier.watTestLimit
                ),
                "SRT Tests" to UsageInfo(
                    used = doc.getLong("srtTestsUsed")?.toInt() ?: 0,
                    limit = tier.srtTestLimit
                ),
                "PPDT Tests" to UsageInfo(
                    used = doc.getLong("ppdtTestsUsed")?.toInt() ?: 0,
                    limit = tier.ppdtTestLimit
                )
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load usage, returning empty", e)
            emptyMap()
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
 * Subscription tier model with pricing and limits
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
    )
}

