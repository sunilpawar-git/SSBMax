package com.ssbmax.ui.profile

import com.ssbmax.testing.BaseViewModelTest
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for StudentProfileViewModel
 * 
 * Tests profile loading and statistics display
 */
class StudentProfileViewModelTest : BaseViewModelTest() {
    
    private lateinit var viewModel: StudentProfileViewModel
    
    @Before
    fun setUp() {
        // ViewModel auto-loads mock data in init
    }
    
    // ==================== Initialization Tests ====================
    
    @Test
    fun `init - loads user profile`() = runTest {
        // When
        viewModel = StudentProfileViewModel()
        advanceTimeBy(100)
        
        // Then
        val state = viewModel.uiState.value
        assertFalse("Should not be loading", state.isLoading)
        assertFalse("Should have user name", state.userName.isEmpty())
    }
    
    @Test
    fun `init - loads user statistics`() = runTest {
        // When
        viewModel = StudentProfileViewModel()
        advanceTimeBy(100)
        
        // Then
        val state = viewModel.uiState.value
        assertTrue("Should have tests attempted", state.totalTestsAttempted > 0)
        assertTrue("Should have study hours", state.totalStudyHours > 0)
    }
    
    @Test
    fun `init - loads recent tests`() = runTest {
        // When
        viewModel = StudentProfileViewModel()
        advanceTimeBy(100)
        
        // Then
        val state = viewModel.uiState.value
        assertTrue("Should have recent tests", state.recentTests.isNotEmpty())
    }
    
    @Test
    fun `init - loads achievements`() = runTest {
        // When
        viewModel = StudentProfileViewModel()
        advanceTimeBy(100)
        
        // Then
        val state = viewModel.uiState.value
        assertTrue("Should have achievements", state.recentAchievements.isNotEmpty())
    }
    
    // ==================== Profile Data Tests ====================
    
    @Test
    fun `displays user name`() = runTest {
        // When
        viewModel = StudentProfileViewModel()
        advanceTimeBy(100)
        
        // Then
        val userName = viewModel.uiState.value.userName
        assertFalse("User name should not be empty", userName.isEmpty())
        assertTrue("User name should have content", userName.length > 3)
    }
    
    @Test
    fun `displays user email`() = runTest {
        // When
        viewModel = StudentProfileViewModel()
        advanceTimeBy(100)
        
        // Then
        val email = viewModel.uiState.value.userEmail
        assertFalse("Email should not be empty", email.isEmpty())
        assertTrue("Email should be valid format", email.contains("@"))
    }
    
    @Test
    fun `displays premium status`() = runTest {
        // When
        viewModel = StudentProfileViewModel()
        advanceTimeBy(100)
        
        // Then
        val isPremium = viewModel.uiState.value.isPremium
        // Mock data shows non-premium user
        assertFalse("Mock user should not be premium", isPremium)
    }
    
    // ==================== Statistics Tests ====================
    
    @Test
    fun `displays total tests attempted`() = runTest {
        // When
        viewModel = StudentProfileViewModel()
        advanceTimeBy(100)
        
        // Then
        val testsAttempted = viewModel.uiState.value.totalTestsAttempted
        assertTrue("Should have positive test count", testsAttempted >= 0)
    }
    
    @Test
    fun `displays total study hours`() = runTest {
        // When
        viewModel = StudentProfileViewModel()
        advanceTimeBy(100)
        
        // Then
        val studyHours = viewModel.uiState.value.totalStudyHours
        assertTrue("Should have positive study hours", studyHours >= 0)
    }
    
    @Test
    fun `displays streak days`() = runTest {
        // When
        viewModel = StudentProfileViewModel()
        advanceTimeBy(100)
        
        // Then
        val streak = viewModel.uiState.value.streakDays
        assertTrue("Should have non-negative streak", streak >= 0)
    }
    
