package com.ssbmax.ui.interview.session

import android.content.Context
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.work.WorkManager
import com.ssbmax.core.domain.model.EntryType
import com.ssbmax.core.domain.model.Gender
import com.ssbmax.core.domain.model.SSBMaxUser
import com.ssbmax.core.domain.model.SubscriptionType
import com.ssbmax.core.domain.model.UserProfile
import com.ssbmax.core.domain.model.UserRole
import com.ssbmax.core.domain.model.interview.InterviewMode
import com.ssbmax.core.domain.model.interview.InterviewQuestion
import com.ssbmax.core.domain.model.interview.InterviewSession
import com.ssbmax.core.domain.model.interview.InterviewStatus
import com.ssbmax.core.domain.model.interview.OLQ
import com.ssbmax.core.domain.model.interview.QuestionSource
import com.ssbmax.core.data.analytics.AnalyticsManager
import com.ssbmax.core.domain.repository.AuthRepository
import com.ssbmax.core.domain.repository.InterviewRepository
import com.ssbmax.core.domain.repository.UserProfileRepository
import com.ssbmax.testing.BaseViewModelTest
import com.ssbmax.utils.tts.TTSService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

/**
 * Unit tests for InterviewSessionViewModel
 *
 * Tests cover:
 * - Session loading
 * - TTS service selection based on subscription
 * - Response submission flow
 * - Error handling
 */
@OptIn(ExperimentalCoroutinesApi::class)
class InterviewSessionViewModelTest : BaseViewModelTest() {

    private lateinit var interviewRepository: InterviewRepository
    private lateinit var authRepository: AuthRepository
    private lateinit var userProfileRepository: UserProfileRepository
    private lateinit var workManager: WorkManager
    private lateinit var analyticsManager: AnalyticsManager
    private lateinit var androidTTSService: TTSService
    private lateinit var sarvamTTSService: TTSService
    private lateinit var elevenLabsTTSService: TTSService
    private lateinit var qwenTTSService: TTSService
    private lateinit var context: Context
    private lateinit var savedStateHandle: SavedStateHandle

    private val testSessionId = "test-session-123"
    private val testUserId = "user-123"

    private val testUser = SSBMaxUser(
        id = testUserId,
        email = "test@example.com",
        displayName = "Test User",
        role = UserRole.STUDENT
    )

    private val testProfile = UserProfile(
        userId = testUserId,
        fullName = "Test User",
        age = 22,
        gender = Gender.MALE,
        entryType = EntryType.GRADUATE,
        subscriptionType = SubscriptionType.FREE
    )

    private val testSession = InterviewSession(
        id = testSessionId,
        userId = testUserId,
        mode = InterviewMode.VOICE_BASED,
        status = InterviewStatus.IN_PROGRESS,
        startedAt = Instant.now(),
        completedAt = null,
        piqSnapshotId = "piq-123",
        consentGiven = true,
        questionIds = listOf("q1", "q2"),
        currentQuestionIndex = 0,
        estimatedDuration = 30
    )

    private val testQuestion = InterviewQuestion(
        id = "q1",
        questionText = "Tell me about yourself",
        expectedOLQs = listOf(OLQ.SELF_CONFIDENCE, OLQ.POWER_OF_EXPRESSION),
        context = null,
        source = QuestionSource.PIQ_BASED
    )

    // TTS event flows for mocking
    private lateinit var androidTTSEvents: MutableSharedFlow<TTSService.TTSEvent>
    private lateinit var sarvamTTSEvents: MutableSharedFlow<TTSService.TTSEvent>
    private lateinit var elevenLabsTTSEvents: MutableSharedFlow<TTSService.TTSEvent>
    private lateinit var qwenTTSEvents: MutableSharedFlow<TTSService.TTSEvent>

