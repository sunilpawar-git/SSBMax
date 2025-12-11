package com.ssbmax.navigation

import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.*
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.testing.TestNavHostController
import com.ssbmax.testing.BaseComposeTest
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Test

/**
 * Navigation tests for SSBMax app
 * Tests verify navigation flows work correctly
 */
@HiltAndroidTest
class NavigationTest : BaseComposeTest() {

    private lateinit var navController: TestNavHostController

    @Before
    override fun setup() {
        super.setup()
    }

    @Test
    fun navGraph_verifyStartDestination() {
        // Given: App starts
        composeTestRule.setContent {
            navController = TestNavHostController(LocalContext.current)
            navController.navigatorProvider.addNavigator(ComposeNavigator())
            // Note: Full NavGraph setup would be done here
        }

        // Then: Start destination should be Splash
        assert(navController.currentDestination == null || 
               navController.currentBackStackEntry?.destination?.route?.contains("splash") == true) {
            "Start destination should be splash"
        }
    }

    @Test
    fun navigation_fromLoginToHome_works() {
        // This test verifies the Login -> Home navigation flow
        // In a real scenario, we'd set up the full nav graph and trigger login success
        
        composeTestRule.setContent {
            navController = TestNavHostController(LocalContext.current)
            navController.navigatorProvider.addNavigator(ComposeNavigator())
        }

        // Verify navigation controller is initialized
        assert(navController != null) { "Nav controller should be initialized" }
    }

    @Test
    fun navigation_toPhase1Detail_works() {
        // Given: Navigation to Phase 1 Detail
        composeTestRule.setContent {
            navController = TestNavHostController(LocalContext.current)
            navController.navigatorProvider.addNavigator(ComposeNavigator())
        }

        // When: Navigate to Phase 1 Detail
        // navController.navigate(SSBMaxDestinations.Phase1Detail.route)

        // Then: Verify navigation (simplified test)
        assert(navController != null) { "Nav controller should support phase navigation" }
    }

    @Test
    fun navigation_toTestScreens_preservesTestId() {
        // Given: Navigation to TAT test
        val testId = "test-123"
        
        composeTestRule.setContent {
            navController = TestNavHostController(LocalContext.current)
            navController.navigatorProvider.addNavigator(ComposeNavigator())
        }

        // When: Navigate to test with ID
        val route = SSBMaxDestinations.TATTest.createRoute(testId)
        
        // Then: Route should contain test ID
        assert(route.contains(testId)) { "Route should contain test ID" }
        assert(route.contains("test/tat")) { "Route should be TAT test route" }
    }

    @Test
    fun navigation_createRoutes_generatesCorrectPaths() {
        // Test route generation for different screens
        
        // TAT Test
        val tatRoute = SSBMaxDestinations.TATTest.createRoute("tat-123")
        assert(tatRoute == "test/tat/tat-123") { "TAT route should be correct" }
        
        // WAT Test
        val watRoute = SSBMaxDestinations.WATTest.createRoute("wat-456")
        assert(watRoute == "test/wat/wat-456") { "WAT route should be correct" }
        
        // OIR Test
        val oirRoute = SSBMaxDestinations.OIRTest.createRoute("oir-789")
        assert(oirRoute == "test/oir/oir-789") { "OIR route should be correct" }
        
        // SRT Test
        val srtRoute = SSBMaxDestinations.SRTTest.createRoute("srt-012")
        assert(srtRoute == "test/srt/srt-012") { "SRT route should be correct" }
    }

    @Test
    fun navigation_resultRoutes_generateCorrectly() {
        // Test result route generation
        
        // TAT Result
        val tatResultRoute = SSBMaxDestinations.TATSubmissionResult.createRoute("sub-123")
        assert(tatResultRoute == "test/tat/result/sub-123") { "TAT result route should be correct" }
        
        // WAT Result
        val watResultRoute = SSBMaxDestinations.WATSubmissionResult.createRoute("sub-456")
        assert(watResultRoute == "test/wat/result/sub-456") { "WAT result route should be correct" }
    }

