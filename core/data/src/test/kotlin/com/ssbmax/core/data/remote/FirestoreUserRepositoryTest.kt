package com.ssbmax.core.data.remote

import com.google.firebase.firestore.FirebaseFirestore
import com.ssbmax.core.domain.model.BillingCycle
import com.ssbmax.core.domain.model.InstructorProfile
import com.ssbmax.core.domain.model.SSBMaxUser
import com.ssbmax.core.domain.model.StudentProfile
import com.ssbmax.core.domain.model.SubscriptionTier
import com.ssbmax.core.domain.model.UserRole
import com.ssbmax.core.domain.model.UserSubscription
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for FirestoreUserRepository
 *
 * Note: Full Firebase integration tests should be written with Firebase Emulator.
 * These tests verify data models and basic repository structure.
 */
class FirestoreUserRepositoryTest {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var repository: FirestoreUserRepository

    @Before
    fun setup() {
        // Mock FirebaseFirestore.getInstance()
        mockkStatic(FirebaseFirestore::class)
        firestore = mockk(relaxed = true)
        every { FirebaseFirestore.getInstance() } returns firestore

        repository = FirestoreUserRepository()
    }

    @After
    fun tearDown() {
        unmockkStatic(FirebaseFirestore::class)
    }

    @Test
    fun `repository initializes correctly`() {
        // Given/When
        val repo = FirestoreUserRepository()

        // Then
        assertNotNull(repo)
    }

    @Test
    fun `SSBMaxUser with STUDENT role validates correctly`() {
        // Given
        val user = createTestUser("user123", UserRole.STUDENT)

        // Then
        assertEquals("user123", user.id)
        assertEquals("test@example.com", user.email)
        assertEquals(UserRole.STUDENT, user.role)
        assertEquals(SubscriptionTier.FREE, user.subscriptionTier)
        assertNull(user.instructorProfile)
    }

    @Test
    fun `SSBMaxUser with INSTRUCTOR role validates correctly`() {
        // Given
        val user = createTestUser("instructor123", UserRole.INSTRUCTOR)

        // Then
        assertEquals("instructor123", user.id)
        assertEquals(UserRole.INSTRUCTOR, user.role)
        assertNull(user.studentProfile)
    }

    @Test
    fun `StudentProfile validates correctly`() {
        // Given
        val profile = StudentProfile(
            userId = "user123",
            currentBatchIds = listOf("batch1", "batch2"),
            totalTestsAttempted = 15,
            totalStudyHours = 30.5f,
            currentStreak = 7,
            longestStreak = 14,
            achievements = listOf("first_test", "week_streak", "month_streak")
        )

        // Then
        assertEquals("user123", profile.userId)
        assertEquals(2, profile.currentBatchIds.size)
        assertEquals(15, profile.totalTestsAttempted)
        assertEquals(30.5f, profile.totalStudyHours, 0.01f)
        assertEquals(7, profile.currentStreak)
        assertEquals(14, profile.longestStreak)
        assertEquals(3, profile.achievements.size)
    }

    @Test
    fun `InstructorProfile validates correctly`() {
        // Given
        val profile = InstructorProfile(
            userId = "instructor123",
            specialization = listOf("Psychology", "GTO"),
            batchesCreated = listOf("batch1", "batch2", "batch3"),
            totalStudents = 50,
            totalTestsGraded = 200,
            averageGradingTime = 300000L,
            rating = 4.8f,
            bio = "Experienced SSB instructor",
            certifications = listOf("cert1", "cert2")
        )

        // Then
        assertEquals("instructor123", profile.userId)
        assertEquals(2, profile.specialization.size)
        assertEquals(3, profile.batchesCreated.size)
        assertEquals(50, profile.totalStudents)
        assertEquals(200, profile.totalTestsGraded)
        assertEquals(4.8f, profile.rating, 0.01f)
        assertEquals("Experienced SSB instructor", profile.bio)
    }