    @Before
    fun setUp() {
        // Initialize TTS event flows
        androidTTSEvents = MutableSharedFlow(extraBufferCapacity = 1)
        sarvamTTSEvents = MutableSharedFlow(extraBufferCapacity = 1)
        elevenLabsTTSEvents = MutableSharedFlow(extraBufferCapacity = 1)
        qwenTTSEvents = MutableSharedFlow(extraBufferCapacity = 1)

        // Mock repositories
        interviewRepository = mockk(relaxed = true)
        authRepository = mockk(relaxed = true)
        userProfileRepository = mockk(relaxed = true)
        workManager = mockk(relaxed = true)
        analyticsManager = mockk(relaxed = true)
        context = mockk(relaxed = true)
        savedStateHandle = SavedStateHandle(mapOf("sessionId" to testSessionId))

        // Mock TTS services
        androidTTSService = mockk(relaxed = true)
        sarvamTTSService = mockk(relaxed = true)
        elevenLabsTTSService = mockk(relaxed = true)
        qwenTTSService = mockk(relaxed = true)

        // Setup TTS service mocks
        every { androidTTSService.events } returns androidTTSEvents
        every { sarvamTTSService.events } returns sarvamTTSEvents
        every { elevenLabsTTSService.events } returns elevenLabsTTSEvents
        every { qwenTTSService.events } returns qwenTTSEvents
        every { androidTTSService.isReady() } returns true
        every { sarvamTTSService.isReady() } returns false // Default: Sarvam not ready
        every { elevenLabsTTSService.isReady() } returns false // Default: ElevenLabs not ready
        every { qwenTTSService.isReady() } returns false // Default: Qwen not ready (Phase 2 - no logic change)

        // Setup auth repository mock
        every { authRepository.currentUser } returns MutableStateFlow(testUser)

        // Setup user profile repository mock
        coEvery { userProfileRepository.getUserProfile(testUserId) } returns flowOf(Result.success(testProfile))

        // Mock context string resources
        every { context.getString(any()) } returns "Mocked String"
        every { context.applicationContext } returns context
    }

    private fun createViewModel(): InterviewSessionViewModel {
        return InterviewSessionViewModel(
            interviewRepository = interviewRepository,
            authRepository = authRepository,
            userProfileRepository = userProfileRepository,
            workManager = workManager,
            analyticsManager = analyticsManager,
            androidTTSService = androidTTSService,
            sarvamTTSService = sarvamTTSService,
            elevenLabsTTSService = elevenLabsTTSService,
            qwenTTSService = qwenTTSService,
            context = context,
            savedStateHandle = savedStateHandle
        )
    }

    // ============================================
    // SESSION LOADING TESTS
    // ============================================

    @Test
    fun `loadSession loads session and first question`() = runTest {
        // Given
        coEvery { interviewRepository.getSession(testSessionId) } returns Result.success(testSession)
        coEvery { interviewRepository.getQuestion("q1") } returns Result.success(testQuestion)

        // When
        val viewModel = createViewModel()
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(testSession, state.session)
        assertEquals(testQuestion, state.currentQuestion)
        assertNull(state.error)
    }

    @Test
    fun `loadSession handles session not found error`() = runTest {
        // Given
        coEvery { interviewRepository.getSession(testSessionId) } returns
            Result.failure(Exception("Session not found"))

        // When
        val viewModel = createViewModel()
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Mocked String", state.error) // Uses mocked getString
    }

    @Test
    fun `initial state has correct defaults`() = runTest {
        // Given
        coEvery { interviewRepository.getSession(testSessionId) } returns Result.success(testSession)
        coEvery { interviewRepository.getQuestion("q1") } returns Result.success(testQuestion)

        // When
        val viewModel = createViewModel()
        advanceUntilIdle()

        // Then - After loading, TTS starts speaking the question automatically
        val state = viewModel.uiState.value
        assertFalse("Should not be completed initially", state.isCompleted)
        assertEquals("Response text should be empty", "", state.responseText)
        assertEquals("Pending responses should be empty", emptyList<PendingResponse>(), state.pendingResponses)
        // Note: isTTSSpeaking is true after session loads because speakQuestion() is called
    }

