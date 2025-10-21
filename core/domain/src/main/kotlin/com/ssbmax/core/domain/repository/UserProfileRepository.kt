package com.ssbmax.core.domain.repository

import com.ssbmax.core.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing user profile data.
 * Handles CRUD operations for user profiles stored in Firestore.
 */
interface UserProfileRepository {
    /**
     * Observes the user profile for a given user ID.
     * Emits updates whenever the profile changes in Firestore.
     *
     * @param userId The unique identifier of the user
     * @return Flow emitting Result with UserProfile or null if not found
     */
    fun getUserProfile(userId: String): Flow<Result<UserProfile?>>

    /**
     * Saves a new user profile to Firestore.
     *
     * @param profile The UserProfile to save
     * @return Result indicating success or failure
     */
    suspend fun saveUserProfile(profile: UserProfile): Result<Unit>

    /**
     * Updates an existing user profile in Firestore.
     *
     * @param profile The UserProfile with updated data
     * @return Result indicating success or failure
     */
    suspend fun updateUserProfile(profile: UserProfile): Result<Unit>

    /**
     * Checks if a user has completed their profile.
     *
     * @param userId The unique identifier of the user
     * @return Flow emitting true if profile exists and is complete, false otherwise
     */
    fun hasCompletedProfile(userId: String): Flow<Boolean>
}

