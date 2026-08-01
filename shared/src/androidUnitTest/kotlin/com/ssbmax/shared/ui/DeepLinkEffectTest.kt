package com.ssbmax.shared.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.createGraph
import com.ssbmax.navigation.DeepLinkParser
import com.ssbmax.navigation.SSBMaxDestinations
import com.ssbmax.navigation.authGraph
import com.ssbmax.navigation.homeGraph
import com.ssbmax.navigation.interviewGraph
import com.ssbmax.shared.domain.util.NoOpLogger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * KMP-convergence Phase 4: pins [DeepLinkEffect]'s three behaviours ported
 * from `app/ui/SSBMaxApp.kt`'s original `LaunchedEffect` -- auth-screen
 * deferral, already-on-target de-dupe, and loud-but-non-fatal failure for a
 * route with no registered [androidx.navigation.NavDeepLink] (e.g.
 * `ROUTE_INTERVIEW_HISTORY`, a pre-existing gap this seam is built to
 * tolerate, not fix).
 *
 * No Koin graph needed: [DeepLinkEffect] takes its dependencies as plain
 * parameters (see its own class doc for why), and building a
 * [NavHostController] graph via the real `*Graph.kt` builder functions
 * doesn't invoke any screen's Composable content -- same precedent as
 * [com.ssbmax.navigation.Phase3aNavigationTest]. Lives in `androidUnitTest`
 * for the same Robolectric/`Context` reason as that test and
 * [com.ssbmax.shared.ui.theme.SSBMaxThemeUiTest].
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
class DeepLinkEffectTest {

    private fun buildController(): NavHostController {
        val navController = NavHostController(RuntimeEnvironment.getApplication())
        navController.navigatorProvider.addNavigator(ComposeNavigator())
        val graph = navController.createGraph(startDestination = SSBMaxDestinations.Splash) {
            authGraph(navController)
            homeGraph(navController, onOpenDrawer = {})
            interviewGraph(navController)
        }
        navController.graph = graph
        return navController
    }

    @Test
    fun `defers navigation while on an auth screen`() = runComposeUiTest {
        val navController = buildController()
        var consumed = false

        setContent {
            DeepLinkEffect(
                navController = navController,
                pendingRoute = "interview/result/abc123",
                isAuthenticated = true,
                onConsume = { consumed = true },
                logger = NoOpLogger()
            )
        }
        waitForIdle()

        assertTrue(navController.currentDestination?.hasRoute<SSBMaxDestinations.Splash>() == true)
        assertFalse(consumed)
    }

    @Test
    fun `navigates once authenticated and past the auth screens`() = runComposeUiTest {
        val navController = buildController()
        navController.navigate(SSBMaxDestinations.StudentHome) {
            popUpTo<SSBMaxDestinations.Splash> { inclusive = true }
        }
        var consumed = false

        setContent {
            DeepLinkEffect(
                navController = navController,
                pendingRoute = "interview/result/abc123",
                isAuthenticated = true,
                onConsume = { consumed = true },
                logger = NoOpLogger()
            )
        }
        waitForIdle()

        assertTrue(navController.currentDestination?.hasRoute<SSBMaxDestinations.InterviewResult>() == true)
        assertTrue(consumed)
    }

    @Test
    fun `skips re-navigating when already on the deep link's target destination`() = runComposeUiTest {
        val navController = buildController()
        navController.navigate(SSBMaxDestinations.InterviewResult("already-here"))
        val backStackSizeBefore = navController.currentBackStack.value.size
        var consumed = false

        setContent {
            DeepLinkEffect(
                navController = navController,
                pendingRoute = "interview/result/some-other-id",
                isAuthenticated = true,
                onConsume = { consumed = true },
                logger = NoOpLogger()
            )
        }
        waitForIdle()

        // Same coarse dedupe as the original: same destination TYPE skips,
        // regardless of the differing trailing id -- matching `.route`
        // never carrying filled-in argument values on either the old
        // string-routed graph or this one.
        assertEquals(backStackSizeBefore, navController.currentBackStack.value.size)
        assertTrue(consumed)
    }

    @Test
    fun `unregistered deep link is tolerated, not crashed`() = runComposeUiTest {
        val navController = buildController()
        navController.navigate(SSBMaxDestinations.StudentHome) {
            popUpTo<SSBMaxDestinations.Splash> { inclusive = true }
        }
        var consumed = false

        setContent {
            DeepLinkEffect(
                navController = navController,
                pendingRoute = DeepLinkParser.ROUTE_INTERVIEW_HISTORY,
                isAuthenticated = true,
                onConsume = { consumed = true },
                logger = NoOpLogger()
            )
        }
        waitForIdle()

        assertTrue(navController.currentDestination?.hasRoute<SSBMaxDestinations.StudentHome>() == true)
        assertTrue(consumed)
    }

    @Test
    fun `does nothing while no user is authenticated`() = runComposeUiTest {
        val navController = buildController()
        navController.navigate(SSBMaxDestinations.StudentHome) {
            popUpTo<SSBMaxDestinations.Splash> { inclusive = true }
        }
        var consumed = false

        setContent {
            DeepLinkEffect(
                navController = navController,
                pendingRoute = "interview/result/abc123",
                isAuthenticated = false,
                onConsume = { consumed = true },
                logger = NoOpLogger()
            )
        }
        waitForIdle()

        assertTrue(navController.currentDestination?.hasRoute<SSBMaxDestinations.StudentHome>() == true)
        assertFalse(consumed)
    }
}
