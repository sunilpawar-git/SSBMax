package com.ssbmax.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.savedstate.read
import com.ssbmax.shared.domain.model.TestPhase
import com.ssbmax.shared.domain.model.TestType
import com.ssbmax.shared.domain.model.UserRole
import com.ssbmax.shared.ui.auth.LoginScreen
import com.ssbmax.shared.ui.auth.RoleSelectionScreen
import com.ssbmax.shared.ui.gto.gd.GDResultScreen
import com.ssbmax.shared.ui.gto.gd.GDTestScreen
import com.ssbmax.shared.ui.gto.gpe.GPEResultScreen
import com.ssbmax.shared.ui.gto.gpe.GPETestScreen
import com.ssbmax.shared.ui.gto.lecturette.LecturetteResultScreen
import com.ssbmax.shared.ui.gto.lecturette.LecturetteTestScreen
import com.ssbmax.shared.ui.home.instructor.InstructorHomeScreen
import com.ssbmax.shared.ui.home.student.StudentHomeScreen
import com.ssbmax.shared.ui.interviewresult.InterviewResultScreen
import com.ssbmax.shared.ui.interviewsession.InterviewSessionScreen
import com.ssbmax.shared.ui.interviewstart.StartInterviewScreen
import com.ssbmax.shared.ui.oir.OIRTestResultScreen
import com.ssbmax.shared.ui.oir.OIRTestScreen
import com.ssbmax.shared.ui.placeholder.NotYetPortedScreen
import com.ssbmax.shared.ui.ppdt.PPDTSubmissionResultScreen
import com.ssbmax.shared.ui.ppdt.PPDTTestScreen
import com.ssbmax.shared.ui.piq.PIQSubmissionResultScreen
import com.ssbmax.shared.ui.piq.PIQTestScreen
import com.ssbmax.shared.ui.profile.StudentProfileScreen
import com.ssbmax.shared.ui.profile.UserProfileScreen
import com.ssbmax.shared.ui.sdt.SDTSubmissionResultScreen
import com.ssbmax.shared.ui.sdt.SDTTestScreen
import com.ssbmax.shared.ui.settings.SettingsScreen
import com.ssbmax.shared.ui.settings.SubscriptionManagementScreen
import com.ssbmax.shared.ui.splash.SplashScreen
import com.ssbmax.shared.ui.srt.SRTSubmissionResultScreen
import com.ssbmax.shared.ui.srt.SRTTestScreen
import com.ssbmax.shared.ui.tat.TATSubmissionResultScreen
import com.ssbmax.shared.ui.tat.TATTestScreen
import com.ssbmax.shared.ui.wat.WATSubmissionResultScreen
import com.ssbmax.shared.ui.wat.WATTestScreen

