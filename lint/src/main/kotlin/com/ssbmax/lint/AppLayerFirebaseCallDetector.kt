package com.ssbmax.lint

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.*
import org.jetbrains.uast.*

/**
 * Detects Firebase imports in UI layer (app module).
 * 
 * Firebase should NEVER be imported directly in:
 * - Composables (screens, components)
 * - ViewModels
 * 
 * Instead, use repositories and use cases.
 */
class AppLayerFirebaseCallDetector : Detector(), SourceCodeScanner {
  companion object {
    val ISSUE = Issue.create(
      id = "AppLayerFirebaseCall",
      briefDescription = "Firebase call in UI layer",
      explanation = "ViewModels and Composables should never directly import or call Firebase. " +
        "Use repository pattern instead — repository implementations live in `shared` " +
        "(GitLive Firebase, KMP-convergence plan's Phase 9f deleted the old `core:data` module).",
      category = Category.SECURITY,
      priority = 10,
      severity = Severity.ERROR,
      implementation = Implementation(
        AppLayerFirebaseCallDetector::class.java,
        Scope.JAVA_FILE_SCOPE
      )
    )
  }

  override fun getApplicableUastTypes() = listOf(UImportStatement::class.java)

  override fun createUastHandler(context: JavaContext): UElementHandler {
    return object : UElementHandler() {
      override fun visitImportStatement(node: UImportStatement) {
        val importPath = node.importReference?.asSourceString() ?: return

        // Check if in app module
        val filePath = context.file.absolutePath
        if (!filePath.contains("/app/src/") && !filePath.contains("\\app\\src\\")) {
          return
        }

        // Check if importing Firebase
        if (!importPath.startsWith("com.google.firebase") &&
            !importPath.startsWith("com.firebase")) {
          return
        }

        // Allow Firebase Auth in app layer (needed for current user checks)
        if (importPath.contains("firebase.auth")) {
          return
        }

        // The anti-pattern this rule targets is UI-layer code (ViewModels and
        // Composables) reaching into Firebase directly instead of going through
        // a repository. Infrastructure entry points legitimately need Firebase
        // and must NOT be flagged, e.g.:
        //  - FirebaseMessagingService subclasses (framework entry point)
        //  - the centralized Crashlytics wrapper (ErrorLogger)
        //  - DI-provided manager/data classes
        // So only report when the file actually declares a ViewModel or Composable.
        val uFile = node.getContainingUFile() ?: return
        if (!isUiLayerFile(uFile)) {
          return
        }

        context.report(
          issue = ISSUE,
          location = context.getLocation(node),
          message = "Firebase ($importPath) should not be imported in a ViewModel or Composable. " +
            "Use repositories from shared instead — inject a repository interface, not Firebase."
        )
      }
    }
  }

  /** True if the file declares a ViewModel subclass or any @Composable function. */
  private fun isUiLayerFile(uFile: UFile): Boolean {
    return uFile.classes.any { uClass ->
      uClass.name?.endsWith("ViewModel") == true ||
        uClass.methods.any { method ->
          method.uAnnotations.any { it.qualifiedName == COMPOSABLE_ANNOTATION }
        }
    }
  }
}

private const val COMPOSABLE_ANNOTATION = "androidx.compose.runtime.Composable"
