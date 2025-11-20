package com.ssbmax.lint

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.*
import org.jetbrains.uast.UBinaryExpression
import org.jetbrains.uast.UBinaryExpressionWithType
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UQualifiedReferenceExpression

/**
 * Custom Lint Detector - StateFlow .value = .value.copy() Anti-Pattern
 *
 * Detects usage of `_uiState.value = _uiState.value.copy(...)` which is an anti-pattern because:
 * - NOT thread-safe (read-modify-write race condition)
 * - Can lead to lost updates in concurrent scenarios
 * - Android best practice is to use .update {} for atomic mutations
 *
 * Example violation:
 * ```kotlin
 * private val _uiState = MutableStateFlow(MyState())
 * 
 * fun updateData() {
 *     _uiState.value = _uiState.value.copy(isLoading = true)  // ❌ ERROR
 * }
 * ```
 *
 * Correct alternative:
 * ```kotlin
 * private val _uiState = MutableStateFlow(MyState())
 * 
 * fun updateData() {
 *     _uiState.update { it.copy(isLoading = true) }  // ✅ CORRECT
 * }
 * ```
 *
 * Why .update {} is better:
 * - Thread-safe (atomic read-modify-write with internal locking)
 * - Prevents lost updates in concurrent scenarios
 * - Official Kotlin Coroutines best practice
 * - Cleaner, more idiomatic code
 *
 * This lint rule has ERROR severity and will fail the build.
 */
class StateFlowValueAssignmentDetector : Detector(), SourceCodeScanner {

    companion object {
        private const val ISSUE_ID = "StateFlowValueAssignment"
        private const val ISSUE_DESCRIPTION = "Use .update {} instead of .value = .value.copy()"
        private const val ISSUE_EXPLANATION = """
            Using `_uiState.value = _uiState.value.copy(...)` is NOT thread-safe and can cause race conditions.

            The Problem - Race Condition:
            ```kotlin
            // Thread 1 reads: {count: 5}
            _uiState.value = _uiState.value.copy(count = _uiState.value.count + 1)
            // Thread 2 reads: {count: 5} (before Thread 1 writes!)
            _uiState.value = _uiState.value.copy(count = _uiState.value.count + 1)
            // Result: count = 6 (WRONG! Should be 7 - one update was lost)
            ```

            ❌ WRONG - Not thread-safe:
            ```kotlin
            _uiState.value = _uiState.value.copy(isLoading = true)
            _state.value = _state.value.copy(data = newData)
            ```

            ✅ CORRECT - Thread-safe with .update {}:
            ```kotlin
            _uiState.update { it.copy(isLoading = true) }
            _state.update { it.copy(data = newData) }
            ```

            Why .update {} is superior:
            • Thread-safe: Uses internal locking for atomic read-modify-write
            • Prevents lost updates in concurrent coroutines
            • Official Kotlin Coroutines best practice
            • More idiomatic and cleaner code
            • Same performance (microsecond overhead is negligible)

            Real-world scenario where this matters:
            ```kotlin
            // User taps button twice quickly
            viewModelScope.launch {
                // Both coroutines read count = 0
                _uiState.value = _uiState.value.copy(count = _uiState.value.count + 1)
            }
            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(count = _uiState.value.count + 1)
            }
            // Result: count = 1 (lost one update!)
            
            // With .update {}, result is always correct: count = 2
            ```

            Documentation:
            https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/update.html
        """

        val ISSUE: Issue = Issue.create(
            id = ISSUE_ID,
            briefDescription = ISSUE_DESCRIPTION,
            explanation = ISSUE_EXPLANATION,
            category = Category.CORRECTNESS,
            priority = 8,
            severity = Severity.ERROR,  // Fail the build
            implementation = Implementation(
                StateFlowValueAssignmentDetector::class.java,
                Scope.JAVA_FILE_SCOPE
            )
        )
    }

    override fun getApplicableUastTypes() = listOf(UBinaryExpression::class.java)

    override fun createUastHandler(context: JavaContext): UElementHandler {
        return object : UElementHandler() {
            override fun visitBinaryExpression(node: UBinaryExpression) {
                // Check if this is an assignment (=)
                if (node.operator.text != "=") return

                val leftOperand = node.leftOperand
                val rightOperand = node.rightOperand

                // Check if left side is *.value (e.g., _uiState.value)
                if (!isStateFlowValueAccess(leftOperand)) return

                // Check if right side is *.value.copy(...) (e.g., _uiState.value.copy(...))
                if (!isStateFlowValueCopyCall(rightOperand)) return

                // Extract StateFlow variable name from left side
                val stateFlowName = extractStateFlowName(leftOperand) ?: return

                // Report the violation
                val location = context.getLocation(node)
                
                context.report(
                    issue = ISSUE,
                    location = location,
                    message = "Use `$stateFlowName.update { it.copy(...) }` instead of `$stateFlowName.value = $stateFlowName.value.copy(...)` for thread-safe state updates",
                    quickfixData = createQuickFix(node, stateFlowName)
                )
            }
        }
    }

    /**
     * Check if expression is *.value (e.g., _uiState.value)
     */
    private fun isStateFlowValueAccess(expr: org.jetbrains.uast.UExpression): Boolean {
        if (expr !is UQualifiedReferenceExpression) return false
        val selector = expr.selector
        return selector.asSourceString() == "value"
    }

    /**
     * Check if expression is *.value.copy(...) pattern
     */
    private fun isStateFlowValueCopyCall(expr: org.jetbrains.uast.UExpression?): Boolean {
        if (expr == null) return false
        
        // Check if it's a qualified reference (something.something)
        if (expr !is UQualifiedReferenceExpression) return false
        
        // Check if the selector is a copy() call
        val selector = expr.selector
        if (selector !is UCallExpression) return false
        if (selector.methodName != "copy") return false
        
        // Check if the receiver is *.value
        val receiver = expr.receiver
        return isStateFlowValueAccess(receiver)
    }

    /**
     * Extract StateFlow variable name from expression (e.g., "_uiState" from "_uiState.value")
     */
    private fun extractStateFlowName(expr: org.jetbrains.uast.UExpression): String? {
        if (expr !is UQualifiedReferenceExpression) return null
        val receiver = expr.receiver
        return receiver.asSourceString()
    }

    /**
     * Create a quick fix to replace .value = .value.copy() with .update {}
     */
    private fun createQuickFix(node: UBinaryExpression, stateFlowName: String): LintFix? {
        val rightOperand = node.rightOperand as? UQualifiedReferenceExpression ?: return null
        val copyCall = rightOperand.selector as? UCallExpression ?: return null
        
        // Extract copy() arguments
        val copyArgs = copyCall.valueArguments.joinToString(", ") { arg ->
            arg.asSourceString()
        }
        
        val oldText = node.sourcePsi?.text ?: return null
        val newText = "$stateFlowName.update { it.copy($copyArgs) }"
        
        return fix()
            .name("Replace with .update { it.copy(...) }")
            .replace()
            .text(oldText)
            .with(newText)
            .shortenNames()
            .reformat(true)
            .build()
    }
}

