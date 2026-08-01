package com.ssbmax.navigation

import androidx.navigation.NavHostController
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.createGraph
import androidx.savedstate.read
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * KMP-convergence Phase 3a, "navigation wiring" table -- one test per row that
 * fixes a missing/incorrect route registration. Builds a real [NavHostController]
 * graph via the same `NavGraphBuilder` extension functions the app uses -- no
 * Compose composition or Koin needed: [ComposeNavigator]'s destinations just
 * hold their content lambda uninvoked until actually rendered, so this only
 * exercises route *registration and matching*, exactly what these rows are
 * about.
 *
 * Lives in `androidUnitTest`, not `commonTest` (same precedent as
 * [com.ssbmax.shared.ui.theme.SSBMaxThemeUiTest]): on the Android target,
 * `org.jetbrains.androidx.navigation:navigation-runtime`'s Gradle module
 * metadata declares a real dependency on the classic
 * `androidx.navigation:navigation-runtime`, whose `NavHostController` actual
 * requires a real `Context` constructor arg (confirmed empirically: a bare
 * `NavHostController()` fails Android compilation with "No value passed for
 * parameter 'context'") -- only the iOS/desktop actual is context-free.
 * Robolectric (already a dependency for the theme UI test above) supplies
 * that Context without needing an instrumented/emulator run.
 */
@RunWith(RobolectricTestRunner::class)
class Phase3aNavigationTest {

    private fun buildController(): NavHostController {
        val navController = NavHostController(RuntimeEnvironment.getApplication())
        navController.navigatorProvider.addNavigator(ComposeNavigator())
        return navController
    }

    // Row #2: `topic/{topicId}` had no `?selectedTab=` arg, so
    // `StudentHomeScreen`'s "topic/OIR?selectedTab=2" (Tests tab preselected)
    // had no matching destination.
    @Test
    fun topicScreen_withSelectedTab_resolvesAndCarriesTheArg() {
        val navController = buildController()
        val graph = navController.createGraph(startDestination = SSBMaxDestinations.StudentHome.route) {
            homeGraph(navController, onOpenDrawer = {})
            studyContentGraph(navController)
        }
        navController.graph = graph

        navController.navigate(SSBMaxDestinations.TopicScreen.createRoute("OIR") + "?selectedTab=2")

        assertEquals(
            SSBMaxDestinations.TopicScreen.route + "?selectedTab={selectedTab}",
            navController.currentDestination?.route
        )
        val selectedTab = navController.currentBackStackEntry?.arguments?.read { getIntOrNull("selectedTab") }
        assertEquals(2, selectedTab)
    }

    // Same destination must still resolve with no query param at all (every
    // other caller of TopicScreen.createRoute doesn't pass one), defaulting
    // selectedTab to the Overview tab (0).
    @Test
    fun topicScreen_withoutSelectedTab_defaultsToOverviewTab() {
        val navController = buildController()
        val graph = navController.createGraph(startDestination = SSBMaxDestinations.StudentHome.route) {
            homeGraph(navController, onOpenDrawer = {})
            studyContentGraph(navController)
        }
        navController.graph = graph

        navController.navigate(SSBMaxDestinations.TopicScreen.createRoute("OIR"))

        assertEquals(
            SSBMaxDestinations.TopicScreen.route + "?selectedTab={selectedTab}",
            navController.currentDestination?.route
        )
        val selectedTab = navController.currentBackStackEntry?.arguments?.read { getIntOrNull("selectedTab") }
        assertEquals(0, selectedTab)
    }

    // Row #9: `student/study` (the BottomNavItem) was never registered.
    @Test
    fun studentStudy_bottomNavRoute_resolves() {
        val navController = buildController()
        val graph = navController.createGraph(startDestination = SSBMaxDestinations.StudentHome.route) {
            homeGraph(navController, onOpenDrawer = {})
            studyContentGraph(navController)
        }
        navController.graph = graph

        navController.navigate(SSBMaxDestinations.StudentStudy.route)

        assertEquals(SSBMaxDestinations.StudentStudy.route, navController.currentDestination?.route)
    }

    // Row #11: `batch/join` was unregistered in both graphs, yet both drawers
    // navigate to it -- a crash on both platforms before this fix.
    @Test
    fun joinBatch_route_resolvesInsteadOfCrashing() {
        val navController = buildController()
        val graph = navController.createGraph(startDestination = SSBMaxDestinations.StudentHome.route) {
            homeGraph(navController, onOpenDrawer = {})
            submissionsResultsGraph(navController)
        }
        navController.graph = graph

        navController.navigate(SSBMaxDestinations.JoinBatch.route)

        assertEquals(SSBMaxDestinations.JoinBatch.route, navController.currentDestination?.route)
    }

    // Characterizes the pre-fix state for row #11: navigating to an
    // unregistered route throws, which is the "crash on both platforms"
    // the plan documents -- proof this test would have failed red before the
    // fix above, not just passed vacuously.
    @Test
    fun joinBatch_withoutRegistration_wouldHaveThrown() {
        val navController = buildController()
        val graph = navController.createGraph(startDestination = SSBMaxDestinations.StudentHome.route) {
            homeGraph(navController, onOpenDrawer = {})
            // submissionsResultsGraph deliberately omitted here
        }
        navController.graph = graph

        assertFailsWith<IllegalArgumentException> {
            navController.navigate(SSBMaxDestinations.JoinBatch.route)
        }
    }
}