    @Test
    fun `displays average score`() = runTest {
        // When
        viewModel = StudentProfileViewModel()
        advanceTimeBy(100)
        
        // Then
        val avgScore = viewModel.uiState.value.averageScore
        assertTrue("Score should be non-negative", avgScore >= 0f)
        assertTrue("Score should be reasonable", avgScore <= 100f)
    }
    
    // ==================== Progress Tests ====================
    
    @Test
    fun `displays phase 1 completion`() = runTest {
        // When
        viewModel = StudentProfileViewModel()
        advanceTimeBy(100)
        
        // Then
        val phase1 = viewModel.uiState.value.phase1Completion
        assertTrue("Phase 1 completion should be 0-100", phase1 in 0..100)
    }
    
    @Test
    fun `displays phase 2 completion`() = runTest {
        // When
        viewModel = StudentProfileViewModel()
        advanceTimeBy(100)
        
        // Then
        val phase2 = viewModel.uiState.value.phase2Completion
        assertTrue("Phase 2 completion should be 0-100", phase2 in 0..100)
    }
    
    // ==================== Recent Tests Tests ====================
    
    @Test
    fun `recent tests have valid data`() = runTest {
        // When
        viewModel = StudentProfileViewModel()
        advanceTimeBy(100)
        
        // Then
        val recentTests = viewModel.uiState.value.recentTests
        assertTrue("Should have at least one test", recentTests.isNotEmpty())
        
        recentTests.forEach { test ->
            assertFalse("Test name should not be empty", test.name.isEmpty())
            assertFalse("Test date should not be empty", test.date.isEmpty())
            assertTrue("Test score should be 0-100", test.score in 0..100)
        }
    }
    
    @Test
    fun `displays multiple recent tests`() = runTest {
        // When
        viewModel = StudentProfileViewModel()
        advanceTimeBy(100)
        
        // Then
        val count = viewModel.uiState.value.recentTests.size
        assertTrue("Should have multiple tests", count > 1)
    }
    
    // ==================== Achievements Tests ====================
    
    @Test
    fun `achievements list is not empty`() = runTest {
        // When
        viewModel = StudentProfileViewModel()
        advanceTimeBy(100)
        
        // Then
        val achievements = viewModel.uiState.value.recentAchievements
        assertTrue("Should have achievements", achievements.isNotEmpty())
    }
    
    @Test
    fun `achievements have meaningful content`() = runTest {
        // When
        viewModel = StudentProfileViewModel()
        advanceTimeBy(100)
        
        // Then
        val achievements = viewModel.uiState.value.recentAchievements
        achievements.forEach { achievement ->
            assertFalse("Achievement should not be empty", achievement.isEmpty())
            assertTrue("Achievement should have content", achievement.length > 5)
        }
    }
    
    // ==================== UI State Tests ====================
    
    @Test
    fun `loading completes after initialization`() = runTest {
        // When
        viewModel = StudentProfileViewModel()
        advanceTimeBy(100)
        
        // Then
        assertFalse("Should finish loading", viewModel.uiState.value.isLoading)
    }
    
    @Test
    fun `photo URL is nullable`() = runTest {
        // When
        viewModel = StudentProfileViewModel()
        advanceTimeBy(100)
        
        // Then - Mock data has no photo URL
        assertNull("Photo URL should be null", viewModel.uiState.value.photoUrl)
    }
    
    @Test
    fun `all required fields are populated`() = runTest {
        // When
        viewModel = StudentProfileViewModel()
        advanceTimeBy(100)
        
        // Then
        val state = viewModel.uiState.value
        assertFalse("User name populated", state.userName.isEmpty())
        assertFalse("Email populated", state.userEmail.isEmpty())
        assertTrue("Tests attempted populated", state.totalTestsAttempted >= 0)
        assertTrue("Study hours populated", state.totalStudyHours >= 0)
        assertTrue("Recent tests populated", state.recentTests.isNotEmpty())
        assertTrue("Achievements populated", state.recentAchievements.isNotEmpty())
    }
}

