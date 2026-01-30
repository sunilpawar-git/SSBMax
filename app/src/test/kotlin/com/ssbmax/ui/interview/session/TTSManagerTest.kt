package com.ssbmax.ui.interview.session

import com.ssbmax.core.data.analytics.AnalyticsManager
import com.ssbmax.core.domain.model.EntryType
import com.ssbmax.core.domain.model.Gender
import com.ssbmax.core.domain.model.SSBMaxUser
import com.ssbmax.core.domain.model.SubscriptionType
import com.ssbmax.core.domain.model.UserProfile
import com.ssbmax.core.domain.model.UserRole
import com.ssbmax.core.domain.repository.AuthRepository
import com.ssbmax.core.domain.repository.UserProfileRepository
import com.ssbmax.utils.tts.TTSService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for TTSManager
 *
 * Tests TTS service selection, speech control, and fallback logic.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TTSManagerTest {

    private lateinit var qwenTTSService: TTSService
    private lateinit var androidTTSService: TTSService
    private lateinit var authRepository: AuthRepository
    private lateinit var userProfileRepository: UserProfileRepository
    private lateinit var analyticsManager: AnalyticsManager

    private lateinit var qwenEvents: MutableSharedFlow<TTSService.TTSEvent>
    private lateinit var androidEvents: MutableSharedFlow<TTSService.TTSEvent>

    private val testDispatcher = StandardTestDispatcher()

    private val testUserId = "user-123"
    private val testUser = SSBMaxUser(
        id = testUserId,
        email = "test@example.com",
        displayName = "Test User",
        role = UserRole.STUDENT
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        qwenEvents = MutableSharedFlow(replay = 1)
        androidEvents = MutableSharedFlow(replay = 1)

        qwenTTSService = mockk(relaxed = true) {
            every { events } returns qwenEvents
            every { isReady() } returns true
            every { isSpeaking() } returns false
        }

        androidTTSService = mockk(relaxed = true) {
            every { events } returns androidEvents
            every { isReady() } returns true
            every { isSpeaking() } returns false
        }

        authRepository = mockk {
            every { currentUser } returns MutableStateFlow(testUser)
        }

        userProfileRepository = mockk()
        analyticsManager = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.createTTSManager(): TTSManager {
        return TTSManager(
            qwenTTSService = qwenTTSService,
            androidTTSService = androidTTSService,
            authRepository = authRepository,
            userProfileRepository = userProfileRepository,
            analyticsManager = analyticsManager,
            scope = this
        )
    }

    private fun mockUserProfile(subscriptionType: SubscriptionType) {
        val profile = UserProfile(
            userId = testUserId,
            fullName = "Test User",
            age = 22,
            gender = Gender.MALE,
            entryType = EntryType.GRADUATE,
            subscriptionType = subscriptionType
        )
        coEvery { userProfileRepository.getUserProfile(testUserId) } returns flowOf(Result.success(profile))
    }

    // ===== Initialization Tests =====

    @Test
    fun `initialize selects QwenTTS for PRO subscription`() = runTest {
        mockUserProfile(SubscriptionType.PRO)
        val manager = createTTSManager()

        manager.initialize(forcePremium = false)
        advanceUntilIdle()

        verify { qwenTTSService.isReady() }
        manager.release()
    }

    @Test
    fun `initialize selects QwenTTS for PREMIUM subscription`() = runTest {
        mockUserProfile(SubscriptionType.PREMIUM)
        val manager = createTTSManager()

        manager.initialize(forcePremium = false)
        advanceUntilIdle()

        verify { qwenTTSService.isReady() }
        manager.release()
    }

    @Test
    fun `initialize selects AndroidTTS for FREE subscription`() = runTest {
        mockUserProfile(SubscriptionType.FREE)
        val manager = createTTSManager()

        manager.initialize(forcePremium = false)
        advanceUntilIdle()

        // Verify Android TTS events are being observed (proves it's the active service)
        verify { androidTTSService.events }
        manager.release()
    }

    @Test
    fun `initialize falls back to AndroidTTS when QwenTTS not ready`() = runTest {
        mockUserProfile(SubscriptionType.PRO)
        every { qwenTTSService.isReady() } returns false

        val manager = createTTSManager()

        manager.initialize(forcePremium = false)
        advanceUntilIdle()

        verify { androidTTSService.isReady() }
        manager.release()
    }

    @Test
    fun `initialize with forcePremium uses QwenTTS regardless of subscription`() = runTest {
        mockUserProfile(SubscriptionType.FREE)
        val manager = createTTSManager()

        manager.initialize(forcePremium = true)
        advanceUntilIdle()

        verify { qwenTTSService.isReady() }
        manager.release()
    }

    // ===== Speak Tests =====

    @Test
    fun `speak calls TTS service when not muted`() = runTest {
        mockUserProfile(SubscriptionType.FREE)
        val manager = createTTSManager()
        manager.initialize(forcePremium = false)
        advanceUntilIdle()

        manager.speak("Hello world")
        advanceUntilIdle()

        coVerify { androidTTSService.speak("Hello world", any()) }
        manager.release()
    }

    @Test
    fun `speak skips when muted`() = runTest {
        mockUserProfile(SubscriptionType.FREE)
        val manager = createTTSManager()
        manager.initialize(forcePremium = false)
        advanceUntilIdle()

        manager.toggleMute(null)
        assertTrue(manager.isTTSMuted.value)

        manager.speak("Hello world")
        advanceUntilIdle()

        coVerify(exactly = 0) { androidTTSService.speak(any(), any()) }
        manager.release()
    }

    // ===== Toggle Mute Tests =====

    @Test
    fun `toggleMute stops speech when muting`() = runTest {
        mockUserProfile(SubscriptionType.FREE)
        val manager = createTTSManager()
        manager.initialize(forcePremium = false)
        advanceUntilIdle()

        assertFalse(manager.isTTSMuted.value)

        manager.toggleMute(null)

        assertTrue(manager.isTTSMuted.value)
        verify { androidTTSService.stop() }
        manager.release()
    }

    @Test
    fun `toggleMute speaks question when unmuting`() = runTest {
        mockUserProfile(SubscriptionType.FREE)
        val manager = createTTSManager()
        manager.initialize(forcePremium = false)
        advanceUntilIdle()

        manager.toggleMute(null)
        assertTrue(manager.isTTSMuted.value)

        manager.toggleMute("What is your name?")
        advanceUntilIdle()

        assertFalse(manager.isTTSMuted.value)
        coVerify { androidTTSService.speak("What is your name?", any()) }
        manager.release()
    }

    // ===== Stop Tests =====

    @Test
    fun `stop calls stop on active TTS service`() = runTest {
        mockUserProfile(SubscriptionType.FREE)
        val manager = createTTSManager()
        manager.initialize(forcePremium = false)
        advanceUntilIdle()

        manager.stop()

        verify { androidTTSService.stop() }
        manager.release()
    }

    // ===== Release Tests =====

    @Test
    fun `release releases both TTS services`() = runTest {
        mockUserProfile(SubscriptionType.FREE)
        val manager = createTTSManager()
        manager.initialize(forcePremium = false)
        advanceUntilIdle()

        manager.release()

        verify { qwenTTSService.release() }
        verify { androidTTSService.release() }
    }
}
