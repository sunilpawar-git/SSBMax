package com.ssbmax.architecture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Phase 1 contract tests for shared edge-to-edge chrome ownership.
 *
 * These are source-level architecture tests because platform safe-area values
 * cannot be deterministically injected into the current JVM test harness. The
 * tests characterize the confirmed pre-implementation boundary; Phase 2 will
 * extend them when the actual app-level bottom chrome is restored.
 */
class BottomChromeInsetContractTest {

    private val projectRoot: File =
        File(System.getProperty("user.dir") ?: ".").parentFile ?: File(".")

    private val scaffoldSource: String
        get() = readSource("shared/src/commonMain/kotlin/com/ssbmax/shared/ui/components/SSBMaxAppScaffold.kt")

    @Test
    fun `root routes all app chrome through the shared scaffold`() {
        val root = readSource("shared/src/commonMain/kotlin/com/ssbmax/shared/ui/SSBMaxRoot.kt")

        assertTrue(
            "SSBMaxRoot must use the shared scaffold as the app-chrome owner",
            root.contains("SSBMaxAppScaffold(navController = navController)")
        )
        assertTrue(
            "SSBMaxRoot must place the shared navigation host inside the shared scaffold",
            root.contains("SSBMaxNavHost(navController = navController, onOpenDrawer = onOpenDrawer)")
        )
    }

    @Test
    fun `app scaffold has one navigation inset boundary`() {
        val navigationInsetUsages =
            Regex("WindowInsets\\.navigationBars").findAll(scaffoldSource).count()

        assertEquals(
            "The current scaffold must reserve navigation space at one boundary until the " +
                "real bottom chrome owns the inset in Phase 2",
            1,
            navigationInsetUsages
        )
        assertTrue(
            "The current scaffold must not contain a hardcoded bottom inset workaround",
            !Regex("padding\\s*\\(\\s*bottom\\s*=|height\\s*\\([^)]*dp").containsMatchIn(scaffoldSource)
        )
    }

    @Test
    fun `auth routes bypass app chrome before drawer composition`() {
        val authGuard = "if (currentDestination.isAuthScreen())"
        val guardIndex = scaffoldSource.indexOf(authGuard)
        val drawerIndex = scaffoldSource.indexOf("ModalNavigationDrawer(")

        assertTrue("Auth-screen chrome guard must exist", guardIndex >= 0)
        assertTrue(
            "Auth screens must bypass the drawer and future bottom chrome",
            drawerIndex > guardIndex
        )
        assertTrue(
            "Auth-screen guard must return without composing app chrome",
            scaffoldSource.substring(guardIndex, drawerIndex).contains("return")
        )
    }

    @Test
    fun `platform hosts remain edge to edge`() {
        val activity = readSource("app/src/main/kotlin/com/ssbmax/MainActivity.kt")
        val iosApp = readSource("iosApp/iosApp/iosAppApp.swift")

        assertTrue("Android must opt into edge-to-edge layout", activity.contains("enableEdgeToEdge()"))
        assertTrue("iOS must expose the full host view to Compose", iosApp.contains(".ignoresSafeArea()"))
    }

    private fun readSource(relativePath: String): String {
        val source = File(projectRoot, relativePath)
        assertTrue("Required source file is missing: $relativePath", source.exists())
        return source.readText()
    }
}
