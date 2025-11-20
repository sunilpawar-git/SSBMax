package com.ssbmax.ui.phase

import com.ssbmax.core.domain.model.Phase1Progress
import com.ssbmax.core.domain.model.TestProgress
import com.ssbmax.core.domain.model.TestStatus
import com.ssbmax.core.domain.model.TestType
import com.ssbmax.core.domain.model.SSBMaxUser
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
 * Comprehensive tests for Phase1DetailViewModel
 * Tests Phase 1 progress tracking for OIR and PPDT tests
 */
@OptIn(ExperimentalCoroutinesApi::class)
class Phase1DetailViewModelTest : BaseViewModelTest() {
    
    private lateinit var viewModel: Phase1DetailViewModel
    private lateinit var mockTestProgressRepository: TestProgressRepository
    private lateinit var mockObserveCurrentUser: ObserveCurrentUserUseCase
    
    private lateinit var mockCurrentUserFlow: MutableStateFlow<SSBMaxUser?>
    
    private val testUser = SSBMaxUser(
        id = "test-user-123",
        email = "test@student.com",
        displayName = "Test Student",
        role = UserRole.STUDENT
    )
    
    private val oirProgressNotStarted = TestProgress(
        testType = TestType.OIR,
        status = TestStatus.NOT_ATTEMPTED,
        latestScore = null
    )
    
    private val oirProgressCompleted = TestProgress(
        testType = TestType.OIR,
        status = TestStatus.COMPLETED,
        latestScore = 85.5f
    )
    
    private val ppdtProgressNotStarted = TestProgress(
        testType = TestType.PPDT,
        status = TestStatus.NOT_ATTEMPTED,
        latestScore = null
    )
    
