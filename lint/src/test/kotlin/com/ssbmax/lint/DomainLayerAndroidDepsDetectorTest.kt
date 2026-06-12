package com.ssbmax.lint

import org.junit.Assert.*
import org.junit.Test

class DomainLayerAndroidDepsDetectorTest {
  
  @Test
  fun `detector can be instantiated`() {
    val detector = DomainLayerAndroidDepsDetector()
    assertNotNull(detector)
  }
  
  @Test
  fun `issue is properly configured`() {
    val issue = DomainLayerAndroidDepsDetector.ISSUE
    assertEquals("DomainLayerAndroidDeps", issue.id)
    assertNotNull(issue)
  }
  
  @Test
  fun `issue has ERROR severity`() {
    val issue = DomainLayerAndroidDepsDetector.ISSUE
    assertEquals(com.android.tools.lint.detector.api.Severity.ERROR, issue.defaultSeverity)
  }
  
  @Test
  fun `detector implements SourceCodeScanner`() {
    val detector = DomainLayerAndroidDepsDetector()
    val applicableTypes = detector.getApplicableUastTypes()
    assertNotNull(applicableTypes)
    assertTrue(applicableTypes.isNotEmpty())
  }
}
