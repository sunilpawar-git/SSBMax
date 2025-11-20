package com.ssbmax.ui.home.instructor

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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*

/**
 * Tests for InstructorHomeViewModel
 * Tests grading statistics loading and authentication
 */
@OptIn(ExperimentalCoroutinesApi::class)
class InstructorHomeViewModelTest {

    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    private lateinit var viewModel: InstructorHomeViewModel
    private val mockGradingQueueRepository = mockk<GradingQueueRepository>()
    private val mockObserveCurrentUser = mockk<ObserveCurrentUserUseCase>()
    private val mockCurrentUserFlow = MutableStateFlow<SSBMaxUser?>(null)

    private val mockInstructor = SSBMaxUser(
        id = "instructor-123",
        email = "instructor@example.com",
        displayName = "Test Instructor",
        photoUrl = null,
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
        // Mock Android Log
        mockkStatic(android.util.Log::class)
        every { android.util.Log.d(any(), any()) } returns 0
        every { android.util.Log.e(any(), any()) } returns 0
        every { android.util.Log.e(any(), any(), any()) } returns 0

        // Mock current user flow
        every { mockObserveCurrentUser() } returns mockCurrentUserFlow
    }

    @Test
    fun `viewModel initializes correctly with authenticated user`() = runTest {
        // Given
        mockCurrentUserFlow.value = mockInstructor
        every { mockGradingQueueRepository.observeGradingStats("instructor-123") } returns
            flowOf(mockGradingStats)

        // When
        viewModel = InstructorHomeViewModel(
            mockGradingQueueRepository,
            mockObserveCurrentUser
        )

        // Wait for initialization
        kotlinx.coroutines.delay(100)

        // Then - state should have mock data loaded
        val state = viewModel.uiState.value
        assertEquals("TotalStudents should have mock data", 24, state.totalStudents)
        assertEquals("ActiveBatches should have mock data", 3, state.activeBatches)
        assertNull("Error should be null", state.error)
    }

    @Test
    fun `loadInstructorData with authenticated user loads grading stats`() = runTest {
        // Given
        mockCurrentUserFlow.value = mockInstructor
        every { mockGradingQueueRepository.observeGradingStats("instructor-123") } returns
            flowOf(mockGradingStats)

        // When
        viewModel = InstructorHomeViewModel(
            mockGradingQueueRepository,
            mockObserveCurrentUser
        )

        // Wait for async initialization
        kotlinx.coroutines.delay(200)

        // Then
        val state = viewModel.uiState.value
        assertFalse("Loading should be false after completion", state.isLoading)
        assertEquals("Pending grading count should match", 15, state.pendingGradingCount)
        assertEquals("Tests graded today should match", 8, state.testsGradedToday)
        assertEquals("Average response time should be converted to hours", 2, state.avgResponseTime)
        assertNull("Error should be null on success", state.error)

        // Mock data should still be present
        assertEquals("Total students should be mock data", 24, state.totalStudents)
        assertEquals("Active batches should be mock data", 3, state.activeBatches)
        assertEquals("Students list should have 4 mock entries", 4, state.students.size)
        assertEquals("Batches list should have 3 mock entries", 3, state.batches.size)
    }

    @Test
    fun `loadInstructorData without authenticated user shows error`() = runTest {
        // Given - no authenticated user
        mockCurrentUserFlow.value = null

        // When
        viewModel = InstructorHomeViewModel(
            mockGradingQueueRepository,
            mockObserveCurrentUser
        )

        // Wait for async initialization
        kotlinx.coroutines.delay(100)

        // Then
        val state = viewModel.uiState.value
        assertFalse("Loading should be false", state.isLoading)
        assertNotNull("Error should not be null", state.error)
        assertTrue("Error should mention login", state.error?.contains("logged in") == true)
    }

    @Test
    fun `grading stats updates are reflected in state`() = runTest {
        // Given
        mockCurrentUserFlow.value = mockInstructor
        val statsFlow = MutableStateFlow(mockGradingStats)
        every { mockGradingQueueRepository.observeGradingStats("instructor-123") } returns statsFlow

        // When
        viewModel = InstructorHomeViewModel(
            mockGradingQueueRepository,
            mockObserveCurrentUser
        )

        // Wait for initial load
        kotlinx.coroutines.delay(200)

        val initialState = viewModel.uiState.value
        assertEquals("Initial pending count should be 15", 15, initialState.pendingGradingCount)

        // When - update stats
        val updatedStats = mockGradingStats.copy(
            totalPending = 20,
            todayGraded = 10,
            averageGradingTimeMinutes = 180 // 3 hours
        )
        statsFlow.value = updatedStats

        // Wait for update
        kotlinx.coroutines.delay(100)

        // Then
        val updatedState = viewModel.uiState.value
        assertEquals("Updated pending count should be 20", 20, updatedState.pendingGradingCount)
        assertEquals("Updated today graded should be 10", 10, updatedState.testsGradedToday)
        assertEquals("Updated avg response time should be 3 hours", 3, updatedState.avgResponseTime)
    }