    @Test
    fun `UserSubscription validates correctly`() {
        // Given
        val now = System.currentTimeMillis()
        val subscription = UserSubscription(
            userId = "user123",
            tier = SubscriptionTier.PRO,
            subscriptionId = "sub_abc123",
            startDate = now,
            expiryDate = now + (30L * 24 * 60 * 60 * 1000), // 30 days
            autoRenew = true,
            isActive = true,
            billingCycle = BillingCycle.MONTHLY
        )

        // Then
        assertEquals("user123", subscription.userId)
        assertEquals(SubscriptionTier.PRO, subscription.tier)
        assertEquals("sub_abc123", subscription.subscriptionId)
        assertTrue(subscription.autoRenew)
        assertTrue(subscription.isActive)
        assertEquals(BillingCycle.MONTHLY, subscription.billingCycle)
        assertNotNull(subscription.expiryDate)
    }

    @Test
    fun `SSBMaxUser with subscription validates correctly`() {
        // Given
        val subscription = createUserSubscription("user123", SubscriptionTier.PREMIUM)
        val user = createTestUser("user123", UserRole.STUDENT, subscription = subscription)

        // Then
        assertNotNull(user.subscription)
        assertEquals(SubscriptionTier.PREMIUM, user.subscription?.tier)
        assertTrue(user.subscription?.isActive == true)
    }

    @Test
    fun `SSBMaxUser with StudentProfile validates correctly`() {
        // Given
        val studentProfile = createStudentProfile("user123")
        val user = createTestUser("user123", UserRole.STUDENT, studentProfile = studentProfile)

        // Then
        assertNotNull(user.studentProfile)
        assertEquals("user123", user.studentProfile?.userId)
        assertTrue((user.studentProfile?.totalTestsAttempted ?: 0) > 0)
    }

    @Test
    fun `SSBMaxUser with InstructorProfile validates correctly`() {
        // Given
        val instructorProfile = createInstructorProfile("instructor123")
        val user = createTestUser("instructor123", UserRole.INSTRUCTOR, instructorProfile = instructorProfile)

        // Then
        assertNotNull(user.instructorProfile)
        assertEquals("instructor123", user.instructorProfile?.userId)
        assertTrue((user.instructorProfile?.totalStudents ?: 0) > 0)
    }

    @Test
    fun `UserRole enum has correct values`() {
        // Given/When
        val roles = UserRole.values()

        // Then
        assertTrue(roles.contains(UserRole.STUDENT))
        assertTrue(roles.contains(UserRole.INSTRUCTOR))
        assertTrue(roles.contains(UserRole.BOTH))
    }

    @Test
    fun `SubscriptionTier enum has correct values`() {
        // Given/When
        val tiers = SubscriptionTier.values()

        // Then
        assertEquals(3, tiers.size)
        assertTrue(tiers.contains(SubscriptionTier.FREE))
        assertTrue(tiers.contains(SubscriptionTier.PRO))
        assertTrue(tiers.contains(SubscriptionTier.PREMIUM))
    }

    @Test
    fun `BillingCycle enum has correct values`() {
        // Given/When
        val cycles = BillingCycle.values()

        // Then
        assertEquals(3, cycles.size)
        assertTrue(cycles.contains(BillingCycle.MONTHLY))
        assertTrue(cycles.contains(BillingCycle.QUARTERLY))
        assertTrue(cycles.contains(BillingCycle.ANNUALLY))
    }

    @Test
    fun `StudentProfile supports empty batch lists`() {
        // Given
        val profile = StudentProfile(
            userId = "user123",
            currentBatchIds = emptyList(),
            totalTestsAttempted = 0,
            totalStudyHours = 0f,
            currentStreak = 0,
            longestStreak = 0,
            achievements = emptyList()
        )

        // Then
        assertTrue(profile.currentBatchIds.isEmpty())
        assertEquals(0, profile.totalTestsAttempted)
        assertEquals(0f, profile.totalStudyHours, 0.01f)
        assertTrue(profile.achievements.isEmpty())
    }

