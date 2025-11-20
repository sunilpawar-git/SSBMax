package com.ssbmax.ui.phase

import com.ssbmax.core.domain.model.Phase2Progress
import com.ssbmax.core.domain.model.SSBMaxUser
import com.ssbmax.core.domain.model.TestProgress
import com.ssbmax.core.domain.model.TestStatus
import com.ssbmax.core.domain.model.TestType
import com.ssbmax.core.domain.model.UserRole
import com.ssbmax.core.domain.repository.TestProgressRepository
import com.ssbmax.core.domain.usecase.auth.ObserveCurrentUserUseCase
import com.ssbmax.testing.BaseViewModelTest
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

/**
 * Comprehensive tests for Phase2DetailViewModel
 * Tests Phase 2 progress tracking for Psychology, GTO, and Interview tests
 */
@OptIn(ExperimentalCoroutinesApi::class)
class Phase2DetailViewModelTest : BaseViewModelTest() {
    
    private lateinit var viewModel: Phase2DetailViewModel
    private lateinit var mockTestProgressRepository: TestProgressRepository
    private lateinit var mockObserveCurrentUser: ObserveCurrentUserUseCase
    
    private lateinit var mockCurrentUserFlow: MutableStateFlow<SSBMaxUser?>
    
    private val testUser = SSBMaxUser(
        id = "test-user-123",
        email = "test@student.com",
        displayName = "Test Student",
        role = UserRole.STUDENT
    )
    
    private val psychologyProgressNotAttempted = TestProgress(
        testType = TestType.TAT,
        status = TestStatus.NOT_ATTEMPTED,
        latestScore = null
    )
    
    private val psychologyProgressCompleted = TestProgress(
        testType = TestType.TAT,
        status = TestStatus.COMPLETED,
        latestScore = 82.0f
    )
    
    private val gtoProgressNotAttempted = TestProgress(
        testType = TestType.GTO_GD,
        status = TestStatus.NOT_ATTEMPTED,
        latestScore = null
    )
    
    private val gtoProgressGraded = TestProgress(
        testType = TestType.GTO_GD,
        status = TestStatus.GRADED,
        latestScore = 88.5f
    )
    
    private val ioProgressNotAttempted = TestProgress(
        testType = TestType.IO,
        status = TestStatus.NOT_ATTEMPTED,
        latestScore = null
    )
    
    private val ioProgressGraded = TestProgress(
        testType = TestType.IO,
        status = TestStatus.GRADED,
        latestScore = 90.0f
    )
    
    @Before
    fun setup() {
        mockTestProgressRepository = mockk()
        mockObserveCurrentUser = mockk()
        
        mockCurrentUserFlow = MutableStateFlow(testUser)
        coEvery { mockObserveCurrentUser() } returns mockCurrentUserFlow
    }
    
    @After
    fun tearDown() {
        unmockkAll()
    }
    
    // ==================== Initialization Tests ====================
    
    @Test
    fun `init loads Phase 2 tests for authenticated user`() = runTest {
        // Given
        val phase2Progress = Phase2Progress(
            psychologyProgress = psychologyProgressCompleted,
            gtoProgress = gtoProgressGraded,
            interviewProgress = ioProgressGraded
        )
        coEvery { mockTestProgressRepository.getPhase2Progress(testUser.id) } returns 
            MutableStateFlow(phase2Progress)
        
        // When
        viewModel = Phase2DetailViewModel(mockTestProgressRepository, mockObserveCurrentUser)
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        assertFalse("Should not be loading", state.isLoading)
        assertNull("Should have no error", state.error)
        assertTrue("Should have tests", state.tests.isNotEmpty())
    }
    
    @Test
    fun `loads all Phase 2 test categories`() = runTest {
        // Given
        val phase2Progress = Phase2Progress(
            psychologyProgress = psychologyProgressNotAttempted,
            gtoProgress = gtoProgressNotAttempted,
            interviewProgress = ioProgressNotAttempted
        )
        coEvery { mockTestProgressRepository.getPhase2Progress(testUser.id) } returns 
            MutableStateFlow(phase2Progress)
        
        // When
        viewModel = Phase2DetailViewModel(mockTestProgressRepository, mockObserveCurrentUser)
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        
        assertEquals("Should have 4 psychology tests", 4, state.psychologyTests.size)
        assertEquals("Should have 1 GTO test", 1, state.gtoTests.size)
        assertEquals("Should have 1 IO test", 1, state.ioTests.size)
        assertEquals("Total should be 6 tests", 6, state.tests.size)
    }
    
