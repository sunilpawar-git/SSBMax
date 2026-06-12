# lint/CLAUDE.md — Custom Lint Rule Development

**Scope:** Creating, testing, and registering Android Lint detectors. This file specializes [claude.md](../claude.md) for the lint module—where code quality rules are enforced.

**Core Principle:** Lint detectors enforce patterns that humans forget. Build fails if violated (non-negotiable). Currently 13 detectors; see [README.md](README.md) for full list.

---

## Understanding the Android Lint Framework

**Overview:**
```
Source Code (Kotlin/Java/XML)
    ↓
UAST (Unified Abstract Syntax Tree)
    ↓
Lint Detectors (analyze AST)
    ↓
Issues (ERROR, WARNING, INFO)
    ↓
Build failure or warnings
```

**Key Components:**
1. **UAST:** Language-agnostic AST (works for Kotlin, Java, XML)
2. **Detector:** Subclass of `Detector`, implements `SourceCodeScanner`
3. **Issue:** Definition (id, severity, message)
4. **Scope:** What to analyze (JAVA_FILE_SCOPE, XML_FILE_SCOPE, etc.)
5. **Handler:** Visits nodes in the AST, reports violations

---

## Creating a New Detector (Template)

**Step 1: Define the Issue**
```kotlin
class MyCustomDetector : Detector(), SourceCodeScanner {
  companion object {
    val ISSUE = Issue.create(
      id = "MyCustomRule",
      briefDescription = "Describes what's wrong",
      explanation = "Longer description of why this matters and how to fix it",
      category = Category.CORRECTNESS,
      priority = 7, // 1-10 (higher = more important)
      severity = Severity.WARNING, // or ERROR for critical rules
      implementation = Implementation(
        MyCustomDetector::class.java,
        Scope.JAVA_FILE_SCOPE
      )
    )
  }
}
```

**Step 2: Implement the Scanner**
```kotlin
override fun getApplicableUastTypes() = listOf(UVariable::class.java)

override fun createUastHandler(context: JavaContext): UElementHandler {
  return object : UElementHandler() {
    override fun visitVariable(node: UVariable) {
      // Check if variable violates our rule
      if (shouldReport(node)) {
        context.report(
          issue = ISSUE,
          location = context.getLocation(node),
          message = "Fix this: ..."
        )
      }
    }
  }
}

private fun shouldReport(node: UVariable): Boolean {
  // Your detection logic here
  return node.name?.startsWith("mutableState") == true
}
```

**Step 3: Register in SSBMaxIssueRegistry**
```kotlin
// In lint/src/main/kotlin/com/ssbmax/lint/SSBMaxIssueRegistry.kt
override fun getIssues(): List<Issue> {
  return listOf(
    // ... existing issues
    MyCustomDetector.ISSUE  // Add here
  )
}
```

---

## Common Detection Patterns

**Pattern 1: Method Call Detection**
```kotlin
// Detect: FirebaseFirestore.getInstance().collection("data")
override fun getApplicableMethodNames() = listOf("collection", "document")

override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
  val receiverType = node.receiver?.getExpressionType()?.presentableText
  if (receiverType?.contains("FirebaseFirestore") == true) {
    context.report(
      issue = FIREBASE_IN_UI,
      location = context.getLocation(node),
      message = "Don't call Firebase directly in UI layer. Use repository pattern."
    )
  }
}
```

**Pattern 2: Variable Assignment Detection**
```kotlin
// Detect: val mutableState = MutableStateFlow(...)
override fun visitVariable(node: UVariable) {
  if (node.name?.contains("Mutable") == true) {
    val initializer = node.uastInitializer
    if (initializer?.asRenderString()?.contains("MutableStateFlow") == true) {
      context.report(
        issue = EXPOSE_STATEFLOW_NOT_MUTABLE,
        location = context.getLocation(node),
        message = "Expose StateFlow<T>, not MutableStateFlow. Use .asStateFlow()."
      )
    }
  }
}
```

**Pattern 3: Import Detection**
```kotlin
// Detect: import com.firebase.*
override fun getApplicableUastTypes() = listOf(UImportStatement::class.java)

override fun visitImport(node: UImportStatement) {
  val importPath = node.importReference?.asRenderString()
  if (importPath?.startsWith("com.firebase") == true && 
      context.file.name.contains("ViewModel")) {
    context.report(
      issue = FIREBASE_IN_VIEWMODEL,
      location = context.getLocation(node),
      message = "ViewModels should not import Firebase. Use repositories."
    )
  }
}
```

---

## Phase 2-4 Detectors (Security Framework)

**Already Implemented:**

1. **HardcodedApiKeyDetector** (ERROR)
   - Detects: Variable assignments like `const val GEMINI_API_KEY = "sk_..."`
   - Scope: String literals with suspicious names/prefixes
   - Message: "Use local.properties or Firebase Remote Config instead"

2. **SensitiveDataInLogsDetector** (ERROR)
   - Detects: Log.d/e/w/i calls with userId, email, authToken arguments
   - Scope: Method calls to logging functions
   - Message: "Use ErrorLogger.log() which sanitizes PII"

