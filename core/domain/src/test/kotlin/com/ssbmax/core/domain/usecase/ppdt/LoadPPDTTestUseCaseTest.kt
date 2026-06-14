package com.ssbmax.core.domain.usecase.ppdt

import com.ssbmax.core.domain.model.EntryType
import com.ssbmax.core.domain.model.Gender
import com.ssbmax.core.domain.model.GenderTag
import com.ssbmax.core.domain.model.PPDTPhase
import com.ssbmax.core.domain.model.PPDTQuestion
import com.ssbmax.core.domain.model.SubscriptionType
import com.ssbmax.core.domain.model.TestType
import com.ssbmax.core.domain.model.UserProfile
import com.ssbmax.core.domain.model.UserRole
import com.ssbmax.core.domain.repository.TestContentRepository
import com.ssbmax.core.domain.repository.TestSessionRepository
import com.ssbmax.core.domain.repository.UserProfileRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LoadPPDTTestUseCaseTest {

    private val mockContentRepo = mockk<TestContentRepository>(relaxed = true)
    private val mockSessionRepo = mockk<TestSessionRepository>(relaxed = true)
    private val mockProfileRepo = mockk<UserProfileRepository>(relaxed = true)

    private val useCase = LoadPPDTTestUseCase(mockContentRepo, mockSessionRepo, mockProfileRepo)

    private val mockQuestion = PPDTQuestion(
        id = "ppdt_q1",
        imageUrl = "https://example.com/img.jpg",
        imageDescription = "A hazy image",
        viewingTimeSeconds = 30,
        writingTimeMinutes = 4,
        minCharacters = 200,
        maxCharacters = 1000
    )

    private fun profileOf(gender: Gender) = UserProfile(
        userId = "u1", fullName = "Test", age = 22, gender = gender,
        entryType = EntryType.GRADUATE, subscriptionType = SubscriptionType.FREE,
        createdAt = 0L
    )

    @Before
    fun setup() {
        coEvery { mockSessionRepo.createTestSession(any(), any(), TestType.PPDT) } returns Result.success("session-1")
        coEvery { mockContentRepo.getPPDTQuestion(genderTag = any()) } returns Result.success(mockQuestion)
        coEvery { mockProfileRepo.getUserProfile(any()) } returns flowOf(Result.success(profileOf(Gender.MALE)))
    }

    // ==================== Gender Routing ====================

    @Test
    fun `male user gets MALE gender tag`() = runTest {
        // WHY: Male candidates should see male-lead or mixed images for appropriate SSB scoring.
        coEvery { mockProfileRepo.getUserProfile(any()) } returns flowOf(Result.success(profileOf(Gender.MALE)))
        useCase("u1", "ppdt_standard")
        coVerify { mockContentRepo.getPPDTQuestion(genderTag = GenderTag.MALE) }
    }

    @Test
    fun `female user gets FEMALE gender tag`() = runTest {
        // WHY: Female candidates must only see female-lead or mixed images for accurate PPDT scoring.
        coEvery { mockProfileRepo.getUserProfile(any()) } returns flowOf(Result.success(profileOf(Gender.FEMALE)))
        useCase("u1", "ppdt_standard")
        coVerify { mockContentRepo.getPPDTQuestion(genderTag = GenderTag.FEMALE) }
    }

    @Test
    fun `OTHER gender uses null tag (full image pool)`() = runTest {
        // WHY: OTHER gender → full image pool (no filter). Same fallback used for network errors.
        coEvery { mockProfileRepo.getUserProfile(any()) } returns flowOf(Result.success(profileOf(Gender.OTHER)))
        useCase("u1", "ppdt_standard")
        coVerify { mockContentRepo.getPPDTQuestion(genderTag = null) }
    }

    @Test
    fun `profile network error falls back to null tag`() = runTest {
        // WHY: Network error ≠ confirmed missing profile. Proceed with full pool to avoid blocking session.
        coEvery { mockProfileRepo.getUserProfile(any()) } returns flowOf(Result.failure(Exception("timeout")))
        useCase("u1", "ppdt_standard")
        coVerify { mockContentRepo.getPPDTQuestion(genderTag = null) }
    }

    // ==================== Profile Gate ====================

    @Test
    fun `null profile returns ProfileIncompleteException`() = runTest {
        // WHY: Server confirming no profile (success + null) means candidate hasn't onboarded.
        coEvery { mockProfileRepo.getUserProfile(any()) } returns flowOf(Result.success(null))
        val result = useCase("u1", "ppdt_standard")
        assertTrue("Should fail", result.isFailure)
        assertTrue("Should be ProfileIncompleteException",
            result.exceptionOrNull() is LoadPPDTTestUseCase.ProfileIncompleteException)
    }

    // ==================== Success Path ====================

    @Test
    fun `success returns session with INSTRUCTIONS phase`() = runTest {
        val result = useCase("u1", "ppdt_standard")
        assertTrue("Should succeed", result.isSuccess)
        val session = result.getOrThrow()
        assertEquals("Should start in INSTRUCTIONS", PPDTPhase.INSTRUCTIONS, session.currentPhase)
        assertEquals("Question should be set", mockQuestion, session.question)
        assertEquals("UserId should match", "u1", session.userId)
    }

    @Test
    fun `session fetch failure propagates as failure result`() = runTest {
        coEvery { mockSessionRepo.createTestSession(any(), any(), any()) } returns
            Result.failure(Exception("Firestore error"))
        val result = useCase("u1", "ppdt_standard")
        assertTrue("Should fail when session creation fails", result.isFailure)
    }

    @Test
    fun `question fetch failure propagates as failure result`() = runTest {
        coEvery { mockContentRepo.getPPDTQuestion(genderTag = any()) } returns
            Result.failure(Exception("No question available"))
        val result = useCase("u1", "ppdt_standard")
        assertTrue("Should fail when question fetch fails", result.isFailure)
    }
}
