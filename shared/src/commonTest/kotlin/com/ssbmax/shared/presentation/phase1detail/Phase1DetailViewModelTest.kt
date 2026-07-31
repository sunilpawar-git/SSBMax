@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.ssbmax.shared.presentation.phase1detail

import com.ssbmax.shared.domain.model.Phase1Progress
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

/**
 * Characterization test for [Phase1DetailViewModel], written retroactively
 * (13-VM gap-closing pass, see the KMP-convergence plan's Phase 1). Pins the
 * current OIR/PPDT progress-mapping + average-score behaviour.
 */
class Phase1DetailViewModelTest {

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

    private fun buildViewModel() = Phase1DetailViewModel(
        testProgressRepository = testProgressRepository,
        observeCurrentUser = ObserveCurrentUserUseCase(authRepository),
        logger = NoOpLogger()
    )

    @Test
    fun `loads OIR and PPDT tests with default not-attempted status`() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(false, state.isLoading)
        assertEquals(null, state.error)
        assertEquals(2, state.tests.size)
        assertEquals(TestType.OIR, state.tests[0].type)
        assertEquals(TestType.PPDT, state.tests[1].type)
        assertEquals(TestStatus.NOT_ATTEMPTED, state.tests[0].status)
        assertEquals(0f, state.averageScore)
    }

    @Test
    fun `blocks unauthenticated access with a login prompt`() = runTest(testDispatcher) {
        authRepository.userFlow.value = null
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(false, state.isLoading)
        assertEquals("Please login to view Phase 1 progress", state.error)
    }

    @Test
    fun `average score reflects only completed tests`() = runTest(testDispatcher) {
        testProgressRepository.phase1Flow = flowOf(
            Phase1Progress(
                oirProgress = TestProgress(TestType.OIR, status = TestStatus.GRADED, latestScore = 80f),
                ppdtProgress = TestProgress(TestType.PPDT, status = TestStatus.NOT_ATTEMPTED)
            )
        )
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(80f, state.averageScore)
        assertEquals(TestStatus.GRADED, state.tests.first { it.type == TestType.OIR }.status)
    }

    @Test
    fun `surfaces an error when the progress flow fails`() = runTest(testDispatcher) {
        testProgressRepository.phase1Flow = flow { throw Exception("firestore unavailable") }
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(false, state.isLoading)
        assertNotNull(state.error)
    }
}
