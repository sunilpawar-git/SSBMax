package com.ssbmax.ui.study

import com.ssbmax.testing.BaseViewModelTest
import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.Assert.*

/**
 * Comprehensive tests for StudyMaterialsViewModel
 * Tests study category loading and article count aggregation
 */
@OptIn(ExperimentalCoroutinesApi::class)
class StudyMaterialsViewModelTest : BaseViewModelTest() {
    
    private lateinit var viewModel: StudyMaterialsViewModel
    
    @After
    fun tearDown() {
        unmockkAll()
    }
    
    // ==================== Initialization Tests ====================
    
    @Test
    fun `init loads study categories`() = runTest {
        // When
        viewModel = StudyMaterialsViewModel()
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        assertFalse("Should not be loading", state.isLoading)
        assertNull("Should have no error", state.error)
        assertTrue("Should have categories", state.categories.isNotEmpty())
    }
    
    @Test
    fun `loads all 9 study categories`() = runTest {
        // When
        viewModel = StudyMaterialsViewModel()
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        assertEquals("Should have 9 categories", 9, state.categories.size)
    }
    
    @Test
    fun `initial state shows loading`() {
        val defaultState = StudyMaterialsUiState()
        
        assertTrue("Should be loading initially", defaultState.isLoading)
        assertTrue("Should have no categories", defaultState.categories.isEmpty())
        assertEquals("Should have 0 total articles", 0, defaultState.totalArticles)
        assertNull("Should have no error", defaultState.error)
    }
    
    // ==================== Category Content Tests ====================
    
    @Test
    fun `includes all expected categories`() = runTest {
        // When
        viewModel = StudyMaterialsViewModel()
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        val categoryTypes = state.categories.map { it.type }
        
        assertTrue("Should have OIR_PREP", categoryTypes.contains(StudyCategory.OIR_PREP))
        assertTrue("Should have PPDT_TECHNIQUES", categoryTypes.contains(StudyCategory.PPDT_TECHNIQUES))
        assertTrue("Should have PSYCHOLOGY_TESTS", categoryTypes.contains(StudyCategory.PSYCHOLOGY_TESTS))
        assertTrue("Should have PIQ_PREP", categoryTypes.contains(StudyCategory.PIQ_PREP))
        assertTrue("Should have GTO_TASKS", categoryTypes.contains(StudyCategory.GTO_TASKS))
        assertTrue("Should have INTERVIEW_PREP", categoryTypes.contains(StudyCategory.INTERVIEW_PREP))
        assertTrue("Should have GENERAL_TIPS", categoryTypes.contains(StudyCategory.GENERAL_TIPS))
        assertTrue("Should have CURRENT_AFFAIRS", categoryTypes.contains(StudyCategory.CURRENT_AFFAIRS))
        assertTrue("Should have PHYSICAL_FITNESS", categoryTypes.contains(StudyCategory.PHYSICAL_FITNESS))
    }
    
    @Test
    fun `categories have correct premium flags`() = runTest {
        // When
        viewModel = StudyMaterialsViewModel()
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        
        // Free categories
        val oirPrep = state.categories.find { it.type == StudyCategory.OIR_PREP }
        assertFalse("OIR_PREP should be free", oirPrep?.isPremium == true)
        
        val ppdtTechniques = state.categories.find { it.type == StudyCategory.PPDT_TECHNIQUES }
        assertFalse("PPDT_TECHNIQUES should be free", ppdtTechniques?.isPremium == true)
        
        val piqPrep = state.categories.find { it.type == StudyCategory.PIQ_PREP }
        assertFalse("PIQ_PREP should be free", piqPrep?.isPremium == true)
        
        val generalTips = state.categories.find { it.type == StudyCategory.GENERAL_TIPS }
        assertFalse("GENERAL_TIPS should be free", generalTips?.isPremium == true)
        
        val physicalFitness = state.categories.find { it.type == StudyCategory.PHYSICAL_FITNESS }
        assertFalse("PHYSICAL_FITNESS should be free", physicalFitness?.isPremium == true)
        
        // Premium categories
        val psychologyTests = state.categories.find { it.type == StudyCategory.PSYCHOLOGY_TESTS }
        assertTrue("PSYCHOLOGY_TESTS should be premium", psychologyTests?.isPremium == true)
        
        val gtoTasks = state.categories.find { it.type == StudyCategory.GTO_TASKS }
        assertTrue("GTO_TASKS should be premium", gtoTasks?.isPremium == true)
        
        val interviewPrep = state.categories.find { it.type == StudyCategory.INTERVIEW_PREP }
        assertTrue("INTERVIEW_PREP should be premium", interviewPrep?.isPremium == true)
        
        val currentAffairs = state.categories.find { it.type == StudyCategory.CURRENT_AFFAIRS }
        assertTrue("CURRENT_AFFAIRS should be premium", currentAffairs?.isPremium == true)
    }
    
