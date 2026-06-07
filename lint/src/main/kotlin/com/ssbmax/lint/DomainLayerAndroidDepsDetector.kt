package com.ssbmax.lint

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.*
import org.jetbrains.uast.*

/**
 * Detects Android framework imports in domain layer.
 * 
 * core:domain should be 100% pure Kotlin with ZERO Android dependencies.
 * This enables:
 * - JVM unit testing (no Android context needed)
 * - Reusability in other platforms (web, CLI tools)
 * - Testability (no mocking Android classes)
 * 
 * Violations:
 * - import android.*
 * - import androidx.* (except maybe @Keep annotations)
 * - import com.android.*
 */
class DomainLayerAndroidDepsDetector : Detector(), SourceCodeScanner {
  companion object {
    val ISSUE = Issue.create(
      id = "DomainLayerAndroidDeps",
      briefDescription = "Android dependency in domain layer",
      explanation = "core:domain should have zero Android dependencies. " +
        "It should be pure Kotlin: use cases, models, repository interfaces only.\n\n" +
        "If you need Android context, move logic to core:data or app layer.",
      category = Category.CORRECTNESS,
      priority = 9,
      severity = Severity.ERROR,
      implementation = Implementation(
        DomainLayerAndroidDepsDetector::class.java,
        Scope.JAVA_FILE_SCOPE
      )
    )
  }

  override fun getApplicableUastTypes() = listOf(UImportStatement::class.java)

  override fun createUastHandler(context: JavaContext): UElementHandler {
    return object : UElementHandler() {
      override fun visitImportStatement(node: UImportStatement) {
        val importPath = node.importReference?.asSourceString() ?: return
        
        // Check if in core:domain module
        val filePath = context.file.absolutePath
        if (!filePath.contains("core/domain") && !filePath.contains("core\\domain")) {
          return
        }
        
        // Check for Android framework imports
        val isAndroidImport = importPath.startsWith("android.") ||
          importPath.startsWith("androidx.") ||
          importPath.startsWith("com.android.")
        
        if (isAndroidImport) {
          context.report(
            issue = ISSUE,
            location = context.getLocation(node),
            message = "core:domain must not depend on Android. " +
              "Import ($importPath) violates clean architecture. " +
              "Move this logic to core:data (repository impl) or app (ViewModel)."
          )
        }
      }
    }
  }
}
