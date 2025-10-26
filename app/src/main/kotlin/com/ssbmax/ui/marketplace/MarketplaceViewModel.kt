package com.ssbmax.ui.marketplace

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssbmax.core.domain.model.CoachingInstitute
import com.ssbmax.core.domain.model.InstituteType
import com.ssbmax.core.domain.model.PriceRange
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Marketplace Screen
 * Manages coaching institute listings, filters, and search
 * 
 * Note: Marketplace feature currently uses mock data (MarketplaceMockData)
 * TODO: Create MarketplaceRepository when backend API is ready
 * TODO: Integrate with payment gateway for bookings
 * TODO: Add user reviews and ratings system
 */
@HiltViewModel
class MarketplaceViewModel @Inject constructor(
    // Marketplace backend not yet implemented
    // Using MarketplaceMockData for UI development
) : ViewModel() {

    private val _uiState = MutableStateFlow(MarketplaceUiState())
    val uiState: StateFlow<MarketplaceUiState> = _uiState.asStateFlow()

    private val allInstitutes: List<CoachingInstitute> by lazy {
        Log.d("Marketplace", "Loading mock institute data")
        MarketplaceMockData.getInstitutes()
    }

    init {
        loadInstitutes()
    }

    private fun loadInstitutes() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            try {
                Log.d("Marketplace", "Loading institutes with filters - " +
                        "search: '${_uiState.value.searchQuery}', " +
                        "type: ${_uiState.value.filterType}, " +
                        "price: ${_uiState.value.filterPriceRange}, " +
                        "city: ${_uiState.value.filterCity}")
                
                // Apply filters
                val filtered = filterInstitutes(
                    institutes = allInstitutes,
                    searchQuery = _uiState.value.searchQuery,
                    filterType = _uiState.value.filterType,
                    filterPriceRange = _uiState.value.filterPriceRange,
                    filterCity = _uiState.value.filterCity
                )
                
                Log.d("Marketplace", "Filtered ${filtered.size} institutes from ${allInstitutes.size} total")
                
                _uiState.update {
                    it.copy(
                        institutes = filtered,
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                Log.e("Marketplace", "Error loading institutes", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load institutes"
                    )
                }
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        loadInstitutes()
    }

    fun onFilterTypeChange(type: InstituteType?) {
        _uiState.update { it.copy(filterType = type) }
        loadInstitutes()
    }

    fun onFilterPriceRangeChange(priceRange: PriceRange?) {
        _uiState.update { it.copy(filterPriceRange = priceRange) }
        loadInstitutes()
    }

    fun onFilterCityChange(city: String?) {
        _uiState.update { it.copy(filterCity = city) }
        loadInstitutes()
    }

    fun clearFilters() {
        _uiState.update {
            it.copy(
                searchQuery = "",
                filterType = null,
                filterPriceRange = null,
                filterCity = null
            )
        }
        loadInstitutes()
    }

    private fun filterInstitutes(
        institutes: List<CoachingInstitute>,
        searchQuery: String,
        filterType: InstituteType?,
        filterPriceRange: PriceRange?,
        filterCity: String?
    ): List<CoachingInstitute> {
        return institutes.filter { institute ->
            // Search filter
            val matchesSearch = searchQuery.isBlank() ||
                    institute.name.contains(searchQuery, ignoreCase = true) ||
                    institute.location.contains(searchQuery, ignoreCase = true) ||
                    institute.description.contains(searchQuery, ignoreCase = true)

            // Type filter
            val matchesType = filterType == null || institute.type == filterType

            // Price filter
            val matchesPrice = filterPriceRange == null || institute.priceRange == filterPriceRange

            // City filter
            val matchesCity = filterCity == null || institute.city.equals(filterCity, ignoreCase = true)

            matchesSearch && matchesType && matchesPrice && matchesCity
        }
    }

    fun refresh() {
        loadInstitutes()
    }
}

/**
 * UI State for Marketplace Screen
 */
data class MarketplaceUiState(
    val institutes: List<CoachingInstitute> = emptyList(),
    val searchQuery: String = "",
    val filterType: InstituteType? = null,
    val filterPriceRange: PriceRange? = null,
    val filterCity: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

