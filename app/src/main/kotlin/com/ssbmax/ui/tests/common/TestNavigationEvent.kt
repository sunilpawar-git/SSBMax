package com.ssbmax.ui.tests.common

import com.ssbmax.core.domain.model.SubscriptionType

/**
 * Navigation events for test screens
 * 
 * Following Android best practices for one-time events:
 * - Events are consumed (won't re-trigger on configuration changes)
 * - Uses Channel/Flow pattern for event emission
 * - Separates state (UI data) from events (navigation, snackbars, etc.)
 * 
 * Reference: https://developer.android.com/jetpack/compose/side-effects#one-time-events
 */
sealed interface TestNavigationEvent {
    /**
     * Navigate to test result screen after successful submission
     * 
     * @param submissionId Unique ID of the submitted test (used to fetch results)
     * @param subscriptionType User's subscription tier (determines result screen type)
     */
    data class NavigateToResult(
        val submissionId: String,
        val subscriptionType: SubscriptionType
    ) : TestNavigationEvent
}

