package com.ssbmax.architecture

import com.ssbmax.shared.di.GEMINI_API_KEY_PROPERTY
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Guards the one failure mode KMP-convergence Phase 9.0 could introduce that
 * neither the compiler nor `checkModules()` can see: `shared`'s
 * `coreInfraModule` builds `KtorGeminiClient` from
 * `getProperty(GEMINI_API_KEY_PROPERTY, "")`, and an empty default means a
 * missing `properties(...)` call at startup produces a *silently* keyless AI
 * service — every Gemini call fails at runtime, nothing fails at build time.
 * `core:data`'s `aiModule` used to mask this by reading `BuildConfig`
 * directly; it was deleted in 9.0, so this is now the only wiring.
 *
 * Source-level assertions, matching [WorkManagerHiltIntegrationTest]'s
 * existing precedent in this package: `SSBMaxApplication.onCreate` can't be
 * unit-tested here (every Robolectric test in this module is `@Ignore`d for an
 * SDK 35 shadow mismatch), and a Koin `checkModules()` run resolves fine with
 * an empty key, so it cannot catch this either.
 *
 * Run with: ./gradlew :app:testDebugUnitTest --tests "*.GeminiKeyWiringTest"
 */
class GeminiKeyWiringTest {

    private val projectRoot: File = File(System.getProperty("user.dir") ?: ".").parentFile ?: File(".")

    @Test
    fun `SSBMaxApplication supplies the Gemini API key as a Koin property`() {
        val source = File(projectRoot, "app/src/main/kotlin/com/ssbmax/SSBMaxApplication.kt")
        assertTrue("SSBMaxApplication.kt not found", source.exists())

        val text = source.readText()

        assertTrue(
            "startKoin must call properties(...) — without it KtorGeminiClient gets an empty " +
                "API key and every AI evaluation fails silently at runtime",
            text.contains("properties(")
        )
        assertTrue(
            "The property must be keyed by shared's GEMINI_API_KEY_PROPERTY constant, not a " +
                "duplicated string literal that can drift from coreInfraModule's reader",
            text.contains("GEMINI_API_KEY_PROPERTY")
        )
        assertTrue(
            "The key's value must come from BuildConfig (local.properties -> BuildConfig), " +
                "never a hardcoded literal",
            text.contains("BuildConfig.GEMINI_API_KEY")
        )
    }

    @Test
    fun `the Gemini property key literal is defined once in shared`() {
        // WHY: Android and iOS supply this property from different sources
        // (BuildConfig vs Info.plist). If either side hardcoded the literal, a
        // rename on one platform would degrade the other to an empty key with
        // no compile error.
        assertTrue(
            "GEMINI_API_KEY_PROPERTY must resolve from shared's di package",
            GEMINI_API_KEY_PROPERTY == "GEMINI_API_KEY"
        )

        val iosBootstrap = File(
            projectRoot,
            "shared/src/iosMain/kotlin/com/ssbmax/shared/platform/AppBootstrap.kt"
        )
        assertTrue("AppBootstrap.kt not found", iosBootstrap.exists())
        assertTrue(
            "iOS's ensureKoinStarted must key its properties() call by the same constant",
            iosBootstrap.readText().contains("GEMINI_API_KEY_PROPERTY")
        )
    }
}
