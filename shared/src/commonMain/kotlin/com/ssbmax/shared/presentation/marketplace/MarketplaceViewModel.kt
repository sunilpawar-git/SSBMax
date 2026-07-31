package com.ssbmax.shared.presentation.marketplace

import com.ssbmax.shared.domain.model.CoachingInstitute
import com.ssbmax.shared.domain.model.InstituteType
import com.ssbmax.shared.domain.model.PriceRange
import com.ssbmax.shared.domain.util.DomainLogger
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * KMP port of the Android `app/.../ui/marketplace/MarketplaceViewModel.kt`.
 * Manages coaching institute listings, filters, and search -- backed by
 * [MarketplaceMockData] on both platforms (no `MarketplaceRepository`/backend
 * exists yet; same open TODO as the Android original).
 *
 * Uses a real `androidx.lifecycle.ViewModel` with `viewModelScope` (Phase 1 of
 * the KMP-convergence plan, see
 * [com.ssbmax.shared.presentation.oir.OIRTestViewModel]'s doc comment for the
 * precedent this mirrors). `ErrorLogger.log` (Android-only, Crashlytics-backed)
 * replaced with [DomainLogger], same seam every other ported ViewModel in
 * this phase uses.
 */
class MarketplaceViewModel(
    private val logger: DomainLogger
) : ViewModel() {
    private val _uiState = MutableStateFlow(MarketplaceUiState())
    val uiState: StateFlow<MarketplaceUiState> = _uiState.asStateFlow()

    private val allInstitutes: List<CoachingInstitute> by lazy {
        MarketplaceMockData.getInstitutes()
    }

    private companion object {
        const val TAG = "MarketplaceViewModel"
    }

    init {
        loadInstitutes()
    }

    private fun loadInstitutes() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                val filtered = filterInstitutes(
                    institutes = allInstitutes,
                    searchQuery = _uiState.value.searchQuery,
                    filterType = _uiState.value.filterType,
                    filterPriceRange = _uiState.value.filterPriceRange,
                    filterCity = _uiState.value.filterCity
                )

                _uiState.update {
                    it.copy(institutes = filtered, isLoading = false, error = null)
                }
            } catch (e: Exception) {
                logger.e(TAG, "Error loading marketplace institutes", e)
                _uiState.update {
                    it.copy(isLoading = false, error = e.message ?: "Failed to load institutes")
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
            it.copy(searchQuery = "", filterType = null, filterPriceRange = null, filterCity = null)
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
            val matchesSearch = searchQuery.isBlank() ||
                institute.name.contains(searchQuery, ignoreCase = true) ||
                institute.location.contains(searchQuery, ignoreCase = true) ||
                institute.description.contains(searchQuery, ignoreCase = true)

            val matchesType = filterType == null || institute.type == filterType
            val matchesPrice = filterPriceRange == null || institute.priceRange == filterPriceRange
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
