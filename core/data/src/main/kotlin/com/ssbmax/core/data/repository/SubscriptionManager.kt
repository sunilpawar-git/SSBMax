package com.ssbmax.core.data.repository

import android.util.Log
import com.ssbmax.core.data.local.dao.TestUsageDao
import com.ssbmax.core.data.local.entity.TestUsageEntity
import com.ssbmax.core.domain.model.SubscriptionTier
import com.ssbmax.core.domain.model.TestType
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages subscription limits and test usage tracking
 */
@Singleton
class SubscriptionManager @Inject constructor(
    private val testUsageDao: TestUsageDao,
    private val userProfileRepository: com.ssbmax.core.domain.repository.UserProfileRepository
) {
    private val TAG = "SubscriptionManager"
    
    /**
     * Check if user can take a test based on their subscription tier
     * Returns true if eligible, false if limit reached
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
            
            Log.d(TAG, "Checking eligibility for $testType, tier: $tier")
            
            // Get current month usage
            val currentMonth = getCurrentMonth()
            val usage = testUsageDao.getUsage(userId, currentMonth)
                ?: createNewMonthUsage(userId, currentMonth)
            
            // Get used count for this test type
            val usedCount = getUsedCountForTestType(usage, testType)
            val limit = getTestLimitForTier(tier)
            
            Log.d(TAG, "Usage: $usedCount/$limit tests this month")
            
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
            Log.e(TAG, "Error checking eligibility", e)
            // Default to allowing test in case of error
            return TestEligibility.Eligible(remainingTests = 999)
        }
    }
    
    /**
     * Record that a test was taken
     */
    suspend fun recordTestUsage(testType: TestType, userId: String) {
        try {
            val currentMonth = getCurrentMonth()
            val usage = testUsageDao.getUsage(userId, currentMonth)
                ?: createNewMonthUsage(userId, currentMonth)
            
            val updatedUsage = when (testType) {
                TestType.OIR -> usage.copy(oirTestsUsed = usage.oirTestsUsed + 1)
                TestType.TAT -> usage.copy(tatTestsUsed = usage.tatTestsUsed + 1)
                TestType.WAT -> usage.copy(watTestsUsed = usage.watTestsUsed + 1)
                TestType.SRT -> usage.copy(srtTestsUsed = usage.srtTestsUsed + 1)
                TestType.PPDT -> usage.copy(ppdtTestsUsed = usage.ppdtTestsUsed + 1)
                TestType.GTO -> usage.copy(gtoTestsUsed = usage.gtoTestsUsed + 1)
                else -> usage
            }
            
            testUsageDao.insertOrReplace(updatedUsage.copy(lastUpdated = System.currentTimeMillis()))
            Log.d(TAG, "Recorded $testType usage for $userId")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error recording test usage", e)
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

