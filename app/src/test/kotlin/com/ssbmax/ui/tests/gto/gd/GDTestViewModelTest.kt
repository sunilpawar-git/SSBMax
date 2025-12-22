package com.ssbmax.ui.tests.gto.gd

import com.ssbmax.testing.BaseViewModelTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for GDTestViewModel - Character validation tests
 * 
 * NOTE: These tests validate the state logic without triggering timer coroutines
 * to avoid test hangs. Full integration tests should be run separately.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GDTestViewModelTest : BaseViewModelTest() {

    // ==================== GDTestUiState Validation Tests ====================

    @Test
    fun `meetsMinCharCount returns false when charCount below 50`() {
        // Given
        val state = GDTestUiState(response = "Short", charCount = 5)

        // Then
        assertFalse("Should not meet min with < 50 chars", state.meetsMinCharCount)
    }

    @Test
    fun `meetsMinCharCount returns true when charCount at or above 50`() {
        // Given
        val state = GDTestUiState(response = "a".repeat(50), charCount = 50)

        // Then
        assertTrue("Should meet min with exactly 50 chars", state.meetsMinCharCount)
    }

    @Test
    fun `meetsMaxCharCount returns false when charCount above 1500`() {
        // Given
        val state = GDTestUiState(response = "a".repeat(1600), charCount = 1600)

        // Then
        assertFalse("Should not meet max with > 1500 chars", state.meetsMaxCharCount)
    }

    @Test
    fun `meetsMaxCharCount returns true when charCount at or below 1500`() {
        // Given
        val state = GDTestUiState(response = "a".repeat(1500), charCount = 1500)

        // Then
        assertTrue("Should meet max with exactly 1500 chars", state.meetsMaxCharCount)
    }

    @Test
    fun `meetsCharCountRequirements returns true for valid range`() {
        // Given
        val state = GDTestUiState(response = "a".repeat(500), charCount = 500)

        // Then
        assertTrue("Should meet requirements with 500 chars", state.meetsCharCountRequirements)
    }

    @Test
    fun `meetsCharCountRequirements returns false when below min`() {
        // Given
        val state = GDTestUiState(response = "Short", charCount = 5)

        // Then
        assertFalse("Should not meet requirements with < 50", state.meetsCharCountRequirements)
    }

    @Test
    fun `meetsCharCountRequirements returns false when above max`() {
        // Given
        val state = GDTestUiState(response = "a".repeat(1600), charCount = 1600)

        // Then
        assertFalse("Should not meet requirements with > 1500", state.meetsCharCountRequirements)
    }
}
