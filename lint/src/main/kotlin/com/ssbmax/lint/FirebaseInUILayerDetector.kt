package com.ssbmax.lint

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.*
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UFile
import org.jetbrains.uast.UImportStatement

/**
 * Lint detector that prevents Firebase dependencies in the UI layer
 *
 * WHY: Clean Architecture principle - UI layer should not depend on infrastructure details
 * WHAT: Detects Firebase imports in app/src/main/kotlin/com/ssbmax/ui/
 * HOW: Use domain abstractions like ObserveCurrentUserUseCase or repositories
 *
 * Examples:
 * ❌ WRONG:
 *   import com.google.firebase.auth.FirebaseAuth
 *   class MyViewModel @Inject constructor(private val auth: FirebaseAuth)
 *
 * ✅ CORRECT:
 *   import com.ssbmax.core.domain.usecase.auth.ObserveCurrentUserUseCase
 *   class MyViewModel @Inject constructor(private val observeUser: ObserveCurrentUserUseCase)
 */
class FirebaseInUILayerDetector : Detector(), SourceCodeScanner {

    companion object {
        private const val EXPLANATION = """
            UI layer (app/src/main/kotlin/com/ssbmax/ui/) should not import Firebase classes directly. \
            This violates Clean Architecture principles and makes testing difficult.

            **Problem:**
            - Tight coupling to Firebase SDK (hard to test, hard to switch providers)
            - Violates Dependency Inversion Principle
            - Inconsistent with rest of codebase

            **Solution:**
            Use domain layer abstractions instead:

            For authentication:
            - Use `ObserveCurrentUserUseCase` to get current user
            - Use `AuthRepository` for auth operations

            For data access:
            - Use repository interfaces from core/domain
            - Implement repositories in core/data (where Firebase is allowed)

            **Example:**
            ```kotlin
            // ❌ WRONG
            @HiltViewModel
            class MyViewModel @Inject constructor(
                private val firebaseAuth: FirebaseAuth
            ) : ViewModel()

            // ✅ CORRECT
            @HiltViewModel
            class MyViewModel @Inject constructor(
                private val observeCurrentUser: ObserveCurrentUserUseCase
            ) : ViewModel()
            ```
        """

        val ISSUE = Issue.create(
            id = "FirebaseInUILayer",
            briefDescription = "Firebase dependencies in UI layer violate Clean Architecture",
            explanation = EXPLANATION,
            category = Category.CORRECTNESS,
            priority = 8,
            severity = Severity.ERROR,
            implementation = Implementation(
                FirebaseInUILayerDetector::class.java,
                Scope.JAVA_FILE_SCOPE
            )
        )

        private val FIREBASE_PACKAGES = listOf(
            "com.google.firebase.auth",
            "com.google.firebase.firestore",
            "com.google.firebase.storage",
            "com.google.firebase.database",
            "com.google.firebase.functions",
            "com.google.firebase.messaging",
            "com.google.firebase.remoteconfig"
        )
    }

    override fun getApplicableUastTypes(): List<Class<out UElement>> {
        return listOf(UImportStatement::class.java)
    }

    override fun createUastHandler(context: JavaContext): UElementHandler {
        return object : UElementHandler() {
            override fun visitImportStatement(node: UImportStatement) {
                checkImport(context, node)
            }
        }
    }

    private fun checkImport(context: JavaContext, import: UImportStatement) {
        val importPath = import.importReference?.asSourceString() ?: return

        // Check if this is a Firebase import
        val isFirebaseImport = FIREBASE_PACKAGES.any { importPath.startsWith(it) }
        if (!isFirebaseImport) return

        // Check if file is in UI layer
        val file = context.file
        val isInUILayer = file.path.contains("/com/ssbmax/ui/")
        if (!isInUILayer) return

        // Report violation
        val message = buildErrorMessage(importPath)
        context.report(
            issue = ISSUE,
            location = context.getLocation(import),
            message = message
        )
    }

    private fun buildErrorMessage(importPath: String): String {
        return when {
            importPath.contains("firebase.auth") -> {
                "Do not import Firebase Auth in UI layer. Use `ObserveCurrentUserUseCase` or `AuthRepository` instead."
            }
            importPath.contains("firebase.firestore") -> {
                "Do not import Firestore in UI layer. Create a repository in core/data and use it via dependency injection."
            }
            importPath.contains("firebase.storage") -> {
                "Do not import Firebase Storage in UI layer. Create a repository in core/data for storage operations."
            }
            else -> {
                "Do not import Firebase classes in UI layer. Use domain layer abstractions (repositories, use cases) instead."
            }
        }
    }
}
