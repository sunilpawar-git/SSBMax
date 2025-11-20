package com.ssbmax.core.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.ssbmax.core.domain.model.SubscriptionTier
import com.ssbmax.core.domain.repository.SubscriptionRepository
import com.ssbmax.core.domain.repository.UsageInfo
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of SubscriptionRepository using Firebase Firestore
 */
@Singleton
class SubscriptionRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : SubscriptionRepository {

    companion object {
        private const val TAG = "SubscriptionRepoImpl"
        private val monthFormat = SimpleDateFormat("yyyy-MM", Locale.US)
    }

    override suspend fun getSubscriptionTier(userId: String): Result<SubscriptionTier> {
        return try {
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
            Result.success(tier)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load subscription tier for user $userId, defaulting to FREE", e)
            Result.success(SubscriptionTier.FREE) // Default to FREE on error
        }
    }

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

            val usageMap = mapOf(
                "OIR Tests" to UsageInfo(
                    used = doc.getLong("oirTestsUsed")?.toInt() ?: 0,
                    limit = when (tier) {
                        SubscriptionTier.FREE -> 1
                        SubscriptionTier.PRO -> 5
                        SubscriptionTier.PREMIUM -> -1 // Unlimited
                    }
                ),
                "PPDT Tests" to UsageInfo(
                    used = doc.getLong("ppdtTestsUsed")?.toInt() ?: 0,
                    limit = when (tier) {
                        SubscriptionTier.FREE -> 1
                        SubscriptionTier.PRO -> 5
                        SubscriptionTier.PREMIUM -> -1
                    }
                ),
                "PIQ Forms" to UsageInfo(
                    used = doc.getLong("piqTestsUsed")?.toInt() ?: 0,
                    limit = when (tier) {
                        SubscriptionTier.FREE -> 1
                        SubscriptionTier.PRO -> -1
                        SubscriptionTier.PREMIUM -> -1
                    }
                ),
                "TAT Tests" to UsageInfo(
                    used = doc.getLong("tatTestsUsed")?.toInt() ?: 0,
                    limit = when (tier) {
                        SubscriptionTier.FREE -> 0
                        SubscriptionTier.PRO -> 3
                        SubscriptionTier.PREMIUM -> -1
                    }
                ),
                "WAT Tests" to UsageInfo(
                    used = doc.getLong("watTestsUsed")?.toInt() ?: 0,
                    limit = when (tier) {
                        SubscriptionTier.FREE -> 0
                        SubscriptionTier.PRO -> 3
                        SubscriptionTier.PREMIUM -> -1
                    }
                ),
                "SRT Tests" to UsageInfo(
                    used = doc.getLong("srtTestsUsed")?.toInt() ?: 0,
                    limit = when (tier) {
                        SubscriptionTier.FREE -> 0
                        SubscriptionTier.PRO -> 3
                        SubscriptionTier.PREMIUM -> -1
                    }
                ),
                "Self Description" to UsageInfo(
                    used = doc.getLong("sdTestsUsed")?.toInt() ?: 0,
                    limit = when (tier) {
                        SubscriptionTier.FREE -> 0
                        SubscriptionTier.PRO -> 3
                        SubscriptionTier.PREMIUM -> -1
                    }
                ),
                "GTO Tests" to UsageInfo(
                    used = doc.getLong("gtoTestsUsed")?.toInt() ?: 0,
                    limit = when (tier) {
                        SubscriptionTier.FREE -> 0
                        SubscriptionTier.PRO -> 3
                        SubscriptionTier.PREMIUM -> -1
                    }
                ),
                "Interview" to UsageInfo(
                    used = doc.getLong("interviewTestsUsed")?.toInt() ?: 0,
                    limit = when (tier) {
                        SubscriptionTier.FREE -> 0
                        SubscriptionTier.PRO -> 1
                        SubscriptionTier.PREMIUM -> -1
                    }
                )
            )

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
        return try {
            firestore.collection("users")
                .document(userId)
                .collection("data")
                .document("subscription")
                .set(mapOf("tier" to tier.name))
                .await()

            Log.d(TAG, "Updated subscription tier for user $userId to $tier")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update subscription tier for user $userId", e)
            Result.failure(Exception("Failed to update subscription tier: ${e.message}", e))
        }
    }

    /**
     * Get the current month in yyyy-MM format
     */
    private fun getCurrentMonth(): String {
        return monthFormat.format(Date())
    }
}
