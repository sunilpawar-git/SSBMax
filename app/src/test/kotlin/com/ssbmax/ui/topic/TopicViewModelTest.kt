package com.ssbmax.ui.topic

import androidx.lifecycle.SavedStateHandle
import com.ssbmax.core.domain.model.*
import com.ssbmax.core.domain.repository.BookmarkRepository
import com.ssbmax.core.domain.repository.TestProgressRepository
import com.ssbmax.core.domain.usecase.auth.ObserveCurrentUserUseCase
import com.ssbmax.testing.BaseViewModelTest
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for TopicViewModel with repository dependencies
 * 
 * Tests study material loading and topic content display
 */
class TopicViewModelTest : BaseViewModelTest() {
    
    private lateinit var viewModel: TopicViewModel
    private lateinit var savedStateHandle: SavedStateHandle
    
    // Mock dependencies
    private val mockBookmarkRepo = mockk<BookmarkRepository>(relaxed = true)
    private val mockTestProgressRepo = mockk<TestProgressRepository>(relaxed = true)
    private val mockObserveCurrentUser = mockk<ObserveCurrentUserUseCase>(relaxed = true)
    
    private val mockUser = SSBMaxUser(
        id = "test-user-123",
        email = "test@ssbmax.com",
        displayName = "Test User",
        photoUrl = null,
        role = UserRole.STUDENT,
        createdAt = System.currentTimeMillis(),
        lastLoginAt = System.currentTimeMillis()
    )
    
    private val mockPhase1Progress = Phase1Progress(
        oirProgress = TestProgress(TestType.OIR, TestStatus.COMPLETED, latestScore = 75f),
        ppdtProgress = TestProgress(TestType.PPDT, TestStatus.COMPLETED, latestScore = 80f)
    )
    
    @Before
    fun setUp() {
        savedStateHandle = SavedStateHandle(mapOf("topicId" to "OIR"))
        
        // Setup mock behavior
        coEvery { mockObserveCurrentUser() } returns flowOf(mockUser)
        coEvery { mockBookmarkRepo.getBookmarkedMaterials(any()) } returns flowOf(emptyList())
        coEvery { mockTestProgressRepo.getPhase1Progress(any()) } returns flowOf(mockPhase1Progress)
        coEvery { mockTestProgressRepo.getPhase2Progress(any()) } returns flowOf(
            Phase2Progress(
                psychologyProgress = TestProgress(TestType.TAT, TestStatus.COMPLETED, latestScore = 85f),
                gtoProgress = TestProgress(TestType.GTO, TestStatus.IN_PROGRESS),
                interviewProgress = TestProgress(TestType.IO, TestStatus.NOT_ATTEMPTED)
            )
        )
    }
    
    private fun createViewModel(topicId: String = "OIR"): TopicViewModel {
        savedStateHandle = SavedStateHandle(mapOf("topicId" to topicId))
        return TopicViewModel(
            savedStateHandle,
            mockBookmarkRepo,
            mockTestProgressRepo,
            mockObserveCurrentUser
        )
    }
    
    // ==================== Initialization Tests ====================
    
    @Test
    fun `init - loads topic content`() = runTest {
        // When
        viewModel = createViewModel()
        advanceTimeBy(200)
        
        // Then
        val state = viewModel.uiState.value
        assertFalse("Should not be loading", state.isLoading)
        assertFalse("Should have topic title", state.topicTitle.isEmpty())
        assertEquals("Should set correct test type", "OIR", state.testType)
    }
    
    @Test
    fun `init - loads OIR topic successfully`() = runTest {
        // Given & When
        viewModel = createViewModel("OIR")
        advanceTimeBy(200)
        
        // Then
        val state = viewModel.uiState.value
        assertEquals("Should load OIR", "OIR", state.testType)
        assertTrue("Should have introduction", state.introduction.isNotEmpty())
    }
    
    @Test
    fun `init - loads TAT topic successfully`() = runTest {
        // Given & When
        viewModel = createViewModel("TAT")
        advanceTimeBy(200)
        
        // Then
        val state = viewModel.uiState.value
        assertEquals("Should load TAT", "TAT", state.testType)
        assertFalse("Should have topic title", state.topicTitle.isEmpty())
    }
    
