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
        "Use repository pattern instead. Firebase imports should only be in core:data module.",
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
        if (importPath.startsWith("com.google.firebase") ||
            importPath.startsWith("com.firebase")) {
          
          // Allow Firebase Auth in app layer (needed for current user checks)
          if (importPath.contains("firebase.auth")) {
            return
          }
          
          context.report(
            issue = ISSUE,
            location = context.getLocation(node),
            message = "Firebase ($importPath) should not be imported in UI layer. " +
              "Use repositories from core:data instead. Inject repository interface in ViewModel, not Firebase."
          )
        }
      }
    }
  }
}
