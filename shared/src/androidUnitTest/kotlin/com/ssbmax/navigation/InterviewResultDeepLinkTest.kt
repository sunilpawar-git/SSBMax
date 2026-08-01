package com.ssbmax.navigation

import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.NavUri
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.createGraph
import androidx.navigation.navOptions
import androidx.navigation.toRoute
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * KMP-convergence Phase 4: [InterviewGraph.interviewGraph] registers a
 * `navDeepLink` on `InterviewResult` shaped exactly like
 * [DeepLinkParser.buildInterviewResultDeepLink]'s output. This is the
 * trickiest part of the deep-link seam under type-safe routing --
 * `NavController.navigate(route: String)` (used by every in-graph
 * `navigate(SSBMaxDestinations.InterviewResult(...))` call) never consults
 * the `deepLinks` list added to a `composable<T>()`; only
 * `navigate(deepLink: NavUri)` does. This test exercises that exact call,
 * the same one [com.ssbmax.shared.ui.DeepLinkEffect] makes, against a real
 * graph -- not an assumption about the library's internals.
 *
 * Lives in `androidUnitTest`, not `commonTest` -- same precedent as
 * [Phase3aNavigationTest]: the Android `NavHostController` actual needs a
 * real `Context`, which Robolectric supplies without an emulator.
 */
@RunWith(RobolectricTestRunner::class)
class InterviewResultDeepLinkTest {

    private fun buildController(): NavHostController {
        val navController = NavHostController(RuntimeEnvironment.getApplication())
        navController.navigatorProvider.addNavigator(ComposeNavigator())
        val graph = navController.createGraph(startDestination = SSBMaxDestinations.StudentHome) {
            homeGraph(navController, onOpenDrawer = {})
            interviewGraph(navController)
        }
        navController.graph = graph
        return navController
    }

    @Test
    fun `notification-shaped deep link resolves to InterviewResult with the right id`() {
        val navController = buildController()
        val deepLink = DeepLinkParser.buildInterviewResultDeepLink("abc123")

        navController.navigate(
            NavUri(DeepLinkParser.SCHEME + DeepLinkParser.parseToRoute(deepLink)!!),
            navOptions { launchSingleTop = true }
        )

        assertTrue(navController.currentDestination?.hasRoute<SSBMaxDestinations.InterviewResult>() == true)
        val route = navController.currentBackStackEntry?.toRoute<SSBMaxDestinations.InterviewResult>()
        assertTrue(route?.resultId == "abc123")
    }

    @Test
    fun `an unregistered deep link (interview history) throws rather than silently no-opping`() {
        val navController = buildController()

        // Characterizes the pre-existing gap DeepLinkEffect's try/catch is
        // built to tolerate: ROUTE_INTERVIEW_HISTORY has never been
        // registered on either platform's graph (NotificationHelper builds
        // it, nothing consumes it) -- not a Phase 4 regression.
        assertFailsWith<IllegalArgumentException> {
            navController.navigate(
                NavUri(DeepLinkParser.SCHEME + DeepLinkParser.ROUTE_INTERVIEW_HISTORY),
                navOptions { launchSingleTop = true }
            )
        }
    }

    @Test
    fun `plain string navigate to the internal route ignores the registered deepLinks entirely`() {
        // The precise, otherwise-undocumented distinction this seam relies
        // on: navigate(route: String) matches only a destination's own
        // generated route, never its `deepLinks` list -- so DeepLinkEffect
        // must call navigate(NavUri), not navigate(String), with the raw
        // deep-link shape.
        val navController = buildController()

        assertFailsWith<IllegalArgumentException> {
            navController.navigate("interview/result/abc123")
        }
    }
}
