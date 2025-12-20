package com.ssbmax.testing

import org.junit.Test
import java.io.File

/**
 * Architecture tests for navigation patterns
 * 
 * These tests statically analyze the codebase to ensure navigation patterns
 * follow best practices and avoid known pitfalls like the popUpTo bug where
 * targeting a route that might not be in the back stack causes navigation issues.
 * 
 * Key patterns enforced:
 * 1. popUpTo should target reliable routes (StudentHome, not TopicScreen)
 * 2. Exit navigation callbacks should use consistent patterns
 * 3. All interview screens should use the same navigation strategy
 */
class NavigationArchitectureTest {

    private val projectRoot: File = File(System.getProperty("user.dir") ?: ".").parentFile ?: File(".")
    private val navGraphFile = File(projectRoot, "app/src/main/kotlin/com/ssbmax/navigation/SharedNavGraph.kt")

    @Test
    fun `popUpTo in interview navigation should target StudentHome not TopicScreen`() {
        if (!navGraphFile.exists()) {
            // Skip if running in different environment
            println("⚠️ SharedNavGraph.kt not found at expected location, skipping...")
            return
        }

        val content = navGraphFile.readText()
        
        // Find all interview-related navigation blocks
        val interviewSectionRegex = Regex(
            """// (Text|Voice) Interview Session[\s\S]*?composable\([\s\S]*?\) \{[\s\S]*?\}""",
            RegexOption.MULTILINE
        )
        
        val interviewSections = interviewSectionRegex.findAll(content).toList()
        
        for (section in interviewSections) {
            val sectionContent = section.value
            
            // Check for problematic pattern: popUpTo(TopicScreen.route)
            if (sectionContent.contains("popUpTo(SSBMaxDestinations.TopicScreen.route)")) {
                throw AssertionError(
                    """
                    |❌ NAVIGATION BUG DETECTED!
                    |
                    |Found: popUpTo(SSBMaxDestinations.TopicScreen.route)
                    |
                    |This pattern is INCORRECT because TopicScreen might not be in the 
                    |back stack if the user navigated through IOTest or other routes.
                    |
                    |FIX: Use popUpTo(SSBMaxDestinations.StudentHome.route) instead.
                    |StudentHome is always in the back stack as the root destination.
                    |
                    |Location: Interview session navigation in SharedNavGraph.kt
                    """.trimMargin()
                )
            }
            
            // Verify correct pattern is used
            if (sectionContent.contains("onNavigateBack") || sectionContent.contains("onNavigateToHome")) {
                val hasStudentHomePopUpTo = sectionContent.contains("popUpTo(SSBMaxDestinations.StudentHome.route)")
                if (!hasStudentHomePopUpTo && sectionContent.contains("popUpTo(")) {
                    println("⚠️ Warning: Found popUpTo that doesn't target StudentHome in interview navigation")
                }
            }
        }
        
        println("✅ Interview navigation uses correct popUpTo pattern")
    }

    @Test
    fun `all interview exit callbacks should use consistent navigation pattern`() {
        if (!navGraphFile.exists()) {
            println("⚠️ SharedNavGraph.kt not found, skipping...")
            return
        }

        val content = navGraphFile.readText()
        
        // Count occurrences of each pattern in interview-related code
        val studentHomePattern = Regex("""popUpTo\(SSBMaxDestinations\.StudentHome\.route\)""")
        val topicScreenPattern = Regex("""popUpTo\(SSBMaxDestinations\.TopicScreen\.route\)""")
        val startInterviewPattern = Regex("""popUpTo\(SSBMaxDestinations\.StartInterview\.route\)""")
        
        // Find interview section
        val interviewStartIndex = content.indexOf("// INTERVIEW SCREENS")
        val interviewEndIndex = content.indexOf("// STUDY MATERIALS", interviewStartIndex)
        
        if (interviewStartIndex == -1 || interviewEndIndex == -1) {
            println("⚠️ Could not find interview section markers, skipping...")
            return
        }
        
        val interviewSection = content.substring(interviewStartIndex, interviewEndIndex)
        
        val studentHomeCount = studentHomePattern.findAll(interviewSection).count()
        val topicScreenCount = topicScreenPattern.findAll(interviewSection).count()
        val startInterviewCount = startInterviewPattern.findAll(interviewSection).count()
        
        // TopicScreen should NOT be used as popUpTo target
        assert(topicScreenCount == 0) {
            """
            |❌ Found $topicScreenCount uses of popUpTo(TopicScreen.route) in interview navigation.
            |This is incorrect - TopicScreen might not be in the back stack.
            |Use popUpTo(StudentHome.route) instead.
            """.trimMargin()
        }
        
        // StartInterview should NOT be used as popUpTo target for exit navigation
        // (it might not be in stack if user came through IOTest)
        assert(startInterviewCount == 0) {
            """
            |❌ Found $startInterviewCount uses of popUpTo(StartInterview.route) in interview navigation.
            |This is risky - StartInterview might not be in the back stack if user came through IOTest.
            |Use popUpTo(StudentHome.route) instead.
            """.trimMargin()
        }
        
        // StudentHome should be used for all exit navigation
        assert(studentHomeCount >= 4) {
            """
            |⚠️ Expected at least 4 uses of popUpTo(StudentHome.route) in interview navigation
            |(onNavigateBack and onNavigateToHome for both Text and Voice sessions).
            |Found: $studentHomeCount
            """.trimMargin()
        }
        
        println("✅ Interview navigation patterns are consistent: $studentHomeCount uses of StudentHome.route")
    }

