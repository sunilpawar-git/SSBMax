package com.ssbmax.core.data.remote

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.ssbmax.core.domain.model.BillingCycle
import com.ssbmax.core.domain.model.InstructorProfile
import com.ssbmax.core.domain.model.SSBMaxUser
import com.ssbmax.core.domain.model.StudentProfile
import com.ssbmax.core.domain.model.SubscriptionTier
import com.ssbmax.core.domain.model.UserRole
import com.ssbmax.core.domain.model.UserSubscription
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firestore User Repository
 * Manages user profiles in Firestore
 */
@Singleton
class FirestoreUserRepository @Inject constructor() {

    private val firestore = FirebaseFirestore.getInstance()
    private val usersCollection = firestore.collection("users")

    companion object {
        private const val FIELD_ID = "id"
        private const val FIELD_EMAIL = "email"
        private const val FIELD_DISPLAY_NAME = "displayName"
        private const val FIELD_PHOTO_URL = "photoUrl"
        private const val FIELD_ROLE = "role"
        private const val FIELD_SUBSCRIPTION_TIER = "subscriptionTier"
        private const val FIELD_SUBSCRIPTION = "subscription"
        private const val FIELD_CREATED_AT = "createdAt"
        private const val FIELD_LAST_LOGIN_AT = "lastLoginAt"
        private const val FIELD_STUDENT_PROFILE = "studentProfile"
        private const val FIELD_INSTRUCTOR_PROFILE = "instructorProfile"
    }

    /**
     * Create or update user in Firestore
     */
    suspend fun saveUser(user: SSBMaxUser): Result<Unit> {
        return try {
            val userMap = mapOf(
                FIELD_ID to user.id,
                FIELD_EMAIL to user.email,
                FIELD_DISPLAY_NAME to user.displayName,
                FIELD_PHOTO_URL to user.photoUrl,
                FIELD_ROLE to user.role.name,
                FIELD_SUBSCRIPTION_TIER to user.subscriptionTier.name,
                FIELD_SUBSCRIPTION to user.subscription?.toMap(),
                FIELD_CREATED_AT to user.createdAt,
                FIELD_LAST_LOGIN_AT to user.lastLoginAt,
                FIELD_STUDENT_PROFILE to user.studentProfile?.toMap(),
                FIELD_INSTRUCTOR_PROFILE to user.instructorProfile?.toMap()
            )
            
            usersCollection.document(user.id)
                .set(userMap, SetOptions.merge())
                .await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to save user: ${e.message}", e))
        }
    }

    /**
     * Get user by ID
     */
    suspend fun getUser(userId: String): Result<SSBMaxUser?> {
        return try {
            val document = usersCollection.document(userId).get().await()
            
            if (!document.exists()) {
                return Result.success(null)
            }
            
            val user = document.toSSBMaxUser()
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to get user: ${e.message}", e))
        }
    }

    /**
     * Get user by email
     */
    suspend fun getUserByEmail(email: String): Result<SSBMaxUser?> {
        return try {
            val querySnapshot = usersCollection
                .whereEqualTo(FIELD_EMAIL, email)
                .limit(1)
                .get()
                .await()
            
            if (querySnapshot.isEmpty) {
                return Result.success(null)
            }
            
            val user = querySnapshot.documents.first().toSSBMaxUser()
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to get user by email: ${e.message}", e))
        }
    }

    /**
     * Observe user changes in real-time
     */
    fun observeUser(userId: String): Flow<SSBMaxUser?> = callbackFlow {
        val registration = usersCollection.document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                if (snapshot != null && snapshot.exists()) {
                    val user = snapshot.toSSBMaxUser()
                    trySend(user)
                } else {
                    trySend(null)
                }
            }
        
        awaitClose {
            registration.remove()
        }
    }

    /**
     * Update user role
     */
    suspend fun updateUserRole(userId: String, role: UserRole): Result<Unit> {
        return try {
            usersCollection.document(userId)
                .update(FIELD_ROLE, role.name)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to update user role: ${e.message}", e))
        }
    }

    /**
     * Update last login timestamp
     */
    suspend fun updateLastLogin(userId: String): Result<Unit> {
        return try {
            usersCollection.document(userId)
                .update(FIELD_LAST_LOGIN_AT, System.currentTimeMillis())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to update last login: ${e.message}", e))
        }
    }

    /**
     * Update student profile
     */
    suspend fun updateStudentProfile(userId: String, profile: StudentProfile): Result<Unit> {
        return try {
            usersCollection.document(userId)
                .update(FIELD_STUDENT_PROFILE, profile.toMap())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to update student profile: ${e.message}", e))
        }
    }

    /**
     * Update instructor profile
     */
    suspend fun updateInstructorProfile(userId: String, profile: InstructorProfile): Result<Unit> {
        return try {
            usersCollection.document(userId)
                .update(FIELD_INSTRUCTOR_PROFILE, profile.toMap())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to update instructor profile: ${e.message}", e))
        }
    }

    /**
     * Delete user
     */
    suspend fun deleteUser(userId: String): Result<Unit> {
        return try {
            usersCollection.document(userId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to delete user: ${e.message}", e))
        }
    }
}

