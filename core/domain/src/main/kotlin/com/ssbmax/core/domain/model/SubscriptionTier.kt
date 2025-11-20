package com.ssbmax.core.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Subscription tiers available in SSBMax
 * IMPORTANT: This is the SINGLE SOURCE OF TRUTH for pricing
 */
enum class SubscriptionTier {
    FREE,
    PRO,
    PREMIUM;
    
    val displayName: String
        get() = when (this) {
            FREE -> "Free"
            PRO -> "Pro"
            PREMIUM -> "Premium"
        }
    
    val description: String
        get() = when (this) {
            FREE -> "Full access to study materials"
            PRO -> "Study materials + practice tests"
            PREMIUM -> "Unlimited tests + AI analysis + Marketplace"
        }
    
    val monthlyPrice: String
        get() = when (this) {
            FREE -> "₹0/month"
            PRO -> "₹99/month"
            PREMIUM -> "₹999/month"
        }
    
    val monthlyPriceInt: Int
        get() = when (this) {
            FREE -> 0
            PRO -> 99
            PREMIUM -> 999
        }
    
    val yearlyPrice: String?
        get() = when (this) {
            FREE -> null
            PRO -> "₹999/year"  // ~16% savings
            PREMIUM -> "₹9,999/year" // ~17% savings
        }
    
    val yearlyPriceInt: Int?
        get() = when (this) {
            FREE -> null
            PRO -> 999
            PREMIUM -> 9999
        }

    val quarterlyPrice: String?
        get() = when (this) {
            FREE -> null
            PRO -> "₹249/quarter"  // ~16% savings
            PREMIUM -> "₹2,699/quarter" // ~10% savings
        }

    val quarterlyPriceInt: Int?
        get() = when (this) {
            FREE -> null
            PRO -> 249
            PREMIUM -> 2699
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
        get() = this != FREE
    
    /**
     * Check if this tier has AI-based test result analysis
     */
    val hasAIAnalysis: Boolean
        get() = this == PREMIUM
    
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
        get() = when (this) {
            FREE -> listOf(
                "1 OIR test per month",
                "1 PPDT test per month",
                "1 PIQ form (required)",
                "Access to all study materials",
                "Basic progress tracking",
                "Community support"
            )
            PRO -> listOf(
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
            PREMIUM -> listOf(
                "Unlimited all tests",
                "AI-powered feedback",
                "Personalized study plans",
                "Expert mentor sessions",
                "SSB Marketplace access",
                "Premium content library",
                "Certificate of completion",
                "Lifetime access to materials"
            )
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
@Parcelize
data class SubscriptionPlan(
    val tier: SubscriptionTier,
    val name: String,
    val price: Int, // in rupees
    val billingPeriod: BillingPeriod,
    val features: List<String>,
    val isPopular: Boolean = false,
    val savings: Int? = null // percentage savings for annual plans
) : Parcelable

/**
 * Billing period for subscriptions
 */
enum class BillingPeriod(val displayName: String, val months: Int) {
    MONTHLY("Monthly", 1),
    QUARTERLY("Quarterly", 3),
    YEARLY("Yearly", 12)
}

