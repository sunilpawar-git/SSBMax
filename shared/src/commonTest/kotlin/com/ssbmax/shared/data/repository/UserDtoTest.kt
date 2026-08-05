package com.ssbmax.shared.data.repository

import com.ssbmax.shared.domain.model.BillingCycle
import com.ssbmax.shared.domain.model.InstructorProfile
import com.ssbmax.shared.domain.model.SSBMaxUser
import com.ssbmax.shared.domain.model.StudentProfile
import com.ssbmax.shared.domain.model.SubscriptionTier
import com.ssbmax.shared.domain.model.UserRole
import com.ssbmax.shared.domain.model.UserSubscription
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Round-trips SSBMaxUser through the Firestore wire DTO. Why it matters: this
 * is the seam GitLiveUserRepository depends on instead of the Android SDK's
 * hand-rolled Map<String, Any?> mapping in FirestoreUserRepository — a silent
 * enum-name or nested-object mismatch here would corrupt user/subscription
 * data without the build ever failing.
 */
class UserDtoTest {

    private val user = SSBMaxUser(
        id = "user-1",
        email = "jordan@example.com",
        displayName = "Jordan Rivera",
        photoUrl = "https://example.com/pic.jpg",
        role = UserRole.BOTH,
        subscription = UserSubscription(
            userId = "user-1",
            tier = SubscriptionTier.PRO,
            subscriptionId = "sub-123",
            startDate = 1_600_000_000_000L,
            expiryDate = 1_700_000_000_000L,
            autoRenew = true,
            isActive = true,
            billingCycle = BillingCycle.ANNUALLY
        ),
        studentProfile = StudentProfile(
            userId = "user-1",
            currentBatchIds = listOf("batch-1", "batch-2"),
            totalTestsAttempted = 10,
            totalStudyHours = 5.5f,
            currentStreak = 3,
            longestStreak = 7,
            achievements = listOf("first-test")
        ),
        instructorProfile = InstructorProfile(
            userId = "user-1",
            specialization = listOf("Psychology"),
            batchesCreated = listOf("batch-1"),
            totalStudents = 20,
            totalTestsGraded = 50,
            averageGradingTime = 120_000L,
            rating = 4.5f,
            bio = "Experienced instructor",
            certifications = listOf("cert-1")
        ),
        createdAt = 1_600_000_000_000L,
        lastLoginAt = 1_700_000_000_000L
    )

    @Test
    fun `domain to dto to domain round-trips every field including nested profiles`() {
        val roundTripped = user.toDto().toDomain()
        assertEquals(user, roundTripped)
    }

    @Test
    fun `null subscription and instructor profile round-trip as null`() {
        val minimal = SSBMaxUser(
            id = "user-2",
            email = "min@example.com",
            displayName = "Min User",
            role = UserRole.STUDENT,
            studentProfile = StudentProfile(userId = "user-2")
        )
        val roundTripped = minimal.toDto().toDomain()
        assertEquals(minimal, roundTripped)
    }

    @Test
    fun `unrecognized enum strings fall back to safe defaults instead of throwing`() {
        val dto = UserDto(id = "u", email = "e", displayName = "d", role = "NOT_A_ROLE")
        val domain = dto.toDomain()
        assertEquals(UserRole.STUDENT, domain.role)
    }
}
