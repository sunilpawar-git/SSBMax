package com.ssbmax.shared.data.repository

import com.ssbmax.shared.domain.model.UserProfile
import com.ssbmax.shared.domain.repository.UserProfileRepository
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock

/**
 * GitLive-Firebase-backed port of the Android UserProfileRepositoryImpl.
 * Stores user profiles under the same path (users/{userId}/data/profile).
 * Uses DocumentReference.snapshots (a Flow) in place of the Android SDK's
 * addSnapshotListener/callbackFlow — GitLive exposes the listener as a Flow
 * directly, so no manual awaitClose wiring is needed here.
 */
class GitLiveUserProfileRepository : UserProfileRepository {

    private fun profileDoc(userId: String) = Firebase.firestore
        .collection(USERS_COLLECTION)
        .document(userId)
        .collection("data")
        .document(PROFILE_DOCUMENT)

    override fun getUserProfile(userId: String): Flow<Result<UserProfile?>> =
        profileDoc(userId).snapshots
            .map<_, Result<UserProfile?>> { snapshot ->
                val profile = if (snapshot.exists) {
                    snapshot.data(UserProfileDto.serializer()).toDomain()
                } else {
                    null
                }
                Result.success(profile)
            }
            .catch { emit(Result.failure(it)) }

    override suspend fun saveUserProfile(profile: UserProfile): Result<Unit> {
        return try {
            profileDoc(profile.userId).set(profile.toDto())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateUserProfile(profile: UserProfile): Result<Unit> {
        return try {
            val updated = profile.copy(updatedAt = Clock.System.now().toEpochMilliseconds())
            profileDoc(updated.userId).set(updated.toDto())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun hasCompletedProfile(userId: String): Flow<Boolean> =
        profileDoc(userId).snapshots
            .map { snapshot ->
                if (!snapshot.exists) return@map false
                snapshot.data(UserProfileDto.serializer()).toDomain().isProfileComplete()
            }
            .catch { emit(false) }

    override suspend fun updateLoginStreak(userId: String): Result<Int> {
        return try {
            val docRef = profileDoc(userId)
            val snapshot = docRef.get()
            if (!snapshot.exists) {
                return Result.failure(Exception("User profile not found"))
            }

            val current = snapshot.data(UserProfileDto.serializer()).toDomain()
            val now = Clock.System.now().toEpochMilliseconds()
            val todayStart = todayStartMillis(now)
            val yesterdayStart = todayStart - DAY_MILLIS
            val lastLogin = current.lastLoginDate

            val newStreak = when {
                lastLogin == null -> 1
                lastLogin >= todayStart -> current.currentStreak
                lastLogin >= yesterdayStart -> current.currentStreak + 1
                else -> 1
            }
            val newLongestStreak = maxOf(current.longestStreak, newStreak)

            docRef.update(
                "currentStreak" to newStreak,
                "lastLoginDate" to now,
                "longestStreak" to newLongestStreak,
                "updatedAt" to now
            )
            Result.success(newStreak)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun todayStartMillis(nowEpochMillis: Long): Long {
        val dayRemainder = nowEpochMillis % DAY_MILLIS
        return nowEpochMillis - dayRemainder
    }

    private companion object {
        const val USERS_COLLECTION = "users"
        const val PROFILE_DOCUMENT = "profile"
        const val DAY_MILLIS = 24L * 60 * 60 * 1000
    }
}