    @Test
    fun `refreshData reloads instructor data`() = runTest {
        // Given
        mockCurrentUserFlow.value = mockInstructor
        val statsFlow = MutableStateFlow(mockGradingStats)
        every { mockGradingQueueRepository.observeGradingStats("instructor-123") } returns statsFlow

        viewModel = InstructorHomeViewModel(
            mockGradingQueueRepository,
            mockObserveCurrentUser
        )

        // Wait for initial load
        kotlinx.coroutines.delay(200)

        // Update stats
        val newStats = mockGradingStats.copy(totalPending = 25)
        statsFlow.value = newStats

        // When - refresh
        viewModel.refreshData()

        // Wait for refresh
        kotlinx.coroutines.delay(200)

        // Then - new stats should be loaded
        val state = viewModel.uiState.value
        assertEquals("Pending count should reflect latest stats", 25, state.pendingGradingCount)
    }

    @Test
    fun `mock student data is loaded correctly`() = runTest {
        // Given
        mockCurrentUserFlow.value = mockInstructor
        every { mockGradingQueueRepository.observeGradingStats("instructor-123") } returns
            flowOf(mockGradingStats)

        // When
        viewModel = InstructorHomeViewModel(
            mockGradingQueueRepository,
            mockObserveCurrentUser
        )

        // Wait for initialization
        kotlinx.coroutines.delay(200)

        // Then - verify mock student data
        val state = viewModel.uiState.value
        assertEquals("Should have 4 mock students", 4, state.students.size)

        val rahul = state.students.find { it.studentName == "Rahul Sharma" }
        assertNotNull("Rahul should be in the list", rahul)
        assertEquals("Rahul's average score should be 78.5", 78.5f, rahul?.averageScore ?: 0f, 0.01f)
        assertEquals("Rahul's tests completed should be 8", 8, rahul?.testsCompleted)

        val sneha = state.students.find { it.studentName == "Sneha Singh" }
        assertNotNull("Sneha should be in the list", sneha)
        assertEquals("Sneha's average score should be 91.0", 91.0f, sneha?.averageScore ?: 0f, 0.01f)
    }

    @Test
    fun `mock batch data is loaded correctly`() = runTest {
        // Given
        mockCurrentUserFlow.value = mockInstructor
        every { mockGradingQueueRepository.observeGradingStats("instructor-123") } returns
            flowOf(mockGradingStats)

        // When
        viewModel = InstructorHomeViewModel(
            mockGradingQueueRepository,
            mockObserveCurrentUser
        )

        // Wait for initialization
        kotlinx.coroutines.delay(200)

        // Then - verify mock batch data
        val state = viewModel.uiState.value
        assertEquals("Should have 3 mock batches", 3, state.batches.size)

        val ndaBatch = state.batches.find { it.name == "NDA Batch 2024" }
        assertNotNull("NDA batch should be in the list", ndaBatch)
        assertEquals("NDA batch invite code should be NDA2024", "NDA2024", ndaBatch?.inviteCode)
        assertEquals("NDA batch student count should be 15", 15, ndaBatch?.studentCount)

        val afcatBatch = state.batches.find { it.name == "AFCAT Group" }
        assertNotNull("AFCAT batch should be in the list", afcatBatch)
        assertEquals("AFCAT batch student count should be 6", 6, afcatBatch?.studentCount)
    }

    @Test
    fun `averageGradingTimeMinutes is correctly converted to hours`() = runTest {
        // Given
        mockCurrentUserFlow.value = mockInstructor
        val statsWithDifferentTime = mockGradingStats.copy(averageGradingTimeMinutes = 90) // 1.5 hours
        every { mockGradingQueueRepository.observeGradingStats("instructor-123") } returns
            flowOf(statsWithDifferentTime)

        // When
        viewModel = InstructorHomeViewModel(
            mockGradingQueueRepository,
            mockObserveCurrentUser
        )

        // Wait for initialization
        kotlinx.coroutines.delay(200)

        // Then
        val state = viewModel.uiState.value
        assertEquals("90 minutes should convert to 1 hour (integer division)", 1, state.avgResponseTime)
    }

    @Test
    fun `zero averageGradingTimeMinutes converts to 0 hours`() = runTest {
        // Given
        mockCurrentUserFlow.value = mockInstructor
        val statsWithZeroTime = mockGradingStats.copy(averageGradingTimeMinutes = 0)
        every { mockGradingQueueRepository.observeGradingStats("instructor-123") } returns
            flowOf(statsWithZeroTime)

        // When
        viewModel = InstructorHomeViewModel(
            mockGradingQueueRepository,
            mockObserveCurrentUser
        )

        // Wait for initialization
        kotlinx.coroutines.delay(200)

        // Then
        val state = viewModel.uiState.value
        assertEquals("0 minutes should convert to 0 hours", 0, state.avgResponseTime)
    }
}
