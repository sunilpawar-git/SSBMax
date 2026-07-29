package com.ssbmax.core.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.ssbmax.shared.domain.model.SubscriptionTier
import com.ssbmax.shared.domain.repository.SubscriptionRepository
import com.ssbmax.shared.domain.repository.UsageInfo
import kotlinx.coroutines.tasks.await

/**
 * Implementation of SubscriptionRepository using Firebase Firestore
 */
class SubscriptionRepositoryImpl(
    private val firestore: FirebaseFirestore
) : SubscriptionRepository {

    companion object {
        private const val TAG = "SubscriptionRepoImpl"
    }

    override suspend fun getSubscriptionTier(userId: String): Result<SubscriptionTier> {
        return RepositoryErrorHandler.executeWithDefault(
            tag = TAG,
            operationName = "load subscription tier for user $userId",
            defaultValue = SubscriptionTier.FREE
        ) {
            val doc = firestore.collection("users")
                .document(userId)
                .collection("data")
                .document("subscription")
                .get()
                .await()

            val tierString = doc.getString("tier") ?: "FREE"
            val tier = when (tierString.uppercase()) {
                "PRO" -> SubscriptionTier.PRO
                "PREMIUM" -> SubscriptionTier.PREMIUM
                else -> SubscriptionTier.FREE
            }

            Log.d(TAG, "Loaded subscription tier for user $userId: $tier")
            tier
        }
    }

    /** Every usage category's PREMIUM limit is unlimited (-1); only FREE/PRO vary by test type. */
    private fun tierLimit(tier: SubscriptionTier, freeLimit: Int, proLimit: Int): Int = when (tier) {
        SubscriptionTier.FREE -> freeLimit
        SubscriptionTier.PRO -> proLimit
        SubscriptionTier.PREMIUM -> -1 // Unlimited
    }

    private fun buildMonthlyUsageMap(
        doc: com.google.firebase.firestore.DocumentSnapshot,
        tier: SubscriptionTier
    ): Map<String, UsageInfo> = mapOf(
        "OIR Tests" to UsageInfo(doc.getLong("oirTestsUsed")?.toInt() ?: 0, tierLimit(tier, 1, 5)),
        "PPDT Tests" to UsageInfo(doc.getLong("ppdtTestsUsed")?.toInt() ?: 0, tierLimit(tier, 1, 5)),
        "PIQ Forms" to UsageInfo(doc.getLong("piqTestsUsed")?.toInt() ?: 0, tierLimit(tier, 1, -1)),
        "TAT Tests" to UsageInfo(doc.getLong("tatTestsUsed")?.toInt() ?: 0, tierLimit(tier, 0, 3)),
        "WAT Tests" to UsageInfo(doc.getLong("watTestsUsed")?.toInt() ?: 0, tierLimit(tier, 0, 3)),
        "SRT Tests" to UsageInfo(doc.getLong("srtTestsUsed")?.toInt() ?: 0, tierLimit(tier, 0, 3)),
        "Self Description" to UsageInfo(doc.getLong("sdTestsUsed")?.toInt() ?: 0, tierLimit(tier, 0, 3)),
        "GTO Tests" to UsageInfo(doc.getLong("gtoTestsUsed")?.toInt() ?: 0, tierLimit(tier, 0, 3)),
        "Interview" to UsageInfo(doc.getLong("interviewTestsUsed")?.toInt() ?: 0, tierLimit(tier, 0, 1))
    )

    override suspend fun getMonthlyUsage(userId: String, month: String): Result<Map<String, UsageInfo>> {
        return try {
            Log.d(TAG, "Loading monthly usage for userId=$userId, month=$month")

            val doc = firestore.collection("users")
                .document(userId)
                .collection("subscription")
                .document("usage_$month")
                .get()
                .await()

            Log.d(TAG, "Firestore doc exists: ${doc.exists()}")

            // Get subscription tier to determine limits
            val tierResult = getSubscriptionTier(userId)
            val tier = tierResult.getOrNull() ?: SubscriptionTier.FREE

            val usageMap = buildMonthlyUsageMap(doc, tier)

            Log.d(TAG, "Successfully loaded ${usageMap.size} usage items")
            usageMap.forEach { (key, value) ->
                Log.d(TAG, "  $key: used=${value.used}, limit=${value.limit}")
            }

            Result.success(usageMap)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load monthly usage for user $userId", e)
            Result.failure(Exception("Failed to load monthly usage: ${e.message}", e))
        }
    }

    override suspend fun updateSubscriptionTier(userId: String, tier: SubscriptionTier): Result<Unit> {
        return RepositoryErrorHandler.executeWithMessage(
            tag = TAG,
            operationName = "update subscription tier for user $userId to $tier",
            errorMessage = "Failed to update subscription tier"
        ) {
            firestore.collection("users")
                .document(userId)
                .collection("data")
                .document("subscription")
                .set(mapOf("tier" to tier.name))
                .await()

            Log.d(TAG, "Updated subscription tier for user $userId to $tier")
        }
    }
}
