package com.ssbmax.lint

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.*
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UField

/**
 * Custom Lint Detector - Singleton Mutable State Anti-Pattern
 *
 * Detects singleton objects (Kotlin `object` declarations) that contain mutable state.
 * This is an anti-pattern that leads to:
 * - Memory leaks
 * - State pollution between tests
 * - Not process-death safe
 * - Difficult to test
 * - Thread safety issues
 *
 * Example violation:
 * ```kotlin
 * object MyHolder {
 *     private var state: String? = null  // ERROR: Mutable state in singleton
 * }
 * ```
 *
 * Correct alternatives:
 * ```kotlin
 * // 1. Use ViewModel for UI state
 * class MyViewModel : ViewModel() {
 *     private val _state = MutableStateFlow<String?>(null)
 *     val state = _state.asStateFlow()
 * }
 *
 * // 2. Use Repository for data
 * class MyRepository @Inject constructor() {
 *     suspend fun getData(): Result<Data>
 * }
 *
 * // 3. Pass data through navigation
 * navController.navigate("route/${dataId}")
 * ```
 */
class SingletonMutableStateDetector : Detector(), SourceCodeScanner {

    companion object {
        private const val ISSUE_ID = "SingletonMutableState"
        private const val ISSUE_DESCRIPTION = "Singleton object with mutable state"
        private val ISSUE_EXPLANATION = """
            Singleton objects (Kotlin `object` declarations) should not contain mutable state.

            This anti-pattern leads to:
            • Memory leaks (state persists forever)
            • State pollution between tests
            • Not process-death safe (state lost on low memory)
            • Thread safety issues
            • Difficult to test and mock

            ❌ WRONG:
            ```kotlin
            object UserHolder {
                var currentUser: User? = null  // ANTI-PATTERN
            }
            ```

            ✅ CORRECT alternatives:

            1. Use ViewModel for UI state:
            ```kotlin
            @HiltViewModel
            class MyViewModel @Inject constructor() : ViewModel() {
                private val _state = MutableStateFlow<User?>(null)
                val state = _state.asStateFlow()
            }
            ```

            2. Use Repository for data management:
            ```kotlin
            @Singleton
            class UserRepository @Inject constructor() {
                suspend fun getCurrentUser(): Result<User>
            }
            ```

            3. Pass IDs via navigation, fetch in ViewModel:
            ```kotlin
            // Navigation
            navController.navigate("details/${'$'}userId")

            // In ViewModel
            fun loadUser(userId: String) {
                viewModelScope.launch {
                    repository.getUser(userId)
                }
            }
            ```

            Safe singleton patterns (exceptions):
            • Immutable constants (val with immutable types)
            • WeakReference-based caches
            • Thread-safe concurrent collections (ConcurrentHashMap)
            • Pure utility functions (no state)
        """

        val ISSUE: Issue = Issue.create(
            id = ISSUE_ID,
            briefDescription = ISSUE_DESCRIPTION,
            explanation = ISSUE_EXPLANATION,
            category = Category.CORRECTNESS,
            priority = 9,  // High priority - this is a serious architectural issue
            severity = Severity.ERROR,
            implementation = Implementation(
                SingletonMutableStateDetector::class.java,
                Scope.JAVA_FILE_SCOPE
            )
        )

        // Safe patterns that we should allow
        private val SAFE_TYPES = setOf(
            "WeakReference",
            "ConcurrentHashMap",
            "ConcurrentLinkedQueue",
            "CopyOnWriteArrayList",
            "AtomicReference",
            "AtomicInteger",
            "AtomicBoolean"
        )
    }

    override fun getApplicableUastTypes() = listOf(UClass::class.java)

    override fun createUastHandler(context: JavaContext): UElementHandler {
        return object : UElementHandler() {
            override fun visitClass(node: UClass) {
                // Only check Kotlin object declarations (singletons)
                val sourcePsi = node.sourcePsi
                if (sourcePsi !is KtObjectDeclaration) return

                // Skip companion objects (they're different pattern)
                if (sourcePsi.isCompanion()) return

                // Check all fields in the object
                for (field in node.fields) {
                    if (isMutableStateField(field)) {
                        // Check if it's a safe pattern
                        if (isSafePattern(field)) continue

                        context.report(
                            issue = ISSUE,
                            location = context.getNameLocation(field),
                            message = "Singleton object `${node.name}` contains mutable state `${field.name}`. " +
                                    "Use ViewModel, Repository, or pass data via navigation instead."
                        )
                    }
                }
            }
        }
    }

    /**
     * Check if a field represents mutable state
     */
    private fun isMutableStateField(field: UField): Boolean {
        // Check if it's a var (mutable variable)
        if (field.sourcePsi?.text?.contains("var ") == true) {
            return true
        }

        // Check for mutable collections even if declared as val
        val typeName = field.type.canonicalText
        if (typeName.contains("Mutable") ||
            typeName.contains("ArrayList") ||
            typeName.contains("HashMap") ||
            typeName.contains("HashSet") ||
            typeName.contains("LinkedList")
        ) {
            return true
        }

        // Check for Compose mutable state
        if (typeName.contains("mutableStateOf") ||
            typeName.contains("MutableState") ||
            typeName.contains("MutableStateFlow") ||
            typeName.contains("MutableSharedFlow")
        ) {
            return true
        }

        return false
    }

    /**
     * Check if this is a safe pattern that we should allow
     */
    private fun isSafePattern(field: UField): Boolean {
        val typeName = field.type.canonicalText

        // Allow WeakReference and thread-safe concurrent collections
        for (safeType in SAFE_TYPES) {
            if (typeName.contains(safeType)) {
                return true
            }
        }

        // Allow lateinit var that are clearly for dependency injection
        // (though this is still not ideal, it's a common pattern)
        if (field.sourcePsi?.text?.contains("lateinit") == true &&
            field.sourcePsi?.text?.contains("@Inject") == true
        ) {
            return true
        }

        return false
    }
}