3. **UnsafeStringInputDetector** (WARNING)
   - Detects: Unvalidated string input (from getStringExtra, textFieldValue)
   - Scope: Variable assignments without validation chain
   - Message: "Call .trim(), .isNotEmpty(), or .matches() to prevent XSS"

4. **FirebaseRulesValidationDetector** (WARNING, placeholder)
   - Delegates to pre-commit hook: `firebase deploy --dry-run`
   - Message: "Firebase rules validated via pre-commit hook"

**To add more:**
1. Define Issue with appropriate severity
2. Implement detection logic (follow patterns above)
3. Write tests (see next section)
4. Register in SSBMaxIssueRegistry
5. Update README.md with example violations + fixes

---

## Testing Lint Detectors (JUnit 4 + LintTestCase)

**Test Template:**
```kotlin
class MyCustomDetectorTest : LintDetectorTest() {
  override fun getDetector(): Detector = MyCustomDetector()
  
  override fun getIssues(): List<Issue> = listOf(MyCustomDetector.ISSUE)
  
  @Test
  fun detectsViolation() {
    lint()
      .files(
        kotlin("""
          fun main() {
            const val GEMINI_API_KEY = "sk-proj-xyz"  // ← violation
          }
        """)
      )
      .run()
      .expectErrorCount(1)
      .expect("""
        src/test.kt:3: Error: Hardcoded API key detected in variable 'GEMINI_API_KEY' [MyCustomRule]
        const val GEMINI_API_KEY = "sk-proj-xyz"
        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
      """)
  }
  
  @Test
  fun allowsValidCode() {
    lint()
      .files(
        kotlin("""
          fun main() {
            const val myVar = "regular string"  // ✅ OK
          }
        """)
      )
      .run()
      .expectClean()
  }
}
```

**Run Tests:**
```bash
./gradlew :lint:testDebugUnitTest -k "MyCustomDetectorTest"
```

---

## Severity & Suppression

**Severity Levels:**
- **ERROR** (priority 9-10): Build fails, non-negotiable (hardcoded secrets, Firebase in UI)
- **WARNING** (priority 7-8): Build passes but flagged (unvalidated input, missing preview)
- **INFO** (priority 5-6): Optional best practices (unused imports, style suggestions)

**Suppression (if needed):**
```kotlin
// In Kotlin: @Suppress annotation
@Suppress("MyCustomRule")
fun unsafeFunction() {
  const val GEMINI_API_KEY = "sk-proj-xyz" // OK with suppression
}

// In XML: tools:ignore
<LinearLayout tools:ignore="MyCustomRule">
  <!-- violation OK here -->
</LinearLayout>

// Via lint.xml (global baseline):
<issues>
  <issue id="MyCustomRule" severity="ignore" />
</issues>
```

**When to suppress:**
- ❌ Never (for ERROR severity)
- ✅ Rare exceptions (with code review approval + explanation)
- ✅ Test code (already separated in testDebugLint)

---

## Integration with Build

**Build Command:**
```bash
# Run lint
./gradlew lintDebug

# Build fails if ERROR severity violations found
./gradlew :app:assembleDebug
# ❌ FAILURE (if lint errors exist)
```

**Lint Baseline (ignore existing violations):**
```bash
# Create baseline for current violations
./gradlew lintDebug -Dlint.baselines.update=true

# Now build only fails on NEW violations
./gradlew assembleDebug
```

**Lint Report:**
```bash
./gradlew lintDebug
# Output: build/reports/lint-results-debug.html
# Open in browser for interactive report
```

---

## Lint Best Practices

1. **Make violations easy to fix:**
   - ✅ Clear message: "Use FirebaseRemoteConfig or BuildConfig"
   - ❌ Vague message: "Invalid usage"

2. **Use specific issue IDs:**
   - ✅ `id = "HardcodedApiKey"` (lowercase, descriptive)
   - ❌ `id = "RULE001"` (generic)

3. **Test both violation + valid code:**
   - Every detector needs tests for what fails + what passes

4. **Respect false positives:**
   - False positives destroy credibility; test edge cases

5. **Document in README:**
   - Example violation + how to fix it
   - When this rule matters (security, performance, etc.)

---

## Advanced: Custom Scopes

**Example: Check only ViewModels**
```kotlin
override fun getApplicableUastTypes() = listOf(UClass::class.java)

override fun visitClass(node: UClass) {
  // Only check ViewModels
  if (node.superTypes.any { it.name?.contains("ViewModel") == true }) {
    // Check for Firebase imports in this ViewModel
  }
}
```

**Example: Check only Composables**
```kotlin
override fun getApplicableUastTypes() = listOf(UMethod::class.java)

override fun visitMethod(node: UMethod) {
  // Check if method has @Composable annotation
  if (node.hasAnnotation("androidx.compose.runtime.Composable")) {
    // Check for disallowed patterns
  }
}
```

---

## References

- **Android Lint API:** https://developer.android.com/studio/write/lint (official docs)
- **UAST Reference:** IntelliJ IDEA plugin development docs
- **Existing Detectors:** See lint/README.md for all 13 detectors
- **Phase 2 Security Detectors:** HardcodedApiKeyDetector, SensitiveDataInLogsDetector, etc.

---

**Last Updated:** June 2026 | **Maintainer:** Sunil Pawar