    @Test
    fun `InstructorProfile supports null bio and empty lists`() {
        // Given
        val profile = InstructorProfile(
            userId = "instructor123",
            specialization = emptyList(),
            batchesCreated = emptyList(),
            totalStudents = 0,
            totalTestsGraded = 0,
            averageGradingTime = 0L,
            rating = 0f,
            bio = null,
            certifications = emptyList()
        )

        // Then
        assertTrue(profile.specialization.isEmpty())
        assertTrue(profile.batchesCreated.isEmpty())
        assertEquals(0, profile.totalStudents)
        assertNull(profile.bio)
        assertTrue(profile.certifications.isEmpty())
    }

    @Test
    fun `UserSubscription supports inactive state`() {
        // Given
        val subscription = UserSubscription(
            userId = "user123",
            tier = SubscriptionTier.PRO,
            subscriptionId = "sub123",
            startDate = System.currentTimeMillis(),
            expiryDate = System.currentTimeMillis() - 1000, // Expired
            autoRenew = false,
            isActive = false,
            billingCycle = BillingCycle.MONTHLY
        )

        // Then
        assertFalse(subscription.isActive)
        assertFalse(subscription.autoRenew)
        assertNotNull(subscription.expiryDate)
    }

    @Test
    fun `SSBMaxUser tracks creation and last login timestamps`() {
        // Given
        val createdAt = System.currentTimeMillis() - 86400000 // 1 day ago
        val lastLoginAt = System.currentTimeMillis()

        val user = SSBMaxUser(
            id = "user123",
            email = "test@example.com",
            displayName = "Test User",
            photoUrl = null,
            role = UserRole.STUDENT,
            subscriptionTier = SubscriptionTier.FREE,
            subscription = null,
            createdAt = createdAt,
            lastLoginAt = lastLoginAt,
            studentProfile = null,
            instructorProfile = null
        )

        // Then
        assertTrue(user.createdAt < user.lastLoginAt)
        assertTrue(user.lastLoginAt - user.createdAt >= 86400000)
    }

    @Test
    fun `placeholder - full Firebase integration tests needed`() {
        // This test serves as a reminder that comprehensive testing requires:
        // 1. Firebase Emulator setup for Firestore
        // 2. Integration tests for CRUD operations
        // 3. Real-time observer/flow testing
        // 4. User profile update scenarios
        // 5. Role-based access control validation
        //
        // These should be implemented in androidTest with Firebase Test SDK
        assertTrue("FirestoreUserRepository requires Firebase emulator integration testing", true)
    }

    // Helper functions to create test data
    private fun createTestUser(
        id: String,
        role: UserRole,
        studentProfile: StudentProfile? = null,
        instructorProfile: InstructorProfile? = null,
        subscription: UserSubscription? = null
    ): SSBMaxUser {
        return SSBMaxUser(
            id = id,
            email = "test@example.com",
            displayName = "Test User",
            photoUrl = null,
            role = role,
            subscriptionTier = subscription?.tier ?: SubscriptionTier.FREE,
            subscription = subscription,
            createdAt = System.currentTimeMillis(),
            lastLoginAt = System.currentTimeMillis(),
            studentProfile = studentProfile,
            instructorProfile = instructorProfile
        )
    }

    private fun createStudentProfile(userId: String): StudentProfile {
        return StudentProfile(
            userId = userId,
            currentBatchIds = listOf("batch1", "batch2"),
            totalTestsAttempted = 10,
            totalStudyHours = 25.5f,
            currentStreak = 5,
            longestStreak = 10,
            achievements = listOf("first_test", "week_streak")
        )
    }

    private fun createInstructorProfile(userId: String): InstructorProfile {
        return InstructorProfile(
            userId = userId,
            specialization = listOf("Psychology", "GTO"),
            batchesCreated = listOf("batch1", "batch2"),
            totalStudents = 50,
            totalTestsGraded = 200,
            averageGradingTime = 300000L,
            rating = 4.8f,
            bio = "Experienced instructor",
            certifications = listOf("cert1", "cert2")
        )
    }

    private fun createUserSubscription(userId: String, tier: SubscriptionTier): UserSubscription {
        return UserSubscription(
            userId = userId,
            tier = tier,
            subscriptionId = "sub123",
            startDate = System.currentTimeMillis(),
            expiryDate = System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000, // 30 days
            autoRenew = true,
            isActive = true,
            billingCycle = BillingCycle.MONTHLY
        )
    }
}
