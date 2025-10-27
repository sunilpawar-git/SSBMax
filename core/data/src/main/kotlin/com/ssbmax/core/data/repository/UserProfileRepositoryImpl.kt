package com.ssbmax.core.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.ssbmax.core.domain.model.EntryType
import com.ssbmax.core.domain.model.Gender
import com.ssbmax.core.domain.model.SubscriptionType
import com.ssbmax.core.domain.model.UserProfile
import com.ssbmax.core.domain.repository.UserProfileRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of UserProfileRepository using Firestore.
 * Stores user profiles under: users/{userId}/profile
 */
@Singleton
class UserProfileRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : UserProfileRepository {

    private companion object {
        const val USERS_COLLECTION = "users"
        const val PROFILE_DOCUMENT = "profile"
    }

    override fun getUserProfile(userId: String): Flow<Result<UserProfile?>> = callbackFlow {
        val docRef = firestore.collection(USERS_COLLECTION)
            .document(userId)
            .collection("data")
            .document(PROFILE_DOCUMENT)

        val listener = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(Result.failure(error))
                return@addSnapshotListener
            }

            val profile = snapshot?.let { doc ->
                if (doc.exists()) doc.toUserProfile() else null
            }
            trySend(Result.success(profile))
        }

        awaitClose { listener.remove() }
    }

    override suspend fun saveUserProfile(profile: UserProfile): Result<Unit> {
        return try {
            val docRef = firestore.collection(USERS_COLLECTION)
                .document(profile.userId)
                .collection("data")
                .document(PROFILE_DOCUMENT)

            docRef.set(profile.toMap()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateUserProfile(profile: UserProfile): Result<Unit> {
        return try {
            val docRef = firestore.collection(USERS_COLLECTION)
                .document(profile.userId)
                .collection("data")
                .document(PROFILE_DOCUMENT)

            val updatedProfile = profile.copy(updatedAt = System.currentTimeMillis())
            docRef.set(updatedProfile.toMap()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun hasCompletedProfile(userId: String): Flow<Boolean> = callbackFlow {
        val docRef = firestore.collection(USERS_COLLECTION)
            .document(userId)
            .collection("data")
            .document(PROFILE_DOCUMENT)

        val listener = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(false)
                return@addSnapshotListener
            }

            val profile = snapshot?.let { doc ->
                if (doc.exists()) doc.toUserProfile() else null
            }
            trySend(profile?.isComplete() ?: false)
        }

        awaitClose { listener.remove() }
    }

    override suspend fun updateLoginStreak(userId: String): Result<Int> {
        return try {
            val docRef = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection("data")
                .document(PROFILE_DOCUMENT)

            // Get current profile
            val snapshot = docRef.get().await()
            if (!snapshot.exists()) {
                return Result.failure(Exception("User profile not found"))
            }

            val currentProfile = snapshot.toUserProfile()
            val now = System.currentTimeMillis()
            val todayStart = getTodayStartMillis()
            val yesterdayStart = todayStart - (24 * 60 * 60 * 1000)
            
            // Store lastLoginDate in local variable for smart cast
            val lastLogin = currentProfile.lastLoginDate

            // Calculate new streak
            val newStreak = when {
                // First login ever or no lastLoginDate
                lastLogin == null -> 1
                
                // Already logged in today - keep current streak
                lastLogin >= todayStart -> currentProfile.currentStreak
                
                // Last login was yesterday - increment streak
                lastLogin >= yesterdayStart && lastLogin < todayStart -> 
                    currentProfile.currentStreak + 1
                
                // Last login was before yesterday - reset to 1
                else -> 1
            }

            val newLongestStreak = maxOf(currentProfile.longestStreak, newStreak)

            // Update only streak-related fields
            val updates = mapOf(
                "currentStreak" to newStreak,
                "lastLoginDate" to now,
                "longestStreak" to newLongestStreak,
                "updatedAt" to now
            )

            docRef.update(updates).await()
            Result.success(newStreak)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun getTodayStartMillis(): Long {
        val calendar = java.util.Calendar.getInstance()
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private fun UserProfile.isComplete(): Boolean {
        return fullName.isNotBlank() && age > 0
    }

    // Mappers
    private fun UserProfile.toMap(): Map<String, Any?> = mapOf(
        "userId" to userId,
        "fullName" to fullName,
        "age" to age,
        "gender" to gender.name,
        "entryType" to entryType.name,
        "profilePictureUrl" to profilePictureUrl,
        "subscriptionType" to subscriptionType.name,
        "currentStreak" to currentStreak,
        "lastLoginDate" to lastLoginDate,
        "longestStreak" to longestStreak,
        "createdAt" to createdAt,
        "updatedAt" to updatedAt
    )

    private fun com.google.firebase.firestore.DocumentSnapshot.toUserProfile(): UserProfile {
        return UserProfile(
            userId = getString("userId") ?: "",
            fullName = getString("fullName") ?: "",
            age = getLong("age")?.toInt() ?: 0,
            gender = getString("gender")?.let { Gender.valueOf(it) } ?: Gender.MALE,
            entryType = getString("entryType")?.let { EntryType.valueOf(it) } ?: EntryType.GRADUATE,
            profilePictureUrl = getString("profilePictureUrl"),
            subscriptionType = getString("subscriptionType")?.let { SubscriptionType.valueOf(it) } ?: SubscriptionType.FREE,
            currentStreak = getLong("currentStreak")?.toInt() ?: 0,
            lastLoginDate = getLong("lastLoginDate"),
            longestStreak = getLong("longestStreak")?.toInt() ?: 0,
            createdAt = getLong("createdAt") ?: System.currentTimeMillis(),
            updatedAt = getLong("updatedAt") ?: System.currentTimeMillis()
        )
    }
}