    @Test
    fun `psychology tests have correct names and types`() = runTest {
        // Given
        val phase2Progress = Phase2Progress(
            psychologyProgress = psychologyProgressNotAttempted,
            gtoProgress = gtoProgressNotAttempted,
            interviewProgress = ioProgressNotAttempted
        )
        coEvery { mockTestProgressRepository.getPhase2Progress(testUser.id) } returns 
            MutableStateFlow(phase2Progress)
        
        // When
        viewModel = Phase2DetailViewModel(mockTestProgressRepository, mockObserveCurrentUser)
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        val psychTests = state.psychologyTests
        
        assertEquals("TAT test", TestType.TAT, psychTests[0].type)
        assertEquals("TAT name", "TAT", psychTests[0].name)
        
        assertEquals("WAT test", TestType.WAT, psychTests[1].type)
        assertEquals("WAT name", "WAT", psychTests[1].name)
        
        assertEquals("SRT test", TestType.SRT, psychTests[2].type)
        assertEquals("SRT name", "SRT", psychTests[2].name)
        
        assertEquals("SD test", TestType.SD, psychTests[3].type)
        assertEquals("SD name", "SD", psychTests[3].name)
    }
    
    @Test
    fun `no authenticated user shows error`() = runTest {
        // Given
        mockCurrentUserFlow.value = null
        
        // When
        viewModel = Phase2DetailViewModel(mockTestProgressRepository, mockObserveCurrentUser)
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        assertFalse("Should not be loading", state.isLoading)
        assertNotNull("Should have error", state.error)
        assertTrue("Error should mention login", 
            state.error?.contains("login", ignoreCase = true) == true)
    }
    
    // ==================== Progress Tracking Tests ====================
    
    @Test
    fun `NOT_ATTEMPTED tests show no score`() = runTest {
        // Given
        val phase2Progress = Phase2Progress(
            psychologyProgress = psychologyProgressNotAttempted,
            gtoProgress = gtoProgressNotAttempted,
            interviewProgress = ioProgressNotAttempted
        )
        coEvery { mockTestProgressRepository.getPhase2Progress(testUser.id) } returns 
            MutableStateFlow(phase2Progress)
        
        // When
        viewModel = Phase2DetailViewModel(mockTestProgressRepository, mockObserveCurrentUser)
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        state.tests.forEach { test ->
            assertEquals("Test should be NOT_ATTEMPTED", TestStatus.NOT_ATTEMPTED, test.status)
            assertNull("NOT_ATTEMPTED test should have no score", test.latestScore)
        }
    }
    
    @Test
    fun `COMPLETED psychology tests show score`() = runTest {
        // Given
        val phase2Progress = Phase2Progress(
            psychologyProgress = psychologyProgressCompleted,
            gtoProgress = gtoProgressNotAttempted,
            interviewProgress = ioProgressNotAttempted
        )
        coEvery { mockTestProgressRepository.getPhase2Progress(testUser.id) } returns 
            MutableStateFlow(phase2Progress)
        
        // When
        viewModel = Phase2DetailViewModel(mockTestProgressRepository, mockObserveCurrentUser)
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        
        // All psychology tests share the same base progress
        state.psychologyTests.forEach { test ->
            assertEquals("Psychology test should be COMPLETED", TestStatus.COMPLETED, test.status)
            assertEquals("Psychology test should have score", 82.0f, test.latestScore ?: 0f, 0.01f)
        }
    }
    
    @Test
    fun `GRADED GTO tests show score`() = runTest {
        // Given
        val phase2Progress = Phase2Progress(
            psychologyProgress = psychologyProgressNotAttempted,
            gtoProgress = gtoProgressGraded,
            interviewProgress = ioProgressNotAttempted
        )
        coEvery { mockTestProgressRepository.getPhase2Progress(testUser.id) } returns 
            MutableStateFlow(phase2Progress)
        
        // When
        viewModel = Phase2DetailViewModel(mockTestProgressRepository, mockObserveCurrentUser)
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        val gtoTest = state.gtoTests[0]
        
        assertEquals("GTO should be GRADED", TestStatus.GRADED, gtoTest.status)
        assertEquals("GTO should have score", 88.5f, gtoTest.latestScore ?: 0f, 0.01f)
    }
    
    @Test
    fun `GRADED IO tests show score`() = runTest {
        // Given
        val phase2Progress = Phase2Progress(
            psychologyProgress = psychologyProgressNotAttempted,
            gtoProgress = gtoProgressNotAttempted,
            interviewProgress = ioProgressGraded
        )
        coEvery { mockTestProgressRepository.getPhase2Progress(testUser.id) } returns 
            MutableStateFlow(phase2Progress)
        
        // When
        viewModel = Phase2DetailViewModel(mockTestProgressRepository, mockObserveCurrentUser)
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        val ioTest = state.ioTests[0]
        
        assertEquals("IO should be GRADED", TestStatus.GRADED, ioTest.status)
        assertEquals("IO should have score", 90.0f, ioTest.latestScore ?: 0f, 0.01f)
    }
    
