package com.ssbmax.lint

import org.junit.Assert.*
import org.junit.Test

class ComponentMissingPreviewDetectorTest {
  
  @Test
  fun `detector can be instantiated`() {
    val detector = ComponentMissingPreviewDetector()
    assertNotNull(detector)
  }
  
  @Test
  fun `issue is properly configured`() {
    val issue = ComponentMissingPreviewDetector.ISSUE
    assertEquals("ComponentMissingPreview", issue.id)
    assertNotNull(issue)
  }
  
  @Test
  fun `issue has WARNING severity`() {
    val issue = ComponentMissingPreviewDetector.ISSUE
    assertEquals(com.android.tools.lint.detector.api.Severity.WARNING, issue.defaultSeverity)
  }
  
  @Test
  fun `detector implements SourceCodeScanner`() {
    val detector = ComponentMissingPreviewDetector()
    val applicableTypes = detector.getApplicableUastTypes()
    assertNotNull(applicableTypes)
    assertTrue(applicableTypes.isNotEmpty())
  }
}
