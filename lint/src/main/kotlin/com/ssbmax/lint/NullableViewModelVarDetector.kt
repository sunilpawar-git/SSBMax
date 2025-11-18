package com.ssbmax.lint

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.*
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UField

/**
 * Lint Detector: Prevents nullable mutable state in ViewModels
 * 
 * PURPOSE: Enforces MVVM architectural pattern where all mutable state
 * must be managed via StateFlow for lifecycle safety and configuration-change survival.
 * 
 * DETECTS:
 * 1. private var timerJob: Job? = null  ← Memory leak risk
 * 2. private var sessionId: String? = null  ← Not configuration-change safe
 * 
 * ALLOWS:
 * - StateFlow/MutableStateFlow (proper reactive state)
 * - LiveData (legacy but acceptable)
 * - Flow<T> (reactive streams)
 * 
 * ERROR SEVERITY: Fails build to prevent merging code with violations.
 */
class NullableViewModelVarDetector : Detector(), Detector.UastScanner {
    
    override fun applicableSuperClasses(): List<String> = listOf(
        "androidx.lifecycle.ViewModel"
    )
    
    override fun getApplicableUastTypes() = listOf(UClass::class.java)
    
    override fun createUastHandler(context: JavaContext) = object : UElementHandler() {
        override fun visitClass(node: UClass) {
            // Only check ViewModels (classes that extend androidx.lifecycle.ViewModel)
            if (!context.evaluator.extendsClass(node, "androidx.lifecycle.ViewModel", false)) {
                return
            }
            
            node.fields.forEach { field ->
                checkField(context, field)
            }
        }
    }
    
    private fun checkField(context: JavaContext, field: UField) {
        // Skip if not mutable
        if (field.isFinal || !field.isVar()) {
            return
        }
        
        val fieldType = field.type.canonicalText
        
        // Check 1: Detect Job? fields (memory leak risk)
        if (fieldType.contains("kotlinx.coroutines.Job?")) {
            context.report(
                issue = JOB_LEAK_ISSUE,
                location = context.getLocation(field),
                message = "Storing `Job?` as ViewModel field causes memory leaks. " +
                         "Use `viewModelScope.launch` directly without storing Job reference."
            )
            return
        }
        
        // Check 2: Detect nullable mutable vars (except allowed types)
        if (fieldType.endsWith("?") && !isAllowedType(fieldType)) {
            context.report(
                issue = NULLABLE_STATE_ISSUE,
                location = context.getLocation(field),
                message = "Mutable nullable state `${field.name}` should be in StateFlow. " +
                         "Move to UiState data class for lifecycle safety and configuration-change survival."
            )
        }
    }
    
    private fun isAllowedType(typeName: String): Boolean {
        val allowedPatterns = listOf(
            "StateFlow",
            "MutableStateFlow",
            "LiveData",
            "MutableLiveData",
            "Flow<"
        )
        return allowedPatterns.any { typeName.contains(it) }
    }
    
    private fun UField.isVar(): Boolean {
        return !this.isFinal
    }
    
    companion object {
        val NULLABLE_STATE_ISSUE = Issue.create(
            id = "NullableViewModelState",
            briefDescription = "Nullable mutable state in ViewModel",
            explanation = """
                All mutable state in ViewModels must be managed via StateFlow for:
                1. **Lifecycle safety**: Automatic cleanup when ViewModel is cleared
                2. **Configuration-change survival**: State persists through screen rotation
                3. **Reactive UI**: Compose/View can observe changes
                4. **Single source of truth**: No fragmented state
                
                **Bad Pattern:**
                ```kotlin
                private var sessionId: String? = null
                private var timerJob: Job? = null
                ```
                
                **Good Pattern:**
                ```kotlin
                data class UiState(
                    val sessionId: String? = null,
                    val isTimerActive: Boolean = false
                )
                private val _uiState = MutableStateFlow(UiState())
                ```
            """.trimIndent(),
            category = Category.CORRECTNESS,
            priority = 8,
            severity = Severity.ERROR,  // Fails build
            implementation = Implementation(
                NullableViewModelVarDetector::class.java,
                Scope.JAVA_FILE_SCOPE
            )
        )
        
        val JOB_LEAK_ISSUE = Issue.create(
            id = "ViewModelJobLeak",
            briefDescription = "Coroutine Job stored as ViewModel field",
            explanation = """
                Storing Job references as fields creates memory leak risk if not properly cancelled.
                ViewModelScope automatically cancels all jobs when ViewModel is cleared.
                
                **Bad Pattern:**
                ```kotlin
                private var timerJob: Job? = null
                
                fun startTimer() {
                    timerJob?.cancel()
                    timerJob = viewModelScope.launch { /* ... */ }
                }
                
                override fun onCleared() {
                    timerJob?.cancel()  // Easy to forget!
                }
                ```
                
                **Good Pattern:**
                ```kotlin
                fun startTimer() {
                    viewModelScope.launch {
                        // Automatically cancelled when ViewModel cleared
                    }
                }
                ```
            """.trimIndent(),
            category = Category.CORRECTNESS,
            priority = 9,
            severity = Severity.ERROR,  // Fails build
            implementation = Implementation(
                NullableViewModelVarDetector::class.java,
                Scope.JAVA_FILE_SCOPE
            )
        )
    }
}