    // ==================== Average Score Calculation Tests ====================
    
    @Test
    fun `average score is zero when no tests completed`() = runTest {
        // Given
        val phase2Progress = Phase2Progress(
            psychologyProgress = psychologyProgressNotAttempted,
            gtoProgress = gtoProgressNotAttempted,
            interviewProgress = ioProgressNotAttempted
        )
        coEvery { mockTestProgressRepository.getPhase2Progress(testUser.id) } returns 
            MutableStateFlow(phase2Progress)
        
        // When
        viewModel = Phase2DetailViewModel(mockTestProgressRepository, mockObserveCurrentUser)
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        assertEquals("Average score should be 0", 0f, state.averageScore, 0.01f)
    }
    
    @Test
    fun `average score calculated from completed psychology tests`() = runTest {
        // Given
        val phase2Progress = Phase2Progress(
            psychologyProgress = psychologyProgressCompleted,  // 82.0
            gtoProgress = gtoProgressNotAttempted,
            interviewProgress = ioProgressNotAttempted
        )
        coEvery { mockTestProgressRepository.getPhase2Progress(testUser.id) } returns 
            MutableStateFlow(phase2Progress)
        
        // When
        viewModel = Phase2DetailViewModel(mockTestProgressRepository, mockObserveCurrentUser)
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        // All 4 psychology tests share same score
        assertEquals("Average score should match psychology score", 
            82.0f, state.averageScore, 0.01f)
    }
    
    @Test
    fun `average score calculated from all three categories`() = runTest {
        // Given
        val phase2Progress = Phase2Progress(
            psychologyProgress = psychologyProgressCompleted,  // 82.0 (x4 tests)
            gtoProgress = gtoProgressGraded,                   // 88.5 (x1 test)
            interviewProgress = ioProgressGraded               // 90.0 (x1 test)
        )
        coEvery { mockTestProgressRepository.getPhase2Progress(testUser.id) } returns 
            MutableStateFlow(phase2Progress)
        
        // When
        viewModel = Phase2DetailViewModel(mockTestProgressRepository, mockObserveCurrentUser)
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        // Average of 4 psychology tests (82.0 each) + 1 GTO (88.5) + 1 IO (90.0)
        val expectedAverage = (82.0f * 4 + 88.5f + 90.0f) / 6f  // ~84.08
        assertEquals("Average should include all test categories", 
            expectedAverage, state.averageScore, 0.01f)
    }
    
    // ==================== Test Details Tests ====================
    
    @Test
    fun `TAT test has correct details`() = runTest {
        // Given
        val phase2Progress = Phase2Progress(
            psychologyProgress = psychologyProgressNotAttempted,
            gtoProgress = gtoProgressNotAttempted,
            interviewProgress = ioProgressNotAttempted
        )
        coEvery { mockTestProgressRepository.getPhase2Progress(testUser.id) } returns 
            MutableStateFlow(phase2Progress)
        
        // When
        viewModel = Phase2DetailViewModel(mockTestProgressRepository, mockObserveCurrentUser)
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        val tatTest = state.psychologyTests.find { it.type == TestType.TAT }
        
        assertNotNull("TAT test should exist", tatTest)
        assertEquals("TAT duration", 30, tatTest?.durationMinutes)
        assertEquals("TAT question count", 12, tatTest?.questionCount)
        assertTrue("TAT description should mention pictures",
            tatTest?.description?.contains("picture", ignoreCase = true) == true)
    }
    
    @Test
    fun `GTO test has correct details`() = runTest {
        // Given
        val phase2Progress = Phase2Progress(
            psychologyProgress = psychologyProgressNotAttempted,
            gtoProgress = gtoProgressNotAttempted,
            interviewProgress = ioProgressNotAttempted
        )
        coEvery { mockTestProgressRepository.getPhase2Progress(testUser.id) } returns 
            MutableStateFlow(phase2Progress)
        
        // When
        viewModel = Phase2DetailViewModel(mockTestProgressRepository, mockObserveCurrentUser)
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        val gtoTest = state.gtoTests[0]
        
        assertEquals("GTO type", TestType.GTO_GD, gtoTest.type)
        assertEquals("GTO duration", 180, gtoTest.durationMinutes)
        assertEquals("GTO question count", 8, gtoTest.questionCount)
        assertTrue("GTO description should mention group",
            gtoTest.description.contains("group", ignoreCase = true))
    }
    
