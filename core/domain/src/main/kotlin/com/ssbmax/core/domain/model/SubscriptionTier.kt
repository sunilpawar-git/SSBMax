package com.ssbmax.core.domain.model

/**
 * Subscription tiers available in SSBMax
 */
enum class SubscriptionTier {
    BASIC,
    PRO,
    PREMIUM_AI,
    PREMIUM;
    
    val displayName: String
        get() = when (this) {
            BASIC -> "Basic"
            PRO -> "Pro"
            PREMIUM_AI -> "Premium (AI)"
            PREMIUM -> "Premium"
        }
    
    val description: String
        get() = when (this) {
            BASIC -> "Overview & study materials access"
            PRO -> "Everything in Basic plus practice tests"
            PREMIUM_AI -> "Everything in Pro plus AI-based analysis"
            PREMIUM -> "Everything in Pro plus SSB Marketplace access"
        }
    
    val monthlyPrice: String
        get() = when (this) {
            BASIC -> "Free"
            PRO -> "₹299/month"
            PREMIUM_AI -> "₹599/month"
            PREMIUM -> "₹999/month"
        }
    
    val yearlyPrice: String?
        get() = when (this) {
            BASIC -> null
            PRO -> "₹2,999/year"
            PREMIUM_AI -> "₹5,999/year"
            PREMIUM -> "₹9,999/year"
        }
    
    /**
     * Check if this tier has access to overview sections
     */
    val hasOverviewAccess: Boolean
        get() = true // All tiers have overview access
    
    /**
     * Check if this tier has full access to study materials
     */
    val hasStudyMaterialsAccess: Boolean
        get() = true // All tiers have full study materials access
    
    /**
     * Check if this tier has access to practice tests
     */
    val hasTestAccess: Boolean
        get() = this != BASIC
    
    /**
     * Check if this tier has AI-based test result analysis
     */
    val hasAIAnalysis: Boolean
        get() = this == PREMIUM_AI || this == PREMIUM
    
    /**
     * Check if this tier has access to SSB Marketplace
     * (online assessors and offline/physical class enrollment)
     */
    val hasMarketplaceAccess: Boolean
        get() = this == PREMIUM
    
    /**
     * Check if this tier has human assessor feedback
     */
    val hasAssessorFeedback: Boolean
        get() = this == PREMIUM
    
    /**
     * Get list of features available in this tier
     */
    val features: List<String>
        get() = buildList {
            add("Access to Overview sections")
            add("Full access to Study Materials")
            
            if (hasTestAccess) {
                add("Practice tests and mock exams")
            }
            
            if (hasAIAnalysis) {
                add("AI-powered test result analysis")
                add("Personalized improvement suggestions")
            }
            
            if (hasMarketplaceAccess) {
                add("SSB Marketplace access")
                add("Book online assessor sessions")
                add("Enroll in offline/physical classes")
            }
            
            if (hasAssessorFeedback) {
                add("Professional assessor feedback")
                add("Detailed performance reports")
            }
        }
}

/**
 * User subscription information
 */
data class UserSubscription(
    val userId: String,
    val tier: SubscriptionTier,
    val subscriptionId: String? = null,
    val startDate: Long = System.currentTimeMillis(),
    val expiryDate: Long? = null,
    val autoRenew: Boolean = false,
    val isActive: Boolean = true,
    val billingCycle: BillingCycle = BillingCycle.MONTHLY
) {
    /**
     * Check if subscription is expired
     */
    val isExpired: Boolean
        get() = expiryDate?.let { it < System.currentTimeMillis() } ?: false
    
    /**
     * Check if subscription needs renewal soon (within 7 days)
     */
    val needsRenewal: Boolean
        get() = expiryDate?.let {
            val daysUntilExpiry = (it - System.currentTimeMillis()) / (1000 * 60 * 60 * 24)
            daysUntilExpiry in 0..7
        } ?: false
}

/**
 * Billing cycle for subscriptions
 */
enum class BillingCycle {
    MONTHLY,
    QUARTERLY,
    ANNUALLY;
    
    val displayName: String
        get() = when (this) {
            MONTHLY -> "Monthly"
            QUARTERLY -> "Quarterly"
            ANNUALLY -> "Annually"
        }
}

/**
 * Subscription plan option for display in upgrade screen
 */
data class SubscriptionPlan(
    val tier: SubscriptionTier,
    val billingCycle: BillingCycle,
    val price: String,
    val savingsText: String? = null,
    val isRecommended: Boolean = false
)