/**
 * commonMain NavHost — Phase 5's first real multi-screen Compose
 * Multiplatform navigation graph, using `org.jetbrains.androidx.navigation`
 * (same `androidx.navigation`/`androidx.navigation.compose` package as the
 * Android-only artifact `app` still uses for its own graph — see this
 * phase's exit report for why the two coexist rather than one replacing the
 * other yet).
 *
 * Structure is the commonMain-portable equivalent of the Android-only
 * `app/.../navigation/{NavGraph,AuthNavGraph,Student/InstructorNavGraph}.kt`
 * set, scoped to exactly the screens ported into `shared/commonMain/ui` so
 * far: Splash -> Login -> RoleSelection -> Student/Instructor home ->
 * OIR test-taking + OIR result -> PPDT test-taking + PPDT result (this
 * session's addition). The other 55 screens (student submissions/study,
 * instructor grading/analytics/batches,
 * every other Phase 1/2 test flow, interview, GTO, etc.) are NOT reachable
 * from here — this is not an oversight, they simply haven't been ported yet
 * (Phase 5 continues). Every sub-navigation callback the two ported home
 * screens expose that targets an unported destination (topic detail, phase
 * detail, non-OIR result screens, notifications, marketplace, analytics,
 * grading, batches, student/batch detail) routes to the single
 * [SSBMaxDestinations.NotYetPorted] destination with the intended screen's
 * display name, rather than navigating to a route this graph never
 * registered (which would crash Nav Compose's destination lookup) or being
 * silently dropped — the graph is honestly navigable end-to-end today
 * without pretending unported work is done.
 *
 * OIR reachability gap, named explicitly: `StudentHomeScreen`'s
 * `onNavigateToPhaseDetail` (Phase 1 detail screen, where the Android app
 * lets a student actually launch a *new* OIR test) is NOT ported — it still
 * routes to `NotYetPorted`. So `OIRTest` is only reachable this session via
 * `OIRTestResultScreen`'s "Retake Test" button, and `OIRTestResult` only via
 * `StudentHomeScreen`'s "view past OIR result" tile (`onNavigateToResult`
 * with `TestType.OIR`). Starting a *first* OIR test from Student Home isn't
 * wired yet — that's gated on porting Phase1DetailScreen, out of this
 * session's scope.
 *
 * PPDT reachability gap, named explicitly (this session's addition): same
 * shape as OIR's gap above -- `onNavigateToPhaseDetail` isn't ported, so
 * there is no in-graph path to *start* a new `PPDTTest` at all (unlike OIR,
 * PPDT's own result screen has no "retake" callback either, so there isn't
 * even a retake path). `PPDTTest` is registered and fully functional if
 * navigated to directly, but nothing in this graph currently does so.
 * `PPDTSubmissionResult` is reachable via `StudentHomeScreen`'s "view past
 * PPDT result" tile (`onNavigateToResult` with `TestType.PPDT`). Also see
 * [com.ssbmax.shared.domain.service.SubmissionAnalysisTrigger]'s doc
 * comment: a submission made via `PPDTTest` today persists correctly but
 * will not be AI-analyzed, so `PPDTSubmissionResult` will show "pending
 * analysis" indefinitely for it.
 *
 * TAT reachability gap, named explicitly (this session's addition): exact
 * same shape as PPDT's gap immediately above -- `onNavigateToPhaseDetail`
 * isn't ported, so there is no in-graph path to *start* a new `TATTest`, and
 * the Android original's `TATSubmissionResultScreen` has no "retake"
 * callback either. `TATTest` is registered and fully functional if navigated
 * to directly; `TATSubmissionResult` is reachable via `StudentHomeScreen`'s
 * "view past TAT result" tile (`onNavigateToResult` with `TestType.TAT`).
 * Same [com.ssbmax.shared.domain.service.SubmissionAnalysisTrigger] caveat
 * applies -- a TAT submission persists but will not be AI-analyzed.
 *
 * WAT reachability gap, named explicitly (this session's addition): exact
 * same shape as TAT's gap immediately above -- `onNavigateToPhaseDetail`
 * isn't ported, so there is no in-graph path to *start* a new `WATTest`, and
 * the Android original's `WATSubmissionResultScreen` has no "retake"
 * callback either. `WATTest` is registered and fully functional if navigated
 * to directly; `WATSubmissionResult` is reachable via `StudentHomeScreen`'s
 * "view past WAT result" tile (`onNavigateToResult` with `TestType.WAT`).
 * Same [com.ssbmax.shared.domain.service.SubmissionAnalysisTrigger] caveat
 * applies -- a WAT submission persists but will not be AI-analyzed.
 *
 * SRT reachability gap, named explicitly (this session's addition): exact
 * same shape as WAT's gap immediately above -- `onNavigateToPhaseDetail`
 * isn't ported, so there is no in-graph path to *start* a new `SRTTest`, and
 * the Android original's `SRTSubmissionResultScreen` has no "retake"
 * callback either. `SRTTest` is registered and fully functional if navigated
 * to directly; `SRTSubmissionResult` is reachable via `StudentHomeScreen`'s
 * "view past SRT result" tile (`onNavigateToResult` with `TestType.SRT`).
 * Same [com.ssbmax.shared.domain.service.SubmissionAnalysisTrigger] caveat
 * applies -- an SRT submission persists but will not be AI-analyzed.
 *
 * SDT reachability gap, named explicitly (this session's addition): exact
 * same shape as SRT's gap immediately above -- `onNavigateToPhaseDetail`
 * isn't ported, so there is no in-graph path to *start* a new `SDTest`, and
 * the Android original's `SDTSubmissionResultScreen` has no "retake"
 * callback either. `SDTest` is registered and fully functional if navigated
 * to directly; `SDSubmissionResult` is reachable via `StudentHomeScreen`'s
 * "view past SDT result" tile (`onNavigateToResult` with `TestType.SD` --
 * note the enum/route naming mismatch: the domain model calls this test type
 * `SD`, `SSBMaxDestinations` calls the routes `SDTest`/`SDSubmissionResult`,
 * but the actual code package/screens are `sdt`/`SDT*`, matching the
 * Android original's own naming -- reconciled here, not introduced by this
 * port). Same [com.ssbmax.shared.domain.service.SubmissionAnalysisTrigger]
 * caveat applies -- an SDT submission persists but will not be AI-analyzed.
 *
 * Used directly by the iOS entry point ([com.ssbmax.shared.ui.MainViewController]),
 * which has no other nav graph. On Android, this graph is NOT yet the
 * `app` module's production entry point (`SSBMaxApp`/`MainActivity` still
 * use the old Android-only graph, which now calls into these same ported
 * `shared` composables for Splash/Login/RoleSelection/Student+InstructorHome
 * — see `app/.../navigation/{AuthNavGraph,Student/InstructorNavGraph}.kt`)
 * — swapping the whole app over to this graph is gated on porting the
 * remaining screens, not this phase.
 */