    @Test
    fun `IO test has correct details`() = runTest {
        // Given
        val phase2Progress = Phase2Progress(
            psychologyProgress = psychologyProgressNotAttempted,
            gtoProgress = gtoProgressNotAttempted,
            interviewProgress = ioProgressNotAttempted
        )
        coEvery { mockTestProgressRepository.getPhase2Progress(testUser.id) } returns 
            MutableStateFlow(phase2Progress)
        
        // When
        viewModel = Phase2DetailViewModel(mockTestProgressRepository, mockObserveCurrentUser)
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        val ioTest = state.ioTests[0]
        
        assertEquals("IO type", TestType.IO, ioTest.type)
        assertEquals("IO duration", 45, ioTest.durationMinutes)
        assertEquals("IO question count", 1, ioTest.questionCount)
        assertTrue("IO description should mention interview",
            ioTest.description.contains("interview", ignoreCase = true))
    }
    
    // ==================== Error Handling Tests ====================
    
    @Test
    fun `repository exception shows error message`() = runTest {
        // Given
        coEvery { mockTestProgressRepository.getPhase2Progress(testUser.id) } throws 
            RuntimeException("Database error")
        
        // When
        viewModel = Phase2DetailViewModel(mockTestProgressRepository, mockObserveCurrentUser)
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        assertFalse("Should not be loading", state.isLoading)
        assertNotNull("Should have error", state.error)
        assertTrue("Error should mention database",
            state.error?.contains("Database error") == true)
    }
    
    // ==================== Reactive Updates Tests ====================
    
    @Test
    fun `ui state updates when repository emits new progress`() = runTest {
        // Given
        val progressFlow = MutableStateFlow(
            Phase2Progress(
                psychologyProgress = psychologyProgressNotAttempted,
                gtoProgress = gtoProgressNotAttempted,
                interviewProgress = ioProgressNotAttempted
            )
        )
        coEvery { mockTestProgressRepository.getPhase2Progress(testUser.id) } returns progressFlow
        
        viewModel = Phase2DetailViewModel(mockTestProgressRepository, mockObserveCurrentUser)
        advanceUntilIdle()
        
        val initialState = viewModel.uiState.value
        assertEquals("Initial average should be 0", 0f, initialState.averageScore, 0.01f)
        
        // When - repository emits updated progress
        progressFlow.value = Phase2Progress(
            psychologyProgress = psychologyProgressCompleted,  // 82.0 (x4)
            gtoProgress = gtoProgressGraded,                   // 88.5 (x1)
            interviewProgress = ioProgressGraded               // 90.0 (x1)
        )
        advanceUntilIdle()
        
        // Then
        val updatedState = viewModel.uiState.value
        val expectedAverage = (82.0f * 4 + 88.5f + 90.0f) / 6f
        assertEquals("Average should update", expectedAverage, updatedState.averageScore, 0.01f)
    }
    
    // ==================== UI State Tests ====================
    
    @Test
    fun `ui state has correct default values`() {
        val defaultState = Phase2DetailUiState()
        
        assertTrue("Default should have no tests", defaultState.tests.isEmpty())
        assertTrue("Default should have no psychology tests", defaultState.psychologyTests.isEmpty())
        assertTrue("Default should have no GTO tests", defaultState.gtoTests.isEmpty())
        assertTrue("Default should have no IO tests", defaultState.ioTests.isEmpty())
        assertEquals("Default average should be 0", 0f, defaultState.averageScore, 0.01f)
        assertTrue("Default should be loading", defaultState.isLoading)
        assertNull("Default should have no error", defaultState.error)
    }
    
    @Test
    fun `tests maintain correct order psychology, GTO, then IO`() = runTest {
        // Given
        val phase2Progress = Phase2Progress(
            psychologyProgress = psychologyProgressCompleted,
            gtoProgress = gtoProgressGraded,
            interviewProgress = ioProgressGraded
        )
        coEvery { mockTestProgressRepository.getPhase2Progress(testUser.id) } returns 
            MutableStateFlow(phase2Progress)
        
        // When
        viewModel = Phase2DetailViewModel(mockTestProgressRepository, mockObserveCurrentUser)
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        val tests = state.tests
        
        // First 4 should be psychology
        assertEquals("Test 0 should be TAT", TestType.TAT, tests[0].type)
        assertEquals("Test 1 should be WAT", TestType.WAT, tests[1].type)
        assertEquals("Test 2 should be SRT", TestType.SRT, tests[2].type)
        assertEquals("Test 3 should be SD", TestType.SD, tests[3].type)
        
        // Then GTO
        assertEquals("Test 4 should be GTO", TestType.GTO_GD, tests[4].type)
        
        // Then IO
        assertEquals("Test 5 should be IO", TestType.IO, tests[5].type)
    }
}

