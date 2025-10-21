package com.ssbmax.core.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.ssbmax.core.domain.model.EntryType
import com.ssbmax.core.domain.model.Gender
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

    override fun hasCompletedProfile(userId: String): Flow<Boolean> {
        return getUserProfile(userId).map { result ->
            result.getOrNull() != null
        }
    }

    // Mappers
    private fun UserProfile.toMap(): Map<String, Any?> = mapOf(
        "userId" to userId,
        "fullName" to fullName,
        "age" to age,
        "gender" to gender.name,
        "entryType" to entryType.name,
        "profilePictureUrl" to profilePictureUrl,
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
            createdAt = getLong("createdAt") ?: System.currentTimeMillis(),
            updatedAt = getLong("updatedAt") ?: System.currentTimeMillis()
        )
    }
}

