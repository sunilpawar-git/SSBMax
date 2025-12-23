package com.ssbmax.testing

import org.junit.Test
import org.junit.Assert.fail
import java.io.File

/**
 * Architecture Tests for SSBMax
 *
 * These tests validate architectural patterns across the entire codebase:
 * 1. No singleton objects with mutable state
 * 2. All test screens follow ID-based navigation pattern
 * 3. All result screens have dedicated ViewModels
 * 4. No *Holder pattern files (anti-pattern)
 *
 * These tests run on every build to prevent architectural regressions.
 */
class ArchitectureTest {

    companion object {
        // Root directory of the project
        private val PROJECT_ROOT = File(System.getProperty("user.dir") ?: ".")

        // Directories to scan - Handle both root and module-level execution
        private val APP_SRC = run {
            val appKotlin = File(PROJECT_ROOT, "app/src/main/kotlin")
            if (appKotlin.exists()) appKotlin 
            else {
                val moduleKotlin = File(PROJECT_ROOT, "src/main/kotlin")
                if (moduleKotlin.exists()) moduleKotlin
                else File(".") // Fallback
            }
        }

        private val CORE_SRC = run {
            val coreDir = File(PROJECT_ROOT, "core")
            if (coreDir.exists()) coreDir
            else File(PROJECT_ROOT, "../core") // Fallback if in app module
        }
    }

    @Test(timeout = 60000)
    fun `no singleton objects with mutable state in codebase`() {
        val violations = mutableListOf<String>()

        // Scan all Kotlin files
        scanKotlinFiles(APP_SRC) { file, content ->
            // Skip test files
            if (file.path.contains("/test/")) return@scanKotlinFiles

            // Look for object declarations
            if (content.contains("object ")) {
                val lines = content.lines()
                var inObject = false
                var objectName = ""
                var braceDepth = 0

                for ((index, line) in lines.withIndex()) {
                    // Detect object declaration
                    if (line.trim().startsWith("object ") && !line.contains("companion")) {
                        inObject = true
                        objectName = line.substringAfter("object ").substringBefore(":").substringBefore("{").trim()
                        braceDepth = 0
                    }

                    if (inObject) {
                        // Track brace depth
                        braceDepth += line.count { it == '{' }
                        braceDepth -= line.count { it == '}' }

                        // Check for mutable state (var, mutableStateOf, Mutable collections)
                        val trimmed = line.trim()
                        if ((trimmed.startsWith("var ") || trimmed.startsWith("private var ")) &&
                            !isAllowedMutablePattern(line)
                        ) {
                            violations.add(
                                "${file.relativePath()}: Singleton object `$objectName` has mutable state at line ${index + 1}\n" +
                                        "  Line: ${line.trim()}\n" +
                                        "  Use ViewModel or Repository instead."
                            )
                        }

                        // End of object
                        if (braceDepth == 0 && line.contains("}")) {
                            inObject = false
                        }
                    }
                }
            }
        }

        if (violations.isNotEmpty()) {
            fail(
                "Found ${violations.size} singleton(s) with mutable state:\n\n" +
                        violations.joinToString("\n\n") +
                        "\n\nSingletons should not contain mutable state. Use ViewModel or Repository pattern instead."
            )
        }
    }

    @Test(timeout = 30000)
    fun `all test screens follow ID-based navigation pattern`() {
        val violations = mutableListOf<String>()

        val testScreenFiles = findFiles(APP_SRC, "*TestScreen.kt")

        for (file in testScreenFiles) {
            val content = file.readText()

            // Skip if not a Composable screen
            if (!content.contains("@Composable")) continue

            // Look for navigation callbacks
            val callbackPattern = Regex("""onTestComplete\s*:\s*\(([^)]+)\)\s*->\s*Unit""")
            val matches = callbackPattern.findAll(content)

            for (match in matches) {
                val params = match.groupValues[1]

                // Check if passing complex objects instead of IDs
                if (!isIDBasedNavigation(params)) {
                    violations.add(
                        "${file.relativePath()}: Test screen uses complex object navigation\n" +
                                "  Found: onTestComplete: ($params) -> Unit\n" +
                                "  Expected: onTestComplete: (submissionId: String, subscriptionType: SubscriptionType) -> Unit"
                    )
                }
            }
        }

        if (violations.isNotEmpty()) {
            fail(
                "Found ${violations.size} test screen(s) not following ID-based navigation:\n\n" +
                        violations.joinToString("\n\n") +
                        "\n\nAll test screens should pass IDs, not complex objects. " +
                        "Result screens should fetch data using ViewModel + Repository."
            )
        }
    }

