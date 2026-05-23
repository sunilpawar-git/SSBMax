package com.ssbmax.ui.home.instructor

import android.util.Log
import com.ssbmax.core.domain.model.Batch
import com.ssbmax.core.domain.model.InstructorGradingStats
import com.ssbmax.core.domain.model.SSBMaxUser
import com.ssbmax.core.domain.model.TestType
import com.ssbmax.core.domain.model.UserRole
import com.ssbmax.core.domain.repository.GradingQueueRepository
import com.ssbmax.core.domain.usecase.auth.ObserveCurrentUserUseCase
import com.ssbmax.testing.TestDispatcherRule
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class InstructorHomeViewModelTest {

    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    private lateinit var viewModel: InstructorHomeViewModel
    private val mockGradingQueueRepository = mockk<GradingQueueRepository>()
    private val mockObserveCurrentUser = mockk<ObserveCurrentUserUseCase>()
    private val mockCurrentUserFlow = MutableStateFlow<SSBMaxUser?>(null)
    private val mockBatchRepository = mockk<com.ssbmax.core.domain.repository.BatchRepository>()

    private val mockInstructor = SSBMaxUser(
        id = "instructor-123",
        email = "instructor@example.com",
        displayName = "Test Instructor",
        role = UserRole.INSTRUCTOR,
        subscriptionTier = com.ssbmax.core.domain.model.SubscriptionTier.FREE,
        subscription = null,
        studentProfile = null,
        instructorProfile = null,
        createdAt = System.currentTimeMillis(),
        lastLoginAt = System.currentTimeMillis()
    )

    private val mockGradingStats = InstructorGradingStats(
        totalPending = 15,
        totalGraded = 50,
        averageGradingTimeMinutes = 120, // 2 hours
        todayGraded = 8,
        weekGraded = 30,
        pendingByTestType = mapOf(
            TestType.TAT to 5,
            TestType.WAT to 4,
            TestType.SRT to 6
        ),
        averageScoreGiven = 76.5f
    )

    @Before
    fun setup() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0

        every { mockObserveCurrentUser() } returns mockCurrentUserFlow

        // Stub the BatchRepository to return a mock list of batches and students
        val testBatch = Batch(
            id = "batch_1",
            name = "NDA Batch 2024",
            inviteCode = "NDA2024",
            studentIds = listOf("student_1", "student_2"),
            instructorId = "instructor-123"
        )
        
        val testStudent = com.ssbmax.core.domain.model.StudentPerformance(
            studentId = "student_1",
            studentName = "Rahul Sharma",
            averageScore = 78.5f,
            testsCompleted = 8,
            lastActiveAt = System.currentTimeMillis(),
            currentStreak = 5,
            phase1Score = 82f,
            phase2Score = 75f
        )

        every { mockBatchRepository.getBatchesForInstructor(any()) } returns flowOf(Result.success(emptyList()))
        every { mockBatchRepository.getBatchesForInstructor("instructor-123") } returns flowOf(Result.success(listOf(testBatch)))
        every { mockBatchRepository.getStudentsInBatch(any()) } returns flowOf(Result.success(emptyList()))
        every { mockBatchRepository.getStudentsInBatch("batch_1") } returns flowOf(Result.success(listOf(testStudent)))
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    @Test
    fun `viewModel initializes correctly with authenticated user`() = runTest {
        mockCurrentUserFlow.value = mockInstructor
        every { mockGradingQueueRepository.observeGradingStats("instructor-123") } returns
            flowOf(mockGradingStats)

        viewModel = InstructorHomeViewModel(
            mockGradingQueueRepository,
            mockObserveCurrentUser,
            mockBatchRepository
        )

        kotlinx.coroutines.delay(100)

        val state = viewModel.uiState.value
        assertEquals("TotalStudents should reflect repository data", 1, state.totalStudents)
        assertEquals("ActiveBatches should reflect repository data", 1, state.activeBatches)
        assertNull("Error should be null", state.error)
    }

    @Test
    fun `loadInstructorData with authenticated user loads grading stats`() = runTest {
        mockCurrentUserFlow.value = mockInstructor
        every { mockGradingQueueRepository.observeGradingStats("instructor-123") } returns
            flowOf(mockGradingStats)

        viewModel = InstructorHomeViewModel(
            mockGradingQueueRepository,
            mockObserveCurrentUser,
            mockBatchRepository
        )

        kotlinx.coroutines.delay(200)

        val state = viewModel.uiState.value
        assertFalse("Loading should be false after completion", state.isLoading)
        assertEquals("Pending grading count should match", 15, state.pendingGradingCount)
        assertEquals("Tests graded today should match", 8, state.testsGradedToday)
        assertEquals("Average response time should be converted to hours", 2, state.avgResponseTime)
        assertNull("Error should be null on success", state.error)

        assertEquals("Total students should reflect repository data", 1, state.totalStudents)
        assertEquals("Active batches should reflect repository data", 1, state.activeBatches)
        assertEquals("Students list should have 1 entry", 1, state.students.size)
        assertEquals("Batches list should have 1 entry", 1, state.batches.size)
    }

    @Test
    fun `loadInstructorData without authenticated user shows error`() = runTest {
        mockCurrentUserFlow.value = null

        viewModel = InstructorHomeViewModel(
            mockGradingQueueRepository,
            mockObserveCurrentUser,
            mockBatchRepository
        )

        kotlinx.coroutines.delay(100)

        val state = viewModel.uiState.value
        assertFalse("Loading should be false", state.isLoading)
        assertNotNull("Error should not be null", state.error)
        assertTrue("Error should mention login", state.error?.contains("logged in") == true)
    }

    @Test
    fun `grading stats updates are reflected in state`() = runTest {
        mockCurrentUserFlow.value = mockInstructor
        val statsFlow = MutableStateFlow(mockGradingStats)
        every { mockGradingQueueRepository.observeGradingStats("instructor-123") } returns statsFlow

        viewModel = InstructorHomeViewModel(
            mockGradingQueueRepository,
            mockObserveCurrentUser,
            mockBatchRepository
        )

        kotlinx.coroutines.delay(200)

        val initialState = viewModel.uiState.value
        assertEquals("Initial pending count should be 15", 15, initialState.pendingGradingCount)

        val updatedStats = mockGradingStats.copy(
            totalPending = 20,
            todayGraded = 10,
            averageGradingTimeMinutes = 180 // 3 hours
        )
        statsFlow.value = updatedStats

        kotlinx.coroutines.delay(100)

        val updatedState = viewModel.uiState.value
        assertEquals("Updated pending count should be 20", 20, updatedState.pendingGradingCount)
        assertEquals("Updated today graded should be 10", 10, updatedState.testsGradedToday)
        assertEquals("Updated avg response time should be 3 hours", 3, updatedState.avgResponseTime)
    }

    @Test
    fun `refreshData reloads instructor data`() = runTest {
        mockCurrentUserFlow.value = mockInstructor
        val statsFlow = MutableStateFlow(mockGradingStats)
        every { mockGradingQueueRepository.observeGradingStats("instructor-123") } returns statsFlow

        viewModel = InstructorHomeViewModel(
            mockGradingQueueRepository,
            mockObserveCurrentUser,
            mockBatchRepository
        )

        kotlinx.coroutines.delay(200)

        val newStats = mockGradingStats.copy(totalPending = 25)
        statsFlow.value = newStats

        viewModel.refreshData()

        kotlinx.coroutines.delay(200)

        val state = viewModel.uiState.value
        assertEquals("Pending count should reflect latest stats", 25, state.pendingGradingCount)
    }

    @Test
    fun `mock student data is loaded correctly`() = runTest {
        mockCurrentUserFlow.value = mockInstructor
        every { mockGradingQueueRepository.observeGradingStats("instructor-123") } returns
            flowOf(mockGradingStats)

        viewModel = InstructorHomeViewModel(
            mockGradingQueueRepository,
            mockObserveCurrentUser,
            mockBatchRepository
        )

        kotlinx.coroutines.delay(200)

        val state = viewModel.uiState.value
        assertEquals("Should have 1 student", 1, state.students.size)

        val rahul = state.students.find { it.studentName == "Rahul Sharma" }
        assertNotNull("Rahul should be in the list", rahul)
        assertEquals("Rahul's average score should be 78.5", 78.5f, rahul?.averageScore ?: 0f, 0.01f)
        assertEquals("Rahul's tests completed should be 8", 8, rahul?.testsCompleted)
    }

    @Test
    fun `mock batch data is loaded correctly`() = runTest {
        mockCurrentUserFlow.value = mockInstructor
        every { mockGradingQueueRepository.observeGradingStats("instructor-123") } returns
            flowOf(mockGradingStats)

        viewModel = InstructorHomeViewModel(
            mockGradingQueueRepository,
            mockObserveCurrentUser,
            mockBatchRepository
        )

        kotlinx.coroutines.delay(200)

        val state = viewModel.uiState.value
        assertEquals("Should have 1 batch", 1, state.batches.size)

        val ndaBatch = state.batches.find { it.name == "NDA Batch 2024" }
        assertNotNull("NDA batch should be in the list", ndaBatch)
        assertEquals("NDA batch invite code should be NDA2024", "NDA2024", ndaBatch?.inviteCode)
        assertEquals("NDA batch student count should be 2", 2, ndaBatch?.studentCount)
    }

    @Test
    fun `averageGradingTimeMinutes is correctly converted to hours`() = runTest {
        mockCurrentUserFlow.value = mockInstructor
        val statsWithDifferentTime = mockGradingStats.copy(averageGradingTimeMinutes = 90) // 1.5 hours
        every { mockGradingQueueRepository.observeGradingStats("instructor-123") } returns
            flowOf(statsWithDifferentTime)

        viewModel = InstructorHomeViewModel(
            mockGradingQueueRepository,
            mockObserveCurrentUser,
            mockBatchRepository
        )

        kotlinx.coroutines.delay(200)

        val state = viewModel.uiState.value
        assertEquals("90 minutes should convert to 1 hour (integer division)", 1, state.avgResponseTime)
    }

    @Test
    fun `zero averageGradingTimeMinutes converts to 0 hours`() = runTest {
        mockCurrentUserFlow.value = mockInstructor
        val statsWithZeroTime = mockGradingStats.copy(averageGradingTimeMinutes = 0)
        every { mockGradingQueueRepository.observeGradingStats("instructor-123") } returns
            flowOf(statsWithZeroTime)

        viewModel = InstructorHomeViewModel(
            mockGradingQueueRepository,
            mockObserveCurrentUser,
            mockBatchRepository
        )

        kotlinx.coroutines.delay(200)

        val state = viewModel.uiState.value
        assertEquals("0 minutes should convert to 0 hours", 0, state.avgResponseTime)
    }
}
