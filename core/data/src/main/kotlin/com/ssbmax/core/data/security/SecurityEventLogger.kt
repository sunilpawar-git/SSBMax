package com.ssbmax.core.data.security

import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.logEvent
import com.ssbmax.core.domain.model.TestType
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Centralized security event logger for detecting and tracking suspicious activity
 * 
 * Logs security events to:
 * 1. Android Log (for debugging)
 * 2. Firebase Analytics (for production monitoring)
 * 
 * Key Use Cases:
 * - Unauthenticated test access attempts
 * - Subscription limit bypass attempts
 * - Usage recording failures
 * - Firestore transaction failures
 */
@Singleton
class SecurityEventLogger @Inject constructor(
    private val firebaseAnalytics: FirebaseAnalytics
) {
    
    companion object {
        private const val TAG = "SecurityEventLogger"
        
        // Firebase Analytics Event Names (max 40 chars, alphanumeric + underscore)
        const val EVENT_UNAUTHENTICATED_ACCESS = "sec_unauth_test_access"
        const val EVENT_LIMIT_BYPASS_ATTEMPT = "sec_limit_bypass_attempt"
        const val EVENT_USAGE_RECORD_FAILURE = "sec_usage_record_fail"
        const val EVENT_TRANSACTION_FAILURE = "sec_transaction_fail"
        const val EVENT_LIMIT_REACHED = "sec_limit_reached"
        const val EVENT_SUSPICIOUS_USAGE_PATTERN = "sec_suspicious_pattern"
        
        // Parameter Keys (max 40 chars)
        const val PARAM_USER_ID = "user_id"
        const val PARAM_TEST_TYPE = "test_type"
        const val PARAM_SUBSCRIPTION_TIER = "subscription_tier"
        const val PARAM_TESTS_USED = "tests_used"
        const val PARAM_TESTS_LIMIT = "tests_limit"
        const val PARAM_ERROR_MESSAGE = "error_message"
        const val PARAM_TIMESTAMP = "timestamp"
        const val PARAM_MONTH = "month"
        const val PARAM_SUBMISSION_ID = "submission_id"
    }
    
    /**
     * Log unauthenticated test access attempt
     * CRITICAL: User tried to access test without logging in
     */
    fun logUnauthenticatedAccess(
        testType: TestType,
        context: String = "unknown"
    ) {
        val message = "🚨 SECURITY: Unauthenticated $testType test access blocked from $context"
        Log.e(TAG, message)
        
        firebaseAnalytics.logEvent(EVENT_UNAUTHENTICATED_ACCESS) {
            param(PARAM_TEST_TYPE, testType.name)
            param(PARAM_TIMESTAMP, System.currentTimeMillis())
            param("context", context)
        }
    }
    
    /**
     * Log subscription limit bypass attempt
     * HIGH PRIORITY: User may be trying to bypass subscription limits
     */
    fun logLimitBypassAttempt(
        userId: String,
        testType: TestType,
        subscriptionTier: String,
        testsUsed: Int,
        testsLimit: Int
    ) {
        val message = "🚨 SECURITY: Limit bypass attempt - User $userId ($subscriptionTier) " +
                "tried $testType test with $testsUsed/$testsLimit used"
        Log.w(TAG, message)
        
        firebaseAnalytics.logEvent(EVENT_LIMIT_BYPASS_ATTEMPT) {
            param(PARAM_USER_ID, userId)
            param(PARAM_TEST_TYPE, testType.name)
            param(PARAM_SUBSCRIPTION_TIER, subscriptionTier)
            param(PARAM_TESTS_USED, testsUsed.toLong())
            param(PARAM_TESTS_LIMIT, testsLimit.toLong())
            param(PARAM_TIMESTAMP, System.currentTimeMillis())
        }
    }
    
    /**
     * Log successful limit enforcement
     * INFO: User was properly blocked at subscription limit
     */
    fun logLimitReached(
        userId: String,
        testType: TestType,
        subscriptionTier: String,
        testsUsed: Int,
        testsLimit: Int
    ) {
        val message = "✅ SECURITY: Limit enforced - User $userId ($subscriptionTier) " +
                "blocked at $testsUsed/$testsLimit for $testType"
        Log.d(TAG, message)
        
        firebaseAnalytics.logEvent(EVENT_LIMIT_REACHED) {
            param(PARAM_USER_ID, userId)
            param(PARAM_TEST_TYPE, testType.name)
            param(PARAM_SUBSCRIPTION_TIER, subscriptionTier)
            param(PARAM_TESTS_USED, testsUsed.toLong())
            param(PARAM_TESTS_LIMIT, testsLimit.toLong())
            param(PARAM_TIMESTAMP, System.currentTimeMillis())
        }
    }
    
    /**
     * Log usage recording failure
     * CRITICAL: Failed to record test usage (data integrity issue)
     */
    fun logUsageRecordFailure(
        userId: String,
        testType: TestType,
        errorMessage: String,
        submissionId: String? = null
    ) {
        val message = "❌ SECURITY: Usage recording failed - User $userId, " +
                "Test $testType, Error: $errorMessage"
        Log.e(TAG, message)
        
        firebaseAnalytics.logEvent(EVENT_USAGE_RECORD_FAILURE) {
            param(PARAM_USER_ID, userId)
            param(PARAM_TEST_TYPE, testType.name)
            param(PARAM_ERROR_MESSAGE, errorMessage.take(100)) // Limit length
            param(PARAM_TIMESTAMP, System.currentTimeMillis())
            submissionId?.let { param(PARAM_SUBMISSION_ID, it) }
        }
    }
    
    /**
     * Log Firestore transaction failure
     * HIGH PRIORITY: Atomic operation failed (may cause inconsistency)
     */
    fun logTransactionFailure(
        userId: String,
        testType: TestType,
        month: String,
        errorMessage: String
    ) {
        val message = "❌ SECURITY: Firestore transaction failed - User $userId, " +
                "Test $testType, Month $month, Error: $errorMessage"
        Log.e(TAG, message)
        
        firebaseAnalytics.logEvent(EVENT_TRANSACTION_FAILURE) {
            param(PARAM_USER_ID, userId)
            param(PARAM_TEST_TYPE, testType.name)
            param(PARAM_MONTH, month)
            param(PARAM_ERROR_MESSAGE, errorMessage.take(100))
            param(PARAM_TIMESTAMP, System.currentTimeMillis())
        }
    }
    
    /**
     * Log suspicious usage pattern
     * WARNING: Abnormal usage detected (e.g., rapid consecutive tests)
     */
    fun logSuspiciousPattern(
        userId: String,
        testType: TestType,
        pattern: String,
        details: String
    ) {
        val message = "⚠️ SECURITY: Suspicious pattern - User $userId, " +
                "Test $testType, Pattern: $pattern, Details: $details"
        Log.w(TAG, message)
        
        firebaseAnalytics.logEvent(EVENT_SUSPICIOUS_USAGE_PATTERN) {
            param(PARAM_USER_ID, userId)
            param(PARAM_TEST_TYPE, testType.name)
            param("pattern", pattern)
            param("details", details.take(100))
            param(PARAM_TIMESTAMP, System.currentTimeMillis())
        }
    }
    
    /**
     * Log general security event with custom parameters
     * For ad-hoc security logging needs
     */
    fun logCustomSecurityEvent(
        eventName: String,
        userId: String? = null,
        parameters: Map<String, Any> = emptyMap()
    ) {
        val message = "🔒 SECURITY: $eventName - User: ${userId ?: "N/A"}, Params: $parameters"
        Log.i(TAG, message)
        
        firebaseAnalytics.logEvent(eventName.take(40)) {
            userId?.let { param(PARAM_USER_ID, it) }
            param(PARAM_TIMESTAMP, System.currentTimeMillis())
            
            parameters.forEach { (key, value) ->
                when (value) {
                    is String -> param(key.take(40), value.take(100))
                    is Long -> param(key.take(40), value)
                    is Double -> param(key.take(40), value)
                    is Int -> param(key.take(40), value.toLong())
                    is Boolean -> param(key.take(40), if (value) 1L else 0L)
                }
            }
        }
    }
}