    @Test
    fun `clearError updates state correctly`() = runTest {
        // Given
        coEvery { interviewRepository.getSession(testSessionId) } returns Result.success(testSession)
        coEvery { interviewRepository.getQuestion("q1") } returns Result.success(testQuestion)

        val viewModel = createViewModel()
        advanceUntilIdle()

        // When - clearError is called
        viewModel.clearError()
        advanceUntilIdle()

        // Then - Error should be cleared
        val state = viewModel.uiState.value
        assertNull("Error should be null after clearError", state.error)
    }

    @Test
    fun `ViewModel loads session on creation`() = runTest {
        // Given
        coEvery { interviewRepository.getSession(testSessionId) } returns Result.success(testSession)
        coEvery { interviewRepository.getQuestion("q1") } returns Result.success(testQuestion)

        // When - Create ViewModel
        val viewModel = createViewModel()
        advanceUntilIdle()

        // Then - Session should be loaded
        val state = viewModel.uiState.value
        assertEquals("Session should be loaded", testSession, state.session)
        assertEquals("Question should be loaded", testQuestion, state.currentQuestion)
    }

    // ============================================
    // UI STATE HELPER TESTS
    // ============================================

    @Test
    fun `canSubmitResponse returns false when response is blank`() {
        // Given
        val state = InterviewSessionUiState(
            isLoading = false,
            responseText = "",
            currentQuestion = testQuestion,
            isSubmittingResponse = false
        )

        // Then
        assertFalse("Cannot submit blank response", state.canSubmitResponse())
    }

    @Test
    fun `canSubmitResponse returns true when response is valid`() {
        // Given
        val state = InterviewSessionUiState(
            isLoading = false,
            responseText = "This is my response",
            currentQuestion = testQuestion,
            isSubmittingResponse = false
        )

        // Then
        assertTrue("Should be able to submit valid response", state.canSubmitResponse())
    }

    @Test
    fun `hasMoreQuestions returns correct value`() {
        // Given - At question 0 of 3
        val state = InterviewSessionUiState(
            currentQuestionIndex = 0,
            totalQuestions = 3
        )

        // Then
        assertTrue("Should have more questions", state.hasMoreQuestions())

        // Given - At last question
        val lastState = state.copy(currentQuestionIndex = 2)
        assertFalse("Should not have more questions", lastState.hasMoreQuestions())
    }

    @Test
    fun `getProgressPercentage calculates correctly`() {
        // Given - At question 1 of 4
        val state = InterviewSessionUiState(
            currentQuestionIndex = 1,
            totalQuestions = 4
        )

        // Then
        assertEquals(25, state.getProgressPercentage())

        // Given - At question 2 of 4
        val midState = state.copy(currentQuestionIndex = 2)
        assertEquals(50, midState.getProgressPercentage())
    }

    // ============================================
    // TTS SERVICE SELECTION TESTS
    // ============================================

    @Test
    fun `initializeTTS uses Sarvam AI for Pro subscription`() = runTest {
        // Given - Pro user with Sarvam AI ready
        val proProfile = testProfile.copy(subscriptionType = SubscriptionType.PRO)
        coEvery { userProfileRepository.getUserProfile(testUserId) } returns flowOf(Result.success(proProfile))
        every { sarvamTTSService.isReady() } returns true
        coEvery { interviewRepository.getSession(testSessionId) } returns Result.success(testSession)
        coEvery { interviewRepository.getQuestion("q1") } returns Result.success(testQuestion)

        // When
        val viewModel = createViewModel()
        advanceUntilIdle()

        // Then - Sarvam AI should be used for Pro tier
        // Verify by checking that Sarvam TTS was initialized
        verify { sarvamTTSService.isReady() }
    }

