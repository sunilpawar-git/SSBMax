package com.ssbmax.shared.presentation.settings.developer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssbmax.shared.domain.model.SubscriptionOverride
import com.ssbmax.shared.platform.isDebugBuild
import com.ssbmax.shared.platform.settings.DeveloperSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * KMP mirror of [com.ssbmax.shared.presentation.settings.theme.ThemeSettingsViewModel]'s shape:
 * a real ViewModel backed by [DeveloperSettings] (the Settings-backed store from Phase 1 of the
 * dev-subscription-override plan), reactive to external changes via its two `StateFlow`s.
 *
 * [DeveloperSettingsUiState.isVisible] is read once from [isDebugBuild] here, not re-checked per
 * recomposition -- the build type can't change at runtime, unlike the override/bypass fields.
 */
class DeveloperSettingsViewModel(
    private val developerSettings: DeveloperSettings
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        DeveloperSettingsUiState(
            isVisible = isDebugBuild(),
            override = developerSettings.getOverride(),
            bypassInterviewPrerequisites = developerSettings.getBypassInterviewPrerequisites()
        )
    )
    val uiState: StateFlow<DeveloperSettingsUiState> = _uiState.asStateFlow()

    init {
        observeOverride()
        observeBypassInterviewPrerequisites()
    }

    private fun observeOverride() {
        viewModelScope.launch {
            developerSettings.overrideFlow.collect { override ->
                _uiState.update { it.copy(override = override) }
            }
        }
    }

    private fun observeBypassInterviewPrerequisites() {
        viewModelScope.launch {
            developerSettings.bypassInterviewPrerequisitesFlow.collect { bypass ->
                _uiState.update { it.copy(bypassInterviewPrerequisites = bypass) }
            }
        }
    }

    fun updateOverride(override: SubscriptionOverride) {
        developerSettings.setOverride(override)
        _uiState.update { it.copy(override = override) }
    }

    fun updateBypassInterviewPrerequisites(bypass: Boolean) {
        developerSettings.setBypassInterviewPrerequisites(bypass)
        _uiState.update { it.copy(bypassInterviewPrerequisites = bypass) }
    }
}