    @Test(timeout = 30000)
    fun `all result screens have dedicated ViewModels`() {
        val violations = mutableListOf<String>()

        val resultScreenFiles = findFiles(APP_SRC, "*ResultScreen.kt")

        for (file in resultScreenFiles) {
            val content = file.readText()

            // Skip if not a Composable screen
            if (!content.contains("@Composable")) continue

            // Check if it uses a ViewModel
            val hasViewModel = content.contains("hiltViewModel()") ||
                    content.contains("viewModel:") ||
                    content.contains("ViewModel =")

            if (!hasViewModel) {
                violations.add(
                    "${file.relativePath()}: Result screen doesn't use ViewModel\n" +
                            "  All result screens should fetch data via ViewModel, not receive it as parameters."
                )
            }
        }

        if (violations.isNotEmpty()) {
            fail(
                "Found ${violations.size} result screen(s) without ViewModels:\n\n" +
                        violations.joinToString("\n\n") +
                        "\n\nResult screens should use dedicated ViewModels to fetch data from repositories."
            )
        }
    }

    @Test(timeout = 30000)
    fun `no Holder pattern files exist in codebase`() {
        val holderFiles = mutableListOf<File>()

        scanKotlinFiles(APP_SRC) { file, content ->
            // Check filename
            if (file.name.contains("Holder.kt") && !file.name.contains("Placeholder")) {
                // Check if it's actually a holder pattern (object with mutable state)
                if (content.contains("object ") && content.contains("var ")) {
                    holderFiles.add(file)
                }
            }
        }

        if (holderFiles.isNotEmpty()) {
            fail(
                "Found ${holderFiles.size} *Holder anti-pattern file(s):\n\n" +
                        holderFiles.joinToString("\n") { it.relativePath() } +
                        "\n\nThe *Holder pattern (singleton state holders) is an anti-pattern. " +
                        "Use ViewModel or Repository instead."
            )
        }
    }

    @Test(timeout = 30000)
    fun `all ViewModels extend androidx lifecycle ViewModel`() {
        val violations = mutableListOf<String>()

        val viewModelFiles = findFiles(APP_SRC, "*ViewModel.kt")

        for (file in viewModelFiles) {
            // Skip test files
            if (file.path.contains("/test/")) continue

            val content = file.readText()

            // Check if it extends ViewModel
            val extendsViewModel = content.contains(": ViewModel()") ||
                    content.contains(": ViewModel {")

            if (!extendsViewModel && !content.contains("interface ")) {
                violations.add(
                    "${file.relativePath()}: ViewModel class doesn't extend androidx.lifecycle.ViewModel"
                )
            }
        }

        if (violations.isNotEmpty()) {
            fail(
                "Found ${violations.size} ViewModel(s) not extending androidx.lifecycle.ViewModel:\n\n" +
                        violations.joinToString("\n\n")
            )
        }
    }

    @Test(timeout = 30000)
    fun `all test result screens follow consistent naming`() {
        val violations = mutableListOf<String>()

        val resultScreenFiles = findFiles(APP_SRC, "*Result*Screen.kt")

        for (file in resultScreenFiles) {
            val fileName = file.nameWithoutExtension

            // Expected pattern: *SubmissionResultScreen or *TestResultScreen
            val followsPattern = fileName.endsWith("SubmissionResultScreen") ||
                    fileName.endsWith("TestResultScreen")

            if (!followsPattern) {
                violations.add(
                    "${file.relativePath()}: Non-standard result screen naming\n" +
                            "  Expected: *SubmissionResultScreen or *TestResultScreen\n" +
                            "  Found: $fileName"
                )
            }
        }

        if (violations.isNotEmpty()) {
            fail(
                "Found ${violations.size} result screen(s) with non-standard naming:\n\n" +
                        violations.joinToString("\n\n") +
                        "\n\nUse consistent naming: *SubmissionResultScreen or *TestResultScreen"
            )
        }
    }

    @Test(timeout = 30000)
    fun `all use cases must be in domain layer not UI layer`() {
        val violations = mutableListOf<String>()

        // Scan app/src for any *UseCase.kt files (they should all be in core/domain)
        scanKotlinFiles(APP_SRC) { file, content ->
            // Skip test files
            if (file.path.contains("/test/")) return@scanKotlinFiles

            // Check if filename ends with UseCase.kt
            if (file.name.endsWith("UseCase.kt")) {
                violations.add(
                    "${file.relativePath()}: Use case found in UI layer\n" +
                            "  Use cases must be in: core/domain/src/main/kotlin/com/ssbmax/core/domain/usecase/\n" +
                            "  Current location violates clean architecture principles."
                )
            }
        }

        if (violations.isNotEmpty()) {
            fail(
                "Found ${violations.size} use case(s) in wrong layer:\n\n" +
                        violations.joinToString("\n\n") +
                        "\n\nAll use cases must be in the domain layer (core/domain module), " +
                        "not in the UI layer (app module)."
            )
        }
    }

