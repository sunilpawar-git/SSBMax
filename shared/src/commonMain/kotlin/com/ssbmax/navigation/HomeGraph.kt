package com.ssbmax.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.ssbmax.shared.domain.model.TestPhase
import com.ssbmax.shared.domain.model.TestType
import com.ssbmax.shared.ui.home.instructor.InstructorHomeScreen
import com.ssbmax.shared.ui.home.student.StudentHomeScreen

fun NavGraphBuilder.homeGraph(navController: NavHostController, onOpenDrawer: () -> Unit) {
    composable(SSBMaxDestinations.StudentHome.route) {
        val notYetPorted: (String) -> Unit = { screen ->
            navController.navigate(SSBMaxDestinations.NotYetPorted.createRoute(screen))
        }
        StudentHomeScreen(
            onNavigateToTopic = { topicId ->
                navController.navigate(SSBMaxDestinations.TopicScreen.createRoute(topicId))
            },
            onNavigateToPhaseDetail = { phase ->
                // Phase1Detail/Phase2Detail are real ported screens (this session) --
                // Topic (their own onNavigateToTopic target) isn't ported yet, so the
                // "start a new test" path still ends at the honest placeholder one
                // level deeper than before, not at this callback.
                if (phase == TestPhase.PHASE_1) {
                    navController.navigate(SSBMaxDestinations.Phase1Detail.route)
                } else {
                    navController.navigate(SSBMaxDestinations.Phase2Detail.route)
                }
            },
            onNavigateToStudy = {
                navController.navigate(SSBMaxDestinations.StudyMaterialsList.route)
            },
            onNavigateToSubmissions = {
                navController.navigate(SSBMaxDestinations.StudentSubmissions.route)
            },
            onNavigateToNotifications = {
                navController.navigate(SSBMaxDestinations.NotificationCenter.route)
            },
            onNavigateToMarketplace = {
                navController.navigate(SSBMaxDestinations.Marketplace.route)
            },
            onNavigateToAnalytics = {
                navController.navigate(SSBMaxDestinations.Analytics.route)
            },
            onNavigateToResult = { testType: TestType, sessionId: String ->
                // OIR, PPDT, TAT, WAT, SRT, SDT, and IO (Interview) are the test-type
                // result screens ported into commonMain/ui so far -- every other test
                // type's result screen still routes to the honest placeholder.
                when (testType) {
                    TestType.OIR -> navController.navigate(SSBMaxDestinations.OIRTestResult.createRoute(sessionId))
                    TestType.PPDT -> navController.navigate(SSBMaxDestinations.PPDTSubmissionResult.createRoute(sessionId))
                    TestType.TAT -> navController.navigate(SSBMaxDestinations.TATSubmissionResult.createRoute(sessionId))
                    TestType.WAT -> navController.navigate(SSBMaxDestinations.WATSubmissionResult.createRoute(sessionId))
                    TestType.SRT -> navController.navigate(SSBMaxDestinations.SRTSubmissionResult.createRoute(sessionId))
                    TestType.SD -> navController.navigate(SSBMaxDestinations.SDSubmissionResult.createRoute(sessionId))
                    TestType.IO -> navController.navigate(SSBMaxDestinations.InterviewResult.createRoute(sessionId))
                    else -> notYetPorted("TestResultScreen")
                }
            },
            onOpenDrawer = onOpenDrawer
        )
    }

    // Instructor vertical (this session): Students/Grading/Analytics/CreateBatch/
    // BatchDetail/StudentDetail all now real ported screens -- matches the
    // Android original's `InstructorNavGraph.kt` wiring exactly.
    // `onOpenDrawer` now opens the real ported drawer (this session's nav-chrome
    // work, see [SSBMaxNavHost]'s own `onOpenDrawer` parameter and
    // [com.ssbmax.shared.ui.components.SSBMaxAppScaffold]).
    composable(SSBMaxDestinations.InstructorHome.route) {
        InstructorHomeScreen(
            onNavigateToStudent = { studentId ->
                navController.navigate(SSBMaxDestinations.StudentDetail.createRoute(studentId))
            },
            onNavigateToGrading = {
                navController.navigate(SSBMaxDestinations.InstructorGrading.route)
            },
            onNavigateToBatchDetail = { batchId ->
                navController.navigate(SSBMaxDestinations.BatchDetail.createRoute(batchId))
            },
            onNavigateToCreateBatch = {
                navController.navigate(SSBMaxDestinations.CreateBatch.route)
            },
            onOpenDrawer = onOpenDrawer
        )
    }
}
