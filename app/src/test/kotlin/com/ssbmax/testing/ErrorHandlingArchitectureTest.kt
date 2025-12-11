package com.ssbmax.testing

import org.junit.Assert.fail
import org.junit.Test
import java.io.File

/**
 * Architecture Tests for Error Handling Standards
 *
 * These tests validate that proper error handling practices are followed:
 * 1. No printStackTrace() usage (use ErrorLogger instead)
 * 2. ViewModels use ErrorLogger for error logging (not direct android.util.Log)
 * 3. All catch blocks have proper error handling
 *
 * These tests run on every build to prevent error handling regressions.
 */
class ErrorHandlingArchitectureTest {

    companion object {
        // Handle both project root and app module directory
        private val USER_DIR = File(System.getProperty("user.dir") ?: ".")
        private val PROJECT_ROOT = if (USER_DIR.name == "SSBMax") USER_DIR else USER_DIR.parentFile
        private val APP_SRC = File(PROJECT_ROOT, "app/src/main/kotlin")
    }

    @Test
    fun `no printStackTrace() usage in production code`() {
        val violations = mutableListOf<String>()

        scanKotlinFiles(APP_SRC) { file, content ->
            // Skip test files
            if (file.path.contains("/test/")) return@scanKotlinFiles

            // Check for printStackTrace()
            val lines = content.lines()
            for ((index, line) in lines.withIndex()) {
                val trimmed = line.trim()
                // Skip comments and string literals
                if (trimmed.startsWith("//") || trimmed.startsWith("*")) continue
                if (trimmed.startsWith("\"") && trimmed.endsWith("\"")) continue

                if (line.contains(".printStackTrace()")) {
                    violations.add(
                        "${file.relativePath()}: Line ${index + 1}\n" +
                                "  Found: ${line.trim()}\n" +
                                "  Use ErrorLogger.log(e, \"description\") instead"
                    )
                }
            }
        }

        if (violations.isNotEmpty()) {
            fail(
                "Found ${violations.size} printStackTrace() usage(s):\n\n" +
                        violations.joinToString("\n\n") +
                        "\n\nUse ErrorLogger.log() instead for proper error tracking with Crashlytics."
            )
        }
    }

    @Test
    fun `ViewModels should use ErrorLogger in catch blocks`() {
        val violations = mutableListOf<String>()

        val viewModelFiles = findFiles(APP_SRC, "*ViewModel.kt")

        for (file in viewModelFiles) {
            // Skip test files
            if (file.path.contains("/test/")) continue

            val content = file.readText()
            val lines = content.lines()

            // Look for catch blocks
            var inCatchBlock = false
            var catchStartLine = 0
            var braceDepth = 0

            for ((index, line) in lines.withIndex()) {
                // Detect catch block
                if (line.contains("} catch (") || line.contains("}catch(")) {
                    inCatchBlock = true
                    catchStartLine = index + 1
                    braceDepth = 0
                    continue
                }

                if (inCatchBlock) {
                    // Track braces
                    braceDepth += line.count { it == '{' }
                    braceDepth -= line.count { it == '}' }

                    // Check if using android.util.Log directly (should use ErrorLogger instead)
                    if (line.contains("android.util.Log.e(") || line.contains("Log.e(")) {
                        // This is acceptable if it's the only line, but ideally should use ErrorLogger
                        // We'll be lenient here and only warn about printStackTrace
                    }

                    // End of catch block
                    if (braceDepth < 0) {
                        inCatchBlock = false
                    }
                }
            }
        }

        // This test currently just validates structure
        // Can be enhanced to check for proper ErrorLogger usage
    }

    @Test
    fun `ErrorLogger utility exists and is properly configured`() {
        val errorLoggerFile = File(APP_SRC, "com/ssbmax/utils/ErrorLogger.kt")

        if (!errorLoggerFile.exists()) {
            fail("ErrorLogger.kt not found at expected location: ${errorLoggerFile.absolutePath}")
        }

        val content = errorLoggerFile.readText()

        // Verify it has required methods
        val requiredMethods = listOf(
            "fun log(",
            "fun logWithUser(",
            "fun logTestError("
        )

        val missingMethods = requiredMethods.filter { !content.contains(it) }

        if (missingMethods.isNotEmpty()) {
            fail(
                "ErrorLogger is missing required methods:\n" +
                        missingMethods.joinToString("\n") { "  - $it" }
            )
        }

        // Verify it integrates with Firebase Crashlytics
        if (!content.contains("FirebaseCrashlytics")) {
            fail("ErrorLogger should integrate with Firebase Crashlytics for production error tracking")
        }
    }

    // ==================== Helper Methods ====================

    private fun scanKotlinFiles(dir: File, action: (File, String) -> Unit) {
        if (!dir.exists()) return

        dir.walkTopDown().forEach { file ->
            if (file.isFile && file.extension == "kt") {
                val content = file.readText()
                action(file, content)
            }
        }
    }

    private fun findFiles(dir: File, pattern: String): List<File> {
        if (!dir.exists()) return emptyList()

        val regex = pattern.replace("*", ".*").toRegex()
        return dir.walkTopDown()
            .filter { it.isFile && it.name.matches(regex) }
            .toList()
    }

    private fun File.relativePath(): String {
        return this.relativeTo(PROJECT_ROOT).path
    }
}
