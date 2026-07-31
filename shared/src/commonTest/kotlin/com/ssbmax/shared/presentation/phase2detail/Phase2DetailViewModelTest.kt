@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.ssbmax.shared.presentation.phase2detail

import com.ssbmax.shared.domain.model.Phase2Progress
import com.ssbmax.shared.domain.model.TestProgress
import com.ssbmax.shared.domain.model.TestStatus
import com.ssbmax.shared.domain.model.TestType
import com.ssbmax.shared.domain.usecase.auth.ObserveCurrentUserUseCase
import com.ssbmax.shared.domain.util.NoOpLogger
import com.ssbmax.shared.presentation.testing.FakeAuthRepository
import com.ssbmax.shared.presentation.testing.FakeTestProgressRepository
import com.ssbmax.shared.presentation.testing.testUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Characterization test for [Phase2DetailViewModel], written retroactively
 * (13-VM gap-closing pass, see the KMP-convergence plan's Phase 1). Pins the
 * current TAT/WAT/SRT/SD + GTO + Interview test-list mapping (4 psychology
 * cards share one `psychologyProgress`, per the ViewModel's own doc comment).
 */
class Phase2DetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var authRepository: FakeAuthRepository
    private lateinit var testProgressRepository: FakeTestProgressRepository

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        authRepository = FakeAuthRepository(initialUser = testUser())
        testProgressRepository = FakeTestProgressRepository()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildViewModel() = Phase2DetailViewModel(
        testProgressRepository = testProgressRepository,
        observeCurrentUser = ObserveCurrentUserUseCase(authRepository),
        logger = NoOpLogger()
    )

    @Test
    fun `loads the 6 phase-2 test cards with default not-attempted status`() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(false, state.isLoading)
        assertEquals(null, state.error)
        assertEquals(6, state.tests.size)
        assertEquals(
            listOf(TestType.TAT, TestType.WAT, TestType.SRT, TestType.SD, TestType.GTO_GD, TestType.IO),
            state.tests.map { it.type }
        )
        assertTrue(state.tests.all { it.status == TestStatus.NOT_ATTEMPTED })
        assertEquals(0f, state.averageScore)
    }

    @Test
    fun `blocks unauthenticated access with a login prompt`() = runTest(testDispatcher) {
        authRepository.userFlow.value = null
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Please login to view Phase 2 progress", viewModel.uiState.value.error)
    }

    @Test
    fun `psychology tests share one progress and average score reflects completed tests`() = runTest(testDispatcher) {
        testProgressRepository.phase2Flow = flowOf(
            Phase2Progress(
                psychologyProgress = TestProgress(TestType.TAT, status = TestStatus.GRADED, latestScore = 60f),
                gtoProgress = TestProgress(TestType.GTO_GD, status = TestStatus.NOT_ATTEMPTED),
                interviewProgress = TestProgress(TestType.IO, status = TestStatus.GRADED, latestScore = 80f)
            )
        )
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        // All 4 psychology cards (TAT/WAT/SRT/SD) share psychologyProgress's GRADED status/score.
        val psychologyTests = state.tests.filter { it.type in listOf(TestType.TAT, TestType.WAT, TestType.SRT, TestType.SD) }
        assertTrue(psychologyTests.all { it.status == TestStatus.GRADED && it.latestScore == 60f })
        assertEquals(64f, state.averageScore) // average of the 5 GRADED cards: four at 60 + one IO at 80 -> 320/5
    }

    @Test
    fun `surfaces an error when the progress flow fails`() = runTest(testDispatcher) {
        testProgressRepository.phase2Flow = flow { throw Exception("firestore unavailable") }
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(false, state.isLoading)
        assertNotNull(state.error)
    }
}