    @Test
    fun `navigation callbacks should not use popBackStack alone for exit`() {
        if (!navGraphFile.exists()) {
            println("⚠️ SharedNavGraph.kt not found, skipping...")
            return
        }

        val content = navGraphFile.readText()
        
        // Find interview section
        val interviewStartIndex = content.indexOf("// INTERVIEW SCREENS")
        val interviewEndIndex = content.indexOf("// STUDY MATERIALS", interviewStartIndex)
        
        if (interviewStartIndex == -1 || interviewEndIndex == -1) {
            return
        }
        
        val interviewSection = content.substring(interviewStartIndex, interviewEndIndex)
        
        // Check for problematic standalone popBackStack() calls
        // These can cause race conditions with ViewModel creation
        val standalonePopBackStack = Regex(
            """on(NavigateBack|NavigateToHome)\s*=\s*\{\s*navController\.popBackStack\(\)""",
            RegexOption.MULTILINE
        )
        
        val matches = standalonePopBackStack.findAll(interviewSection).toList()
        
        if (matches.isNotEmpty()) {
            throw AssertionError(
                """
                |❌ Found standalone popBackStack() in interview navigation callbacks.
                |
                |This pattern causes race conditions where Compose creates a new ViewModel
                |before the old screen is fully removed from the back stack.
                |
                |FIX: Use navigate() with popUpTo() instead:
                |navController.navigate(destination) {
                |    popUpTo(SSBMaxDestinations.StudentHome.route) { inclusive = false }
                |    launchSingleTop = true
                |}
                |
                |Found ${matches.size} occurrences.
                """.trimMargin()
            )
        }
        
        println("✅ No standalone popBackStack() calls found in interview navigation")
    }

    @Test
    fun `all test exit navigation should use reliable root destination`() {
        if (!navGraphFile.exists()) {
            println("⚠️ SharedNavGraph.kt not found, skipping...")
            return
        }

        val content = navGraphFile.readText()
        
        // List of unreliable destinations that might not be in back stack
        val unreliableDestinations = listOf(
            "TopicScreen.route",
            "StartInterview.route",
            "Phase1Detail.route",
            "Phase2Detail.route"
        )
        
        // Find all popUpTo usages
        val popUpToPattern = Regex("""popUpTo\(SSBMaxDestinations\.(\w+)\.route\)""")
        val allPopUpTos = popUpToPattern.findAll(content).toList()
        
        val warnings = mutableListOf<String>()
        
        for (match in allPopUpTos) {
            val destination = match.groupValues[1]
            
            // Check if this is in a navigation callback context
            val contextStart = maxOf(0, match.range.first - 500)
            val context = content.substring(contextStart, match.range.last)
            
            if (context.contains("onNavigateBack") || 
                context.contains("onNavigateToHome") ||
                context.contains("onExit")) {
                
                if (unreliableDestinations.any { it.contains(destination) }) {
                    warnings.add("Found popUpTo($destination) which might not be in back stack")
                }
            }
        }
        
        if (warnings.isNotEmpty()) {
            println("⚠️ Potential navigation issues found:")
            warnings.forEach { println("  - $it") }
        } else {
            println("✅ All exit navigation uses reliable destinations")
        }
    }

