package com.ssbmax.lint

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.*
import org.jetbrains.uast.*

/**
 * Detects Composables in designsystem/ without @Preview.
 * 
 * Every reusable component should have a @Preview for:
 * - Visual validation during development
 * - Documentation (what does it look like?)
 * - Regression detection (if preview breaks, design changed unexpectedly)
 * 
 * Single-use screen Composables (in app/ui/) are exempt.
 */
class ComponentMissingPreviewDetector : Detector(), SourceCodeScanner {
  companion object {
    val ISSUE = Issue.create(
      id = "ComponentMissingPreview",
      briefDescription = "Designsystem component missing @Preview",
      explanation = "Every reusable Composable in core:designsystem should have a @Preview " +
        "for visual validation and documentation.\n\n" +
        "Add: @Preview @Composable fun MyComponentPreview() { MyComponent(...) }",
      category = Category.USABILITY,
      priority = 5,
      severity = Severity.WARNING,
      implementation = Implementation(
        ComponentMissingPreviewDetector::class.java,
        Scope.JAVA_FILE_SCOPE
      )
    )
  }

  override fun getApplicableUastTypes() = listOf(UMethod::class.java)

  override fun createUastHandler(context: JavaContext): UElementHandler {
    return object : UElementHandler() {
      override fun visitMethod(node: UMethod) {
        // Only check core:designsystem/ files
        val filePath = context.file.absolutePath
        if (!filePath.contains("designsystem/src") && 
            !filePath.contains("designsystem\\src")) {
          return
        }
        
        // Only check Composables
        if (!node.hasAnnotation("androidx.compose.runtime.Composable")) {
          return
        }
        
        // Check if it's a Composable (name starts with capital letter, returns Unit/void)
        val isComposable = node.name?.first()?.isUpperCase() == true &&
          node.returnTypeElement?.text?.contains("Unit") != false
        
        if (!isComposable) {
          return
        }
        
        // Check if file contains a matching Preview
        val fileContent = context.file.readText()
        val composableName = node.name ?: return
        
        // Look for @Preview followed by function with Preview in name or calls this composable
        val hasPreview = fileContent.contains("@Preview") &&
          (fileContent.contains("${composableName}Preview") ||
           fileContent.contains("fun ${composableName}LightPreview"))
        
        if (!hasPreview) {
          context.report(
            issue = ISSUE,
            location = context.getNameLocation(node),
            message = "Composable '$composableName' in designsystem should have a @Preview. " +
              "Add: @Preview @Composable fun ${composableName}Preview() { SSBMaxTheme { $composableName(...) } }"
          )
        }
      }
    }
  }
}
