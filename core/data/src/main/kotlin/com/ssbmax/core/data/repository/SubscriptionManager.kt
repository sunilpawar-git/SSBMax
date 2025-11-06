package com.ssbmax.core.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.ssbmax.core.data.local.dao.TestUsageDao
import com.ssbmax.core.data.local.entity.TestUsageEntity
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
 */
@Singleton
class SubscriptionManager @Inject constructor(
    private val testUsageDao: TestUsageDao,
    private val userProfileRepository: com.ssbmax.core.domain.repository.UserProfileRepository,
    private val firestore: FirebaseFirestore
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
            val limit = getTestLimitForTier(tier)
            
            Log.d(TAG, "📊 Usage: $usedCount/$limit tests this month (from Firestore)")
            
            // Also sync to local Room DB for offline reference
            testUsageDao.insertOrReplace(usage)
            
            return if (usedCount < limit) {
                TestEligibility.Eligible(remainingTests = limit - usedCount)
            } else {
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
                    TestType.GTO -> "gtoTestsUsed"
                    TestType.IO -> "interviewTestsUsed"
                    TestType.SD -> "sdTestsUsed"
                    else -> null
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
    
    private fun getTestLimitForTier(tier: SubscriptionTier): Int {
        return when (tier) {
            SubscriptionTier.FREE -> 1
            SubscriptionTier.PRO -> 5
            SubscriptionTier.PREMIUM -> Int.MAX_VALUE
        }
    }
    
    private fun getUsedCountForTestType(usage: TestUsageEntity, testType: TestType): Int {
        // For simplicity, count ALL tests towards the limit (not per-test-type)
        return usage.oirTestsUsed + usage.tatTestsUsed + usage.watTestsUsed +
               usage.srtTestsUsed + usage.ppdtTestsUsed + usage.gtoTestsUsed
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
                    gtoTestsUsed = (doc.getLong("gtoTestsUsed") ?: 0).toInt(),
                    interviewTestsUsed = (doc.getLong("interviewTestsUsed") ?: 0).toInt(),
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
                "gtoTestsUsed" to usage.gtoTestsUsed,
                "interviewTestsUsed" to usage.interviewTestsUsed,
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

