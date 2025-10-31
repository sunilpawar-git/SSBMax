package com.ssbmax.core.data.analytics

import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.auth.FirebaseAuth
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Analytics Manager for tracking user events and conversions
 * 
 * Tracks:
 * - Test usage and completion
 * - Subscription tier changes
 * - Upgrade conversions
 * - Feature usage patterns
 * - User engagement metrics
 */
@Singleton
class AnalyticsManager @Inject constructor(
    private val firebaseAnalytics: FirebaseAnalytics,
    private val firebaseAuth: FirebaseAuth
) {
    
    companion object {
        private const val TAG = "AnalyticsManager"
        
        // Event Names
        private const val EVENT_TEST_STARTED = "test_started"
        private const val EVENT_TEST_COMPLETED = "test_completed"
        private const val EVENT_TEST_LIMIT_REACHED = "test_limit_reached"
        private const val EVENT_SUBSCRIPTION_VIEW = "subscription_view"
        private const val EVENT_UPGRADE_INITIATED = "upgrade_initiated"
        private const val EVENT_UPGRADE_COMPLETED = "upgrade_completed"
        private const val EVENT_STUDY_MATERIAL_VIEWED = "study_material_viewed"
        private const val EVENT_FEATURE_USED = "feature_used"
        
        // Parameter Names
        private const val PARAM_TEST_TYPE = "test_type"
        private const val PARAM_TEST_ID = "test_id"
        private const val PARAM_SCORE = "score"
        private const val PARAM_TIME_SPENT = "time_spent_seconds"
        private const val PARAM_QUESTIONS_ANSWERED = "questions_answered"
        private const val PARAM_FROM_TIER = "from_tier"
        private const val PARAM_TO_TIER = "to_tier"
        private const val PARAM_MATERIAL_ID = "material_id"
        private const val PARAM_MATERIAL_CATEGORY = "material_category"
        private const val PARAM_FEATURE_NAME = "feature_name"
        private const val PARAM_CURRENT_TIER = "current_tier"
        private const val PARAM_SOURCE = "source"
    }
    
    /**
     * Track when a test is started
     */
    fun trackTestStarted(testType: String, testId: String) {
        try {
            firebaseAnalytics.logEvent(EVENT_TEST_STARTED) {
                param(PARAM_TEST_TYPE, testType)
                param(PARAM_TEST_ID, testId)
                param(PARAM_CURRENT_TIER, getCurrentUserTier())
            }
            Log.d(TAG, "📊 Test started: $testType")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to track test started", e)
        }
    }
    
    /**
     * Track when a test is completed
     */
    fun trackTestCompleted(
        testType: String,
        testId: String,
        score: Float,
        timeSpentSeconds: Long,
        questionsAnswered: Int
    ) {
        try {
            firebaseAnalytics.logEvent(EVENT_TEST_COMPLETED) {
                param(PARAM_TEST_TYPE, testType)
                param(PARAM_TEST_ID, testId)
                param(PARAM_SCORE, score.toDouble())
                param(PARAM_TIME_SPENT, timeSpentSeconds)
                param(PARAM_QUESTIONS_ANSWERED, questionsAnswered.toLong())
                param(PARAM_CURRENT_TIER, getCurrentUserTier())
            }
            Log.d(TAG, "📊 Test completed: $testType, score=$score")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to track test completed", e)
        }
    }
    
    /**
     * Track when user hits test limit (conversion opportunity)
     */
    fun trackTestLimitReached(
        testType: String,
        currentTier: String,
        currentUsage: Int,
        limit: Int
    ) {
        try {
            firebaseAnalytics.logEvent(EVENT_TEST_LIMIT_REACHED) {
                param(PARAM_TEST_TYPE, testType)
                param(PARAM_CURRENT_TIER, currentTier)
                param("current_usage", currentUsage.toLong())
                param("limit", limit.toLong())
            }
            Log.d(TAG, "📊 Test limit reached: $testType, tier=$currentTier")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to track test limit reached", e)
        }
    }
    
    /**
     * Track when user views subscription screen
     */
    fun trackSubscriptionView(source: String) {
        try {
            firebaseAnalytics.logEvent(EVENT_SUBSCRIPTION_VIEW) {
                param(PARAM_SOURCE, source)
                param(PARAM_CURRENT_TIER, getCurrentUserTier())
            }
            Log.d(TAG, "📊 Subscription viewed from: $source")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to track subscription view", e)
        }
    }
    
    /**
     * Track when user initiates upgrade process
     */
    fun trackUpgradeInitiated(fromTier: String, toTier: String, source: String) {
        try {
            firebaseAnalytics.logEvent(EVENT_UPGRADE_INITIATED) {
                param(PARAM_FROM_TIER, fromTier)
                param(PARAM_TO_TIER, toTier)
                param(PARAM_SOURCE, source)
            }
            Log.d(TAG, "📊 Upgrade initiated: $fromTier → $toTier from $source")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to track upgrade initiated", e)
        }
    }
    
    /**
     * Track when upgrade is completed (conversion!)
     */
    fun trackUpgradeCompleted(fromTier: String, toTier: String, priceInRupees: Int) {
        try {
            firebaseAnalytics.logEvent(EVENT_UPGRADE_COMPLETED) {
                param(PARAM_FROM_TIER, fromTier)
                param(PARAM_TO_TIER, toTier)
                param(FirebaseAnalytics.Param.VALUE, priceInRupees.toDouble())
                param(FirebaseAnalytics.Param.CURRENCY, "INR")
            }
            
            // Track as purchase event for revenue tracking
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.PURCHASE) {
                param(FirebaseAnalytics.Param.ITEM_ID, "subscription_$toTier")
                param(FirebaseAnalytics.Param.ITEM_NAME, "$toTier Subscription")
                param(FirebaseAnalytics.Param.ITEM_CATEGORY, "Subscription")
                param(FirebaseAnalytics.Param.VALUE, priceInRupees.toDouble())
                param(FirebaseAnalytics.Param.CURRENCY, "INR")
            }
            
            Log.d(TAG, "📊 💰 Upgrade completed: $fromTier → $toTier (₹$priceInRupees)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to track upgrade completed", e)
        }
    }
    
    /**
     * Track study material views
     */
    fun trackStudyMaterialViewed(materialId: String, category: String) {
        try {
            firebaseAnalytics.logEvent(EVENT_STUDY_MATERIAL_VIEWED) {
                param(PARAM_MATERIAL_ID, materialId)
                param(PARAM_MATERIAL_CATEGORY, category)
                param(PARAM_CURRENT_TIER, getCurrentUserTier())
            }
            Log.d(TAG, "📊 Study material viewed: $category/$materialId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to track study material viewed", e)
        }
    }
    
    /**
     * Track feature usage
     */
    fun trackFeatureUsed(featureName: String, parameters: Map<String, Any> = emptyMap()) {
        try {
            firebaseAnalytics.logEvent(EVENT_FEATURE_USED) {
                param(PARAM_FEATURE_NAME, featureName)
                param(PARAM_CURRENT_TIER, getCurrentUserTier())
                parameters.forEach { (key, value) ->
                    when (value) {
                        is String -> param(key, value)
                        is Long -> param(key, value)
                        is Double -> param(key, value)
                        is Int -> param(key, value.toLong())
                        is Float -> param(key, value.toDouble())
                        else -> param(key, value.toString())
                    }
                }
            }
            Log.d(TAG, "📊 Feature used: $featureName")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to track feature used", e)
        }
    }
    
    /**
     * Set user properties for segmentation
     */
    fun setUserProperties(subscriptionTier: String, accountAgeInDays: Int) {
        try {
            firebaseAnalytics.setUserProperty("subscription_tier", subscriptionTier)
            firebaseAnalytics.setUserProperty("account_age_days", accountAgeInDays.toString())
            Log.d(TAG, "📊 User properties set: tier=$subscriptionTier, age=$accountAgeInDays days")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set user properties", e)
        }
    }
    
    /**
     * Track screen views
     */
    fun trackScreenView(screenName: String, screenClass: String) {
        try {
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW) {
                param(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
                param(FirebaseAnalytics.Param.SCREEN_CLASS, screenClass)
            }
            Log.d(TAG, "📊 Screen view: $screenName")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to track screen view", e)
        }
    }
    
    /**
     * Track user engagement time
     */
    fun trackEngagement(durationSeconds: Long, activityType: String) {
        try {
            firebaseAnalytics.logEvent("user_engagement") {
                param("engagement_time_msec", durationSeconds * 1000)
                param("activity_type", activityType)
                param(PARAM_CURRENT_TIER, getCurrentUserTier())
            }
            Log.d(TAG, "📊 Engagement tracked: $activityType for ${durationSeconds}s")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to track engagement", e)
        }
    }
    
    /**
     * Get current user's subscription tier (for context)
     * This should ideally be fetched from a local cache or repository
     */
    private fun getCurrentUserTier(): String {
        // TODO: Get from repository instead of hardcoding
        return "FREE" // Default, should be updated based on actual user tier
    }
}

