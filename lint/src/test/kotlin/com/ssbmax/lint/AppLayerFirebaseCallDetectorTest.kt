package com.ssbmax.lint

import org.junit.Assert.*
import org.junit.Test

class AppLayerFirebaseCallDetectorTest {
  
  @Test
  fun `detector can be instantiated`() {
    val detector = AppLayerFirebaseCallDetector()
    assertNotNull(detector)
  }
  
  @Test
  fun `issue is properly configured`() {
    val issue = AppLayerFirebaseCallDetector.ISSUE
    assertEquals("AppLayerFirebaseCall", issue.id)
    assertNotNull(issue)
  }
  
  @Test
  fun `issue has ERROR severity`() {
    val issue = AppLayerFirebaseCallDetector.ISSUE
    assertEquals(com.android.tools.lint.detector.api.Severity.ERROR, issue.defaultSeverity)
  }
  
  @Test
  fun `detector implements SourceCodeScanner`() {
    val detector = AppLayerFirebaseCallDetector()
    val applicableTypes = detector.getApplicableUastTypes()
    assertNotNull(applicableTypes)
    assertTrue(applicableTypes.isNotEmpty())
  }
}