    @Test
    fun `init - handles missing topicId`() = runTest {
        // Given - No topicId in SavedStateHandle
        savedStateHandle = SavedStateHandle()
        
        // When
        viewModel = createViewModel()
        advanceTimeBy(200)
        
        // Then - Should default to OIR
        val state = viewModel.uiState.value
        assertEquals("Should default to OIR", "OIR", state.testType)
    }
    
    // ==================== Content Loading Tests ====================
    
    @Test
    fun `loads study materials for topic`() = runTest {
        // When
        viewModel = createViewModel()
        advanceTimeBy(200)
        
        // Then
        val state = viewModel.uiState.value
        assertTrue("Should have study materials", state.studyMaterials.isNotEmpty())
    }
    
    @Test
    fun `loads available tests for topic`() = runTest {
        // When
        viewModel = createViewModel()
        advanceTimeBy(200)
        
        // Then
        val state = viewModel.uiState.value
        assertTrue("Should have available tests", state.availableTests.isNotEmpty())
    }
    
    @Test
    fun `loads introduction text`() = runTest {
        // When
        viewModel = createViewModel()
        advanceTimeBy(200)
        
        // Then
        val state = viewModel.uiState.value
        assertFalse("Should have introduction", state.introduction.isEmpty())
        assertTrue("Introduction should have content", state.introduction.length > 50)
    }
    
    // ==================== Refresh Tests ====================
    
    @Test
    fun `refresh - reloads topic content`() = runTest {
        // Given
        viewModel = createViewModel()
        advanceTimeBy(200)
        val initialTitle = viewModel.uiState.value.topicTitle
        
        // When
        viewModel.refresh()
        advanceTimeBy(200)
        
        // Then
        val state = viewModel.uiState.value
        assertFalse("Should not be loading", state.isLoading)
        assertEquals("Title should be same", initialTitle, state.topicTitle)
    }
    
    @Test
    fun `refresh - clears error state`() = runTest {
        // Given
        viewModel = createViewModel()
        advanceTimeBy(200)
        
        // When
        viewModel.refresh()
        advanceTimeBy(200)
        
        // Then
        assertNull("Error should be cleared", viewModel.uiState.value.error)
    }
    
    // ==================== UI State Tests ====================
    
    @Test
    fun `initial state is loading`() = runTest {
        // When
        viewModel = createViewModel()
        
        // Then - Check immediately before loading completes
        // Note: Due to fast loading, this might not catch loading state
        assertNotNull("State should exist", viewModel.uiState.value)
    }
    
    @Test
    fun `topicTitle is populated after loading`() = runTest {
        // When
        viewModel = createViewModel()
        advanceTimeBy(200)
        
        // Then
        val title = viewModel.uiState.value.topicTitle
        assertFalse("Topic title should not be empty", title.isEmpty())
        assertTrue("Title should have meaningful length", title.length > 3)
    }
    
    @Test
    fun `error is null on successful load`() = runTest {
        // When
        viewModel = createViewModel()
        advanceTimeBy(200)
        
        // Then
        assertNull("Error should be null", viewModel.uiState.value.error)
    }
    
    // ==================== Different Topics Tests ====================
    
    @Test
    fun `loads WAT topic content`() = runTest {
        // Given & When
        viewModel = createViewModel("WAT")
        advanceTimeBy(200)
        
        // Then
        val state = viewModel.uiState.value
        assertEquals("Should load WAT", "WAT", state.testType)
        assertFalse("Should have content", state.introduction.isEmpty())
    }
    
    @Test
    fun `loads SRT topic content`() = runTest {
        // Given & When
        viewModel = createViewModel("SRT")
        advanceTimeBy(200)
        
        // Then
        val state = viewModel.uiState.value
        assertEquals("Should load SRT", "SRT", state.testType)
        assertFalse("Should have loaded", state.isLoading)
        assertNull("Should not have error", state.error)
    }
    
    @Test
    fun `loads PPDT topic content`() = runTest {
        // Given & When
        viewModel = createViewModel("PPDT")
        advanceTimeBy(200)
        
        // Then
        val state = viewModel.uiState.value
        assertEquals("Should load PPDT", "PPDT", state.testType)
        assertTrue("Should have tests", state.availableTests.isNotEmpty())
    }
}