@Composable
fun SSBMaxNavHost(
    navController: NavHostController = rememberNavController(),
    startDestination: String = SSBMaxDestinations.Splash.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(SSBMaxDestinations.Splash.route) {
            SplashScreen(
                onNavigateToLogin = {
                    navController.navigate(SSBMaxDestinations.Login.route) {
                        popUpTo(SSBMaxDestinations.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToHome = { isStudent ->
                    val destination = if (isStudent) {
                        SSBMaxDestinations.StudentHome.route
                    } else {
                        SSBMaxDestinations.InstructorHome.route
                    }
                    navController.navigate(destination) {
                        popUpTo(SSBMaxDestinations.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToRoleSelection = {
                    navController.navigate(SSBMaxDestinations.RoleSelection.route) {
                        popUpTo(SSBMaxDestinations.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToProfileOnboarding = {
                    // UserProfileScreen isn't ported yet (Phase 5 continuation) --
                    // land on the honest placeholder rather than silently dropping
                    // the onboarding step.
                    navController.navigate(SSBMaxDestinations.UserProfile.route) {
                        popUpTo(SSBMaxDestinations.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(SSBMaxDestinations.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(SSBMaxDestinations.StudentHome.route) {
                        popUpTo(SSBMaxDestinations.Login.route) { inclusive = true }
                    }
                },
                onNeedsRoleSelection = {
                    navController.navigate(SSBMaxDestinations.RoleSelection.route)
                }
            )
        }

        composable(SSBMaxDestinations.RoleSelection.route) {
            RoleSelectionScreen(
                onRoleSelected = { role ->
                    val destination = when {
                        role == UserRole.INSTRUCTOR -> SSBMaxDestinations.InstructorHome.route
                        else -> SSBMaxDestinations.StudentHome.route
                    }
                    navController.navigate(destination) {
                        popUpTo(SSBMaxDestinations.RoleSelection.route) { inclusive = true }
                    }
                }
            )
        }

        composable(SSBMaxDestinations.StudentHome.route) {
            val notYetPorted: (String) -> Unit = { screen ->
                navController.navigate(SSBMaxDestinations.NotYetPorted.createRoute(screen))
            }
            StudentHomeScreen(
                onNavigateToTopic = { notYetPorted("TopicScreen") },
                onNavigateToPhaseDetail = { phase ->
                    notYetPorted(if (phase == TestPhase.PHASE_1) "Phase1DetailScreen" else "Phase2DetailScreen")
                },
                onNavigateToStudy = { notYetPorted("StudyMaterialsScreen") },
                onNavigateToSubmissions = { notYetPorted("SubmissionsListScreen") },
                onNavigateToNotifications = { notYetPorted("NotificationCenterScreen") },
                onNavigateToMarketplace = { notYetPorted("MarketplaceScreen") },
                onNavigateToAnalytics = { notYetPorted("AnalyticsScreen") },
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
                onOpenDrawer = { notYetPorted("NavigationDrawer") }
            )
        }

        composable(
            route = SSBMaxDestinations.OIRTest.route,
            arguments = listOf(navArgument("testId") { type = NavType.StringType })
        ) { backStackEntry ->
            val testId = backStackEntry.arguments?.read { getStringOrNull("testId") } ?: "oir_standard"
            OIRTestScreen(
                onTestComplete = { submissionId, _ ->
                    navController.navigate(SSBMaxDestinations.OIRTestResult.createRoute(submissionId)) {
                        popUpTo(SSBMaxDestinations.OIRTest.createRoute(testId)) { inclusive = true }
                    }
                },
                onNavigateBack = { navController.navigateUp() }
            )
        }

        composable(
            route = SSBMaxDestinations.OIRTestResult.route,
            arguments = listOf(navArgument("sessionId") { type = NavType.StringType })
        ) { backStackEntry ->
            val submissionId = backStackEntry.arguments?.read { getStringOrNull("sessionId") } ?: ""
            OIRTestResultScreen(
                submissionId = submissionId,
                onNavigateHome = {
                    navController.navigate(SSBMaxDestinations.StudentHome.route) {
                        popUpTo(SSBMaxDestinations.StudentHome.route) { inclusive = true }
                    }
                },
                onRetakeTest = {
                    navController.navigate(SSBMaxDestinations.OIRTest.createRoute("oir_standard")) {
                        popUpTo(SSBMaxDestinations.OIRTestResult.createRoute(submissionId)) { inclusive = true }
                    }
                },
                onReviewAnswers = {
                    // Review-answers screen isn't ported yet (same gap as the Android
                    // original, which also just has a `// TODO` here) -- not a new gap
                    // introduced by this port.
                }
            )
        }

        composable(
            route = SSBMaxDestinations.PPDTTest.route,
            arguments = listOf(navArgument("testId") { type = NavType.StringType })
        ) { backStackEntry ->
            val testId = backStackEntry.arguments?.read { getStringOrNull("testId") } ?: "ppdt_standard"
            PPDTTestScreen(
                testId = testId,
                onTestComplete = { submissionId, _ ->
                    navController.navigate(SSBMaxDestinations.PPDTSubmissionResult.createRoute(submissionId)) {
                        popUpTo(SSBMaxDestinations.PPDTTest.createRoute(testId)) { inclusive = true }
                    }
                },
                onNavigateBack = { navController.navigateUp() }
            )
        }

        // PPDT reachability gap, named explicitly (same shape as the OIR gap
        // documented above): the Android original's `PPDTSubmissionResultScreen`
        // has no "retake test" callback either (only `onNavigateHome`), so unlike
        // OIR's result screen, there is genuinely no in-graph path back to
        // `PPDTTest` today -- it's reachable only via `StudentHomeScreen`'s
        // "view past PPDT result" tile landing on `PPDTSubmissionResult`, which
        // itself has no forward link to `PPDTTest`. The route is still registered
        // here (not omitted) so a future direct-navigation caller or deep link has
        // somewhere real to land, consistent with this graph's own "no crash on
        // unregistered destination" principle.
        composable(
            route = SSBMaxDestinations.PPDTSubmissionResult.route,
            arguments = listOf(navArgument("submissionId") { type = NavType.StringType })
        ) { backStackEntry ->
            val submissionId = backStackEntry.arguments?.read { getStringOrNull("submissionId") } ?: ""
            PPDTSubmissionResultScreen(
                submissionId = submissionId,
                onNavigateHome = {
                    navController.navigate(SSBMaxDestinations.StudentHome.route) {
                        popUpTo(SSBMaxDestinations.StudentHome.route) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = SSBMaxDestinations.TATTest.route,
            arguments = listOf(navArgument("testId") { type = NavType.StringType })
        ) { backStackEntry ->
            val testId = backStackEntry.arguments?.read { getStringOrNull("testId") } ?: "tat_standard"
            TATTestScreen(
                testId = testId,
                onTestComplete = { submissionId, _ ->
                    navController.navigate(SSBMaxDestinations.TATSubmissionResult.createRoute(submissionId)) {
                        popUpTo(SSBMaxDestinations.TATTest.createRoute(testId)) { inclusive = true }
                    }
                },
                onNavigateBack = { navController.navigateUp() }
            )
        }

        // TAT reachability gap, named explicitly (same shape as the PPDT gap
        // documented above): the Android original's `TATSubmissionResultScreen`
        // has no "retake test" callback either (only `onNavigateHome`), so there is
        // genuinely no in-graph path back to `TATTest` today -- it's reachable only
        // via `StudentHomeScreen`'s "view past TAT result" tile landing on
        // `TATSubmissionResult`, which itself has no forward link to `TATTest`. The
        // route is still registered here (not omitted) so a future direct-navigation
        // caller or deep link has somewhere real to land, consistent with this
        // graph's own "no crash on unregistered destination" principle.
        composable(
            route = SSBMaxDestinations.TATSubmissionResult.route,
            arguments = listOf(navArgument("submissionId") { type = NavType.StringType })
        ) { backStackEntry ->
            val submissionId = backStackEntry.arguments?.read { getStringOrNull("submissionId") } ?: ""
            TATSubmissionResultScreen(
                submissionId = submissionId,
                onNavigateHome = {
                    navController.navigate(SSBMaxDestinations.StudentHome.route) {
                        popUpTo(SSBMaxDestinations.StudentHome.route) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = SSBMaxDestinations.WATTest.route,
            arguments = listOf(navArgument("testId") { type = NavType.StringType })
        ) { backStackEntry ->
            val testId = backStackEntry.arguments?.read { getStringOrNull("testId") } ?: "wat_standard"
            WATTestScreen(
                testId = testId,
                onTestComplete = { submissionId, _ ->
                    navController.navigate(SSBMaxDestinations.WATSubmissionResult.createRoute(submissionId)) {
                        popUpTo(SSBMaxDestinations.WATTest.createRoute(testId)) { inclusive = true }
                    }
                },
                onNavigateBack = { navController.navigateUp() }
            )
        }

        // WAT reachability gap, named explicitly (same shape as the TAT gap
        // documented above): the Android original's `WATSubmissionResultScreen`
        // has no "retake test" callback either (only `onNavigateHome`), so there is
        // genuinely no in-graph path back to `WATTest` today -- it's reachable only
        // via `StudentHomeScreen`'s "view past WAT result" tile landing on
        // `WATSubmissionResult`, which itself has no forward link to `WATTest`. The
        // route is still registered here (not omitted) so a future direct-navigation
        // caller or deep link has somewhere real to land, consistent with this
        // graph's own "no crash on unregistered destination" principle.
        composable(
            route = SSBMaxDestinations.WATSubmissionResult.route,
            arguments = listOf(navArgument("submissionId") { type = NavType.StringType })
        ) { backStackEntry ->
            val submissionId = backStackEntry.arguments?.read { getStringOrNull("submissionId") } ?: ""
            WATSubmissionResultScreen(
                submissionId = submissionId,
                onNavigateHome = {
                    navController.navigate(SSBMaxDestinations.StudentHome.route) {
                        popUpTo(SSBMaxDestinations.StudentHome.route) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = SSBMaxDestinations.SRTTest.route,
            arguments = listOf(navArgument("testId") { type = NavType.StringType })
        ) { backStackEntry ->
            val testId = backStackEntry.arguments?.read { getStringOrNull("testId") } ?: "srt_standard"
            SRTTestScreen(
                testId = testId,
                onTestComplete = { submissionId, _ ->
                    navController.navigate(SSBMaxDestinations.SRTSubmissionResult.createRoute(submissionId)) {
                        popUpTo(SSBMaxDestinations.SRTTest.createRoute(testId)) { inclusive = true }
                    }
                },
                onNavigateBack = { navController.navigateUp() }
            )
        }

        // SRT reachability gap, named explicitly (same shape as the WAT gap
        // documented above): the Android original's `SRTSubmissionResultScreen`
        // has no "retake test" callback either (only `onNavigateHome`), so there is
        // genuinely no in-graph path back to `SRTTest` today -- it's reachable only
        // via `StudentHomeScreen`'s "view past SRT result" tile landing on
        // `SRTSubmissionResult`, which itself has no forward link to `SRTTest`. The
        // route is still registered here (not omitted) so a future direct-navigation
        // caller or deep link has somewhere real to land, consistent with this
        // graph's own "no crash on unregistered destination" principle.
        composable(
            route = SSBMaxDestinations.SRTSubmissionResult.route,
            arguments = listOf(navArgument("submissionId") { type = NavType.StringType })
        ) { backStackEntry ->
            val submissionId = backStackEntry.arguments?.read { getStringOrNull("submissionId") } ?: ""
            SRTSubmissionResultScreen(
                submissionId = submissionId,
                onNavigateHome = {
                    navController.navigate(SSBMaxDestinations.StudentHome.route) {
                        popUpTo(SSBMaxDestinations.StudentHome.route) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = SSBMaxDestinations.SDTest.route,
            arguments = listOf(navArgument("testId") { type = NavType.StringType })
        ) { backStackEntry ->
            val testId = backStackEntry.arguments?.read { getStringOrNull("testId") } ?: "sdt_standard"
            SDTTestScreen(
                testId = testId,
                onTestComplete = { submissionId, _ ->
                    navController.navigate(SSBMaxDestinations.SDSubmissionResult.createRoute(submissionId)) {
                        popUpTo(SSBMaxDestinations.SDTest.createRoute(testId)) { inclusive = true }
                    }
                },
                onNavigateBack = { navController.navigateUp() }
            )
        }

        // SDT reachability gap, named explicitly (same shape as the SRT gap
        // documented above): the Android original's `SDTSubmissionResultScreen`
        // has no "retake test" callback either (only `onNavigateHome`), so there is
        // genuinely no in-graph path back to `SDTest` today -- it's reachable only
        // via `StudentHomeScreen`'s "view past SDT result" tile landing on
        // `SDSubmissionResult`, which itself has no forward link to `SDTest`. The
        // route is still registered here (not omitted) so a future direct-navigation
        // caller or deep link has somewhere real to land, consistent with this
        // graph's own "no crash on unregistered destination" principle.
        composable(
            route = SSBMaxDestinations.SDSubmissionResult.route,
            arguments = listOf(navArgument("submissionId") { type = NavType.StringType })
        ) { backStackEntry ->
            val submissionId = backStackEntry.arguments?.read { getStringOrNull("submissionId") } ?: ""
            SDTSubmissionResultScreen(
                submissionId = submissionId,
                onNavigateHome = {
                    navController.navigate(SSBMaxDestinations.StudentHome.route) {
                        popUpTo(SSBMaxDestinations.StudentHome.route) { inclusive = true }
                    }
                }
            )
        }

        // PIQ (Personal Information Questionnaire) -- untimed ~90-field form,
        // not a scored test; unlike every other test type in this graph, its
        // own screen calls `onNavigateToResult(submissionId)` directly (no
        // `onTestComplete(submissionId, subscriptionType)` -- PIQ has no
        // downstream use for subscriptionType, see PIQTestViewModel.kt's doc
        // comment). Same reachability gap as SRT/SDT above: no in-graph path
        // back to `PIQTest` from `PIQSubmissionResult` (no retake callback in
        // the Android original either) -- both routes registered regardless.
        composable(
            route = SSBMaxDestinations.PIQTest.route,
            arguments = listOf(navArgument("testId") { type = NavType.StringType })
        ) { backStackEntry ->
            val testId = backStackEntry.arguments?.read { getStringOrNull("testId") } ?: "piq_standard"
            PIQTestScreen(
                testId = testId,
                onNavigateBack = { navController.navigateUp() },
                onNavigateToResult = { submissionId ->
                    navController.navigate(SSBMaxDestinations.PIQSubmissionResult.createRoute(submissionId)) {
                        popUpTo(SSBMaxDestinations.PIQTest.createRoute(testId)) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = SSBMaxDestinations.PIQSubmissionResult.route,
            arguments = listOf(navArgument("submissionId") { type = NavType.StringType })
        ) { backStackEntry ->
            val submissionId = backStackEntry.arguments?.read { getStringOrNull("submissionId") } ?: ""
            PIQSubmissionResultScreen(
                submissionId = submissionId,
                onNavigateHome = {
                    navController.navigate(SSBMaxDestinations.StudentHome.route) {
                        popUpTo(SSBMaxDestinations.StudentHome.route) { inclusive = true }
                    }
                }
            )
        }

        // GTO - Group Discussion (this session's addition). Same reachability gap as
        // every other test type above: `onNavigateToPhaseDetail` isn't ported, so there
        // is no in-graph path to *start* a new `GTOGDTest`, and the Android original's
        // `GDResultScreen` has no "retake" callback either. Both routes registered
        // regardless, same "no crash on unregistered destination" principle. Unlike
        // OIR/PPDT/TAT/WAT/SRT/SDT, `StudentHomeScreen.onNavigateToResult` doesn't
        // switch on TestType.GTO_GD yet (that switch is a StudentHomeScreen change, out
        // of this session's scope) -- so GDResult is reachable only via direct
        // navigation/deep link, not from Student Home, until that's wired.
        composable(
            route = SSBMaxDestinations.GTOGDTest.route,
            arguments = listOf(navArgument("testId") { type = NavType.StringType })
        ) { backStackEntry ->
            val testId = backStackEntry.arguments?.read { getStringOrNull("testId") } ?: "gto_gd_standard"
            GDTestScreen(
                testId = testId,
                onTestComplete = { submissionId, _ ->
                    navController.navigate(SSBMaxDestinations.GTOGDResult.createRoute(submissionId)) {
                        popUpTo(SSBMaxDestinations.GTOGDTest.createRoute(testId)) { inclusive = true }
                    }
                },
                onNavigateBack = { navController.navigateUp() }
            )
        }

        composable(
            route = SSBMaxDestinations.GTOGDResult.route,
            arguments = listOf(navArgument("submissionId") { type = NavType.StringType })
        ) { backStackEntry ->
            val submissionId = backStackEntry.arguments?.read { getStringOrNull("submissionId") } ?: ""
            GDResultScreen(
                submissionId = submissionId,
                onNavigateHome = {
                    navController.navigate(SSBMaxDestinations.StudentHome.route) {
                        popUpTo(SSBMaxDestinations.StudentHome.route) { inclusive = true }
                    }
                }
            )
        }

        // GTO - Lecturette (this session's addition). Same shape as GD's gap above.
        composable(
            route = SSBMaxDestinations.GTOLecturetteTest.route,
            arguments = listOf(navArgument("testId") { type = NavType.StringType })
        ) { backStackEntry ->
            val testId = backStackEntry.arguments?.read { getStringOrNull("testId") } ?: "gto_lecturette_standard"
            LecturetteTestScreen(
                testId = testId,
                onTestComplete = { submissionId, _ ->
                    navController.navigate(SSBMaxDestinations.GTOLecturetteResult.createRoute(submissionId)) {
                        popUpTo(SSBMaxDestinations.GTOLecturetteTest.createRoute(testId)) { inclusive = true }
                    }
                },
                onNavigateBack = { navController.navigateUp() }
            )
        }

        composable(
            route = SSBMaxDestinations.GTOLecturetteResult.route,
            arguments = listOf(navArgument("submissionId") { type = NavType.StringType })
        ) { backStackEntry ->
            val submissionId = backStackEntry.arguments?.read { getStringOrNull("submissionId") } ?: ""
            LecturetteResultScreen(
                submissionId = submissionId,
                onNavigateHome = {
                    navController.navigate(SSBMaxDestinations.StudentHome.route) {
                        popUpTo(SSBMaxDestinations.StudentHome.route) { inclusive = true }
                    }
                }
            )
        }

        // GTO - Group Planning Exercise (this session's addition). Same shape as
        // GD's/Lecturette's gap above.
        composable(
            route = SSBMaxDestinations.GTOGPETest.route,
            arguments = listOf(navArgument("testId") { type = NavType.StringType })
        ) { backStackEntry ->
            val testId = backStackEntry.arguments?.read { getStringOrNull("testId") } ?: "gpe_standard"
            GPETestScreen(
                testId = testId,
                onTestComplete = { submissionId, _ ->
                    navController.navigate(SSBMaxDestinations.GTOGPEResult.createRoute(submissionId)) {
                        popUpTo(SSBMaxDestinations.GTOGPETest.createRoute(testId)) { inclusive = true }
                    }
                },
                onNavigateBack = { navController.navigateUp() }
            )
        }

        composable(
            route = SSBMaxDestinations.GTOGPEResult.route,
            arguments = listOf(navArgument("submissionId") { type = NavType.StringType })
        ) { backStackEntry ->
            val submissionId = backStackEntry.arguments?.read { getStringOrNull("submissionId") } ?: ""
            GPEResultScreen(
                submissionId = submissionId,
                onNavigateHome = {
                    navController.navigate(SSBMaxDestinations.StudentHome.route) {
                        popUpTo(SSBMaxDestinations.StudentHome.route) { inclusive = true }
                    }
                }
            )
        }

        // Interview vertical (start/session/result, this session). Reachability gap,
        // named explicitly (same shape as every other vertical above): `StudentHomeScreen`
        // has no "start new interview" callback at all (the Android original's own
        // `StartInterviewScreen` is reached via a route not exposed through any ported
        // home-screen callback yet), so `StartInterview` is registered but not reachable
        // from Student Home -- only via direct navigation/deep link. `InterviewResult` IS
        // reachable via `StudentHomeScreen`'s "view past interview result" tile
        // (`onNavigateToResult` with `TestType.IO`, wired above). See
        // [com.ssbmax.shared.presentation.interviewsession.InterviewCompleter]'s doc
        // comment for this vertical's own async-analysis finding: a completed interview
        // session persists correctly but is not yet AI-analyzed through this path, same
        // as every other Phase 5 vertical. This vertical's mic/audio-recording
        // investigation found no audio recording anywhere in the Android original (plain
        // text input + platform keyboard voice-to-text), so no new platform shim was
        // needed here at all.
        composable(SSBMaxDestinations.StartInterview.route) {
            StartInterviewScreen(
                onNavigateBack = { navController.navigateUp() },
                onNavigateToSession = { sessionId ->
                    navController.navigate(SSBMaxDestinations.VoiceInterviewSession.createRoute(sessionId)) {
                        popUpTo(SSBMaxDestinations.StartInterview.route) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = SSBMaxDestinations.VoiceInterviewSession.route,
            arguments = listOf(navArgument("sessionId") { type = NavType.StringType })
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.read { getStringOrNull("sessionId") } ?: ""
            InterviewSessionScreen(
                sessionId = sessionId,
                onNavigateBack = { navController.navigateUp() },
                onNavigateToResult = { resultId ->
                    navController.navigate(SSBMaxDestinations.InterviewResult.createRoute(resultId)) {
                        popUpTo(SSBMaxDestinations.VoiceInterviewSession.createRoute(sessionId)) { inclusive = true }
                    }
                },
                onNavigateToHome = {
                    navController.navigate(SSBMaxDestinations.StudentHome.route) {
                        popUpTo(SSBMaxDestinations.StudentHome.route) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = SSBMaxDestinations.InterviewResult.route,
            arguments = listOf(navArgument("resultId") { type = NavType.StringType })
        ) { backStackEntry ->
            val resultId = backStackEntry.arguments?.read { getStringOrNull("resultId") } ?: ""
            InterviewResultScreen(
                resultId = resultId,
                onNavigateBack = {
                    navController.navigate(SSBMaxDestinations.StudentHome.route) {
                        popUpTo(SSBMaxDestinations.StudentHome.route) { inclusive = true }
                    }
                }
            )
        }

        composable(SSBMaxDestinations.InstructorHome.route) {
            val notYetPorted: (String) -> Unit = { screen ->
                navController.navigate(SSBMaxDestinations.NotYetPorted.createRoute(screen))
            }
            InstructorHomeScreen(
                onNavigateToStudent = { notYetPorted("StudentDetailScreen") },
                onNavigateToGrading = { notYetPorted("GradingQueueScreen") },
                onNavigateToBatchDetail = { notYetPorted("BatchDetailScreen") },
                onNavigateToCreateBatch = { notYetPorted("CreateBatchScreen") },
                onOpenDrawer = { notYetPorted("NavigationDrawer") }
            )
        }

        // Profile vertical (this session). UserProfileScreen (create/edit form)
        // replaces the earlier NotYetPortedScreen placeholder -- Splash's
        // onNavigateToProfileOnboarding still lands here (isOnboarding defaults
        // false; the onboarding query-string variant `createOnboardingRoute()`
        // is not parsed by this route registration, a named gap: Nav Compose
        // needs the query param declared in the route pattern itself to bind
        // it, and nothing in this graph currently calls that variant anyway --
        // Splash navigates to the plain `UserProfile.route`).
        composable(SSBMaxDestinations.UserProfile.route) {
            UserProfileScreen(
                onNavigateBack = { navController.navigateUp() },
                onProfileSaved = {
                    navController.navigate(SSBMaxDestinations.StudentHome.route) {
                        popUpTo(SSBMaxDestinations.UserProfile.route) { inclusive = true }
                    }
                }
            )
        }

        // StudentProfile (summary/stats display, distinct from UserProfile's
        // create/edit form above). Settings/achievements/history callbacks
        // route to Settings (now ported, this session) and the honest
        // placeholder respectively -- there is no ported AchievementsScreen/
        // TestHistoryScreen yet.
        composable(SSBMaxDestinations.StudentProfile.route) {
            val notYetPorted: (String) -> Unit = { screen ->
                navController.navigate(SSBMaxDestinations.NotYetPorted.createRoute(screen))
            }
            StudentProfileScreen(
                onNavigateToSettings = { navController.navigate(SSBMaxDestinations.Settings.route) },
                onNavigateToAchievements = { notYetPorted("AchievementsScreen") },
                onNavigateToHistory = { notYetPorted("TestHistoryScreen") }
            )
        }

        // Settings vertical (this session). onNavigateToFAQ/onNavigateToUpgrade
        // route to the honest placeholder -- there is no ported FAQScreen/
        // UpgradeScreen yet; onNavigateToSubscriptionManagement routes to the
        // real SubscriptionManagementScreen registered below.
        composable(SSBMaxDestinations.Settings.route) {
            val notYetPorted: (String) -> Unit = { screen ->
                navController.navigate(SSBMaxDestinations.NotYetPorted.createRoute(screen))
            }
            SettingsScreen(
                onNavigateBack = { navController.navigateUp() },
                onNavigateToFAQ = { notYetPorted("FAQScreen") },
                onNavigateToUpgrade = { notYetPorted("UpgradeScreen") },
                onNavigateToSubscriptionManagement = {
                    navController.navigate(SSBMaxDestinations.SubscriptionManagement.route)
                }
            )
        }

        // Subscription Management (this session). onUpgrade has no ported
        // real-billing destination yet (PlayBillingClient/StoreKitBillingClient
        // exist as Phase 4 shims but are NOT wired into SubscriptionManager --
        // a known, separately-tracked gap; see this plan's own risk register) --
        // routes to the honest placeholder rather than pretending a purchase
        // flow exists.
        composable(SSBMaxDestinations.SubscriptionManagement.route) {
            SubscriptionManagementScreen(
                onNavigateBack = { navController.navigateUp() },
                onUpgrade = { tier ->
                    navController.navigate(SSBMaxDestinations.NotYetPorted.createRoute("UpgradeFlow(${tier.name})"))
                }
            )
        }

        composable(
            route = SSBMaxDestinations.NotYetPorted.route,
            arguments = listOf(
                navArgument("screen") {
                    type = NavType.StringType
                    defaultValue = "Screen"
                }
            )
        ) { backStackEntry ->
            // `NavBackStackEntry.arguments` is a multiplatform `SavedState` (androidx.savedstate),
            // not the Android-only `Bundle.getString(...)` API -- read via the `SavedStateReader`
            // extension, which is the actual common-target-safe accessor (verified against
            // androidx.savedstate 1.3.0-beta01 sources; `Bundle.getString` doesn't compile for iOS).
            val screen = backStackEntry.arguments?.read { getStringOrNull("screen") } ?: "Screen"
            NotYetPortedScreen(screen)
        }
    }
}
