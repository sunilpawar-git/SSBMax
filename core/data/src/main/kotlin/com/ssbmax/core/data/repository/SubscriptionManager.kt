package com.ssbmax.core.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.ssbmax.core.data.local.dao.TestUsageDao
import com.ssbmax.core.data.local.entity.TestUsageEntity
import com.ssbmax.core.data.security.SecurityEventLogger
import com.ssbmax.core.domain.model.SubscriptionTier
import com.ssbmax.core.domain.model.TestType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages subscription limits and test usage tracking
 * 
 * SECURITY: Usage data is stored in Firestore (server-side) to prevent bypass via cache clearing
 * 
 * 🔒 IMPORTANT: When implementing new test ViewModels (GTO, IO, SD), ensure they:
 * 1. Call canTakeTest() BEFORE loading test content
 * 2. Call recordTestUsage() AFTER successful submission
 * 3. Implement authentication guards (observeCurrentUser)
 * 4. Log security events via SecurityEventLogger
 * 
 * See: docs/SECURITY_CHECKLIST.md for complete implementation guide
 * Reference: app/src/main/kotlin/com/ssbmax/ui/tests/tat/TATTestViewModel.kt
 */
@Singleton
class SubscriptionManager @Inject constructor(
    private val testUsageDao: TestUsageDao,
    private val userProfileRepository: com.ssbmax.core.domain.repository.UserProfileRepository,
    private val firestore: FirebaseFirestore,
    private val securityLogger: SecurityEventLogger,
    private val debugConfig: com.ssbmax.core.data.debug.DebugConfig
) {
    private val TAG = "SubscriptionManager"
    
    /**
     * Check if user can take a test based on their subscription tier
     * Returns true if eligible, false if limit reached
     * 
     * SECURITY: Reads from Firestore (server-side) to prevent cache-clearing bypass
     */
    suspend fun canTakeTest(testType: TestType, userId: String): TestEligibility {
        try {
            // 🔓 DEBUG BYPASS: Allow unlimited tests in debug builds
            // This is ONLY active in debug variant, production builds have this disabled
            // APPLIES TO ALL TESTS: OIR, PPDT, PIQ, WAT, SRT, TAT, GTO, Self Description, Interview
            Log.d(TAG, "🔍 Debug bypass status: ${debugConfig.bypassSubscriptionLimits}")
            if (debugConfig.bypassSubscriptionLimits) {
                Log.w(TAG, "════════════════════════════════════════")
                Log.w(TAG, "🔓 DEBUG BYPASS ACTIVE!")
                Log.w(TAG, "   Unlimited tests enabled for $testType")
                Log.w(TAG, "   User: $userId")
                Log.w(TAG, "   Returning: 999 remaining tests")
                Log.w(TAG, "⚠️  This bypass is ONLY active when BYPASS_SUBSCRIPTION_LIMITS=true")
                Log.w(TAG, "📋 Affected tests: ALL (OIR, PPDT, PIQ, WAT, SRT, TAT, GTO, SD, Interview)")
                Log.w(TAG, "💡 To test real subscription limits, set BYPASS_SUBSCRIPTION_LIMITS=false in build.gradle.kts")
                Log.w(TAG, "════════════════════════════════════════")
                return TestEligibility.Eligible(remainingTests = 999)
            } else {
                Log.d(TAG, "✅ Debug bypass is DISABLED - checking real subscription limits")
            }
            
            // Get user's subscription tier
            val userProfile = userProfileRepository.getUserProfile(userId).first().getOrNull()
            // Convert SubscriptionType to SubscriptionTier
            val tier = when (userProfile?.subscriptionType) {
                com.ssbmax.core.domain.model.SubscriptionType.PRO -> SubscriptionTier.PRO
                com.ssbmax.core.domain.model.SubscriptionType.PREMIUM -> SubscriptionTier.PREMIUM
                else -> SubscriptionTier.FREE
            }
            
            Log.d(TAG, "🔍 Checking eligibility for $testType, tier: $tier")
            
            // Get current month usage FROM FIRESTORE (server-side)
            val currentMonth = getCurrentMonth()
            val usage = getUsageFromFirestore(userId, currentMonth)
            
            // Get used count for this test type
            val usedCount = getUsedCountForTestType(usage, testType)
            val limit = getTestLimitForTier(tier, testType)
            
            Log.d(TAG, "📊 Usage: $usedCount/$limit tests this month for $testType (from Firestore)")
            
            // Also sync to local Room DB for offline reference
            testUsageDao.insertOrReplace(usage)
            
            return if (usedCount < limit) {
                TestEligibility.Eligible(remainingTests = limit - usedCount)
            } else {
                // SECURITY: Log when limit is properly enforced
                securityLogger.logLimitReached(
                    userId = userId,
                    testType = testType,
                    subscriptionTier = tier.name,
                    testsUsed = usedCount,
                    testsLimit = limit
                )
                
                TestEligibility.LimitReached(
                    tier = tier,
                    limit = limit,
                    usedCount = usedCount,
                    resetsAt = getMonthResetDate()
                )
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error checking eligibility", e)
            // SECURITY: On error, block test to prevent bypass
            return TestEligibility.LimitReached(
                tier = SubscriptionTier.FREE,
                limit = 1,
                usedCount = 1,
                resetsAt = getMonthResetDate()
            )
        }
    }
    
    /**
     * Record that a test was taken
     * 
     * SECURITY: Uses Firestore transaction for atomic increment
     * RACE CONDITION PREVENTION: Multiple simultaneous submissions handled correctly
     */
    suspend fun recordTestUsage(testType: TestType, userId: String, submissionId: String? = null) {
        try {
            val currentMonth = getCurrentMonth()
            val docRef = firestore.collection("users")
                .document(userId)
                .collection("subscription")
                .document("usage_$currentMonth")
            
            // Use Firestore Transaction for atomic operations
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                
                // Idempotency check: Prevent duplicate recording for same submission
                if (submissionId != null && snapshot.exists()) {
                    val recordedSubmissions = snapshot.get("recordedSubmissions") as? List<*> ?: emptyList<String>()
                    if (recordedSubmissions.contains(submissionId)) {
                        Log.d(TAG, "⚠️ Submission $submissionId already recorded, skipping (idempotent)")
                        return@runTransaction // Exit transaction without recording
                    }
                }
                
                if (!snapshot.exists()) {
                    // Create new document with initial data
                    val initialData = hashMapOf(
                        "userId" to userId,
                        "month" to currentMonth,
                        "oirTestsUsed" to 0,
                        "tatTestsUsed" to 0,
                        "watTestsUsed" to 0,
                        "srtTestsUsed" to 0,
                        "ppdtTestsUsed" to 0,
                        "gtoTestsUsed" to 0,
                        "interviewTestsUsed" to 0,
                        "sdTestsUsed" to 0,
                        "lastUpdated" to System.currentTimeMillis(),
                        "recordedSubmissions" to (if (submissionId != null) listOf(submissionId) else emptyList<String>())
                    )
                    transaction.set(docRef, initialData)
                    Log.d(TAG, "📝 Created new usage document for $currentMonth")
                } else {
                    // Document exists, update with atomic increment
                    val updates = hashMapOf<String, Any>(
                        "lastUpdated" to System.currentTimeMillis()
                    )
                    
                    // Add submission ID to recorded list if provided
                    if (submissionId != null) {
                        val existingSubmissions = snapshot.get("recordedSubmissions") as? List<*> ?: emptyList<String>()
                        updates["recordedSubmissions"] = existingSubmissions + submissionId
                    }
                    
                    transaction.update(docRef, updates)
                }
                
                // Atomic increment using FieldValue (prevents race conditions)
                val fieldName = when (testType) {
                    TestType.OIR -> "oirTestsUsed"
                    TestType.TAT -> "tatTestsUsed"
                    TestType.WAT -> "watTestsUsed"
                    TestType.SRT -> "srtTestsUsed"
                    TestType.PPDT -> "ppdtTestsUsed"
                    TestType.PIQ -> "piqTestsUsed"
                    // GTO Tasks
                    TestType.GTO_GD, TestType.GTO_GPE, TestType.GTO_PGT, TestType.GTO_GOR,
                    TestType.GTO_HGT, TestType.GTO_LECTURETTE, TestType.GTO_IO, TestType.GTO_CT -> "gtoTestsUsed"
                    TestType.IO -> "interviewTestsUsed"
                    TestType.SD -> "sdTestsUsed"
                }
                
                if (fieldName != null) {
                    transaction.update(docRef, fieldName, com.google.firebase.firestore.FieldValue.increment(1))
                }
            }.await()
            
            // After successful Firestore transaction, update local Room DB
            val usage = getUsageFromFirestore(userId, currentMonth)
            testUsageDao.insertOrReplace(usage)
            
            Log.d(TAG, "✅ Atomically recorded $testType usage for $userId (Firestore transaction)")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error recording test usage atomically", e)
            
            // SECURITY: Log transaction failure for monitoring
            securityLogger.logTransactionFailure(
                userId = userId,
                testType = testType,
                month = getCurrentMonth(),
                errorMessage = e.message ?: "Unknown error"
            )
            
            throw e // Propagate error so ViewModels know submission failed
        }
    }
    
    /**
     * Get total tests used this month
     */
    suspend fun getTotalTestsUsedThisMonth(userId: String): Int {
        return try {
            val currentMonth = getCurrentMonth()
            val usage = testUsageDao.getUsage(userId, currentMonth)
            
            usage?.let {
                it.oirTestsUsed + it.tatTestsUsed + it.watTestsUsed +
                it.srtTestsUsed + it.ppdtTestsUsed + it.gtoTestsUsed
            } ?: 0
        } catch (e: Exception) {
            Log.e(TAG, "Error getting total usage", e)
            0
        }
    }
    
    // Helper functions
    
    /**
     * Get test limit for a specific test type and tier
     * SINGLE SOURCE OF TRUTH for subscription limits
     */
    private fun getTestLimitForTier(tier: SubscriptionTier, testType: TestType): Int {
        return when (tier) {
            SubscriptionTier.FREE -> when (testType) {
                TestType.OIR -> 1
                TestType.PPDT -> 1
                TestType.PIQ -> 1
                TestType.TAT -> 0
                TestType.WAT -> 0
                TestType.SRT -> 0
                TestType.SD -> 0  // Self Description
                // GTO Tests (8 individual tests, each with separate limits)
                TestType.GTO_GD, TestType.GTO_GPE, TestType.GTO_PGT, TestType.GTO_GOR,
                TestType.GTO_HGT, TestType.GTO_LECTURETTE, TestType.GTO_IO, TestType.GTO_CT -> 0
                TestType.IO -> 0  // Interview Officer
            }
            SubscriptionTier.PRO -> when (testType) {
                TestType.OIR -> 5
                TestType.PPDT -> 5
                TestType.PIQ -> Int.MAX_VALUE  // Unlimited
                TestType.TAT -> 3
                TestType.WAT -> 3
                TestType.SRT -> 3
                TestType.SD -> 3  // Self Description
                // GTO Tests: 3 attempts per sub-test
                TestType.GTO_GD, TestType.GTO_GPE, TestType.GTO_PGT, TestType.GTO_GOR,
                TestType.GTO_HGT, TestType.GTO_LECTURETTE, TestType.GTO_IO, TestType.GTO_CT -> 3
                TestType.IO -> 1  // Interview Officer
            }
            SubscriptionTier.PREMIUM -> Int.MAX_VALUE  // Unlimited for all
        }
    }
    
    /**
     * Get used count for a specific test type
     * Returns the actual count for that test type only
     */
    private fun getUsedCountForTestType(usage: TestUsageEntity, testType: TestType): Int {
        return when (testType) {
            TestType.OIR -> usage.oirTestsUsed
            TestType.PPDT -> usage.ppdtTestsUsed
            TestType.PIQ -> usage.piqTestsUsed
            TestType.TAT -> usage.tatTestsUsed
            TestType.WAT -> usage.watTestsUsed
            TestType.SRT -> usage.srtTestsUsed
            TestType.SD -> usage.sdTestsUsed  // Self Description
            // All GTO tests count towards gtoTestsUsed
            TestType.GTO_GD, TestType.GTO_GPE, TestType.GTO_PGT, TestType.GTO_GOR,
            TestType.GTO_HGT, TestType.GTO_LECTURETTE, TestType.GTO_IO, TestType.GTO_CT -> usage.gtoTestsUsed
            TestType.IO -> usage.interviewTestsUsed  // Interview Officer
        }
    }
    
    private fun getCurrentMonth(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM", Locale.US)
        return dateFormat.format(Date())
    }
    
    private fun getMonthResetDate(): String {
        val dateFormat = SimpleDateFormat("MMM 1, yyyy", Locale.US)
        val calendar = java.util.Calendar.getInstance()
        calendar.add(java.util.Calendar.MONTH, 1)
        calendar.set(java.util.Calendar.DAY_OF_MONTH, 1)
        return dateFormat.format(calendar.time)
    }
    
    private suspend fun createNewMonthUsage(userId: String, month: String): TestUsageEntity {
        val newUsage = TestUsageEntity(
            id = "${userId}_$month",
            userId = userId,
            month = month
        )
        testUsageDao.insertOrReplace(newUsage)
        return newUsage
    }
    
    /**
     * Get usage from Firestore (server-side source of truth)
     * SECURITY: Cannot be bypassed by clearing app cache
     */
    private suspend fun getUsageFromFirestore(userId: String, month: String): TestUsageEntity {
        return try {
            val doc = firestore.collection("users")
                .document(userId)
                .collection("subscription")
                .document("usage_$month")
                .get()
                .await()
            
            if (doc.exists()) {
                Log.d(TAG, "📥 Retrieved usage from Firestore for $month")
                TestUsageEntity(
                    id = "${userId}_$month",
                    userId = userId,
                    month = month,
                    oirTestsUsed = (doc.getLong("oirTestsUsed") ?: 0).toInt(),
                    tatTestsUsed = (doc.getLong("tatTestsUsed") ?: 0).toInt(),
                    watTestsUsed = (doc.getLong("watTestsUsed") ?: 0).toInt(),
                    srtTestsUsed = (doc.getLong("srtTestsUsed") ?: 0).toInt(),
                    ppdtTestsUsed = (doc.getLong("ppdtTestsUsed") ?: 0).toInt(),
                    piqTestsUsed = (doc.getLong("piqTestsUsed") ?: 0).toInt(),
                    gtoTestsUsed = (doc.getLong("gtoTestsUsed") ?: 0).toInt(),
                    interviewTestsUsed = (doc.getLong("interviewTestsUsed") ?: 0).toInt(),
                    sdTestsUsed = (doc.getLong("sdTestsUsed") ?: 0).toInt(),
                    lastUpdated = doc.getLong("lastUpdated") ?: System.currentTimeMillis()
                )
            } else {
                Log.d(TAG, "📝 No Firestore usage found for $month, creating new record")
                // Create new usage in Firestore
                val newUsage = TestUsageEntity(
                    id = "${userId}_$month",
                    userId = userId,
                    month = month
                )
                saveUsageToFirestore(userId, month, newUsage)
                newUsage
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getting Firestore usage", e)
            throw e
        }
    }
    
    /**
     * Save usage to Firestore (server-side persistence)
     * SECURITY: Cannot be bypassed by clearing app cache
     */
    private suspend fun saveUsageToFirestore(userId: String, month: String, usage: TestUsageEntity) {
        try {
            val data = hashMapOf(
                "userId" to userId,
                "month" to month,
                "oirTestsUsed" to usage.oirTestsUsed,
                "tatTestsUsed" to usage.tatTestsUsed,
                "watTestsUsed" to usage.watTestsUsed,
                "srtTestsUsed" to usage.srtTestsUsed,
                "ppdtTestsUsed" to usage.ppdtTestsUsed,
                "piqTestsUsed" to usage.piqTestsUsed,
                "gtoTestsUsed" to usage.gtoTestsUsed,
                "interviewTestsUsed" to usage.interviewTestsUsed,
                "sdTestsUsed" to usage.sdTestsUsed,
                "lastUpdated" to usage.lastUpdated
            )
            
            firestore.collection("users")
                .document(userId)
                .collection("subscription")
                .document("usage_$month")
                .set(data)
                .await()
            
            Log.d(TAG, "📤 Saved usage to Firestore for $month")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error saving usage to Firestore", e)
            throw e
        }
    }
}

/**
 * Result of eligibility check
 */
sealed class TestEligibility {
    data class Eligible(val remainingTests: Int) : TestEligibility()
    data class LimitReached(
        val tier: SubscriptionTier,
        val limit: Int,
        val usedCount: Int,
        val resetsAt: String
    ) : TestEligibility()
}