/**
 * Extension functions for mapping between Firestore and domain models
 */
private fun com.google.firebase.firestore.DocumentSnapshot.toSSBMaxUser(): SSBMaxUser? {
    return try {
        SSBMaxUser(
            id = getString("id") ?: return null,
            email = getString("email") ?: return null,
            displayName = getString("displayName") ?: return null,
            photoUrl = getString("photoUrl"),
            role = UserRole.valueOf(getString("role") ?: "STUDENT"),
            subscriptionTier = SubscriptionTier.valueOf(getString("subscriptionTier") ?: "BASIC"),
            subscription = get("subscription")?.let { mapToUserSubscription(it as? Map<*, *>) },
            createdAt = getLong("createdAt") ?: System.currentTimeMillis(),
            lastLoginAt = getLong("lastLoginAt") ?: System.currentTimeMillis(),
            studentProfile = get("studentProfile")?.let { mapToStudentProfile(it as? Map<*, *>) },
            instructorProfile = get("instructorProfile")?.let { mapToInstructorProfile(it as? Map<*, *>) }
        )
    } catch (e: Exception) {
        null
    }
}

private fun mapToStudentProfile(map: Map<*, *>?): StudentProfile? {
    if (map == null) return null
    return try {
        StudentProfile(
            userId = map["userId"] as? String ?: return null,
            currentBatchIds = (map["currentBatchIds"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
            totalTestsAttempted = (map["totalTestsAttempted"] as? Long)?.toInt() ?: 0,
            totalStudyHours = (map["totalStudyHours"] as? Double)?.toFloat() ?: 0f,
            currentStreak = (map["currentStreak"] as? Long)?.toInt() ?: 0,
            longestStreak = (map["longestStreak"] as? Long)?.toInt() ?: 0,
            achievements = (map["achievements"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
        )
    } catch (e: Exception) {
        null
    }
}

private fun mapToInstructorProfile(map: Map<*, *>?): InstructorProfile? {
    if (map == null) return null
    return try {
        InstructorProfile(
            userId = map["userId"] as? String ?: return null,
            specialization = (map["specialization"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
            batchesCreated = (map["batchesCreated"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
            totalStudents = (map["totalStudents"] as? Long)?.toInt() ?: 0,
            totalTestsGraded = (map["totalTestsGraded"] as? Long)?.toInt() ?: 0,
            averageGradingTime = map["averageGradingTime"] as? Long ?: 0L,
            rating = (map["rating"] as? Double)?.toFloat() ?: 0f,
            bio = map["bio"] as? String,
            certifications = (map["certifications"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
        )
    } catch (e: Exception) {
        null
    }
}

private fun StudentProfile.toMap(): Map<String, Any?> {
    return mapOf(
        "userId" to userId,
        "currentBatchIds" to currentBatchIds,
        "totalTestsAttempted" to totalTestsAttempted,
        "totalStudyHours" to totalStudyHours,
        "currentStreak" to currentStreak,
        "longestStreak" to longestStreak,
        "achievements" to achievements
    )
}

private fun InstructorProfile.toMap(): Map<String, Any?> {
    return mapOf(
        "userId" to userId,
        "specialization" to specialization,
        "batchesCreated" to batchesCreated,
        "totalStudents" to totalStudents,
        "totalTestsGraded" to totalTestsGraded,
        "averageGradingTime" to averageGradingTime,
        "rating" to rating,
        "bio" to bio,
        "certifications" to certifications
    )
}

private fun mapToUserSubscription(map: Map<*, *>?): UserSubscription? {
    if (map == null) return null
    return try {
        UserSubscription(
            userId = map["userId"] as? String ?: return null,
            tier = SubscriptionTier.valueOf(map["tier"] as? String ?: "BASIC"),
            subscriptionId = map["subscriptionId"] as? String,
            startDate = (map["startDate"] as? Number)?.toLong() ?: System.currentTimeMillis(),
            expiryDate = (map["expiryDate"] as? Number)?.toLong(),
            autoRenew = map["autoRenew"] as? Boolean ?: false,
            isActive = map["isActive"] as? Boolean ?: true,
            billingCycle = BillingCycle.valueOf(map["billingCycle"] as? String ?: "MONTHLY")
        )
    } catch (e: Exception) {
        null
    }
}

private fun UserSubscription.toMap(): Map<String, Any?> {
    return mapOf(
        "userId" to userId,
        "tier" to tier.name,
        "subscriptionId" to subscriptionId,
        "startDate" to startDate,
        "expiryDate" to expiryDate,
        "autoRenew" to autoRenew,
        "isActive" to isActive,
        "billingCycle" to billingCycle.name
    )
}

