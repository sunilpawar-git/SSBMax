package com.ssbmax.ui.interview.session

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
 * Tests TTS service initialization and speech control.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TTSManagerTest {

    private lateinit var androidTTSService: TTSService
    private lateinit var androidEvents: MutableSharedFlow<TTSService.TTSEvent>

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        androidEvents = MutableSharedFlow(replay = 1)

        androidTTSService = mockk(relaxed = true) {
            every { events } returns androidEvents
            every { isReady() } returns true
            every { isSpeaking() } returns false
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.createTTSManager(): TTSManager {
        return TTSManager(
            androidTTSService = androidTTSService,
            scope = this
        )
    }

    // ===== Initialization Tests =====

    @Test
    fun `initialize sets up Android TTS`() = runTest {
        val manager = createTTSManager()

        manager.initialize()
        advanceUntilIdle()

        // Verify Android TTS events are being observed
        verify { androidTTSService.events }
        manager.release()
    }

    // ===== Speak Tests =====

    @Test
    fun `speak calls TTS service when not muted`() = runTest {
        val manager = createTTSManager()
        manager.initialize()
        advanceUntilIdle()

        manager.speak("Hello world")
        advanceUntilIdle()

        coVerify { androidTTSService.speak("Hello world", any()) }
        manager.release()
    }

    @Test
    fun `speak skips when muted`() = runTest {
        val manager = createTTSManager()
        manager.initialize()
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
        val manager = createTTSManager()
        manager.initialize()
        advanceUntilIdle()

        assertFalse(manager.isTTSMuted.value)

        manager.toggleMute(null)

        assertTrue(manager.isTTSMuted.value)
        verify { androidTTSService.stop() }
        manager.release()
    }

    @Test
    fun `toggleMute speaks question when unmuting`() = runTest {
        val manager = createTTSManager()
        manager.initialize()
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
    fun `stop calls stop on TTS service`() = runTest {
        val manager = createTTSManager()
        manager.initialize()
        advanceUntilIdle()

        manager.stop()

        verify { androidTTSService.stop() }
        manager.release()
    }

    // ===== Release Tests =====

    @Test
    fun `release releases TTS service`() = runTest {
        val manager = createTTSManager()
        manager.initialize()
        advanceUntilIdle()

        manager.release()

        verify { androidTTSService.release() }
    }
}