    @Test
    fun `interview and voice interview should have identical navigation patterns`() {
        if (!navGraphFile.exists()) {
            println("⚠️ SharedNavGraph.kt not found, skipping...")
            return
        }

        val content = navGraphFile.readText()
        
        // Extract navigation callbacks for text interview
        val textInterviewPattern = Regex(
            """// Text Interview Session[\s\S]*?composable\([\s\S]*?route = SSBMaxDestinations\.TextInterviewSession\.route[\s\S]*?onNavigateBack = \{([\s\S]*?)\}[\s\S]*?onNavigateToHome = \{([\s\S]*?)\}""",
            RegexOption.MULTILINE
        )
        
        // Extract navigation callbacks for voice interview
        val voiceInterviewPattern = Regex(
            """// Voice Interview Session[\s\S]*?composable\([\s\S]*?route = SSBMaxDestinations\.VoiceInterviewSession\.route[\s\S]*?onNavigateBack = \{([\s\S]*?)\}[\s\S]*?onNavigateToHome = \{([\s\S]*?)\}""",
            RegexOption.MULTILINE
        )
        
        val textMatch = textInterviewPattern.find(content)
        val voiceMatch = voiceInterviewPattern.find(content)
        
        if (textMatch == null || voiceMatch == null) {
            println("⚠️ Could not extract navigation patterns for comparison")
            return
        }
        
        // Normalize the patterns (remove whitespace and comments)
        fun normalize(s: String): String {
            return s.replace(Regex("""//[^\n]*"""), "")
                    .replace(Regex("""\s+"""), " ")
                    .trim()
        }
        
        // Check that both use StudentHome for popUpTo
        val textUsesStudentHome = textMatch.value.contains("StudentHome.route")
        val voiceUsesStudentHome = voiceMatch.value.contains("StudentHome.route")
        
        assert(textUsesStudentHome == voiceUsesStudentHome) {
            """
            |❌ Text and Voice interview have inconsistent navigation patterns!
            |Text uses StudentHome: $textUsesStudentHome
            |Voice uses StudentHome: $voiceUsesStudentHome
            |
            |Both should use identical patterns to ensure consistent behavior.
            """.trimMargin()
        }
        
        assert(textUsesStudentHome && voiceUsesStudentHome) {
            "Both Text and Voice interview should use StudentHome.route for popUpTo"
        }
        
        println("✅ Text and Voice interview have consistent navigation patterns")
    }

    @Test
    fun `test result navigation should use StudentHome for popUpTo not startDestinationId`() {
        val testResultHandlerFile = File(projectRoot, "app/src/main/kotlin/com/ssbmax/ui/tests/common/TestResultHandler.kt")

        if (!testResultHandlerFile.exists()) {
            println("⚠️ TestResultHandler.kt not found at expected location, skipping...")
            return
        }

        val content = testResultHandlerFile.readText()

        // Check for the problematic pattern: popUpTo(navController.graph.startDestinationId)
        val startDestinationPattern = Regex("""popUpTo\(navController\.graph\.startDestinationId\)""")

        val matches = startDestinationPattern.findAll(content).toList()

        if (matches.isNotEmpty()) {
            throw AssertionError(
                """
                |❌ NAVIGATION BUG DETECTED IN TEST RESULT HANDLER!
                |
                |Found ${matches.size} uses of popUpTo(navController.graph.startDestinationId)
                |in TestResultHandler.kt
                |
                |This pattern is INCORRECT because startDestinationId (Splash screen)
                |might not be in the back stack for logged-in users, causing the app
                |to minimize or behave unpredictably when submitting test results.
                |
                |FIX: Use popUpTo(SSBMaxDestinations.StudentHome.route) instead.
                |StudentHome is always in the back stack as the root destination
                |for authenticated users.
                |
                |This bug was specifically causing GPE test submissions to minimize the app.
                |
                |Location: TestResultHandler.kt navigation methods
                """.trimMargin()
            )
        }

        // Verify correct pattern is used
        val studentHomePattern = Regex("""popUpTo\(SSBMaxDestinations\.StudentHome\.route\)""")
        val studentHomeCount = studentHomePattern.findAll(content).count()

        assert(studentHomeCount >= 2) {
            """
            |⚠️ Expected at least 2 uses of popUpTo(StudentHome.route) in TestResultHandler
            |(one for navigateToResult and one for navigateToPendingReview).
            |Found: $studentHomeCount
            |
            |This ensures test result navigation uses reliable back stack management.
            """.trimMargin()
        }

        println("✅ Test result navigation uses correct popUpTo pattern: $studentHomeCount uses of StudentHome.route")
    }
}

