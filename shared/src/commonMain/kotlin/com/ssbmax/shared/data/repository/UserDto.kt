package com.ssbmax.shared.data.repository

import com.ssbmax.shared.domain.model.InstructorProfile
import com.ssbmax.shared.domain.model.SSBMaxUser
import com.ssbmax.shared.domain.model.StudentProfile
import com.ssbmax.shared.domain.model.UserRole
import kotlinx.serialization.Serializable

/**
 * Wire-format DTO for users/{userId} — mirrors the field set the Android
 * FirestoreUserRepository hand-maps to/from a raw Map<String, Any?>.
 */
@Serializable
data class UserDto(
    val id: String = "",
    val email: String = "",
    val displayName: String = "",
    val photoUrl: String? = null,
    val role: String = UserRole.STUDENT.name,
    val createdAt: Long = 0L,
    val lastLoginAt: Long = 0L,
    val studentProfile: StudentProfileDto? = null,
    val instructorProfile: InstructorProfileDto? = null
)

@Serializable
data class StudentProfileDto(
    val userId: String = "",
    val currentBatchIds: List<String> = emptyList(),
    val totalTestsAttempted: Int = 0,
    val totalStudyHours: Float = 0f,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val achievements: List<String> = emptyList()
)

@Serializable
data class InstructorProfileDto(
    val userId: String = "",
    val specialization: List<String> = emptyList(),
    val batchesCreated: List<String> = emptyList(),
    val totalStudents: Int = 0,
    val totalTestsGraded: Int = 0,
    val averageGradingTime: Long = 0L,
    val rating: Float = 0f,
    val bio: String? = null,
    val certifications: List<String> = emptyList()
)

fun SSBMaxUser.toDto(): UserDto = UserDto(
    id = id,
    email = email,
    displayName = displayName,
    photoUrl = photoUrl,
    role = role.name,
    createdAt = createdAt,
    lastLoginAt = lastLoginAt,
    studentProfile = studentProfile?.toDto(),
    instructorProfile = instructorProfile?.toDto()
)

fun UserDto.toDomain(): SSBMaxUser = SSBMaxUser(
    id = id,
    email = email,
    displayName = displayName,
    photoUrl = photoUrl,
    role = runCatching { UserRole.valueOf(role) }.getOrDefault(UserRole.STUDENT),
    createdAt = createdAt,
    lastLoginAt = lastLoginAt,
    studentProfile = studentProfile?.toDomain(),
    instructorProfile = instructorProfile?.toDomain()
)

private fun StudentProfile.toDto(): StudentProfileDto = StudentProfileDto(
    userId = userId,
    currentBatchIds = currentBatchIds,
    totalTestsAttempted = totalTestsAttempted,
    totalStudyHours = totalStudyHours,
    currentStreak = currentStreak,
    longestStreak = longestStreak,
    achievements = achievements
)

private fun StudentProfileDto.toDomain(): StudentProfile = StudentProfile(
    userId = userId,
    currentBatchIds = currentBatchIds,
    totalTestsAttempted = totalTestsAttempted,
    totalStudyHours = totalStudyHours,
    currentStreak = currentStreak,
    longestStreak = longestStreak,
    achievements = achievements
)

private fun InstructorProfile.toDto(): InstructorProfileDto = InstructorProfileDto(
    userId = userId,
    specialization = specialization,
    batchesCreated = batchesCreated,
    totalStudents = totalStudents,
    totalTestsGraded = totalTestsGraded,
    averageGradingTime = averageGradingTime,
    rating = rating,
    bio = bio,
    certifications = certifications
)

private fun InstructorProfileDto.toDomain(): InstructorProfile = InstructorProfile(
    userId = userId,
    specialization = specialization,
    batchesCreated = batchesCreated,
    totalStudents = totalStudents,
    totalTestsGraded = totalTestsGraded,
    averageGradingTime = averageGradingTime,
    rating = rating,
    bio = bio,
    certifications = certifications
)
