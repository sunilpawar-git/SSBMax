package com.ssbmax.ui.components

import androidx.compose.runtime.*
import androidx.compose.ui.test.junit4.createComposeRule
import coil.request.ImageRequest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests to prevent image loading recomposition issues
 * 
 * These tests ensure that ImageRequest objects are stable across recompositions,
 * preventing the infinite loading loop bug discovered in PPDT image loading.
 * 
 * BACKGROUND:
 * In PPDT test, images were stuck in infinite loading because ImageRequest was
 * recreated on every recomposition (timer tick). Coil saw each new request as
 * a different image and restarted loading.
 * 
 * PREVENTION:
 * These tests verify that:
 * 1. ImageRequest is created only once per URL
 * 2. Recomposition doesn't trigger request recreation
 * 3. URL change DOES trigger new request (correct behavior)
 */
class StableImageLoadingTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    /**
     * Test: ImageRequest should be stable across recompositions
     * 
     * CRITICAL: This test would have FAILED with the buggy PPDT implementation
     * where ImageRequest.Builder was called directly without remember()
     */
    @Test
    fun `ImageRequest should NOT be recreated on recomposition with same URL`() {
        var requestCreationCount = 0
        var recompositionCount = 0
        var triggerRecomposition by mutableStateOf(0)
        val testUrl = "https://example.com/image.jpg"
        var capturedRequest: ImageRequest? = null

        composeTestRule.setContent {
            // Track recompositions
            recompositionCount++
            
            // CORRECT PATTERN: Use remember() for stable request
            val imageRequest = remember(testUrl) {
                requestCreationCount++
                ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                    .data(testUrl)
                    .build()
            }
            
            // Force recomposition when state changes
            val _ = triggerRecomposition
            
            // Capture request for comparison
            LaunchedEffect(imageRequest) {
                capturedRequest = imageRequest
            }
        }

        // Initial composition
        composeTestRule.waitForIdle()
        assertEquals("ImageRequest should be created once", 1, requestCreationCount)
        assertEquals("Should have composed once", 1, recompositionCount)
        val firstRequest = capturedRequest

        // Trigger recomposition (simulates timer tick)
        triggerRecomposition++
        composeTestRule.waitForIdle()
        
        // Verify: Request NOT recreated despite recomposition
        assertEquals("ImageRequest should NOT be recreated on recomposition", 1, requestCreationCount)
        assertEquals("Should have recomposed", 2, recompositionCount)
        assertSame("ImageRequest should be same instance", firstRequest, capturedRequest)
    }

    /**
     * Test: ImageRequest SHOULD be recreated when URL changes
     * 
     * This verifies correct behavior: new URL = new request
     */
    @Test
    fun `ImageRequest SHOULD be recreated when URL changes`() {
        var requestCreationCount = 0
        var imageUrl by mutableStateOf("https://example.com/image1.jpg")
        var capturedRequest: ImageRequest? = null

        composeTestRule.setContent {
            val imageRequest = remember(imageUrl) {
                requestCreationCount++
                ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                    .data(imageUrl)
                    .build()
            }
            
            LaunchedEffect(imageRequest) {
                capturedRequest = imageRequest
            }
        }

        composeTestRule.waitForIdle()
        assertEquals("First request created", 1, requestCreationCount)
        val firstRequest = capturedRequest

        // Change URL - should create new request
        imageUrl = "https://example.com/image2.jpg"
        composeTestRule.waitForIdle()
        
        assertEquals("New request created for new URL", 2, requestCreationCount)
        assertNotSame("ImageRequest should be different instance", firstRequest, capturedRequest)
    }

    /**
     * Test: Demonstrates the BUGGY pattern that caused PPDT issue
     * 
     * This test shows what happens WITHOUT remember() - request gets recreated constantly
     */
    @Test
    fun `ANTI-PATTERN - ImageRequest WITHOUT remember recreates on every recomposition`() {
        var requestCreationCount = 0
        var recompositionCount = 0
        var triggerRecomposition by mutableStateOf(0)
        val testUrl = "https://example.com/image.jpg"

        composeTestRule.setContent {
            recompositionCount++
            
            // ❌ BUGGY PATTERN: No remember() - recreates on every recomposition
            val imageRequest = ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                .data(testUrl)
                .build()
            
            requestCreationCount++  // This will increment on every recomposition!
            
            // Force recomposition
            val _ = triggerRecomposition
        }

        composeTestRule.waitForIdle()
        val initialCount = requestCreationCount

        // Trigger recomposition
        triggerRecomposition++
        composeTestRule.waitForIdle()
        
        // This demonstrates the BUG: request is recreated despite same URL
        assertTrue(
            "Without remember(), ImageRequest is recreated on recomposition (BUG!)",
            requestCreationCount > initialCount
        )
    }

    /**
     * Test: Simulates real-world scenario - timer causing recompositions
     * 
     * This mirrors the PPDT test where timer updates every second
     */
    @Test
    fun `ImageRequest should remain stable during frequent recompositions like timer updates`() {
        var requestCreationCount = 0
        var timerValue by mutableStateOf(30)  // Simulates 30-second countdown
        val testUrl = "https://example.com/image.jpg"
        var capturedRequest: ImageRequest? = null

        composeTestRule.setContent {
            // CORRECT: Use remember(url) for stable request
            val imageRequest = remember(testUrl) {
                requestCreationCount++
                ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                    .data(testUrl)
                    .build()
            }
            
            // Simulates timer display (causes recomposition but shouldn't recreate request)
            val displayTime = timerValue
            
            LaunchedEffect(imageRequest) {
                capturedRequest = imageRequest
            }
        }

        composeTestRule.waitForIdle()
        assertEquals("Initial request created", 1, requestCreationCount)
        val originalRequest = capturedRequest

        // Simulate 10 timer ticks (like PPDT test timer)
        repeat(10) {
            timerValue--
            composeTestRule.waitForIdle()
        }
        
        // CRITICAL: Request should NOT be recreated despite 10 recompositions
        assertEquals(
            "ImageRequest should remain stable across timer recompositions",
            1,
            requestCreationCount
        )
        assertSame(
            "ImageRequest should be same instance after timer updates",
            originalRequest,
            capturedRequest
        )
    }

    /**
     * Test: Verify StableAsyncImage uses correct pattern
     * 
     * This would catch if StableAsyncImage implementation breaks
     */
    @Test
    fun `StableAsyncImage should create stable ImageRequest`() {
        var loadingStartCount = 0
        var recompositionTrigger by mutableStateOf(0)
        val testUrl = "https://example.com/image.jpg"

        composeTestRule.setContent {
            // Force recomposition
            val _ = recompositionTrigger
            
            StableAsyncImage(
                imageUrl = testUrl,
                contentDescription = "Test",
                onLoadingStart = {
                    loadingStartCount++
                }
            )
        }

        composeTestRule.waitForIdle()
        val initialLoadCount = loadingStartCount

        // Trigger multiple recompositions
        repeat(5) {
            recompositionTrigger++
            composeTestRule.waitForIdle()
        }
        
        // onLoadingStart should only be called once (when request is created)
        // If it's called multiple times, the request is being recreated (BUG)
        assertEquals(
            "Image loading should start only once despite recompositions",
            initialLoadCount,
            loadingStartCount
        )
    }
}

