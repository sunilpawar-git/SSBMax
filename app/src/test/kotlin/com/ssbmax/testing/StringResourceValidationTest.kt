package com.ssbmax.testing

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * String Resource Validation Test
 *
 * Validates that all migrated screens use stringResource() instead of hardcoded strings.
 * This test prevents regressions after the tech debt remediation project.
 *
 * Coverage:
 * - WATTestScreen.kt
 * - SRTTestScreen.kt
 * - TATTestScreen.kt
 * - OIRTestScreen.kt
 * - PPDTTestScreen.kt
 * - PIQTestScreen.kt
 * - TestDetailGradingScreen.kt
 * - StudyMaterialDetailScreen.kt
 * - StudentTestsScreen.kt
 */
class StringResourceValidationTest {

    private val projectRoot = File(System.getProperty("user.dir")).let { buildDir ->
        // When running from Gradle, user.dir points to the project root already
        // When running from a module, we may need to navigate up
        if (File(buildDir, "app").exists()) {
            buildDir
        } else if (buildDir.name == "app") {
            buildDir.parentFile
        } else {
            buildDir
        }
    }

    private val migratedScreens = listOf(
        "app/src/main/kotlin/com/ssbmax/ui/tests/wat/WATTestScreen.kt",
        "app/src/main/kotlin/com/ssbmax/ui/tests/srt/SRTTestScreen.kt",
        "app/src/main/kotlin/com/ssbmax/ui/tests/tat/TATTestScreen.kt",
        "app/src/main/kotlin/com/ssbmax/ui/tests/oir/OIRTestScreen.kt",
        "app/src/main/kotlin/com/ssbmax/ui/tests/ppdt/PPDTTestScreen.kt",
        "app/src/main/kotlin/com/ssbmax/ui/tests/piq/PIQTestScreen.kt",
        "app/src/main/kotlin/com/ssbmax/ui/grading/TestDetailGradingScreen.kt",
        "app/src/main/kotlin/com/ssbmax/ui/study/StudyMaterialDetailScreen.kt",
        "app/src/main/kotlin/com/ssbmax/ui/tests/StudentTestsScreen.kt"
    )

    @Test
    fun `all migrated screens use stringResource for UI text`() {
        val violations = mutableListOf<String>()

        migratedScreens.forEach { screenPath ->
            val file = File(projectRoot, screenPath)
            if (!file.exists()) {
                violations.add("$screenPath: File not found")
                return@forEach
            }

            val content = file.readText()

            // Pattern to find Text() composables with hardcoded strings
            // Matches: Text("hardcoded"), Text(text = "hardcoded")
            // Excludes: Text(stringResource(...)), Text(uiState.variable), Text(formattedString)
            val hardcodedTextPattern = Regex(
                """Text\s*\(\s*(?:text\s*=\s*)?"([^"]+)""""
            )

            val matches = hardcodedTextPattern.findAll(content)

            matches.forEach { match ->
                val hardcodedText = match.groupValues[1]

                // Allow certain exceptions:
                // 1. Empty strings
                // 2. Single special characters (like "•", "—", etc.)
                // 3. Format specifiers (like "%d", "%s")
                // 4. Spacing characters
                // 5. String interpolation expressions (contains ${...})
                val isException = hardcodedText.isEmpty() ||
                                 hardcodedText.length == 1 ||
                                 hardcodedText.matches(Regex("""%\d*[ds]""")) ||
                                 hardcodedText.matches(Regex("""\s+""")) ||
                                 hardcodedText.contains("\${") // String interpolation

                if (!isException) {
                    // Get line number for better debugging
                    val lineNumber = content.substring(0, match.range.first)
                        .count { it == '\n' } + 1

                    violations.add(
                        "$screenPath:$lineNumber: Found hardcoded text: \"$hardcodedText\""
                    )
                }
            }
        }

        assertTrue(
            "Found hardcoded strings in migrated screens:\n${violations.joinToString("\n")}\n\n" +
            "All user-facing text should use stringResource(R.string.xxx)",
            violations.isEmpty()
        )
    }

