package com.ssbmax.lint

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.*
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.visitor.AbstractUastVisitor

/**
 * Custom Lint Detector - ViewModel Lifecycle Management
 *
 * Detects ViewModels that start coroutines/timers/jobs but don't cancel them in onCleared().
 * This leads to memory leaks and wasted resources.
 *
 * Example violation:
 * ```kotlin
 * class MyViewModel : ViewModel() {
 *     private val timer = Timer()  // ERROR: Not cancelled in onCleared()
 *
 *     fun startTimer() {
 *         timer.schedule(...)
 *     }
 * }
 * ```
 *
 * Correct pattern:
 * ```kotlin
 * class MyViewModel : ViewModel() {
 *     private val timer = Timer()
 *
 *     override fun onCleared() {
 *         super.onCleared()
 *         timer.cancel()  // CORRECT: Cleanup in onCleared()
 *     }
 * }
 * ```
 */
class ViewModelLifecycleDetector : Detector(), SourceCodeScanner {

    companion object {
        private const val ISSUE_ID = "ViewModelLifecycleCleanup"
        private const val ISSUE_DESCRIPTION = "ViewModel doesn't cancel resources in onCleared()"
        private const val ISSUE_EXPLANATION = """
            ViewModels that create Timers, Jobs, or other resources should cancel them in onCleared()
            to prevent memory leaks.

            Common resources that need cleanup:
            • Timer / TimerTask
            • CoroutineScope / Job
            • Flow / Channel subscriptions
            • Listeners / Callbacks
            • Database cursors
            • Network connections

            ❌ WRONG:
            ```kotlin
            class MyViewModel : ViewModel() {
                private val timer = Timer()  // LEAK: Never cancelled

                fun startPolling() {
                    timer.scheduleAtFixedRate(...)
                }
            }
            ```

            ✅ CORRECT:
            ```kotlin
            class MyViewModel : ViewModel() {
                private val timer = Timer()

                override fun onCleared() {
                    super.onCleared()
                    timer.cancel()  // Cleanup
                }
            }
            ```

            ✅ BEST: Use viewModelScope (auto-cancelled):
            ```kotlin
            class MyViewModel : ViewModel() {
                fun startPolling() {
                    viewModelScope.launch {
                        // Automatically cancelled when ViewModel is cleared
                    }
                }
            }
            ```
        """

        val ISSUE: Issue = Issue.create(
            id = ISSUE_ID,
            briefDescription = ISSUE_DESCRIPTION,
            explanation = ISSUE_EXPLANATION,
            category = Category.CORRECTNESS,
            priority = 8,
            severity = Severity.ERROR,
            implementation = Implementation(
                ViewModelLifecycleDetector::class.java,
                Scope.JAVA_FILE_SCOPE
            )
        )

        // Resource types that need cleanup
        private val RESOURCE_TYPES = setOf(
            "Timer",
            "TimerTask",
            "Job",
            "CoroutineScope",
            "Channel",
            "BroadcastChannel",
            "Closeable"
        )

        // Methods that indicate cleanup
        private val CLEANUP_METHODS = setOf(
            "cancel",
            "close",
            "dispose",
            "shutdown",
            "stop",
            "release"
        )
    }

    override fun getApplicableUastTypes() = listOf(UClass::class.java)

    override fun createUastHandler(context: JavaContext): UElementHandler {
        return object : UElementHandler() {
            override fun visitClass(node: UClass) {
                // Only check classes that extend ViewModel
                if (!isViewModel(node)) return

                // Find fields that are resource types
                val resourceFields = node.fields.filter { field ->
                    RESOURCE_TYPES.any { resourceType ->
                        field.type.canonicalText.contains(resourceType)
                    }
                }

                if (resourceFields.isEmpty()) return

                // Check if onCleared() exists and cleans up resources
                val onClearedMethod = node.methods.find { it.name == "onCleared" }

                if (onClearedMethod == null) {
                    // No onCleared method - report all resource fields
                    for (field in resourceFields) {
                        context.report(
                            issue = ISSUE,
                            location = context.getNameLocation(field),
                            message = "ViewModel has ${field.type.canonicalText} `${field.name}` but no onCleared() method. " +
                                    "Add onCleared() and cancel this resource to prevent leaks."
                        )
                    }
                } else {
                    // onCleared exists - check if it cleans up each resource
                    for (field in resourceFields) {
                        if (!isResourceCleanedUp(field.name, onClearedMethod)) {
                            context.report(
                                issue = ISSUE,
                                location = context.getNameLocation(field),
                                message = "${field.type.canonicalText} `${field.name}` is not cancelled in onCleared(). " +
                                        "Call ${field.name}.cancel() or similar cleanup method."
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * Check if class extends ViewModel
     */
    private fun isViewModel(uClass: UClass): Boolean {
        var currentClass: UClass? = uClass
        while (currentClass != null) {
            if (currentClass.qualifiedName == "androidx.lifecycle.ViewModel") {
                return true
            }
            currentClass = currentClass.superClass
        }
        return false
    }

    /**
     * Check if a resource field is cleaned up in onCleared method
     */
    private fun isResourceCleanedUp(fieldName: String, onClearedMethod: UMethod): Boolean {
        var foundCleanup = false

        onClearedMethod.accept(object : AbstractUastVisitor() {
            override fun visitMethod(node: UMethod): Boolean {
                // Check method body for cleanup calls
                val methodText = node.sourcePsi?.text ?: ""

                // Look for patterns like:
                // fieldName.cancel()
                // fieldName.close()
                // fieldName?.cancel()
                for (cleanupMethod in CLEANUP_METHODS) {
                    if (methodText.contains("$fieldName.$cleanupMethod") ||
                        methodText.contains("$fieldName?.$cleanupMethod")
                    ) {
                        foundCleanup = true
                        return true
                    }
                }

                return super.visitMethod(node)
            }
        })

        return foundCleanup
    }
}