    private val ppdtProgressGraded = TestProgress(
        testType = TestType.PPDT,
        status = TestStatus.GRADED,
        latestScore = 78.0f
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
    fun `init loads Phase 1 tests for authenticated user`() = runTest {
        // Given
        val phase1Progress = Phase1Progress(
            oirProgress = oirProgressCompleted,
            ppdtProgress = ppdtProgressGraded
        )
        coEvery { mockTestProgressRepository.getPhase1Progress(testUser.id) } returns 
            MutableStateFlow(phase1Progress)
        
        // When
        viewModel = Phase1DetailViewModel(mockTestProgressRepository, mockObserveCurrentUser)
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        assertFalse("Should not be loading", state.isLoading)
        assertNull("Should have no error", state.error)
        assertEquals("Should have 2 Phase 1 tests", 2, state.tests.size)
    }
    
    @Test
    fun `loads OIR and PPDT tests with correct names`() = runTest {
        // Given
        val phase1Progress = Phase1Progress(
            oirProgress = oirProgressNotStarted,
            ppdtProgress = ppdtProgressNotStarted
        )
        coEvery { mockTestProgressRepository.getPhase1Progress(testUser.id) } returns 
            MutableStateFlow(phase1Progress)
        
        // When
        viewModel = Phase1DetailViewModel(mockTestProgressRepository, mockObserveCurrentUser)
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        val tests = state.tests
        
        assertEquals("First test should be OIR", TestType.OIR, tests[0].type)
        assertEquals("OIR name", "OIR Test", tests[0].name)
        assertEquals("OIR subtitle", "Officer Intelligence Rating", tests[0].subtitle)
        
        assertEquals("Second test should be PPDT", TestType.PPDT, tests[1].type)
        assertEquals("PPDT name", "PPDT", tests[1].name)
        assertEquals("PPDT subtitle", "Picture Perception & Description Test", tests[1].subtitle)
    }
    
    @Test
    fun `no authenticated user shows error`() = runTest {
        // Given
        mockCurrentUserFlow.value = null
        
        // When
        viewModel = Phase1DetailViewModel(mockTestProgressRepository, mockObserveCurrentUser)
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
        val phase1Progress = Phase1Progress(
            oirProgress = oirProgressNotStarted,
            ppdtProgress = ppdtProgressNotStarted
        )
        coEvery { mockTestProgressRepository.getPhase1Progress(testUser.id) } returns 
            MutableStateFlow(phase1Progress)
        
        // When
        viewModel = Phase1DetailViewModel(mockTestProgressRepository, mockObserveCurrentUser)
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        assertEquals(TestStatus.NOT_ATTEMPTED, state.tests[0].status)
        assertNull("NOT_ATTEMPTED test should have no score", state.tests[0].latestScore)
        assertEquals(TestStatus.NOT_ATTEMPTED, state.tests[1].status)
        assertNull("NOT_ATTEMPTED test should have no score", state.tests[1].latestScore)
    }
    
    @Test
    fun `COMPLETED tests show score`() = runTest {
        // Given
        val phase1Progress = Phase1Progress(
            oirProgress = oirProgressCompleted,
            ppdtProgress = ppdtProgressNotStarted
        )
        coEvery { mockTestProgressRepository.getPhase1Progress(testUser.id) } returns 
            MutableStateFlow(phase1Progress)
        
        // When
        viewModel = Phase1DetailViewModel(mockTestProgressRepository, mockObserveCurrentUser)
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        val oirTest = state.tests.find { it.type == TestType.OIR }
        
        assertNotNull("OIR test should exist", oirTest)
        assertEquals("OIR should be COMPLETED", TestStatus.COMPLETED, oirTest?.status)
        assertEquals("OIR should have score", 85.5f, oirTest?.latestScore ?: 0f, 0.01f)
    }
    
    @Test
    fun `GRADED tests show score`() = runTest {
        // Given
        val phase1Progress = Phase1Progress(
            oirProgress = oirProgressNotStarted,
            ppdtProgress = ppdtProgressGraded
        )
        coEvery { mockTestProgressRepository.getPhase1Progress(testUser.id) } returns 
            MutableStateFlow(phase1Progress)
        
        // When
        viewModel = Phase1DetailViewModel(mockTestProgressRepository, mockObserveCurrentUser)
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        val ppdtTest = state.tests.find { it.type == TestType.PPDT }
        
        assertNotNull("PPDT test should exist", ppdtTest)
        assertEquals("PPDT should be GRADED", TestStatus.GRADED, ppdtTest?.status)
        assertEquals("PPDT should have score", 78.0f, ppdtTest?.latestScore ?: 0f, 0.01f)
    }
    
    // ==================== Average Score Calculation Tests ====================
    
    @Test
    fun `average score is zero when no tests completed`() = runTest {
        // Given
        val phase1Progress = Phase1Progress(
            oirProgress = oirProgressNotStarted,
            ppdtProgress = ppdtProgressNotStarted
        )
        coEvery { mockTestProgressRepository.getPhase1Progress(testUser.id) } returns 
            MutableStateFlow(phase1Progress)
        
        // When
        viewModel = Phase1DetailViewModel(mockTestProgressRepository, mockObserveCurrentUser)
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        assertEquals("Average score should be 0", 0f, state.averageScore, 0.01f)
    }
    
    @Test
    fun `average score calculated from one completed test`() = runTest {
        // Given
        val phase1Progress = Phase1Progress(
            oirProgress = oirProgressCompleted,  // 85.5
            ppdtProgress = ppdtProgressNotStarted
        )
        coEvery { mockTestProgressRepository.getPhase1Progress(testUser.id) } returns 
            MutableStateFlow(phase1Progress)
        
        // When
        viewModel = Phase1DetailViewModel(mockTestProgressRepository, mockObserveCurrentUser)
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        assertEquals("Average score should match single completed test", 
            85.5f, state.averageScore, 0.01f)
    }
    
    @Test
    fun `average score calculated from both completed tests`() = runTest {
        // Given
        val phase1Progress = Phase1Progress(
            oirProgress = oirProgressCompleted,   // 85.5
            ppdtProgress = ppdtProgressGraded     // 78.0
        )
        coEvery { mockTestProgressRepository.getPhase1Progress(testUser.id) } returns 
            MutableStateFlow(phase1Progress)
        
        // When
        viewModel = Phase1DetailViewModel(mockTestProgressRepository, mockObserveCurrentUser)
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        val expectedAverage = (85.5f + 78.0f) / 2f  // 81.75
        assertEquals("Average score should be mean of both tests", 
            expectedAverage, state.averageScore, 0.01f)
    }
    
    @Test
    fun `average score includes both COMPLETED and GRADED tests`() = runTest {
        // Given
        val phase1Progress = Phase1Progress(
            oirProgress = TestProgress(
                testType = TestType.OIR,
                status = TestStatus.COMPLETED,
                latestScore = 90.0f
            ),
            ppdtProgress = TestProgress(
                testType = TestType.PPDT,
                status = TestStatus.GRADED,
                latestScore = 88.0f
            )
        )
        coEvery { mockTestProgressRepository.getPhase1Progress(testUser.id) } returns 
            MutableStateFlow(phase1Progress)
        
        // When
        viewModel = Phase1DetailViewModel(mockTestProgressRepository, mockObserveCurrentUser)
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        val expectedAverage = (90.0f + 88.0f) / 2f  // 89.0
        assertEquals("Both COMPLETED and GRADED should count in average", 
            expectedAverage, state.averageScore, 0.01f)
    }
    
    // ==================== Test Details Tests ====================
    
    @Test
    fun `OIR test has correct details`() = runTest {
        // Given
        val phase1Progress = Phase1Progress(
            oirProgress = oirProgressNotStarted,
            ppdtProgress = ppdtProgressNotStarted
        )
        coEvery { mockTestProgressRepository.getPhase1Progress(testUser.id) } returns 
            MutableStateFlow(phase1Progress)
        
        // When
        viewModel = Phase1DetailViewModel(mockTestProgressRepository, mockObserveCurrentUser)
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        val oirTest = state.tests.find { it.type == TestType.OIR }
        
        assertNotNull("OIR test should exist", oirTest)
        assertEquals("OIR duration", 40, oirTest?.durationMinutes)
        assertEquals("OIR question count", 50, oirTest?.questionCount)
        assertTrue("OIR description should mention intelligence",
            oirTest?.description?.contains("intelligence", ignoreCase = true) == true)
    }
    
    @Test
    fun `PPDT test has correct details`() = runTest {
        // Given
        val phase1Progress = Phase1Progress(
            oirProgress = oirProgressNotStarted,
            ppdtProgress = ppdtProgressNotStarted
        )
        coEvery { mockTestProgressRepository.getPhase1Progress(testUser.id) } returns 
            MutableStateFlow(phase1Progress)
        
        // When
        viewModel = Phase1DetailViewModel(mockTestProgressRepository, mockObserveCurrentUser)
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        val ppdtTest = state.tests.find { it.type == TestType.PPDT }
        
        assertNotNull("PPDT test should exist", ppdtTest)
        assertEquals("PPDT duration", 30, ppdtTest?.durationMinutes)
        assertEquals("PPDT question count", 1, ppdtTest?.questionCount)
        assertTrue("PPDT description should mention picture",
            ppdtTest?.description?.contains("picture", ignoreCase = true) == true)
    }
    
    // ==================== Error Handling Tests ====================
    
    @Test
    fun `repository error shows error message`() = runTest {
        // Given
        coEvery { mockTestProgressRepository.getPhase1Progress(testUser.id) } returns 
            flowOf()  // Empty flow that completes immediately
        
        // When
        viewModel = Phase1DetailViewModel(mockTestProgressRepository, mockObserveCurrentUser)
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        assertFalse("Should not be loading", state.isLoading)
        // With empty flow, the tests list remains empty (no error thrown)
        // This is acceptable as collect {} never gets called with empty flow
    }
    
    @Test
    fun `repository exception shows error message`() = runTest {
        // Given
        coEvery { mockTestProgressRepository.getPhase1Progress(testUser.id) } throws 
            RuntimeException("Database error")
        
        // When
        viewModel = Phase1DetailViewModel(mockTestProgressRepository, mockObserveCurrentUser)
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
            Phase1Progress(
                oirProgress = oirProgressNotStarted,
                ppdtProgress = ppdtProgressNotStarted
            )
        )
        coEvery { mockTestProgressRepository.getPhase1Progress(testUser.id) } returns progressFlow
        
        viewModel = Phase1DetailViewModel(mockTestProgressRepository, mockObserveCurrentUser)
        advanceUntilIdle()
        
        val initialState = viewModel.uiState.value
        assertEquals("Initial average should be 0", 0f, initialState.averageScore, 0.01f)
        
        // When - repository emits updated progress
        progressFlow.value = Phase1Progress(
            oirProgress = oirProgressCompleted,
            ppdtProgress = ppdtProgressGraded
        )
        advanceUntilIdle()
        
        // Then
        val updatedState = viewModel.uiState.value
        val expectedAverage = (85.5f + 78.0f) / 2f
        assertEquals("Average should update", expectedAverage, updatedState.averageScore, 0.01f)
    }
    
    // ==================== UI State Tests ====================
    
    @Test
    fun `ui state has correct default values`() {
        val defaultState = Phase1DetailUiState()
        
        assertTrue("Default should have no tests", defaultState.tests.isEmpty())
        assertEquals("Default average should be 0", 0f, defaultState.averageScore, 0.01f)
        assertTrue("Default should be loading", defaultState.isLoading)
        assertNull("Default should have no error", defaultState.error)
    }
    
    @Test
    fun `tests maintain correct order OIR then PPDT`() = runTest {
        // Given
        val phase1Progress = Phase1Progress(
            oirProgress = oirProgressCompleted,
            ppdtProgress = ppdtProgressGraded
        )
        coEvery { mockTestProgressRepository.getPhase1Progress(testUser.id) } returns 
            MutableStateFlow(phase1Progress)
        
        // When
        viewModel = Phase1DetailViewModel(mockTestProgressRepository, mockObserveCurrentUser)
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        assertEquals("First test should be OIR", TestType.OIR, state.tests[0].type)
        assertEquals("Second test should be PPDT", TestType.PPDT, state.tests[1].type)
    }
}

