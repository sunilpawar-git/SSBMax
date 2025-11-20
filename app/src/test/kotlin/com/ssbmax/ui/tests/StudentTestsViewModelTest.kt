package com.ssbmax.ui.tests

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import com.ssbmax.core.domain.model.*
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
 * Comprehensive tests for StudentTestsViewModel
 * Tests test overview display with Phase 1 and Phase 2 tests
 */
@OptIn(ExperimentalCoroutinesApi::class)
class StudentTestsViewModelTest : BaseViewModelTest() {
    
    private lateinit var viewModel: StudentTestsViewModel
    private lateinit var mockTestProgressRepository: TestProgressRepository
    private lateinit var mockObserveCurrentUser: ObserveCurrentUserUseCase
    
    private lateinit var mockCurrentUserFlow: MutableStateFlow<SSBMaxUser?>
    
    private val testUser = SSBMaxUser(
        id = "test-user-123",
        email = "test@student.com",
        displayName = "Test Student",
        role = UserRole.STUDENT
    )
    
    private val phase1ProgressEmpty = Phase1Progress(
        oirProgress = TestProgress(TestType.OIR, TestStatus.NOT_ATTEMPTED),
        ppdtProgress = TestProgress(TestType.PPDT, TestStatus.NOT_ATTEMPTED)
    )
    
    private val phase1ProgressWithScores = Phase1Progress(
        oirProgress = TestProgress(TestType.OIR, TestStatus.COMPLETED, latestScore = 85.0f),
        ppdtProgress = TestProgress(TestType.PPDT, TestStatus.GRADED, latestScore = 78.0f)
    )
    
    private val phase2ProgressEmpty = Phase2Progress(
        psychologyProgress = TestProgress(TestType.TAT, TestStatus.NOT_ATTEMPTED),
        gtoProgress = TestProgress(TestType.GTO_GD, TestStatus.NOT_ATTEMPTED),
        interviewProgress = TestProgress(TestType.IO, TestStatus.NOT_ATTEMPTED)
    )
    