    @Test(timeout = 30000)
    fun `domain layer must not depend on UI layer`() {
        val violations = mutableListOf<String>()

        val coreDomainSrc = File(CORE_SRC, "domain/src/main/kotlin")

        if (!coreDomainSrc.exists()) return

        scanKotlinFiles(coreDomainSrc) { file, content ->
            // Skip test files
            if (file.path.contains("/test/")) return@scanKotlinFiles

            // Check for UI layer imports
            val uiImportPattern = Regex("""import\s+com\.ssbmax\.ui\.""")
            val matches = uiImportPattern.findAll(content)

            for (match in matches) {
                val line = content.lines().find { it.contains(match.value) }
                violations.add(
                    "${file.relativePath()}: Domain layer imports UI layer\n" +
                            "  Line: ${line?.trim()}\n" +
                            "  Domain layer must not depend on UI layer (clean architecture violation)."
                )
            }
        }

        if (violations.isNotEmpty()) {
            fail(
                "Found ${violations.size} domain layer file(s) depending on UI layer:\n\n" +
                        violations.joinToString("\n\n") +
                        "\n\nDomain layer must be independent of UI layer. " +
                        "Use repository interfaces in domain layer, implement them in data/app layers."
            )
        }
    }

    @Test(timeout = 30000)
    fun `UI layer must not import Firebase classes directly`() {
        val violations = mutableListOf<String>()

        scanKotlinFiles(APP_SRC) { file, content ->
            // Only check UI layer
            if (!file.path.contains("/com/ssbmax/ui/")) return@scanKotlinFiles
            if (file.path.contains("/test/")) return@scanKotlinFiles

            // Check for Firebase imports
            val firebaseImportPattern = Regex("""import\s+com\.google\.firebase\.""")
            val matches = firebaseImportPattern.findAll(content)

            for (match in matches) {
                val line = content.lines().find { it.contains(match.value) }
                val importPath = line?.substringAfter("import ")?.trim() ?: ""

                val suggestion = when {
                    importPath.contains("firebase.auth") ->
                        "Use ObserveCurrentUserUseCase or AuthRepository instead"
                    importPath.contains("firebase.firestore") ->
                        "Create a repository in core/data and inject it"
                    importPath.contains("firebase.storage") ->
                        "Create a storage repository in core/data"
                    else ->
                        "Use domain layer abstractions (repositories, use cases)"
                }

                violations.add(
                    "${file.relativePath()}: UI layer imports Firebase\n" +
                            "  Line: ${line?.trim()}\n" +
                            "  Solution: $suggestion"
                )
            }
        }

        if (violations.isNotEmpty()) {
            fail(
                "Found ${violations.size} UI layer file(s) importing Firebase:\n\n" +
                        violations.joinToString("\n\n") +
                        "\n\nUI layer must not depend on Firebase. Use domain abstractions:\n" +
                        "- For auth: ObserveCurrentUserUseCase, AuthRepository\n" +
                        "- For data: Create repository in core/data module\n\n" +
                        "This ensures Clean Architecture, testability, and flexibility."
            )
        }
    }

    // ==================== Helper Methods ====================

    /**
     * Scan all Kotlin files in a directory recursively
     */
    private fun scanKotlinFiles(dir: File, action: (File, String) -> Unit) {
        if (!dir.exists()) return

        dir.walkTopDown().forEach { file ->
            if (file.isFile && file.extension == "kt") {
                val content = file.readText()
                action(file, content)
            }
        }
    }

    /**
     * Find files matching a pattern
     */
    private fun findFiles(dir: File, pattern: String): List<File> {
        if (!dir.exists()) return emptyList()

        val regex = pattern.replace("*", ".*").toRegex()
        return dir.walkTopDown()
            .filter { it.isFile && it.name.matches(regex) }
            .toList()
    }

    /**
     * Check if navigation params are ID-based (simple types)
     */
    private fun isIDBasedNavigation(params: String): Boolean {
        val cleanParams = params.trim()

        // Empty or Unit is OK
        if (cleanParams.isEmpty() || cleanParams == "Unit") return true

        // Check each parameter
        val paramList = cleanParams.split(",")
        for (param in paramList) {
            val type = param.substringAfter(":").trim()

            // Simple types are OK
            val simpleTypes = setOf("String", "Int", "Long", "Boolean", "SubscriptionType", "TestType")
            if (simpleTypes.any { type.contains(it) }) continue

            // Complex type found
            return false
        }

        return true
    }

