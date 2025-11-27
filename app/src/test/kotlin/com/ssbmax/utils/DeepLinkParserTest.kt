package com.ssbmax.utils

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for DeepLinkParser
 * Tests deep link parsing and building logic used for notification navigation
 */
class DeepLinkParserTest {

    // ========================
    // Constants Tests
    // ========================

    @Test
    fun `scheme constant has correct value`() {
        assertEquals("ssbmax://", DeepLinkParser.SCHEME)
    }

    @Test
    fun `route constants have correct values`() {
        assertEquals("interview/result/", DeepLinkParser.ROUTE_INTERVIEW_RESULT)
        assertEquals("interview/history", DeepLinkParser.ROUTE_INTERVIEW_HISTORY)
    }

    // ========================
    // buildInterviewResultDeepLink() Tests
    // ========================

    @Test
    fun `buildInterviewResultDeepLink creates correct deep link`() {
        val resultId = "abc123"
        val deepLink = DeepLinkParser.buildInterviewResultDeepLink(resultId)
        assertEquals("ssbmax://interview/result/abc123", deepLink)
    }

    @Test
    fun `buildInterviewResultDeepLink handles Firebase-style IDs`() {
        val firebaseId = "1a2B3c4D5e6F7g8H9i0J"
        val deepLink = DeepLinkParser.buildInterviewResultDeepLink(firebaseId)
        assertEquals("ssbmax://interview/result/$firebaseId", deepLink)
    }

    // ========================
    // buildInterviewHistoryDeepLink() Tests
    // ========================

    @Test
    fun `buildInterviewHistoryDeepLink creates correct deep link`() {
        val deepLink = DeepLinkParser.buildInterviewHistoryDeepLink()
        assertEquals("ssbmax://interview/history", deepLink)
    }

    // ========================
    // parseToRoute() Tests
    // ========================

    @Test
    fun `parseToRoute returns route for valid ssbmax deep link`() {
        val deepLink = "ssbmax://interview/result/abc123"
        val result = DeepLinkParser.parseToRoute(deepLink)
        assertEquals("interview/result/abc123", result)
    }

    @Test
    fun `parseToRoute returns route without scheme if already a route`() {
        val route = "interview/result/abc123"
        val result = DeepLinkParser.parseToRoute(route)
        assertEquals("interview/result/abc123", result)
    }

    @Test
    fun `parseToRoute returns null for null input`() {
        val result = DeepLinkParser.parseToRoute(null)
        assertNull(result)
    }

    @Test
    fun `parseToRoute returns null for blank input`() {
        val result = DeepLinkParser.parseToRoute("")
        assertNull(result)
        
        val result2 = DeepLinkParser.parseToRoute("   ")
        assertNull(result2)
    }

    @Test
    fun `parseToRoute returns null for unsupported scheme`() {
        val deepLink = "https://ssbmax.com/interview/result/abc123"
        val result = DeepLinkParser.parseToRoute(deepLink)
        assertNull(result)
    }

    @Test
    fun `parseToRoute handles various valid routes`() {
        // Interview result
        assertEquals(
            "interview/result/xyz789",
            DeepLinkParser.parseToRoute("ssbmax://interview/result/xyz789")
        )
        
        // Interview history
        assertEquals(
            "interview/history",
            DeepLinkParser.parseToRoute("ssbmax://interview/history")
        )
        
        // Student home
        assertEquals(
            "student/home",
            DeepLinkParser.parseToRoute("ssbmax://student/home")
        )
    }

    // ========================
    // Round-trip Tests (build -> parse)
    // ========================

    @Test
    fun `round trip interview result deep link works correctly`() {
        val resultId = "test-result-456"
        val deepLink = DeepLinkParser.buildInterviewResultDeepLink(resultId)
        val parsedRoute = DeepLinkParser.parseToRoute(deepLink)
        val extractedId = DeepLinkParser.extractInterviewResultId(deepLink)
        
        assertEquals("interview/result/$resultId", parsedRoute)
        assertEquals(resultId, extractedId)
    }

    @Test
    fun `round trip interview history deep link works correctly`() {
        val deepLink = DeepLinkParser.buildInterviewHistoryDeepLink()
        val parsedRoute = DeepLinkParser.parseToRoute(deepLink)
        
        assertEquals("interview/history", parsedRoute)
    }

    // ========================
    // isInterviewResultLink() Tests
    // ========================

    @Test
    fun `isInterviewResultLink returns true for interview result links`() {
        assertTrue(DeepLinkParser.isInterviewResultLink("ssbmax://interview/result/abc123"))
        assertTrue(DeepLinkParser.isInterviewResultLink("interview/result/abc123"))
    }

    @Test
    fun `isInterviewResultLink returns false for non-interview links`() {
        assertFalse(DeepLinkParser.isInterviewResultLink("ssbmax://interview/history"))
        assertFalse(DeepLinkParser.isInterviewResultLink("ssbmax://student/home"))
        assertFalse(DeepLinkParser.isInterviewResultLink(null))
        assertFalse(DeepLinkParser.isInterviewResultLink(""))
    }

    // ========================
    // extractInterviewResultId() Tests
    // ========================

    @Test
    fun `extractInterviewResultId returns correct ID from deep link`() {
        val deepLink = "ssbmax://interview/result/abc123"
        val resultId = DeepLinkParser.extractInterviewResultId(deepLink)
        assertEquals("abc123", resultId)
    }

    @Test
    fun `extractInterviewResultId returns correct ID from route`() {
        val route = "interview/result/result-xyz-789"
        val resultId = DeepLinkParser.extractInterviewResultId(route)
        assertEquals("result-xyz-789", resultId)
    }

    @Test
    fun `extractInterviewResultId returns null for non-interview links`() {
        assertNull(DeepLinkParser.extractInterviewResultId("ssbmax://student/home"))
        assertNull(DeepLinkParser.extractInterviewResultId("interview/history"))
        assertNull(DeepLinkParser.extractInterviewResultId(null))
    }

    @Test
    fun `extractInterviewResultId returns null if no ID present`() {
        // Just the path without ID
        assertNull(DeepLinkParser.extractInterviewResultId("interview/result/"))
    }

    // ========================
    // Edge Cases
    // ========================

    @Test
    fun `parseToRoute handles deep link with special characters in ID`() {
        val deepLink = "ssbmax://interview/result/abc-123_xyz"
        val result = DeepLinkParser.parseToRoute(deepLink)
        assertEquals("interview/result/abc-123_xyz", result)
    }

    @Test
    fun `parseToRoute handles Firebase-style document IDs`() {
        // Firebase document IDs can be quite long and contain various characters
        val firebaseId = "1a2B3c4D5e6F7g8H9i0J"
        val deepLink = "ssbmax://interview/result/$firebaseId"
        val result = DeepLinkParser.parseToRoute(deepLink)
        assertEquals("interview/result/$firebaseId", result)
    }
}

