package com.ssbmax.ui.components.drawer

import com.ssbmax.core.domain.model.UserProfile

/**
 * UI state for the navigation drawer.
 * Manages user profile display and expandable sections.
 */
data class DrawerUiState(
    val userProfile: UserProfile? = null,
    val currentRoute: String = "",
    val phase1Expanded: Boolean = false,
    val phase2Expanded: Boolean = false
)