    private val phase2ProgressWithScores = Phase2Progress(
        psychologyProgress = TestProgress(TestType.TAT, TestStatus.COMPLETED, latestScore = 82.0f),
        gtoProgress = TestProgress(TestType.GTO_GD, TestStatus.GRADED, latestScore = 88.5f),
        interviewProgress = TestProgress(TestType.IO, TestStatus.GRADED, latestScore = 90.0f)
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
    fun `init loads all tests for authenticated user`() = runTest {
        // Given
        coEvery { mockTestProgressRepository.getPhase1Progress(testUser.id) } returns 
            flowOf(phase1ProgressEmpty)
        coEvery { mockTestProgressRepository.getPhase2Progress(testUser.id) } returns 
            flowOf(phase2ProgressEmpty)
        
        // When
        viewModel = StudentTestsViewModel(mockTestProgressRepository, mockObserveCurrentUser)
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        assertFalse("Should not be loading", state.isLoading)
        assertNull("Should have no error", state.error)
        assertTrue("Should have phase1 tests", state.phase1Tests.isNotEmpty())
        assertTrue("Should have phase2 tests", state.phase2Tests.isNotEmpty())
    }
    
    @Test
    fun `initial state shows loading`() {
        val defaultState = StudentTestsUiState()
        
        assertTrue("Should be loading initially", defaultState.isLoading)
        assertTrue("Should have no phase1 tests", defaultState.phase1Tests.isEmpty())
        assertTrue("Should have no phase2 tests", defaultState.phase2Tests.isEmpty())
        assertNull("Should have no error", defaultState.error)
    }
    
    @Test
    fun `no authenticated user shows error`() = runTest {
        // Given
        mockCurrentUserFlow.value = null
        
        // When
        viewModel = StudentTestsViewModel(mockTestProgressRepository, mockObserveCurrentUser)
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        assertFalse("Should not be loading", state.isLoading)
        assertNotNull("Should have error", state.error)
        assertTrue("Error should mention login", 
            state.error?.contains("login", ignoreCase = true) == true)
    }
    
    // ==================== Phase 1 Tests ====================
    
    @Test
    fun `loads Phase 1 tests with correct count`() = runTest {
        // Given
        coEvery { mockTestProgressRepository.getPhase1Progress(testUser.id) } returns 
            flowOf(phase1ProgressEmpty)
        coEvery { mockTestProgressRepository.getPhase2Progress(testUser.id) } returns 
            flowOf(phase2ProgressEmpty)
        
        // When
        viewModel = StudentTestsViewModel(mockTestProgressRepository, mockObserveCurrentUser)
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        assertEquals("Should have 2 Phase 1 tests", 2, state.phase1Tests.size)
    }
    
    @Test
    fun `Phase 1 tests have OIR and PPDT`() = runTest {
        // Given
        coEvery { mockTestProgressRepository.getPhase1Progress(testUser.id) } returns 
            flowOf(phase1ProgressEmpty)
        coEvery { mockTestProgressRepository.getPhase2Progress(testUser.id) } returns 
            flowOf(phase2ProgressEmpty)
        
        // When
        viewModel = StudentTestsViewModel(mockTestProgressRepository, mockObserveCurrentUser)
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        val testTypes = state.phase1Tests.map { it.type }
        
        assertTrue("Should have OIR test", testTypes.contains(TestType.OIR))
        assertTrue("Should have PPDT test", testTypes.contains(TestType.PPDT))
    }
    
    @Test
    fun `Phase 1 OIR test has correct details`() = runTest {
        // Given
        coEvery { mockTestProgressRepository.getPhase1Progress(testUser.id) } returns 
            flowOf(phase1ProgressEmpty)
        coEvery { mockTestProgressRepository.getPhase2Progress(testUser.id) } returns 
            flowOf(phase2ProgressEmpty)
        
        // When
        viewModel = StudentTestsViewModel(mockTestProgressRepository, mockObserveCurrentUser)
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        val oirTest = state.phase1Tests.find { it.type == TestType.OIR }
        
        assertNotNull("OIR test should exist", oirTest)
        assertEquals("OIR name", "OIR Test", oirTest?.name)
        assertEquals("OIR category", "Screening", oirTest?.category)
        assertEquals("OIR duration", 40, oirTest?.durationMinutes)
        assertEquals("OIR question count", 50, oirTest?.questionCount)
    }
    
    @Test
    fun `Phase 1 tests display scores when completed`() = runTest {
        // Given
        coEvery { mockTestProgressRepository.getPhase1Progress(testUser.id) } returns 
            flowOf(phase1ProgressWithScores)
        coEvery { mockTestProgressRepository.getPhase2Progress(testUser.id) } returns 
            flowOf(phase2ProgressEmpty)
        
        // When
        viewModel = StudentTestsViewModel(mockTestProgressRepository, mockObserveCurrentUser)
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        val oirTest = state.phase1Tests.find { it.type == TestType.OIR }
        val ppdtTest = state.phase1Tests.find { it.type == TestType.PPDT }
        
        assertEquals("OIR should have score", 85.0f, oirTest?.latestScore ?: 0f, 0.01f)
        assertEquals("OIR should be COMPLETED", TestStatus.COMPLETED, oirTest?.status)
        
        assertEquals("PPDT should have score", 78.0f, ppdtTest?.latestScore ?: 0f, 0.01f)
        assertEquals("PPDT should be GRADED", TestStatus.GRADED, ppdtTest?.status)
    }
    
    // ==================== Phase 2 Tests ====================
    
    @Test
    fun `loads Phase 2 tests with correct count`() = runTest {
        // Given
        coEvery { mockTestProgressRepository.getPhase1Progress(testUser.id) } returns 
            flowOf(phase1ProgressEmpty)
        coEvery { mockTestProgressRepository.getPhase2Progress(testUser.id) } returns 
            flowOf(phase2ProgressEmpty)
        
        // When
        viewModel = StudentTestsViewModel(mockTestProgressRepository, mockObserveCurrentUser)
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        // 4 Psychology + 8 GTO + 1 Interview = 13 tests
        assertEquals("Should have 13 Phase 2 tests", 13, state.phase2Tests.size)
    }
    
    @Test
    fun `Phase 2 includes all psychology tests`() = runTest {
        // Given
        coEvery { mockTestProgressRepository.getPhase1Progress(testUser.id) } returns 
            flowOf(phase1ProgressEmpty)
        coEvery { mockTestProgressRepository.getPhase2Progress(testUser.id) } returns 
            flowOf(phase2ProgressEmpty)
        
        // When
        viewModel = StudentTestsViewModel(mockTestProgressRepository, mockObserveCurrentUser)
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        val psychologyTests = state.phase2Tests.filter { it.category == "Psychology" }
        
        assertEquals("Should have 4 psychology tests", 4, psychologyTests.size)
        
        val testTypes = psychologyTests.map { it.type }
        assertTrue("Should have TAT", testTypes.contains(TestType.TAT))
        assertTrue("Should have WAT", testTypes.contains(TestType.WAT))
        assertTrue("Should have SRT", testTypes.contains(TestType.SRT))
        assertTrue("Should have SD", testTypes.contains(TestType.SD))
    }
    
    @Test
    fun `Phase 2 includes all 8 GTO tests`() = runTest {
        // Given
        coEvery { mockTestProgressRepository.getPhase1Progress(testUser.id) } returns 
            flowOf(phase1ProgressEmpty)
        coEvery { mockTestProgressRepository.getPhase2Progress(testUser.id) } returns 
            flowOf(phase2ProgressEmpty)
        
        // When
        viewModel = StudentTestsViewModel(mockTestProgressRepository, mockObserveCurrentUser)
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        val gtoTests = state.phase2Tests.filter { it.category == "GTO" }
        
        assertEquals("Should have 8 GTO tests", 8, gtoTests.size)
        
        val testTypes = gtoTests.map { it.type }
        assertTrue("Should have Group Discussion", testTypes.contains(TestType.GTO_GD))
        assertTrue("Should have Group Planning Exercise", testTypes.contains(TestType.GTO_GPE))
        assertTrue("Should have Progressive Group Task", testTypes.contains(TestType.GTO_PGT))
        assertTrue("Should have Group Obstacle Race", testTypes.contains(TestType.GTO_GOR))
        assertTrue("Should have Half Group Task", testTypes.contains(TestType.GTO_HGT))
        assertTrue("Should have Lecturette", testTypes.contains(TestType.GTO_LECTURETTE))
        assertTrue("Should have Individual Obstacles", testTypes.contains(TestType.GTO_IO))
        assertTrue("Should have Command Task", testTypes.contains(TestType.GTO_CT))
    }
    
    @Test
    fun `Phase 2 includes Personal Interview`() = runTest {
        // Given
        coEvery { mockTestProgressRepository.getPhase1Progress(testUser.id) } returns 
            flowOf(phase1ProgressEmpty)
        coEvery { mockTestProgressRepository.getPhase2Progress(testUser.id) } returns 
            flowOf(phase2ProgressEmpty)
        
        // When
        viewModel = StudentTestsViewModel(mockTestProgressRepository, mockObserveCurrentUser)
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        val interviewTests = state.phase2Tests.filter { it.category == "Interview" }
        
        assertEquals("Should have 1 interview test", 1, interviewTests.size)
        assertEquals("Should be Personal Interview", TestType.IO, interviewTests[0].type)
    }
    
    @Test
    fun `psychology tests share same progress status`() = runTest {
        // Given
        coEvery { mockTestProgressRepository.getPhase1Progress(testUser.id) } returns 
            flowOf(phase1ProgressEmpty)
        coEvery { mockTestProgressRepository.getPhase2Progress(testUser.id) } returns 
            flowOf(phase2ProgressWithScores)
        
        // When
        viewModel = StudentTestsViewModel(mockTestProgressRepository, mockObserveCurrentUser)
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        val psychologyTests = state.phase2Tests.filter { it.category == "Psychology" }
        
        // All psychology tests should have same status and score (shared progress in domain)
        psychologyTests.forEach { test ->
            assertEquals("Psychology test should be COMPLETED", 
                TestStatus.COMPLETED, test.status)
            assertEquals("Psychology test should have score", 
                82.0f, test.latestScore ?: 0f, 0.01f)
        }
    }
    
    @Test
    fun `GTO tests share same progress status`() = runTest {
        // Given
        coEvery { mockTestProgressRepository.getPhase1Progress(testUser.id) } returns 
            flowOf(phase1ProgressEmpty)
        coEvery { mockTestProgressRepository.getPhase2Progress(testUser.id) } returns 
            flowOf(phase2ProgressWithScores)
        
        // When
        viewModel = StudentTestsViewModel(mockTestProgressRepository, mockObserveCurrentUser)
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        val gtoTests = state.phase2Tests.filter { it.category == "GTO" }
        
        // All GTO tests should have same status and score (shared progress in domain)
        gtoTests.forEach { test ->
            assertEquals("GTO test should be GRADED", TestStatus.GRADED, test.status)
            assertEquals("GTO test should have score", 88.5f, test.latestScore ?: 0f, 0.01f)
        }
    }
    
    // ==================== Test Categories Tests ====================
    
    @Test
    fun `categorizes tests correctly`() = runTest {
        // Given
        coEvery { mockTestProgressRepository.getPhase1Progress(testUser.id) } returns 
            flowOf(phase1ProgressEmpty)
        coEvery { mockTestProgressRepository.getPhase2Progress(testUser.id) } returns 
            flowOf(phase2ProgressEmpty)
        
        // When
        viewModel = StudentTestsViewModel(mockTestProgressRepository, mockObserveCurrentUser)
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        val allTests = state.phase1Tests + state.phase2Tests
        
        val categories = allTests.map { it.category }.distinct()
        assertTrue("Should have Screening category", categories.contains("Screening"))
        assertTrue("Should have Psychology category", categories.contains("Psychology"))
        assertTrue("Should have GTO category", categories.contains("GTO"))
        assertTrue("Should have Interview category", categories.contains("Interview"))
    }
    
    // ==================== Error Handling Tests ====================
    
    @Test
    fun `handles repository exception gracefully`() = runTest {
        // Given
        coEvery { mockTestProgressRepository.getPhase1Progress(testUser.id) } throws 
            RuntimeException("Database error")
        
        // When
        viewModel = StudentTestsViewModel(mockTestProgressRepository, mockObserveCurrentUser)
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        assertFalse("Should not be loading", state.isLoading)
        assertNotNull("Should have error", state.error)
        assertTrue("Error should mention database",
            state.error?.contains("Database error") == true)
    }
    
    // ==================== Total Test Count Verification ====================
    
    @Test
    fun `total test count is 15`() = runTest {
        // Given
        coEvery { mockTestProgressRepository.getPhase1Progress(testUser.id) } returns 
            flowOf(phase1ProgressEmpty)
        coEvery { mockTestProgressRepository.getPhase2Progress(testUser.id) } returns 
            flowOf(phase2ProgressEmpty)
        
        // When
        viewModel = StudentTestsViewModel(mockTestProgressRepository, mockObserveCurrentUser)
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        val totalTests = state.phase1Tests.size + state.phase2Tests.size
        
        // 2 Phase1 (OIR, PPDT) + 13 Phase2 (4 Psych + 8 GTO + 1 Interview) = 15 total
        assertEquals("Should have 15 total tests", 15, totalTests)
    }
    
    @Test
    fun `all tests have required fields populated`() = runTest {
        // Given
        coEvery { mockTestProgressRepository.getPhase1Progress(testUser.id) } returns 
            flowOf(phase1ProgressEmpty)
        coEvery { mockTestProgressRepository.getPhase2Progress(testUser.id) } returns 
            flowOf(phase2ProgressEmpty)
        
        // When
        viewModel = StudentTestsViewModel(mockTestProgressRepository, mockObserveCurrentUser)
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        val allTests = state.phase1Tests + state.phase2Tests
        
        allTests.forEach { test ->
            assertFalse("Test name should not be blank", test.name.isBlank())
            assertFalse("Test category should not be blank", test.category.isBlank())
            assertTrue("Duration should be positive", test.durationMinutes > 0)
            assertTrue("Question count should be positive", test.questionCount > 0)
        }
    }
}

