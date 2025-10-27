package com.ssbmax.core.data.repository

import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.ssbmax.core.data.local.SSBDatabase
import com.ssbmax.core.data.preferences.ThemePreferenceManager
import com.ssbmax.core.data.remote.FirebaseAuthService
import com.ssbmax.core.data.remote.FirebaseInitializer
import com.ssbmax.core.data.remote.FirestoreSubmissionRepository
import com.ssbmax.core.data.remote.FirestoreUserRepository
import com.ssbmax.core.domain.model.*
import com.ssbmax.core.domain.repository.AuthRepository
import com.ssbmax.core.domain.repository.GradingQueueRepository
import com.ssbmax.testing.BaseRepositoryTest
import com.ssbmax.testing.FirebaseTestHelper
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.*
import org.junit.runner.RunWith
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

/**
 * Integration tests for GradingQueueRepositoryImpl
 *
 * Tests the instructor grading workflow where submissions are queued for grading,
 * instructors can claim submissions, and track grading progress.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class GradingQueueRepositoryImplTest : BaseRepositoryTest() {

    @Inject
    lateinit var gradingQueueRepository: GradingQueueRepository

    @Inject
    lateinit var firestore: FirebaseFirestore

    @Inject
    lateinit var authRepository: AuthRepository

    @Inject
    lateinit var firebaseTestHelper: FirebaseTestHelper

    private lateinit var testInstructor: SSBMaxUser
    private lateinit var testStudent: SSBMaxUser
    private lateinit var testTATSubmission: TATSubmission
    private lateinit var testWATSubmission: WATSubmission

    @Before
    override fun setup() {
        super.setup()

        // Initialize Firebase for testing
        FirebaseInitializer.initialize()
        firebaseTestHelper.setupEmulator()

        // Create test users
        testInstructor = SSBMaxUser(
            id = "instructor_001",
            email = "instructor@test.com",
            displayName = "Test Instructor",
            photoUrl = null,
            role = UserRole.INSTRUCTOR,
            createdAt = System.currentTimeMillis(),
            lastLoginAt = System.currentTimeMillis()
        )

        testStudent = SSBMaxUser(
            id = "student_001",
            email = "student@test.com",
            displayName = "Test Student",
            photoUrl = null,
            role = UserRole.STUDENT,
            createdAt = System.currentTimeMillis(),
            lastLoginAt = System.currentTimeMillis()
        )

        // Create test submissions
        testTATSubmission = createTestTATSubmission()
        testWATSubmission = createTestWATSubmission()
    }

    @After
    override fun tearDown() {
        super.tearDown()
        firebaseTestHelper.cleanup()
    }

    @Test
    fun `getPendingSubmissions returns submissions awaiting grading`() = runTest(timeout = 30.seconds) {
        // Given: Create test submissions in Firestore
        firebaseTestHelper.createTestSubmission(testTATSubmission)
        firebaseTestHelper.createTestSubmission(testWATSubmission)

        // When: Get pending submissions
        val pendingSubmissions = gradingQueueRepository.getPendingSubmissions().first()

        // Then: Should return both submissions
        Assert.assertTrue("Should have pending submissions", pendingSubmissions.isSuccess)
        val submissions = pendingSubmissions.getOrNull() ?: emptyList()

        // Verify TAT submission
        val tatSubmission = submissions.find { it.submissionId == testTATSubmission.id }
        Assert.assertNotNull("TAT submission should be in queue", tatSubmission)
        Assert.assertEquals("TAT submission should have correct type", TestType.TAT, tatSubmission?.testType)
        Assert.assertNull("TAT submission should not be claimed", tatSubmission?.claimedByInstructorId)

        // Verify WAT submission
        val watSubmission = submissions.find { it.submissionId == testWATSubmission.id }
        Assert.assertNotNull("WAT submission should be in queue", watSubmission)
        Assert.assertEquals("WAT submission should have correct type", TestType.WAT, watSubmission?.testType)
        Assert.assertNull("WAT submission should not be claimed", watSubmission?.claimedByInstructorId)
    }

    @Test
    fun `claimSubmission successfully claims submission for instructor`() = runTest(timeout = 30.seconds) {
        // Given: Create test submission
        firebaseTestHelper.createTestSubmission(testTATSubmission)

        // When: Instructor claims the submission
        val claimResult = gradingQueueRepository.claimSubmission(
            submissionId = testTATSubmission.id,
            instructorId = testInstructor.id
        )

        // Then: Claim should succeed
        Assert.assertTrue("Claim should succeed", claimResult.isSuccess)

        // Verify submission is now claimed
        val updatedSubmissions = gradingQueueRepository.getPendingSubmissions().first()
        Assert.assertTrue("Should get updated submissions", updatedSubmissions.isSuccess)

        val claimedSubmission = updatedSubmissions.getOrNull()?.find { it.submissionId == testTATSubmission.id }
        Assert.assertNotNull("Submission should still exist", claimedSubmission)
        Assert.assertEquals("Submission should be claimed by instructor", testInstructor.id, claimedSubmission?.claimedByInstructorId)
    }

    @Test
    fun `claimSubmission fails if already claimed by another instructor`() = runTest(timeout = 30.seconds) {
        // Given: Create and claim submission with first instructor
        firebaseTestHelper.createTestSubmission(testTATSubmission)
        val firstClaim = gradingQueueRepository.claimSubmission(testTATSubmission.id, testInstructor.id)
        Assert.assertTrue("First claim should succeed", firstClaim.isSuccess)

        // When: Second instructor tries to claim the same submission
        val secondInstructorId = "instructor_002"
        val secondClaim = gradingQueueRepository.claimSubmission(testTATSubmission.id, secondInstructorId)

        // Then: Second claim should fail
        Assert.assertTrue("Second claim should fail", secondClaim.isFailure)
        Assert.assertTrue("Should indicate already claimed", secondClaim.exceptionOrNull()?.message?.contains("already claimed") == true)
    }

    @Test
    fun `getInstructorQueue returns submissions claimed by specific instructor`() = runTest(timeout = 30.seconds) {
        // Given: Create multiple submissions and have instructor claim some
        val anotherSubmission = testTATSubmission.copy(id = "tat_submission_002", userId = "student_002")

        firebaseTestHelper.createTestSubmission(testTATSubmission)
        firebaseTestHelper.createTestSubmission(anotherSubmission)
        firebaseTestHelper.createTestSubmission(testWATSubmission)

        // Instructor claims TAT submissions
        gradingQueueRepository.claimSubmission(testTATSubmission.id, testInstructor.id)
        gradingQueueRepository.claimSubmission(anotherSubmission.id, testInstructor.id)
        // WAT submission remains unclaimed

        // When: Get instructor's queue
        val instructorQueue = gradingQueueRepository.getInstructorQueue(testInstructor.id).first()

        // Then: Should return only claimed submissions
        Assert.assertTrue("Should get instructor queue", instructorQueue.isSuccess)
        val queueItems = instructorQueue.getOrNull() ?: emptyList()

        Assert.assertEquals("Should have 2 items in instructor queue", 2, queueItems.size)

        // Verify both TAT submissions are in queue
        val tatIds = queueItems.map { it.submissionId }.toSet()
        Assert.assertTrue("Should contain first TAT submission", testTATSubmission.id in tatIds)
        Assert.assertTrue("Should contain second TAT submission", anotherSubmission.id in tatIds)
        Assert.assertFalse("Should not contain WAT submission", testWATSubmission.id in tatIds)

        // Verify all items are claimed by this instructor
        queueItems.forEach { item ->
            Assert.assertEquals("Item should be claimed by instructor", testInstructor.id, item.claimedByInstructorId)
        }
    }

    @Test
    fun `submitGrading completes grading workflow and removes from queue`() = runTest(timeout = 30.seconds) {
        // Given: Create and claim submission
        firebaseTestHelper.createTestSubmission(testTATSubmission)
        gradingQueueRepository.claimSubmission(testTATSubmission.id, testInstructor.id)

        val grading = TATGrading(
            submissionId = testTATSubmission.id,
            instructorId = testInstructor.id,
            overallScore = 85f,
            thematicPerceptionScore = 18f,
            imaginationScore = 17f,
            characterDepictionScore = 17f,
            emotionalToneScore = 17f,
            narrativeStructureScore = 16f,
            feedback = "Excellent storytelling with strong leadership themes",
            storyWiseFeedback = listOf(
                StoryGrading(
                    storyId = testTATSubmission.stories[0].questionId,
                    score = 85f,
                    strengths = listOf("Leadership theme", "Positive resolution"),
                    areasForImprovement = listOf("Add more emotional depth")
                )
            ),
            gradedAt = System.currentTimeMillis()
        )

        // When: Submit grading
        val submitResult = gradingQueueRepository.submitGrading(grading)

        // Then: Grading submission should succeed
        Assert.assertTrue("Grading submission should succeed", submitResult.isSuccess)

        // Submission should be removed from pending queue
        val pendingAfterGrading = gradingQueueRepository.getPendingSubmissions().first()
        Assert.assertTrue("Should get pending submissions", pendingAfterGrading.isSuccess)

        val stillPending = pendingAfterGrading.getOrNull()?.find { it.submissionId == testTATSubmission.id }
        Assert.assertNull("Graded submission should be removed from pending queue", stillPending)
    }

    @Test
    fun `releaseSubmission returns claimed submission back to queue`() = runTest(timeout = 30.seconds) {
        // Given: Create and claim submission
        firebaseTestHelper.createTestSubmission(testTATSubmission)
        gradingQueueRepository.claimSubmission(testTATSubmission.id, testInstructor.id)

        // Verify it's claimed
        val claimedSubmissions = gradingQueueRepository.getInstructorQueue(testInstructor.id).first()
        Assert.assertTrue("Should have claimed submission", claimedSubmissions.getOrNull()?.isNotEmpty() == true)

        // When: Release the submission
        val releaseResult = gradingQueueRepository.releaseSubmission(testTATSubmission.id, testInstructor.id)

        // Then: Release should succeed
        Assert.assertTrue("Release should succeed", releaseResult.isSuccess)

        // Submission should be back in pending queue and unclaimed
        val pendingAfterRelease = gradingQueueRepository.getPendingSubmissions().first()
        Assert.assertTrue("Should get pending submissions", pendingAfterRelease.isSuccess)

        val releasedSubmission = pendingAfterRelease.getOrNull()?.find { it.submissionId == testTATSubmission.id }
        Assert.assertNotNull("Submission should be back in pending queue", releasedSubmission)
        Assert.assertNull("Submission should be unclaimed", releasedSubmission?.claimedByInstructorId)

        // Should not be in instructor's queue anymore
        val instructorQueueAfterRelease = gradingQueueRepository.getInstructorQueue(testInstructor.id).first()
        Assert.assertTrue("Should get instructor queue", instructorQueueAfterRelease.isSuccess)

        val stillInQueue = instructorQueueAfterRelease.getOrNull()?.find { it.submissionId == testTATSubmission.id }
        Assert.assertNull("Submission should not be in instructor queue", stillInQueue)
    }

    @Test
    fun `getSubmissionDetails returns full submission data for grading`() = runTest(timeout = 30.seconds) {
        // Given: Create test submission
        firebaseTestHelper.createTestSubmission(testTATSubmission)

        // When: Get submission details
        val detailsResult = gradingQueueRepository.getSubmissionDetails(testTATSubmission.id)

        // Then: Should return complete submission data
        Assert.assertTrue("Should get submission details", detailsResult.isSuccess)
        val submission = detailsResult.getOrNull()

        Assert.assertNotNull("Submission should exist", submission)
        Assert.assertEquals("Should have correct submission ID", testTATSubmission.id, submission?.id)
        Assert.assertEquals("Should have correct user ID", testTATSubmission.userId, submission?.userId)
        Assert.assertEquals("Should have correct test type", TestType.TAT, submission?.testType)
        Assert.assertEquals("Should have correct number of responses", testTATSubmission.stories.size, submission?.responses?.size)
    }

    @Test
    fun `observeQueueUpdates provides real-time queue changes`() = runTest(timeout = 30.seconds) {
        // Given: Start observing queue
        gradingQueueRepository.observeQueueUpdates().test {
            val initialQueue = awaitItem()
            Assert.assertTrue("Initial queue should be empty or contain existing items", initialQueue.size >= 0)

            // When: Add new submission
            firebaseTestHelper.createTestSubmission(testTATSubmission)

            // Then: Should receive queue update
            val updatedQueue = awaitItem()
            val newSubmission = updatedQueue.find { it.submissionId == testTATSubmission.id }
            Assert.assertNotNull("New submission should appear in queue updates", newSubmission)

            // When: Claim the submission
            gradingQueueRepository.claimSubmission(testTATSubmission.id, testInstructor.id)

            // Then: Should receive another update
            val claimedQueue = awaitItem()
            val claimedSubmission = claimedQueue.find { it.submissionId == testTATSubmission.id }
            Assert.assertNotNull("Claimed submission should still exist", claimedSubmission)
            Assert.assertEquals("Should show as claimed", testInstructor.id, claimedSubmission?.claimedByInstructorId)
        }
    }

    // ==================== HELPER METHODS ====================

    private fun createTestTATSubmission(): TATSubmission {
        val storyResponse = TATStoryResponse(
            questionId = "tat_q_1",
            story = "A young officer leads his team through a difficult situation, showing courage and leadership.",
            charactersCount = 120,
            viewingTimeTakenSeconds = 30,
            writingTimeTakenSeconds = 180,
            submittedAt = System.currentTimeMillis()
        )

        return TATSubmission(
            id = "tat_submission_001",
            userId = testStudent.id,
            testId = "tat_test_001",
            stories = listOf(storyResponse),
            totalTimeTakenMinutes = 4,
            submittedAt = System.currentTimeMillis(),
            aiPreliminaryScore = TATAIScore(
                overallScore = 78f,
                thematicPerceptionScore = 16f,
                imaginationScore = 15f,
                characterDepictionScore = 16f,
                emotionalToneScore = 16f,
                narrativeStructureScore = 15f,
                feedback = "Good leadership themes",
                storyWiseAnalysis = listOf(
                    StoryAnalysis(
                        questionId = "tat_q_1",
                        sequenceNumber = 1,
                        score = 78f,
                        themes = listOf("Leadership", "Courage"),
                        sentimentScore = 0.7f,
                        keyInsights = listOf("Shows initiative")
                    )
                ),
                strengths = listOf("Leadership themes"),
                areasForImprovement = listOf("Add more detail")
            )
        )
    }

    private fun createTestWATSubmission(): WATSubmission {
        val wordResponses = listOf(
            WATWordResponse(
                wordId = "wat_w_1",
                word = "Leadership",
                response = "Leading a team to success",
                timeTakenSeconds = 8,
                submittedAt = System.currentTimeMillis(),
                isSkipped = false
            ),
            WATWordResponse(
                wordId = "wat_w_2",
                word = "Courage",
                response = "Facing fears bravely",
                timeTakenSeconds = 6,
                submittedAt = System.currentTimeMillis(),
                isSkipped = false
            )
        )

        return WATSubmission(
            id = "wat_submission_001",
            userId = testStudent.id,
            testId = "wat_test_001",
            responses = wordResponses,
            totalTimeTakenMinutes = 15,
            submittedAt = System.currentTimeMillis(),
            aiPreliminaryScore = WATAIScore(
                overallScore = 82f,
                positiveResponses = 1,
                negativeResponses = 0,
                neutralResponses = 1,
                uniqueResponses = 2,
                averageResponseLength = 12.5f,
                creativityScore = 16f,
                relevanceScore = 17f,
                sentimentScore = 0.8f,
                feedback = "Good positive associations",
                wordWiseAnalysis = wordResponses.map { response ->
                    WordAnalysis(
                        wordId = response.wordId,
                        word = response.word,
                        response = response.response,
                        score = 82f,
                        sentiment = if (response.response.contains("success") || response.response.contains("bravely")) "positive" else "neutral",
                        themes = listOf("Leadership", "Success"),
                        isOriginal = true
                    )
                },
                strengths = listOf("Positive outlook", "Good associations"),
                areasForImprovement = listOf("More detailed responses")
            )
        )
    }
}
