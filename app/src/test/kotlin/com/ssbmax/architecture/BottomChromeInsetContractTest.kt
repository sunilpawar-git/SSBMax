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
 * tests characterize the confirmed shared-scaffold boundary; any future
 * app-level bottom chrome must preserve this single-inset ownership contract.
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
    fun `app scaffold delegates system insets to routed screens`() {
        assertTrue(
            "The drawer wrapper must not reserve the navigation-bar inset",
            !scaffoldSource.contains("WindowInsets.navigationBars")
        )
        assertTrue(
            "The drawer wrapper must not consume insets intended for routed screens",
            !scaffoldSource.contains("consumeWindowInsets")
        )
        assertTrue(
            "The drawer wrapper must use zero content insets",
            scaffoldSource.contains("WindowInsets(0, 0, 0, 0)")
        )
        assertTrue(
            "The scaffold must not contain a hardcoded bottom inset workaround",
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
    fun `no outer app chrome claims the navigation bar inset`() {
        val commonMain = File(projectRoot, "shared/src/commonMain")
        val outerInsetClaims = commonMain.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filter { source ->
                val text = source.readText()
                text.contains("WindowInsets.navigationBars") ||
                    text.contains("navigationBarsPadding()") ||
                    text.contains("consumeWindowInsets")
            }
            .map { it.relativeTo(projectRoot).path.replace(File.separatorChar, '/') }
            .toList()

        assertEquals(
            "Without an app-level bottom bar, routed screens must own the navigation-bar inset",
            emptyList<String>(),
            outerInsetClaims
        )
    }

    @Test
    fun `legacy app-level bottom navigation scaffolding is absent`() {
        val commonMain = File(projectRoot, "shared/src/commonMain")
        val legacyBottomNavDeclarations = commonMain.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filter { source -> source.readText().contains("sealed class BottomNavItem") }
            .toList()
        val legacyAndroidScaffolds = commonMain.walkTopDown()
            .filter { it.isFile && it.name == "SSBMaxScaffold.kt" }
            .toList()

        assertTrue(
            "The unused BottomNavItem model must not be reintroduced without a real app chrome owner",
            legacyBottomNavDeclarations.isEmpty()
        )
        assertTrue(
            "The deleted Android-only app scaffold must not return to shared commonMain",
            legacyAndroidScaffolds.isEmpty()
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
