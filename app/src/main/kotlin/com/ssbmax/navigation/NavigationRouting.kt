package com.ssbmax.navigation

import androidx.navigation.NavHostController
import com.ssbmax.core.domain.model.TestType

/**
 * Canonical test session ID for a [TestType] launched from a type-only entry
 * point (no specific test instance already selected), e.g. the Student Tests
 * overview screen.
 */
fun standardTestId(testType: TestType): String =
    "${testType.name.lowercase()}_standard"

/**
 * Single source of truth for routing a test ID to its test-taking screen.
 * Falls back to the generic GTOTest placeholder route for GTO test types
 * without a dedicated screen yet (PGT, GOR, HGT, IO, CT) — intentional, not
 * dead code.
 */
fun navigateToTestId(navController: NavHostController, testId: String) {
    when {
        testId.startsWith("oir_") ->
            navController.navigate(SSBMaxDestinations.OIRTest.createRoute(testId))
        testId.startsWith("ppdt_") ->
            navController.navigate(SSBMaxDestinations.PPDTTest.createRoute(testId))
        testId.startsWith("tat_") ->
            navController.navigate(SSBMaxDestinations.TATTest.createRoute(testId))
        testId.startsWith("wat_") ->
            navController.navigate(SSBMaxDestinations.WATTest.createRoute(testId))
        testId.startsWith("srt_") ->
            navController.navigate(SSBMaxDestinations.SRTTest.createRoute(testId))
        testId.startsWith("sd_") ->
            navController.navigate(SSBMaxDestinations.SDTest.createRoute(testId))
        testId.startsWith("piq_") ->
            navController.navigate(SSBMaxDestinations.PIQTest.createRoute(testId))
        // GTO Tests - Route to specific screens
        testId.startsWith("gto_gd_") ->
            navController.navigate(SSBMaxDestinations.GTOGDTest.createRoute(testId))
        testId.startsWith("gto_gpe_") ->
            navController.navigate(SSBMaxDestinations.GTOGPETest.createRoute(testId))
        testId.startsWith("gto_lecturette_") ->
            navController.navigate(SSBMaxDestinations.GTOLecturetteTest.createRoute(testId))
        testId.startsWith("gto_") ->
            navController.navigate(SSBMaxDestinations.GTOTest.createRoute(testId))
        testId.startsWith("io_") ->
            navController.navigate(SSBMaxDestinations.IOTest.createRoute(testId))
        else -> {
            android.util.Log.w("Navigation", "Unknown test ID: $testId")
        }
    }
}
