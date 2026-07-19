package com.ssbmax.shared.data.repository

import com.ssbmax.shared.domain.model.EntryType
import com.ssbmax.shared.domain.model.Gender
import com.ssbmax.shared.domain.model.SubscriptionType
import com.ssbmax.shared.domain.model.UserProfile
import kotlinx.serialization.Serializable

/**
 * Wire-format DTO for the GitLive Firestore typed document read/write at
 * users/{userId}/data/profile — mirrors the field set the Android
 * UserProfileRepositoryImpl hand-maps to/from a raw Map<String, Any?>.
 */
@Serializable
data class UserProfileDto(
    val userId: String = "",
    val fullName: String = "",
    val age: Int = 0,
    val gender: String = Gender.MALE.name,
    val entryType: String = EntryType.GRADUATE.name,
    val profilePictureUrl: String? = null,
    val subscriptionType: String = SubscriptionType.FREE.name,
    val currentStreak: Int = 0,
    val lastLoginDate: Long? = null,
    val longestStreak: Int = 0,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

fun UserProfile.toDto(): UserProfileDto = UserProfileDto(
    userId = userId,
    fullName = fullName,
    age = age,
    gender = gender.name,
    entryType = entryType.name,
    profilePictureUrl = profilePictureUrl,
    subscriptionType = subscriptionType.name,
    currentStreak = currentStreak,
    lastLoginDate = lastLoginDate,
    longestStreak = longestStreak,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun UserProfileDto.toDomain(): UserProfile = UserProfile(
    userId = userId,
    fullName = fullName,
    age = age,
    gender = runCatching { Gender.valueOf(gender) }.getOrDefault(Gender.MALE),
    entryType = runCatching { EntryType.valueOf(entryType) }.getOrDefault(EntryType.GRADUATE),
    profilePictureUrl = profilePictureUrl,
    subscriptionType = runCatching { SubscriptionType.valueOf(subscriptionType) }.getOrDefault(SubscriptionType.FREE),
    currentStreak = currentStreak,
    lastLoginDate = lastLoginDate,
    longestStreak = longestStreak,
    createdAt = createdAt,
    updatedAt = updatedAt
)

/** Mirrors UserProfile.isComplete() from the Android UserProfileRepositoryImpl. */
fun UserProfile.isProfileComplete(): Boolean = fullName.isNotBlank() && age > 0
