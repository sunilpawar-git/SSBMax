package com.ssbmax.ui.faq

import androidx.lifecycle.ViewModel
import com.ssbmax.core.domain.model.FAQCategory
import com.ssbmax.core.domain.model.FAQItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/**
 * ViewModel for FAQ Screen
 * Manages FAQ content, search, and filtering
 */
@HiltViewModel
class FAQViewModel @Inject constructor() : ViewModel() {
    
    private val _uiState = MutableStateFlow(FAQUiState())
    val uiState: StateFlow<FAQUiState> = _uiState.asStateFlow()
    
    init {
        loadFAQs()
    }
    
    private fun loadFAQs() {
        val faqs = FAQContentProvider.getFAQItems()
        _uiState.update {
            it.copy(
                allFAQs = faqs,
                displayedFAQs = faqs
            )
        }
    }
    
    fun filterByCategory(category: FAQCategory?) {
        val currentQuery = _uiState.value.searchQuery
        _uiState.update {
            val filtered = if (category == null) {
                if (currentQuery.isBlank()) it.allFAQs
                else it.allFAQs.filter { faq -> matchesQuery(faq, currentQuery) }
            } else {
                it.allFAQs
                    .filter { faq -> faq.category == category }
                    .filter { faq -> 
                        if (currentQuery.isBlank()) true 
                        else matchesQuery(faq, currentQuery) 
                    }
            }
            
            it.copy(
                selectedCategory = category,
                displayedFAQs = filtered
            )
        }
    }
    
    fun searchFAQs(query: String) {
        val currentCategory = _uiState.value.selectedCategory
        _uiState.update {
            val filtered = if (query.isBlank()) {
                if (currentCategory == null) it.allFAQs
                else it.allFAQs.filter { faq -> faq.category == currentCategory }
            } else {
                it.allFAQs
                    .filter { faq -> 
                        if (currentCategory == null) true 
                        else faq.category == currentCategory 
                    }
                    .filter { faq -> matchesQuery(faq, query) }
            }
            
            it.copy(
                searchQuery = query,
                displayedFAQs = filtered
            )
        }
    }
    
    fun toggleFAQExpansion(faqId: String) {
        _uiState.update {
            val newExpanded = if (it.expandedFAQIds.contains(faqId)) {
                it.expandedFAQIds - faqId
            } else {
                it.expandedFAQIds + faqId
            }
            it.copy(expandedFAQIds = newExpanded)
        }
    }
    
    private fun matchesQuery(faq: FAQItem, query: String): Boolean {
        return faq.question.contains(query, ignoreCase = true) ||
               faq.answer.contains(query, ignoreCase = true)
    }
}

/**
 * UI state for FAQ screen
 */
data class FAQUiState(
    val allFAQs: List<FAQItem> = emptyList(),
    val displayedFAQs: List<FAQItem> = emptyList(),
    val selectedCategory: FAQCategory? = null,
    val searchQuery: String = "",
    val expandedFAQIds: Set<String> = emptySet()
)