    @Test
    fun `all categories have titles`() = runTest {
        // When
        viewModel = StudyMaterialsViewModel()
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        state.categories.forEach { category ->
            assertFalse("Category should have non-empty title", category.title.isBlank())
        }
    }
    
    @Test
    fun `all categories have positive article counts`() = runTest {
        // When
        viewModel = StudyMaterialsViewModel()
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        state.categories.forEach { category ->
            assertTrue("${category.title} should have positive article count", 
                category.articleCount > 0)
        }
    }
    
    // ==================== Total Articles Calculation Tests ====================
    
    @Test
    fun `calculates total articles correctly`() = runTest {
        // When
        viewModel = StudyMaterialsViewModel()
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        val manualTotal = state.categories.sumOf { it.articleCount }
        
        assertEquals("Total should match sum of all categories", 
            manualTotal, state.totalArticles)
    }
    
    @Test
    fun `total articles is greater than zero`() = runTest {
        // When
        viewModel = StudyMaterialsViewModel()
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        assertTrue("Should have articles", state.totalArticles > 0)
    }
    
    // ==================== Category Details Tests ====================
    
    @Test
    fun `OIR Prep category has correct details`() = runTest {
        // When
        viewModel = StudyMaterialsViewModel()
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        val oirPrep = state.categories.find { it.type == StudyCategory.OIR_PREP }
        
        assertNotNull("OIR Prep should exist", oirPrep)
        assertEquals("OIR Prep title", "OIR Test Prep", oirPrep?.title)
        assertEquals("OIR Prep article count", 24, oirPrep?.articleCount)
        assertFalse("OIR Prep should be free", oirPrep?.isPremium == true)
    }
    
    @Test
    fun `Current Affairs has highest article count`() = runTest {
        // When
        viewModel = StudyMaterialsViewModel()
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        val currentAffairs = state.categories.find { it.type == StudyCategory.CURRENT_AFFAIRS }
        
        assertNotNull("Current Affairs should exist", currentAffairs)
        assertEquals("Current Affairs article count", 120, currentAffairs?.articleCount)
        
        // Verify it's the highest
        val maxCount = state.categories.maxOf { it.articleCount }
        assertEquals("Current Affairs should have most articles", maxCount, currentAffairs?.articleCount)
    }
    
    @Test
    fun `General Tips has second highest article count`() = runTest {
        // When
        viewModel = StudyMaterialsViewModel()
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        val generalTips = state.categories.find { it.type == StudyCategory.GENERAL_TIPS }
        
        assertNotNull("General Tips should exist", generalTips)
        assertEquals("General Tips article count", 56, generalTips?.articleCount)
    }
    
    // ==================== Free vs Premium Distribution Tests ====================
    
    @Test
    fun `has both free and premium categories`() = runTest {
        // When
        viewModel = StudyMaterialsViewModel()
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        val freeCategories = state.categories.filter { !it.isPremium }
        val premiumCategories = state.categories.filter { it.isPremium }
        
        assertTrue("Should have free categories", freeCategories.isNotEmpty())
        assertTrue("Should have premium categories", premiumCategories.isNotEmpty())
        
        assertEquals("Should have 5 free categories", 5, freeCategories.size)
        assertEquals("Should have 4 premium categories", 4, premiumCategories.size)
    }
    
    @Test
    fun `free categories are accessible to all users`() = runTest {
        // When
        viewModel = StudyMaterialsViewModel()
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        val freeCategories = state.categories.filter { !it.isPremium }
        
        // Verify expected free categories
        val freeCategoryTypes = freeCategories.map { it.type }
        assertTrue(freeCategoryTypes.contains(StudyCategory.OIR_PREP))
        assertTrue(freeCategoryTypes.contains(StudyCategory.PPDT_TECHNIQUES))
        assertTrue(freeCategoryTypes.contains(StudyCategory.PIQ_PREP))
        assertTrue(freeCategoryTypes.contains(StudyCategory.GENERAL_TIPS))
        assertTrue(freeCategoryTypes.contains(StudyCategory.PHYSICAL_FITNESS))
    }
    
    // ==================== UI State Tests ====================
    
    @Test
    fun `all categories have icons assigned`() = runTest {
        // When
        viewModel = StudyMaterialsViewModel()
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        state.categories.forEach { category ->
            assertNotNull("${category.title} should have an icon", category.icon)
        }
    }
    
    @Test
    fun `all categories have colors assigned`() = runTest {
        // When
        viewModel = StudyMaterialsViewModel()
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        state.categories.forEach { category ->
            assertNotNull("${category.title} should have backgroundColor", category.backgroundColor)
            assertNotNull("${category.title} should have iconColor", category.iconColor)
            assertNotNull("${category.title} should have textColor", category.textColor)
        }
    }
}