    @Test
    fun `all string resource references exist in strings xml`() {
        val violations = mutableListOf<String>()
        val stringsXmlPath = "app/src/main/res/values/strings.xml"
        val stringsXmlFile = File(projectRoot, stringsXmlPath)

        if (!stringsXmlFile.exists()) {
            throw AssertionError("strings.xml not found at $stringsXmlPath")
        }

        val stringsXmlContent = stringsXmlFile.readText()

        // Extract all defined string resource names from strings.xml
        val definedResources = Regex("""<string\s+name="([^"]+)"""")
            .findAll(stringsXmlContent)
            .map { it.groupValues[1] }
            .toSet()

        migratedScreens.forEach { screenPath ->
            val file = File(projectRoot, screenPath)
            if (!file.exists()) return@forEach

            val content = file.readText()

            // Pattern to find stringResource(R.string.xxx) calls
            val stringResourcePattern = Regex(
                """stringResource\(R\.string\.([a-z_][a-z0-9_]*)\)"""
            )

            val matches = stringResourcePattern.findAll(content)

            matches.forEach { match ->
                val resourceName = match.groupValues[1]

                if (resourceName !in definedResources) {
                    val lineNumber = content.substring(0, match.range.first)
                        .count { it == '\n' } + 1

                    violations.add(
                        "$screenPath:$lineNumber: String resource not found in strings.xml: R.string.$resourceName"
                    )
                }
            }
        }

        assertTrue(
            "Found references to undefined string resources:\n${violations.joinToString("\n")}\n\n" +
            "All stringResource() calls must reference valid entries in strings.xml",
            violations.isEmpty()
        )
    }

    @Test
    fun `formatted strings have correct parameter counts`() {
        val violations = mutableListOf<String>()
        val stringsXmlPath = "app/src/main/res/values/strings.xml"
        val stringsXmlFile = File(projectRoot, stringsXmlPath)

        if (!stringsXmlFile.exists()) return

        val stringsXmlContent = stringsXmlFile.readText()

        // Build a map of string resources with format parameters
        val formattedStrings = mutableMapOf<String, Int>()

        val stringEntryPattern = Regex("""<string\s+name="([^"]+)"[^>]*>([^<]+)</string>""")
        stringEntryPattern.findAll(stringsXmlContent).forEach { match ->
            val name = match.groupValues[1]
            val value = match.groupValues[2]

            // Count format specifiers: %1$d, %2$s, etc.
            val formatCount = Regex("""%\d+\$[ds]""").findAll(value).count()

            if (formatCount > 0) {
                formattedStrings[name] = formatCount
            }
        }

        migratedScreens.forEach { screenPath ->
            val file = File(projectRoot, screenPath)
            if (!file.exists()) return@forEach

            val content = file.readText()

            // Pattern to find stringResource calls with parameters
            // Matches: stringResource(R.string.xxx, param1, param2)
            val stringResourceWithParamsPattern = Regex(
                """stringResource\(R\.string\.([a-z_][a-z0-9_]*)(?:\s*,\s*([^)]+))?\)"""
            )

            stringResourceWithParamsPattern.findAll(content).forEach { match ->
                val resourceName = match.groupValues[1]
                val params = match.groupValues[2]

                if (resourceName in formattedStrings) {
                    val expectedParamCount = formattedStrings[resourceName]!!

                    // Count actual parameters passed
                    val actualParamCount = if (params.isBlank()) {
                        0
                    } else {
                        // Count commas (rough estimate, but works for most cases)
                        params.split(",").size
                    }

                    if (actualParamCount != expectedParamCount) {
                        val lineNumber = content.substring(0, match.range.first)
                            .count { it == '\n' } + 1

                        violations.add(
                            "$screenPath:$lineNumber: Parameter count mismatch for R.string.$resourceName - " +
                            "expected $expectedParamCount parameters, found $actualParamCount"
                        )
                    }
                }
            }
        }

        assertTrue(
            "Found parameter count mismatches in stringResource calls:\n${violations.joinToString("\n")}\n\n" +
            "Formatted strings must be called with the correct number of parameters",
            violations.isEmpty()
        )
    }

    @Test
    fun `migrated screens have required imports`() {
        val violations = mutableListOf<String>()

        migratedScreens.forEach { screenPath ->
            val file = File(projectRoot, screenPath)
            if (!file.exists()) {
                violations.add("$screenPath: File not found")
                return@forEach
            }

            val content = file.readText()

            val hasStringResourceImport = content.contains("import androidx.compose.ui.res.stringResource")
            val hasRImport = content.contains("import com.ssbmax.R")

            if (!hasStringResourceImport) {
                violations.add("$screenPath: Missing import for stringResource")
            }

            if (!hasRImport) {
                violations.add("$screenPath: Missing import for com.ssbmax.R")
            }
        }

        assertTrue(
            "Found missing required imports:\n${violations.joinToString("\n")}\n\n" +
            "All migrated screens must import:\n" +
            "- androidx.compose.ui.res.stringResource\n" +
            "- com.ssbmax.R",
            violations.isEmpty()
        )
    }
}
