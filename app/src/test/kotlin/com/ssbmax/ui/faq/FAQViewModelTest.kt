package com.ssbmax.ui.faq

import com.ssbmax.core.domain.model.FAQCategory
import com.ssbmax.core.domain.model.FAQItem
import com.ssbmax.testing.BaseViewModelTest
import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

/**
 * Comprehensive tests for FAQViewModel
 * Tests FAQ loading, search, filtering, and expansion
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FAQViewModelTest : BaseViewModelTest() {
    
    private lateinit var viewModel: FAQViewModel
    
    @Before
    fun setup() {
        // FAQViewModel has no dependencies, loads from static provider
        viewModel = FAQViewModel()
    }
    
    @After
    fun tearDown() {
        unmockkAll()
    }
    
    // ==================== Initialization Tests ====================
    
    @Test
    fun `init loads FAQs from content provider`() = runTest {
        // When - ViewModel already initialized in setup
        
        // Then
        val state = viewModel.uiState.value
        assertFalse("Should have FAQs", state.allFAQs.isEmpty())
        assertFalse("Should display FAQs", state.displayedFAQs.isEmpty())
        assertEquals("Displayed should match all FAQs initially", 
            state.allFAQs.size, state.displayedFAQs.size)
    }
    
    @Test
    fun `initial state has no category selected`() = runTest {
        // When
        val state = viewModel.uiState.value
        
        // Then
        assertNull("Should have no category selected", state.selectedCategory)
        assertEquals("Search query should be empty", "", state.searchQuery)
        assertTrue("No FAQs should be expanded", state.expandedFAQIds.isEmpty())
    }
    
    // ==================== Category Filter Tests ====================
    
    @Test
    fun `filterByCategory GENERAL shows only general FAQs`() = runTest {
        // When
        viewModel.filterByCategory(FAQCategory.GENERAL)
        
        // Then
        val state = viewModel.uiState.value
        assertEquals("Should select GENERAL category", FAQCategory.GENERAL, state.selectedCategory)
        assertTrue("Should have filtered FAQs", state.displayedFAQs.isNotEmpty())
        assertTrue("All displayed FAQs should be GENERAL category",
            state.displayedFAQs.all { it.category == FAQCategory.GENERAL }
        )
    }
    
    @Test
    fun `filterByCategory TESTS shows only test-related FAQs`() = runTest {
        // When
        viewModel.filterByCategory(FAQCategory.TESTS)
        
        // Then
        val state = viewModel.uiState.value
        assertEquals("Should select TESTS category", FAQCategory.TESTS, state.selectedCategory)
        assertTrue("All displayed FAQs should be TESTS category",
            state.displayedFAQs.all { it.category == FAQCategory.TESTS }
        )
    }
    
    @Test
    fun `filterByCategory SUBSCRIPTION shows only subscription FAQs`() = runTest {
        // When
        viewModel.filterByCategory(FAQCategory.SUBSCRIPTION)
        
        // Then
        val state = viewModel.uiState.value
        assertEquals("Should select SUBSCRIPTION category", 
            FAQCategory.SUBSCRIPTION, state.selectedCategory)
        assertTrue("All displayed FAQs should be SUBSCRIPTION category",
            state.displayedFAQs.all { it.category == FAQCategory.SUBSCRIPTION }
        )
    }
    
    @Test
    fun `filterByCategory null shows all FAQs`() = runTest {
        // Given - first filter to a category
        viewModel.filterByCategory(FAQCategory.GENERAL)
        val filteredCount = viewModel.uiState.value.displayedFAQs.size
        
        // When - reset filter
        viewModel.filterByCategory(null)
        
        // Then
        val state = viewModel.uiState.value
        assertNull("Should have no category selected", state.selectedCategory)
        assertTrue("Should show more FAQs than filtered", 
            state.displayedFAQs.size > filteredCount)
        assertEquals("Should show all FAQs", state.allFAQs.size, state.displayedFAQs.size)
    }
    
    // ==================== Search Tests ====================
    
    @Test
    fun `searchFAQs with query filters FAQs by question text`() = runTest {
        // When - search for a common word
        viewModel.searchFAQs("SSB")
        
        // Then
        val state = viewModel.uiState.value
        assertEquals("Search query should be stored", "SSB", state.searchQuery)
        assertTrue("Should have results", state.displayedFAQs.isNotEmpty())
        assertTrue("Results should match query in question or answer",
            state.displayedFAQs.all { faq ->
                faq.question.contains("SSB", ignoreCase = true) ||
                faq.answer.contains("SSB", ignoreCase = true)
            }
        )
    }
    
    @Test
    fun `searchFAQs with query filters FAQs by answer text`() = runTest {
        // When - search for word likely in answers
        viewModel.searchFAQs("test")
        
        // Then
        val state = viewModel.uiState.value
        assertTrue("Should have results", state.displayedFAQs.isNotEmpty())
        assertTrue("Results should match query",
            state.displayedFAQs.all { faq ->
                faq.question.contains("test", ignoreCase = true) ||
                faq.answer.contains("test", ignoreCase = true)
            }
        )
    }
    
    @Test
    fun `searchFAQs is case insensitive`() = runTest {
        // When - search with different cases
        viewModel.searchFAQs("ssb")
        val lowerCaseResults = viewModel.uiState.value.displayedFAQs.size
        
        viewModel.searchFAQs("SSB")
        val upperCaseResults = viewModel.uiState.value.displayedFAQs.size
        
        viewModel.searchFAQs("Ssb")
        val mixedCaseResults = viewModel.uiState.value.displayedFAQs.size
        
        // Then
        assertEquals("Case should not matter", lowerCaseResults, upperCaseResults)
        assertEquals("Case should not matter", upperCaseResults, mixedCaseResults)
    }
    
    @Test
    fun `searchFAQs with empty query shows all FAQs`() = runTest {
        // Given - first do a search
        viewModel.searchFAQs("specific query")
        val searchedCount = viewModel.uiState.value.displayedFAQs.size
        
        // When - clear search
        viewModel.searchFAQs("")
        
        // Then
        val state = viewModel.uiState.value
        assertEquals("Search query should be empty", "", state.searchQuery)
        assertTrue("Should show more FAQs after clearing search",
            state.displayedFAQs.size >= searchedCount)
        assertEquals("Should show all FAQs", state.allFAQs.size, state.displayedFAQs.size)
    }
    
    @Test
    fun `searchFAQs with no matches returns empty list`() = runTest {
        // When - search for something that definitely doesn't exist
        viewModel.searchFAQs("xyzabc123nonexistent")
        
        // Then
        val state = viewModel.uiState.value
        assertTrue("Should have no results", state.displayedFAQs.isEmpty())
    }
    
    // ==================== Combined Filter and Search Tests ====================
    
    @Test
    fun `filterByCategory then searchFAQs applies both filters`() = runTest {
        // Given - filter by category first
        viewModel.filterByCategory(FAQCategory.GENERAL)
        val categoryCount = viewModel.uiState.value.displayedFAQs.size
        
        // When - then search within that category
        viewModel.searchFAQs("SSB")
        
        // Then
        val state = viewModel.uiState.value
        assertEquals("Category should still be selected", 
            FAQCategory.GENERAL, state.selectedCategory)
        assertEquals("Search query should be stored", "SSB", state.searchQuery)
        assertTrue("Should have fewer results than category alone",
            state.displayedFAQs.size <= categoryCount)
        assertTrue("All results should match both category and query",
            state.displayedFAQs.all { faq ->
                faq.category == FAQCategory.GENERAL &&
                (faq.question.contains("SSB", ignoreCase = true) ||
                 faq.answer.contains("SSB", ignoreCase = true))
            }
        )
    }
    
    @Test
    fun `searchFAQs then filterByCategory applies both filters`() = runTest {
        // Given - search first
        viewModel.searchFAQs("test")
        val searchCount = viewModel.uiState.value.displayedFAQs.size
        
        // When - then filter by category
        viewModel.filterByCategory(FAQCategory.TESTS)
        
        // Then
        val state = viewModel.uiState.value
        assertEquals("Category should be selected", FAQCategory.TESTS, state.selectedCategory)
        assertEquals("Search query should be preserved", "test", state.searchQuery)
        assertTrue("Should have results", state.displayedFAQs.isNotEmpty())
        assertTrue("All results should match both query and category",
            state.displayedFAQs.all { faq ->
                faq.category == FAQCategory.TESTS &&
                (faq.question.contains("test", ignoreCase = true) ||
                 faq.answer.contains("test", ignoreCase = true))
            }
        )
    }
    
    // ==================== FAQ Expansion Tests ====================
    
    @Test
    fun `toggleFAQExpansion expands FAQ`() = runTest {
        // Given
        val faqId = "faq_1"
        
        // When
        viewModel.toggleFAQExpansion(faqId)
        
        // Then
        val state = viewModel.uiState.value
        assertTrue("FAQ should be expanded", state.expandedFAQIds.contains(faqId))
    }
    
    @Test
    fun `toggleFAQExpansion collapses already expanded FAQ`() = runTest {
        // Given - expand first
        val faqId = "faq_1"
        viewModel.toggleFAQExpansion(faqId)
        assertTrue("FAQ should be expanded", 
            viewModel.uiState.value.expandedFAQIds.contains(faqId))
        
        // When - toggle again
        viewModel.toggleFAQExpansion(faqId)
        
        // Then
        val state = viewModel.uiState.value
        assertFalse("FAQ should be collapsed", state.expandedFAQIds.contains(faqId))
    }
    
    @Test
    fun `multiple FAQs can be expanded simultaneously`() = runTest {
        // When
        viewModel.toggleFAQExpansion("faq_1")
        viewModel.toggleFAQExpansion("faq_2")
        viewModel.toggleFAQExpansion("faq_3")
        
        // Then
        val state = viewModel.uiState.value
        assertEquals("Should have 3 expanded FAQs", 3, state.expandedFAQIds.size)
        assertTrue("FAQ 1 should be expanded", state.expandedFAQIds.contains("faq_1"))
        assertTrue("FAQ 2 should be expanded", state.expandedFAQIds.contains("faq_2"))
        assertTrue("FAQ 3 should be expanded", state.expandedFAQIds.contains("faq_3"))
    }
    
    @Test
    fun `expansion state persists across filtering`() = runTest {
        // Given - expand some FAQs
        viewModel.toggleFAQExpansion("faq_1")
        viewModel.toggleFAQExpansion("faq_2")
        
        // When - apply filter
        viewModel.filterByCategory(FAQCategory.GENERAL)
        
        // Then
        val state = viewModel.uiState.value
        assertTrue("Expansion state should persist", state.expandedFAQIds.contains("faq_1"))
        assertTrue("Expansion state should persist", state.expandedFAQIds.contains("faq_2"))
    }
    
    @Test
    fun `expansion state persists across search`() = runTest {
        // Given - expand some FAQs
        viewModel.toggleFAQExpansion("faq_1")
        
        // When - search
        viewModel.searchFAQs("SSB")
        
        // Then
        val state = viewModel.uiState.value
        assertTrue("Expansion state should persist", state.expandedFAQIds.contains("faq_1"))
    }
    
    // ==================== UI State Tests ====================
    
    @Test
    fun `ui state has correct default values`() {
        val defaultState = FAQUiState()
        
        assertTrue(defaultState.allFAQs.isEmpty())
        assertTrue(defaultState.displayedFAQs.isEmpty())
        assertNull(defaultState.selectedCategory)
        assertEquals("", defaultState.searchQuery)
        assertTrue(defaultState.expandedFAQIds.isEmpty())
    }
    
    @Test
    fun `FAQ categories have correct display names`() {
        assertEquals("General", FAQCategory.GENERAL.displayName)
        assertEquals("Tests & Assessments", FAQCategory.TESTS.displayName)
        assertEquals("Subscription & Billing", FAQCategory.SUBSCRIPTION.displayName)
        assertEquals("Technical Support", FAQCategory.TECHNICAL.displayName)
        assertEquals("SSB Process", FAQCategory.SSB_PROCESS.displayName)
    }
}

