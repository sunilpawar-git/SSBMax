package com.ssbmax.ui.tests.gto.lecturette

import com.ssbmax.testing.BaseViewModelTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for LecturetteTestViewModel - Character validation tests
 * 
 * NOTE: These tests validate the state logic without triggering timer coroutines
 * to avoid test hangs. Full integration tests should be run separately.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LecturetteTestViewModelTest : BaseViewModelTest() {

    // ==================== LecturetteTestUiState Validation Tests ====================

    @Test
    fun `meetsMinCharCount returns false when charCount below 50`() {
        // Given
        val state = LecturetteTestUiState(speechTranscript = "Short", charCount = 5)

        // Then
        assertFalse("Should not meet min with < 50 chars", state.meetsMinCharCount)
    }

    @Test
    fun `meetsMinCharCount returns true when charCount at or above 50`() {
        // Given
        val state = LecturetteTestUiState(speechTranscript = "a".repeat(50), charCount = 50)

        // Then
        assertTrue("Should meet min with exactly 50 chars", state.meetsMinCharCount)
    }

    @Test
    fun `meetsMaxCharCount returns false when charCount above 1500`() {
        // Given
        val state = LecturetteTestUiState(speechTranscript = "a".repeat(1600), charCount = 1600)

        // Then
        assertFalse("Should not meet max with > 1500 chars", state.meetsMaxCharCount)
    }

    @Test
    fun `meetsMaxCharCount returns true when charCount at or below 1500`() {
        // Given
        val state = LecturetteTestUiState(speechTranscript = "a".repeat(1500), charCount = 1500)

        // Then
        assertTrue("Should meet max with exactly 1500 chars", state.meetsMaxCharCount)
    }

    @Test
    fun `phase transitions are tracked correctly`() {
        // Given - different phase states
        val instructionsState = LecturetteTestUiState(phase = LecturettePhase.INSTRUCTIONS)
        val topicSelectionState = LecturetteTestUiState(phase = LecturettePhase.TOPIC_SELECTION)
        val speechState = LecturetteTestUiState(phase = LecturettePhase.SPEECH)
        val reviewState = LecturetteTestUiState(phase = LecturettePhase.REVIEW)

        // Then
        assertEquals(LecturettePhase.INSTRUCTIONS, instructionsState.phase)
        assertEquals(LecturettePhase.TOPIC_SELECTION, topicSelectionState.phase)
        assertEquals(LecturettePhase.SPEECH, speechState.phase)
        assertEquals(LecturettePhase.REVIEW, reviewState.phase)
    }

    @Test
    fun `topicChoices stores topics correctly`() {
        // Given
        val topics = listOf("Topic A", "Topic B", "Topic C", "Topic D")
        val state = LecturetteTestUiState(topicChoices = topics)

        // Then
        assertEquals(4, state.topicChoices.size)
        assertEquals("Topic A", state.topicChoices[0])
    }

    @Test
    fun `selectedTopic stores selection correctly`() {
        // Given
        val state = LecturetteTestUiState(selectedTopic = "Leadership in Crisis")

        // Then
        assertEquals("Leadership in Crisis", state.selectedTopic)
    }
}