    /**
     * Check if mutable pattern is allowed (e.g., WeakReference, ConcurrentHashMap)
     */
    private fun isAllowedMutablePattern(line: String): Boolean {
        val allowedPatterns = setOf(
            "WeakReference",
            "ConcurrentHashMap",
            "AtomicReference",
            "AtomicInteger",
            "AtomicBoolean"
        )

        return allowedPatterns.any { line.contains(it) }
    }

    @Test(timeout = 30000)
    fun `all ViewModels must not have nullable mutable vars`() {
        val violations = mutableListOf<String>()

        val viewModelFiles = findFiles(APP_SRC, "*ViewModel.kt")

        for (file in viewModelFiles) {
            // Skip test files
            if (file.path.contains("/test/")) continue

            val content = file.readText()

            // Check if it's actually a ViewModel class
            if (!content.contains(": ViewModel()") && !content.contains(": ViewModel {")) {
                continue
            }

            val lines = content.lines()
            for ((index, line) in lines.withIndex()) {
                val trimmed = line.trim()

                // Look for nullable var declarations
                if ((trimmed.startsWith("private var ") || trimmed.startsWith("var ")) &&
                    trimmed.contains("?") &&
                    !isAllowedMutableState(trimmed)
                ) {
                    // Extract variable name
                    val varName = trimmed.substringAfter("var ")
                        .substringBefore(":")
                        .substringBefore("=")
                        .trim()

                    violations.add(
                        "${file.relativePath()}: Line ${index + 1}\n" +
                                "  Variable: $varName\n" +
                                "  Code: ${trimmed.take(80)}\n" +
                                "  Problem: Nullable mutable var in ViewModel\n" +
                                "  Solution: Move to StateFlow<UiState> for lifecycle safety"
                    )
                }
            }
        }

        if (violations.isNotEmpty()) {
            fail(
                "Found ${violations.size} ViewModel(s) with nullable mutable vars:\n\n" +
                        violations.joinToString("\n\n") +
                        "\n\nAll mutable state in ViewModels must be in StateFlow:\n" +
                        "❌ BAD: private var timerJob: Job? = null\n" +
                        "❌ BAD: private var sessionId: String? = null\n\n" +
                        "✅ GOOD: data class UiState(val session: MySession? = null)\n" +
                        "✅ GOOD: private val _uiState = MutableStateFlow(UiState())\n\n" +
                        "Lint rule will catch these during build. Fix to proceed."
            )
        }
    }

    @Test(timeout = 30000)
    fun `ViewModels must not store Job references`() {
        val violations = mutableListOf<String>()

        val viewModelFiles = findFiles(APP_SRC, "*ViewModel.kt")

        for (file in viewModelFiles) {
            // Skip test files
            if (file.path.contains("/test/")) continue

            val content = file.readText()

            // Check if it's actually a ViewModel class
            if (!content.contains(": ViewModel()") && !content.contains(": ViewModel {")) {
                continue
            }

            // Look for Job? field declarations
            val jobPattern = Regex("""private\s+var\s+\w+\s*:\s*Job\?""")
            val matches = jobPattern.findAll(content)

            for (match in matches) {
                val line = content.lines().find { it.contains(match.value) }
                violations.add(
                    "${file.relativePath()}: Stores Job? as field\n" +
                            "  Code: ${line?.trim()?.take(80)}\n" +
                            "  Problem: Memory leak risk if not cancelled properly\n" +
                            "  Solution: Use viewModelScope.launch directly without storing Job"
                )
            }
        }

        if (violations.isNotEmpty()) {
            fail(
                "Found ${violations.size} ViewModel(s) storing Job references:\n\n" +
                        violations.joinToString("\n\n") +
                        "\n\nJob references create memory leak risk:\n" +
                        "❌ BAD:\n" +
                        "  private var timerJob: Job? = null\n" +
                        "  fun startTimer() { timerJob = viewModelScope.launch { ... } }\n\n" +
                        "✅ GOOD:\n" +
                        "  fun startTimer() { viewModelScope.launch { ... } }\n" +
                        "  // ViewModelScope auto-cancels all jobs in onCleared()"
            )
        }
    }