    @Test
    fun `initializeTTS uses Android TTS for Free subscription`() = runTest {
        // Given - Free user
        val freeProfile = testProfile.copy(subscriptionType = SubscriptionType.FREE)
        coEvery { userProfileRepository.getUserProfile(testUserId) } returns flowOf(Result.success(freeProfile))
        coEvery { interviewRepository.getSession(testSessionId) } returns Result.success(testSession)
        coEvery { interviewRepository.getQuestion("q1") } returns Result.success(testQuestion)

        // When
        val viewModel = createViewModel()
        advanceUntilIdle()
        
        // Emit TTS ready event on Android TTS to verify it's listening
        androidTTSEvents.emit(TTSService.TTSEvent.Ready)
        advanceUntilIdle()

        // Then - Android TTS events should be observed (Free tier uses Android TTS)
        val state = viewModel.uiState.value
        assertTrue("TTS should be ready for Free tier", state.isTTSReady)
    }

    @Test
    fun `initializeTTS falls back to ElevenLabs when Sarvam unavailable`() = runTest {
        // Given - Premium user but Sarvam is not ready, ElevenLabs is ready
        val premiumProfile = testProfile.copy(subscriptionType = SubscriptionType.PREMIUM)
        coEvery { userProfileRepository.getUserProfile(testUserId) } returns flowOf(Result.success(premiumProfile))
        every { sarvamTTSService.isReady() } returns false
        every { elevenLabsTTSService.isReady() } returns true
        coEvery { interviewRepository.getSession(testSessionId) } returns Result.success(testSession)
        coEvery { interviewRepository.getQuestion("q1") } returns Result.success(testQuestion)

        // When
        val viewModel = createViewModel()
        advanceUntilIdle()

        // Then - ElevenLabs should be checked as fallback
        verify { sarvamTTSService.isReady() }
        verify { elevenLabsTTSService.isReady() }
    }

    @Test
    fun `initializeTTS falls back to Android TTS when no premium services available`() = runTest {
        // Given - Premium user but neither Sarvam nor ElevenLabs are ready
        val premiumProfile = testProfile.copy(subscriptionType = SubscriptionType.PREMIUM)
        coEvery { userProfileRepository.getUserProfile(testUserId) } returns flowOf(Result.success(premiumProfile))
        every { sarvamTTSService.isReady() } returns false
        every { elevenLabsTTSService.isReady() } returns false
        coEvery { interviewRepository.getSession(testSessionId) } returns Result.success(testSession)
        coEvery { interviewRepository.getQuestion("q1") } returns Result.success(testQuestion)

        // When
        val viewModel = createViewModel()
        advanceUntilIdle()

        // Then - Both premium services should have been checked before falling back
        verify { sarvamTTSService.isReady() }
        verify { elevenLabsTTSService.isReady() }
    }

    @Test
    fun `TTS speaks question when ready event received`() = runTest {
        // Given
        coEvery { interviewRepository.getSession(testSessionId) } returns Result.success(testSession)
        coEvery { interviewRepository.getQuestion("q1") } returns Result.success(testQuestion)
        
        // When
        val viewModel = createViewModel()
        advanceUntilIdle()
        
        // Emit TTS ready event
        androidTTSEvents.emit(TTSService.TTSEvent.Ready)
        advanceUntilIdle()

        // Then - TTS should be ready and speak is called (through the event handler)
        val state = viewModel.uiState.value
        assertTrue("TTS should be ready", state.isTTSReady)
    }

    @Test
    fun `TTS speaking state updates on speech complete`() = runTest {
        // Given
        coEvery { interviewRepository.getSession(testSessionId) } returns Result.success(testSession)
        coEvery { interviewRepository.getQuestion("q1") } returns Result.success(testQuestion)
        
        // When
        val viewModel = createViewModel()
        advanceUntilIdle()
        
        // Emit speech complete event
        androidTTSEvents.emit(TTSService.TTSEvent.SpeechComplete)
        advanceUntilIdle()

        // Then - Speaking state should be false
        val state = viewModel.uiState.value
        assertFalse("TTS should not be speaking after complete", state.isTTSSpeaking)
    }
}