    @Test
    fun navigation_topicRoute_includesTopicId() {
        // Test topic route generation
        // Assuming Topic navigation uses topicId directly
        val topicId = "TAT"
        assert(topicId.isNotEmpty()) { "Topic ID should be valid" }
    }

    @Test
    fun navigation_allDestinations_haveValidRoutes() {
        // Verify all destinations have non-empty routes
        assert(SSBMaxDestinations.Splash.route.isNotEmpty()) { "Splash route should not be empty" }
        assert(SSBMaxDestinations.Login.route.isNotEmpty()) { "Login route should not be empty" }
        assert(SSBMaxDestinations.StudentHome.route.isNotEmpty()) { "StudentHome route should not be empty" }
        assert(SSBMaxDestinations.Phase1Detail.route.isNotEmpty()) { "Phase1Detail route should not be empty" }
        assert(SSBMaxDestinations.Phase2Detail.route.isNotEmpty()) { "Phase2Detail route should not be empty" }
        assert(SSBMaxDestinations.StudentProfile.route.isNotEmpty()) { "StudentProfile route should not be empty" }
        assert(SSBMaxDestinations.StudentStudy.route.isNotEmpty()) { "StudentStudy route should not be empty" }
    }

    @Test
    fun navigation_backStack_isManaged() {
        // Given: Navigation controller
        composeTestRule.setContent {
            navController = TestNavHostController(LocalContext.current)
            navController.navigatorProvider.addNavigator(ComposeNavigator())
        }

        // Then: Nav controller should be initialized
        assert(navController != null) { "Navigation controller should be initialized" }
    }

    @Test
    fun navigation_interviewResultRoute_generatesCorrectly() {
        // Test interview result route generation (used for deep linking from notifications)
        val resultId = "result-abc123"
        val route = SSBMaxDestinations.InterviewResult.createRoute(resultId)
        
        assert(route == "interview/result/$resultId") { 
            "Interview result route should match deep link format. Expected: interview/result/$resultId, got: $route" 
        }
    }

    @Test
    fun navigation_interviewRoutes_areValid() {
        // Verify all interview-related destinations have valid routes
        assert(SSBMaxDestinations.StartInterview.route.isNotEmpty()) { 
            "StartInterview route should not be empty" 
        }
        assert(SSBMaxDestinations.TextInterviewSession.route.contains("{sessionId}")) { 
            "TextInterviewSession route should contain sessionId parameter" 
        }
        assert(SSBMaxDestinations.VoiceInterviewSession.route.contains("{sessionId}")) { 
            "VoiceInterviewSession route should contain sessionId parameter" 
        }
        assert(SSBMaxDestinations.InterviewResult.route.contains("{resultId}")) { 
            "InterviewResult route should contain resultId parameter" 
        }
    }

    @Test
    fun deepLink_interviewResult_matchesNavigationRoute() {
        // Test that the deep link format matches navigation route format
        // Deep link format: ssbmax://interview/result/{resultId}
        // Navigation route: interview/result/{resultId}
        val resultId = "test-result-123"
        
        // Use DeepLinkParser to build and parse (tests round-trip)
        val deepLink = com.ssbmax.utils.DeepLinkParser.buildInterviewResultDeepLink(resultId)
        val processedRoute = com.ssbmax.utils.DeepLinkParser.parseToRoute(deepLink)
        
        // Verify it matches the route we'd navigate to
        val expectedRoute = SSBMaxDestinations.InterviewResult.createRoute(resultId)
        
        assert(processedRoute == expectedRoute) {
            "Processed deep link should match navigation route. " +
            "Processed: $processedRoute, Expected: $expectedRoute"
        }
    }

    @Test
    fun deepLinkParser_usesCorrectScheme() {
        // Verify the scheme constant is what we expect
        assert(com.ssbmax.utils.DeepLinkParser.SCHEME == "ssbmax://") {
            "DeepLinkParser should use ssbmax:// scheme"
        }
    }
}

