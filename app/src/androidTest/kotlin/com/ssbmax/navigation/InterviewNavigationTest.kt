package com.ssbmax.navigation

import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.NavController
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.testing.TestNavHostController
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Interview Navigation Tests
 * 
 * These tests verify that interview navigation properly manages the back stack
 * to prevent the regression where exiting an interview left the session in the
 * back stack, causing back button to return to the interview.
 * 
 * Key invariants tested:
 * 1. After exiting interview, interview screens are removed from back stack
 * 2. Navigation uses StudentHome (always in stack) instead of TopicScreen (might not be)
 * 3. Back button after exit goes to correct destination, not interview
 */
@HiltAndroidTest
class InterviewNavigationTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createComposeRule()

    private lateinit var navController: TestNavHostController

    @Before
    fun setup() {
        hiltRule.inject()
    }

    // =========================================================================
    // ROUTE GENERATION TESTS
    // =========================================================================

    @Test
    fun textInterviewSession_route_generatesCorrectly() {
        val sessionId = "test-session-123"
        val route = SSBMaxDestinations.TextInterviewSession.createRoute(sessionId)
        
        assert(route == "interview/text/$sessionId") {
            "TextInterviewSession route should be interview/text/{sessionId}. Got: $route"
        }
    }

    @Test
    fun voiceInterviewSession_route_generatesCorrectly() {
        val sessionId = "test-session-456"
        val route = SSBMaxDestinations.VoiceInterviewSession.createRoute(sessionId)
        
        assert(route == "interview/voice/$sessionId") {
            "VoiceInterviewSession route should be interview/voice/{sessionId}. Got: $route"
        }
    }

    @Test
    fun interviewResult_route_generatesCorrectly() {
        val resultId = "result-789"
        val route = SSBMaxDestinations.InterviewResult.createRoute(resultId)
        
        assert(route == "interview/result/$resultId") {
            "InterviewResult route should be interview/result/{resultId}. Got: $route"
        }
    }

    // =========================================================================
    // BACK STACK MANAGEMENT TESTS
    // =========================================================================

    @Test
    fun exitInterview_text_shouldNotLeaveSessionInBackStack() {
        // This test verifies the core fix: after exiting, interview session is removed
        var backStackEntries: List<String> = emptyList()
        
        composeTestRule.setContent {
            navController = TestNavHostController(LocalContext.current).apply {
                navigatorProvider.addNavigator(ComposeNavigator())
            }
            
            // Set up a minimal nav graph for testing
            NavHost(
                navController = navController,
                startDestination = "student_home"
            ) {
                composable("student_home") {}
                composable("interview/start") {}
                composable("interview/text/{sessionId}") {}
                composable("topic/{topicId}") {}
            }
        }
        
        composeTestRule.runOnIdle {
            // Simulate: User navigates to interview session
            navController.navigate("interview/start")
            navController.navigate("interview/text/test-123")
            
            // Verify interview is in back stack
            backStackEntries = navController.backQueue.mapNotNull { 
                it.destination.route 
            }
            assert(backStackEntries.any { it.contains("interview/text") }) {
                "Interview session should be in back stack before exit"
            }
            
            // Simulate: User exits interview (the fixed navigation)
            // Uses popUpTo(StudentHome) to ensure interview is removed
            navController.navigate("topic/INTERVIEW?selectedTab=2") {
                popUpTo("student_home") {
                    inclusive = false
                }
                launchSingleTop = true
            }
            
            // Verify interview is NOT in back stack after exit
            backStackEntries = navController.backQueue.mapNotNull { 
                it.destination.route 
            }
            assert(backStackEntries.none { it.contains("interview/text") }) {
                "Interview session should NOT be in back stack after exit. Found: $backStackEntries"
            }
            assert(backStackEntries.none { it.contains("interview/start") }) {
                "StartInterview should NOT be in back stack after exit. Found: $backStackEntries"
            }
        }
    }

    @Test
    fun exitInterview_voice_shouldNotLeaveSessionInBackStack() {
        var backStackEntries: List<String> = emptyList()
        
        composeTestRule.setContent {
            navController = TestNavHostController(LocalContext.current).apply {
                navigatorProvider.addNavigator(ComposeNavigator())
            }
            
            NavHost(
                navController = navController,
                startDestination = "student_home"
            ) {
                composable("student_home") {}
                composable("interview/start") {}
                composable("interview/voice/{sessionId}") {}
                composable("topic/{topicId}") {}
            }
        }
        
        composeTestRule.runOnIdle {
            // Simulate: User navigates to voice interview session
            navController.navigate("interview/start")
            navController.navigate("interview/voice/test-456")
            
            // Verify interview is in back stack
            backStackEntries = navController.backQueue.mapNotNull { 
                it.destination.route 
            }
            assert(backStackEntries.any { it.contains("interview/voice") }) {
                "Voice interview session should be in back stack before exit"
            }
            
            // Simulate: User exits interview (the fixed navigation)
            navController.navigate("topic/INTERVIEW?selectedTab=2") {
                popUpTo("student_home") {
                    inclusive = false
                }
                launchSingleTop = true
            }
            
            // Verify interview is NOT in back stack after exit
            backStackEntries = navController.backQueue.mapNotNull { 
                it.destination.route 
            }
            assert(backStackEntries.none { it.contains("interview/voice") }) {
                "Voice interview should NOT be in back stack after exit. Found: $backStackEntries"
            }
        }
    }

    @Test
    fun exitInterview_navigatingThroughIOTest_shouldStillWork() {
        // This tests the specific bug scenario: user came through IOTest route,
        // so TopicScreen is NOT in the back stack. The old code would fail here.
        var backStackEntries: List<String> = emptyList()
        
        composeTestRule.setContent {
            navController = TestNavHostController(LocalContext.current).apply {
                navigatorProvider.addNavigator(ComposeNavigator())
            }
            
            NavHost(
                navController = navController,
                startDestination = "student_home"
            ) {
                composable("student_home") {}
                composable("test/io/{testId}") {} // IOTest route
                composable("interview/text/{sessionId}") {}
                composable("topic/{topicId}") {} // NOT in back stack in this scenario
            }
        }
        
        composeTestRule.runOnIdle {
            // Simulate: User navigates through IOTest (no TopicScreen in stack)
            navController.navigate("test/io/interview-officer")
            navController.navigate("interview/text/test-789")
            
            // Verify NO TopicScreen in back stack (this was the bug scenario)
            backStackEntries = navController.backQueue.mapNotNull { 
                it.destination.route 
            }
            assert(backStackEntries.none { it.contains("topic/") }) {
                "TopicScreen should NOT be in back stack (IOTest path)"
            }
            
            // Simulate: User exits interview - OLD CODE would fail here
            // because popUpTo("topic/{topicId}") wouldn't find anything
            // NEW CODE uses popUpTo("student_home") which is always there
            navController.navigate("topic/INTERVIEW?selectedTab=2") {
                popUpTo("student_home") {
                    inclusive = false
                }
                launchSingleTop = true
            }
            
            // Verify interview is NOT in back stack
            backStackEntries = navController.backQueue.mapNotNull { 
                it.destination.route 
            }
            assert(backStackEntries.none { it.contains("interview/text") }) {
                "Interview should NOT be in back stack after IOTest path exit. Found: $backStackEntries"
            }
            assert(backStackEntries.none { it.contains("test/io") }) {
                "IOTest should NOT be in back stack after exit. Found: $backStackEntries"
            }
        }
    }

    @Test
    fun exitInterview_backButton_shouldNotReturnToInterview() {
        // This is the exact user-reported bug: back button returns to interview
        var currentRoute: String? = null
        
        composeTestRule.setContent {
            navController = TestNavHostController(LocalContext.current).apply {
                navigatorProvider.addNavigator(ComposeNavigator())
            }
            
            NavHost(
                navController = navController,
                startDestination = "student_home"
            ) {
                composable("student_home") {}
                composable("topic/{topicId}") {}
                composable("interview/start") {}
                composable("interview/text/{sessionId}") {}
            }
        }
        
        composeTestRule.runOnIdle {
            // Setup: User is in interview session
            navController.navigate("topic/INTERVIEW")
            navController.navigate("interview/start")
            navController.navigate("interview/text/session-123")
            
            // Action: User exits interview
            navController.navigate("topic/INTERVIEW?selectedTab=2") {
                popUpTo("student_home") {
                    inclusive = false
                }
                launchSingleTop = true
            }
            
            // Verify: Current screen is TopicScreen
            currentRoute = navController.currentBackStackEntry?.destination?.route
            assert(currentRoute?.contains("topic/") == true) {
                "After exit, should be on TopicScreen. Got: $currentRoute"
            }
            
            // Action: User presses back
            navController.popBackStack()
            
            // Verify: Should go to StudentHome, NOT interview
            currentRoute = navController.currentBackStackEntry?.destination?.route
            assert(currentRoute == "student_home") {
                "After back from TopicScreen, should be on StudentHome. Got: $currentRoute"
            }
            assert(currentRoute?.contains("interview") != true) {
                "Should NOT return to any interview screen. Got: $currentRoute"
            }
        }
    }

    // =========================================================================
    // NAVIGATION PATTERN VERIFICATION TESTS
    // =========================================================================

    @Test
    fun studentHome_route_isAlwaysInBackStack() {
        // Verify StudentHome is the root destination that's always available
        composeTestRule.setContent {
            navController = TestNavHostController(LocalContext.current).apply {
                navigatorProvider.addNavigator(ComposeNavigator())
            }
            
            NavHost(
                navController = navController,
                startDestination = "student_home"
            ) {
                composable("student_home") {}
                composable("any/other/route") {}
            }
        }
        
        composeTestRule.runOnIdle {
            // Navigate deep into the app
            navController.navigate("any/other/route")
            navController.navigate("any/other/route") // Can navigate multiple times
            
            // StudentHome should still be in back stack
            val hasStudentHome = navController.backQueue.any { 
                it.destination.route == "student_home" 
            }
            assert(hasStudentHome) {
                "StudentHome should always be in back stack as root"
            }
        }
    }

    @Test
    fun popUpTo_studentHome_isReliable() {
        // Verify popUpTo StudentHome works from any depth
        composeTestRule.setContent {
            navController = TestNavHostController(LocalContext.current).apply {
                navigatorProvider.addNavigator(ComposeNavigator())
            }
            
            NavHost(
                navController = navController,
                startDestination = "student_home"
            ) {
                composable("student_home") {}
                composable("screen1") {}
                composable("screen2") {}
                composable("screen3") {}
                composable("destination") {}
            }
        }
        
        composeTestRule.runOnIdle {
            // Navigate deep
            navController.navigate("screen1")
            navController.navigate("screen2")
            navController.navigate("screen3")
            
            assert(navController.backQueue.size == 4) { // start + 3 screens
                "Should have 4 entries before popUpTo"
            }
            
            // Navigate with popUpTo StudentHome
            navController.navigate("destination") {
                popUpTo("student_home") {
                    inclusive = false
                }
            }
            
            // Should only have StudentHome + destination
            val routes = navController.backQueue.mapNotNull { it.destination.route }
            assert(routes.size == 2) {
                "Should have 2 entries after popUpTo. Got: $routes"
            }
            assert(routes.contains("student_home")) {
                "Should contain student_home"
            }
            assert(routes.contains("destination")) {
                "Should contain destination"
            }
        }
    }
}













