package com.ssbmax.navigation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * commonMain seam both platforms' notification-tap edges feed into
 * (KMP-convergence Phase 4) -- Android's `MainActivity.onCreate`/
 * `onNewIntent` and iOS's `AppDelegate`
 * `userNotificationCenter(_:didReceive:)`, closing iOS's total prior gap
 * (an explicit no-op).
 *
 * A [StateFlow], not a `Channel`: a cold-start notification tap arrives long
 * before the nav graph clears the auth screens, and a hot-event channel
 * would drop it before [com.ssbmax.shared.ui.DeepLinkEffect] starts
 * collecting.
 */
class DeepLinkGateway {

    private val _pendingRoute = MutableStateFlow<String?>(null)

    /** The parsed, scheme-stripped pending route, or null if none is waiting. */
    val pendingRoute: StateFlow<String?> = _pendingRoute.asStateFlow()

    /**
     * [raw] is whatever the platform edge received -- a full `ssbmax://...`
     * deep link, or an already scheme-stripped route. Resolved via
     * [DeepLinkParser]; blank/unparseable input is dropped rather than
     * clearing an existing pending route.
     */
    fun submit(raw: String?) {
        val route = DeepLinkParser.parseToRoute(raw) ?: return
        _pendingRoute.value = route
    }

    /** Marks the current pending route as handled. */
    fun consume() {
        _pendingRoute.value = null
    }
}
