package com.ssbmax.core.data.analytics

import android.os.Bundle
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics
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
        
        // Interview Funnel Events
        private const val EVENT_INTERVIEW_STARTED = "interview_started"
        private const val EVENT_INTERVIEW_QUESTION_ANSWERED = "interview_question_answered"
        private const val EVENT_INTERVIEW_COMPLETED = "interview_completed"
        private const val EVENT_INTERVIEW_ABANDONED = "interview_abandoned"
        private const val EVENT_INTERVIEW_RESULT_VIEWED = "interview_result_viewed"
        private const val EVENT_TTS_USED = "tts_service_used"
        
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
            val bundle = Bundle().apply {
                putString(PARAM_TEST_TYPE, testType)
                putString(PARAM_TEST_ID, testId)
                putString(PARAM_CURRENT_TIER, getCurrentUserTier())
            }
            firebaseAnalytics.logEvent(EVENT_TEST_STARTED, bundle)
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
            val bundle = Bundle().apply {
                putString(PARAM_TEST_TYPE, testType)
                putString(PARAM_TEST_ID, testId)
                putDouble(PARAM_SCORE, score.toDouble())
                putLong(PARAM_TIME_SPENT, timeSpentSeconds)
                putLong(PARAM_QUESTIONS_ANSWERED, questionsAnswered.toLong())
                putString(PARAM_CURRENT_TIER, getCurrentUserTier())
            }
            firebaseAnalytics.logEvent(EVENT_TEST_COMPLETED, bundle)
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
            val bundle = Bundle().apply {
                putString(PARAM_TEST_TYPE, testType)
                putString(PARAM_CURRENT_TIER, currentTier)
                putLong("current_usage", currentUsage.toLong())
                putLong("limit", limit.toLong())
            }
            firebaseAnalytics.logEvent(EVENT_TEST_LIMIT_REACHED, bundle)
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
            val bundle = Bundle().apply {
                putString(PARAM_SOURCE, source)
                putString(PARAM_CURRENT_TIER, getCurrentUserTier())
            }
            firebaseAnalytics.logEvent(EVENT_SUBSCRIPTION_VIEW, bundle)
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
            val bundle = Bundle().apply {
                putString(PARAM_FROM_TIER, fromTier)
                putString(PARAM_TO_TIER, toTier)
                putString(PARAM_SOURCE, source)
            }
            firebaseAnalytics.logEvent(EVENT_UPGRADE_INITIATED, bundle)
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
            val upgradeBundle = Bundle().apply {
                putString(PARAM_FROM_TIER, fromTier)
                putString(PARAM_TO_TIER, toTier)
                putDouble(FirebaseAnalytics.Param.VALUE, priceInRupees.toDouble())
                putString(FirebaseAnalytics.Param.CURRENCY, "INR")
            }
            firebaseAnalytics.logEvent(EVENT_UPGRADE_COMPLETED, upgradeBundle)
            
            // Track as purchase event for revenue tracking
            val purchaseBundle = Bundle().apply {
                putString(FirebaseAnalytics.Param.ITEM_ID, "subscription_$toTier")
                putString(FirebaseAnalytics.Param.ITEM_NAME, "$toTier Subscription")
                putString(FirebaseAnalytics.Param.ITEM_CATEGORY, "Subscription")
                putDouble(FirebaseAnalytics.Param.VALUE, priceInRupees.toDouble())
                putString(FirebaseAnalytics.Param.CURRENCY, "INR")
            }
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.PURCHASE, purchaseBundle)
            
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
            val bundle = Bundle().apply {
                putString(PARAM_MATERIAL_ID, materialId)
                putString(PARAM_MATERIAL_CATEGORY, category)
                putString(PARAM_CURRENT_TIER, getCurrentUserTier())
            }
            firebaseAnalytics.logEvent(EVENT_STUDY_MATERIAL_VIEWED, bundle)
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
            val bundle = Bundle().apply {
                putString(PARAM_FEATURE_NAME, featureName)
                putString(PARAM_CURRENT_TIER, getCurrentUserTier())
                parameters.forEach { (key, value) ->
                    when (value) {
                        is String -> putString(key, value)
                        is Long -> putLong(key, value)
                        is Double -> putDouble(key, value)
                        is Int -> putLong(key, value.toLong())
                        is Float -> putDouble(key, value.toDouble())
                        else -> putString(key, value.toString())
                    }
                }
            }
            firebaseAnalytics.logEvent(EVENT_FEATURE_USED, bundle)
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
            val bundle = Bundle().apply {
                putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
                putString(FirebaseAnalytics.Param.SCREEN_CLASS, screenClass)
            }
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle)
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
            val bundle = Bundle().apply {
                putLong("engagement_time_msec", durationSeconds * 1000)
                putString("activity_type", activityType)
                putString(PARAM_CURRENT_TIER, getCurrentUserTier())
            }
            firebaseAnalytics.logEvent("user_engagement", bundle)
            Log.d(TAG, "📊 Engagement tracked: $activityType for ${durationSeconds}s")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to track engagement", e)
        }
    }
    
    // ============================================
    // INTERVIEW FUNNEL TRACKING
    // ============================================
    
    /**
     * Track when an interview is started
     * @param mode Interview mode (TEXT_BASED or VOICE_BASED)
     * @param tier User's subscription tier
     */
    fun trackInterviewStarted(mode: String, tier: String) {
        try {
            val bundle = Bundle().apply {
                putString("interview_mode", mode)
                putString(PARAM_CURRENT_TIER, tier)
            }
            firebaseAnalytics.logEvent(EVENT_INTERVIEW_STARTED, bundle)
            Log.d(TAG, "📊 Interview started: mode=$mode, tier=$tier")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to track interview started", e)
        }
    }
    
    /**
     * Track when a question is answered during interview
     * @param questionIndex Current question index (0-based)
     * @param totalQuestions Total number of questions
     * @param thinkingTimeSec Time spent thinking before answering
     */
    fun trackInterviewQuestionAnswered(
        questionIndex: Int,
        totalQuestions: Int,
        thinkingTimeSec: Int
    ) {
        try {
            val bundle = Bundle().apply {
                putLong("question_index", questionIndex.toLong())
                putLong("total_questions", totalQuestions.toLong())
                putLong("thinking_time_sec", thinkingTimeSec.toLong())
                putLong("progress_percent", ((questionIndex + 1) * 100 / totalQuestions).toLong())
            }
            firebaseAnalytics.logEvent(EVENT_INTERVIEW_QUESTION_ANSWERED, bundle)
            Log.d(TAG, "📊 Interview question answered: ${questionIndex + 1}/$totalQuestions")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to track interview question answered", e)
        }
    }
    
    /**
     * Track when an interview is completed successfully
     * @param mode Interview mode (TEXT_BASED or VOICE_BASED)
     * @param durationSec Total interview duration in seconds
     * @param questionsAnswered Number of questions answered
     */
    fun trackInterviewCompleted(mode: String, durationSec: Long, questionsAnswered: Int) {
        try {
            val bundle = Bundle().apply {
                putString("interview_mode", mode)
                putLong("duration_sec", durationSec)
                putLong("questions_answered", questionsAnswered.toLong())
                putString(PARAM_CURRENT_TIER, getCurrentUserTier())
            }
            firebaseAnalytics.logEvent(EVENT_INTERVIEW_COMPLETED, bundle)
            Log.d(TAG, "📊 Interview completed: mode=$mode, duration=${durationSec}s")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to track interview completed", e)
        }
    }
    
    /**
     * Track when an interview is abandoned (user exits early)
     * @param mode Interview mode (TEXT_BASED or VOICE_BASED)
     * @param questionIndex Question index where user abandoned (0-based)
     * @param totalQuestions Total number of questions
     */
    fun trackInterviewAbandoned(mode: String, questionIndex: Int, totalQuestions: Int) {
        try {
            val bundle = Bundle().apply {
                putString("interview_mode", mode)
                putLong("abandoned_at_question", questionIndex.toLong())
                putLong("total_questions", totalQuestions.toLong())
                putLong("progress_percent", (questionIndex * 100 / totalQuestions).toLong())
                putString(PARAM_CURRENT_TIER, getCurrentUserTier())
            }
            firebaseAnalytics.logEvent(EVENT_INTERVIEW_ABANDONED, bundle)
            Log.d(TAG, "📊 Interview abandoned at question ${questionIndex + 1}/$totalQuestions")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to track interview abandoned", e)
        }
    }
    
    /**
     * Track when interview result is viewed
     * @param resultId Interview result ID
     * @param overallRating Final rating (1-10 SSB scale)
     */
    fun trackInterviewResultViewed(resultId: String, overallRating: Int) {
        try {
            val bundle = Bundle().apply {
                putString("result_id", resultId)
                putLong("overall_rating", overallRating.toLong())
                putString(PARAM_CURRENT_TIER, getCurrentUserTier())
            }
            firebaseAnalytics.logEvent(EVENT_INTERVIEW_RESULT_VIEWED, bundle)
            Log.d(TAG, "📊 Interview result viewed: rating=$overallRating")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to track interview result viewed", e)
        }
    }
    
    /**
     * Track TTS service usage and performance
     * @param service TTS service name (sarvam_ai, elevenlabs, android)
     * @param latencyMs API response latency in milliseconds
     * @param success Whether the TTS call was successful
     */
    fun trackTTSUsed(service: String, latencyMs: Long, success: Boolean) {
        try {
            val bundle = Bundle().apply {
                putString("tts_service", service)
                putLong("latency_ms", latencyMs)
                putLong("success", if (success) 1L else 0L)
                putString(PARAM_CURRENT_TIER, getCurrentUserTier())
            }
            firebaseAnalytics.logEvent(EVENT_TTS_USED, bundle)
            Log.d(TAG, "📊 TTS used: service=$service, latency=${latencyMs}ms, success=$success")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to track TTS used", e)
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