    /**
     * Check if mutable state pattern is allowed in ViewModels
     */
    private fun isAllowedMutableState(line: String): Boolean {
        val allowedPatterns = setOf(
            "StateFlow",
            "MutableStateFlow",
            "LiveData",
            "MutableLiveData",
            "Flow<",
            "// DEPRECATED",  // Marked for removal
            "// PHASE"  // Migration in progress
        )

        return allowedPatterns.any { line.contains(it) }
    }

    /**
     * Get relative path from project root
     */
    private fun File.relativePath(): String {
        return this.relativeTo(PROJECT_ROOT).path
    }

    @Test(timeout = 30000)
    fun `MainActivity must handle configuration changes to prevent process death`() {
        val violations = mutableListOf<String>()
        
        // Path to AndroidManifest.xml - try multiple possible paths
        val possiblePaths = listOf(
            File(PROJECT_ROOT, "app/src/main/AndroidManifest.xml"),
            File(System.getProperty("user.dir"), "app/src/main/AndroidManifest.xml"),
            File(System.getProperty("user.dir"), "../app/src/main/AndroidManifest.xml"),
            File(System.getProperty("user.dir"), "../../app/src/main/AndroidManifest.xml")
        )
        
        val manifestFile = possiblePaths.firstOrNull { it.exists() }
        
        if (manifestFile == null) {
            fail("AndroidManifest.xml not found at expected locations. Tried:\n${possiblePaths.joinToString("\n") { it.absolutePath }}")
            return  // Never reached due to fail(), but satisfies compiler
        }
        
        val manifestContent = manifestFile.readText()
        
        // Check if MainActivity is declared
        if (!manifestContent.contains("android:name=\".MainActivity\"")) {
            fail("MainActivity declaration not found in AndroidManifest.xml")
        }
        
        // Required configuration changes to prevent process death during rotation
        val requiredConfigChanges = setOf(
            "orientation",      // Portrait ↔ Landscape rotation
            "screenSize",       // Screen dimensions change (required for API 13+)
            "screenLayout",     // Screen layout changes (size buckets)
            "keyboardHidden"    // Physical keyboard shown/hidden
        )
        
        // Extract MainActivity declaration block
        val activityPattern = Regex(
            """<activity[^>]*android:name="\.MainActivity"[^>]*>.*?</activity>""",
            RegexOption.DOT_MATCHES_ALL
        )
        val activityMatch = activityPattern.find(manifestContent)
        
        if (activityMatch == null) {
            fail("Could not parse MainActivity declaration in AndroidManifest.xml")
            return  // Never reached due to fail(), but satisfies compiler
        }
        
        val activityDeclaration = activityMatch.value
        
        // Check for android:configChanges attribute
        val configChangesPattern = Regex("""android:configChanges="([^"]+)"""")
        val configChangesMatch = configChangesPattern.find(activityDeclaration)
        
        if (configChangesMatch == null) {
            violations.add(
                "MainActivity is missing android:configChanges attribute\n" +
                "  Location: app/src/main/AndroidManifest.xml\n" +
                "  Problem: Without this, Android destroys MainActivity on rotation, causing process death\n" +
                "  Required: android:configChanges=\"orientation|screenSize|screenLayout|keyboardHidden\"\n" +
                "  Impact: Tests (TAT, WAT, SRT, PPDT) will stop and return to home screen on rotation"
            )
        } else {
            val declaredConfigChanges = configChangesMatch.groupValues[1]
                .split("|")
                .map { it.trim() }
                .toSet()
            
            // Check for missing required config changes
            val missingConfigs = requiredConfigChanges - declaredConfigChanges
            
            if (missingConfigs.isNotEmpty()) {
                violations.add(
                    "MainActivity android:configChanges is incomplete\n" +
                    "  Current: $declaredConfigChanges\n" +
                    "  Missing: $missingConfigs\n" +
                    "  Required: $requiredConfigChanges\n" +
                    "  Problem: Incomplete config will not prevent process death on all rotation scenarios"
                )
            }
        }
        
        if (violations.isNotEmpty()) {
            fail(
                "MainActivity configuration validation failed:\n\n" +
                violations.joinToString("\n\n") +
                "\n\nWhy this matters:\n" +
                "- Without android:configChanges, MainActivity is destroyed on rotation\n" +
                "- Under low memory, Android KILLS the entire process during recreation\n" +
                "- Tests lose state, timers stop, and users are sent back to home screen\n\n" +
                "Fix: Ensure MainActivity declaration includes:\n" +
                "  android:configChanges=\"orientation|screenSize|screenLayout|keyboardHidden\"\n\n" +
                "This test prevents regression of the process death bug fix."
            )
        }
    }
}
