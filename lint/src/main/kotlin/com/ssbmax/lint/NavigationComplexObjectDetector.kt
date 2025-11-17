package com.ssbmax.lint

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.*
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.ULambdaExpression
import org.jetbrains.uast.UParameter
import org.jetbrains.uast.getParentOfType

/**
 * Custom Lint Detector - Navigation Complex Object Anti-Pattern
 *
 * Detects navigation callbacks that pass complex objects instead of IDs.
 * This anti-pattern leads to:
 * - Not process-death safe (objects lost on configuration change)
 * - Transaction size limits (Parcelable has 1MB limit)
 * - Tight coupling between screens
 * - Difficult to deep link
 *
 * Example violations:
 * ```kotlin
 * @Composable
 * fun MyTestScreen(
 *     onComplete: (TestResult) -> Unit  // ERROR: Passing complex object
 * )
 *
 * @Composable
 * fun AnotherScreen(
 *     onSubmit: (User, Settings, Data) -> Unit  // ERROR: Multiple complex objects
 * )
 * ```
 *
 * Correct pattern:
 * ```kotlin
 * @Composable
 * fun MyTestScreen(
 *     onComplete: (submissionId: String, subscriptionType: SubscriptionType) -> Unit  // CORRECT
 * )
 *
 * // In ViewModel of result screen
 * fun loadResult(submissionId: String) {
 *     viewModelScope.launch {
 *         repository.getResult(submissionId)
 *     }
 * }
 * ```
 */
class NavigationComplexObjectDetector : Detector(), SourceCodeScanner {

    companion object {
        private const val ISSUE_ID = "NavigationComplexObject"
        private const val ISSUE_DESCRIPTION = "Navigation callback passes complex object instead of ID"
        private const val ISSUE_EXPLANATION = """
            Navigation callbacks in Compose should pass simple types (IDs, enums) rather than complex objects.

            Why this is important:
            • Process-death safety: Complex objects are lost on low memory/rotation
            • Size limits: Parcelable has 1MB transaction limit
            • Deep linking: Can't create deep links with complex objects
            • Decoupling: Destination should fetch its own data

            ❌ WRONG:
            ```kotlin
            @Composable
            fun TestScreen(
                onComplete: (TestResult) -> Unit  // ANTI-PATTERN
            ) {
                // Passes entire result object
                onComplete(testResult)
            }
            ```

            ✅ CORRECT:
            ```kotlin
            @Composable
            fun TestScreen(
                onComplete: (submissionId: String, subscriptionType: SubscriptionType) -> Unit
            ) {
                // Pass only ID and minimal metadata
                onComplete(submissionId, subscriptionType)
            }

            // In ResultScreen's ViewModel
            @HiltViewModel
            class ResultViewModel @Inject constructor(
                private val repository: SubmissionRepository
            ) : ViewModel() {
                fun loadResult(submissionId: String) {
                    viewModelScope.launch {
                        val result = repository.getResult(submissionId)
                        _uiState.value = result
                    }
                }
            }
            ```

            Allowed simple types:
            • String (IDs, routes)
            • Int, Long (numeric IDs)
            • Boolean (flags)
            • Enum classes
            • Unit (no parameters)
        """

        val ISSUE: Issue = Issue.create(
            id = ISSUE_ID,
            briefDescription = ISSUE_DESCRIPTION,
            explanation = ISSUE_EXPLANATION,
            category = Category.CORRECTNESS,
            priority = 7,
            severity = Severity.WARNING,  // Warning not error - some cases might be intentional
            implementation = Implementation(
                NavigationComplexObjectDetector::class.java,
                Scope.JAVA_FILE_SCOPE
            )
        )

        // Simple types that are safe to pass via navigation
        private val SAFE_PRIMITIVE_TYPES = setOf(
            "String",
            "Int",
            "Long",
            "Float",
            "Double",
            "Boolean",
            "Unit",
            "Char"
        )

        // Patterns that indicate navigation callbacks
        private val NAVIGATION_CALLBACK_PATTERNS = setOf(
            "onNavigate",
            "onComplete",
            "onSubmit",
            "onFinish",
            "onResult",
            "onTestComplete",
            "onNavigateTo"
        )
    }

    override fun getApplicableUastTypes() = listOf(UParameter::class.java)

    override fun createUastHandler(context: JavaContext): UElementHandler {
        return object : UElementHandler() {
            override fun visitParameter(node: UParameter) {
                // Only check parameters in @Composable functions
                val method = node.getParentOfType(org.jetbrains.uast.UMethod::class.java) ?: return
                if (!hasComposableAnnotation(method)) return

                // Check if parameter name suggests navigation callback
                val paramName = node.name
                if (!isNavigationCallback(paramName)) return

                // Check if parameter is a function type
                val typeText = node.type.canonicalText
                if (!typeText.contains("->")) return

                // Parse function parameters to check for complex types
                val complexParams = findComplexParameters(typeText)
                if (complexParams.isNotEmpty()) {
                    context.report(
                        issue = ISSUE,
                        location = context.getNameLocation(node),
                        message = "Navigation callback `$paramName` passes complex type(s): ${complexParams.joinToString()}. " +
                                "Pass IDs instead and fetch data in destination's ViewModel."
                    )
                }
            }
        }
    }

    /**
     * Check if method has @Composable annotation
     */
    private fun hasComposableAnnotation(method: org.jetbrains.uast.UMethod): Boolean {
        return method.uAnnotations.any { it.qualifiedName == "androidx.compose.runtime.Composable" }
    }

    /**
     * Check if parameter name suggests it's a navigation callback
     */
    private fun isNavigationCallback(paramName: String): Boolean {
        return NAVIGATION_CALLBACK_PATTERNS.any { pattern ->
            paramName.startsWith(pattern, ignoreCase = true)
        }
    }

    /**
     * Find complex parameter types in function signature
     * Returns list of complex type names
     */
    private fun findComplexParameters(functionType: String): List<String> {
        val complexParams = mutableListOf<String>()

        // Extract parameter types from function type string
        // Format: (ParamType1, ParamType2) -> ReturnType
        val paramsSection = functionType.substringAfter("(").substringBefore(")")
        if (paramsSection.isEmpty() || paramsSection == "Unit") {
            return emptyList()
        }

        // Split by comma (handling nested generics)
        val params = splitParameterTypes(paramsSection)

        for (param in params) {
            val cleanParam = param.trim()
            if (cleanParam.isEmpty()) continue

            // Extract the base type name
            val baseType = extractBaseTypeName(cleanParam)

            // Check if it's a complex type
            if (isComplexType(baseType)) {
                complexParams.add(baseType)
            }
        }

        return complexParams
    }

    /**
     * Split parameter types handling nested generics
     */
    private fun splitParameterTypes(params: String): List<String> {
        val result = mutableListOf<String>()
        var depth = 0
        var current = StringBuilder()

        for (char in params) {
            when (char) {
                '<' -> {
                    depth++
                    current.append(char)
                }
                '>' -> {
                    depth--
                    current.append(char)
                }
                ',' -> {
                    if (depth == 0) {
                        result.add(current.toString())
                        current = StringBuilder()
                    } else {
                        current.append(char)
                    }
                }
                else -> current.append(char)
            }
        }

        if (current.isNotEmpty()) {
            result.add(current.toString())
        }

        return result
    }

    /**
     * Extract base type name from potentially qualified type
     * E.g., "com.example.MyClass" -> "MyClass"
     *       "List<String>" -> "List"
     */
    private fun extractBaseTypeName(type: String): String {
        val withoutGenerics = type.substringBefore("<").substringBefore("?").trim()
        return withoutGenerics.substringAfterLast(".")
    }

    /**
     * Check if a type is complex (not a safe primitive or enum)
     */
    private fun isComplexType(typeName: String): Boolean {
        // Allow safe primitive types
        if (SAFE_PRIMITIVE_TYPES.contains(typeName)) {
            return false
        }

        // Allow enum types (usually end with Type, Status, etc.)
        if (typeName.endsWith("Type") ||
            typeName.endsWith("Status") ||
            typeName.endsWith("Mode") ||
            typeName.endsWith("State") && typeName.length < 15  // Short enum-like names
        ) {
            return false
        }

        // Allow Unit lambda (no parameters)
        if (typeName == "Unit") {
            return false
        }

        // Everything else is considered complex
        return true
    }
}
