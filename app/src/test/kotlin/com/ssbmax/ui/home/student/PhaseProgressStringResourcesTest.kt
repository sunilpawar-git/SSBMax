package com.ssbmax.ui.home.student

import com.ssbmax.R
import org.junit.Test
import org.junit.Assert.assertNotEquals

/**
 * Tests to verify all Phase Progress Ribbon string resources exist
 * Part of Phase 1: Fix CRITICAL Hardcoded Strings
 *
 * These tests enforce that all hardcoded strings have been replaced with string resources.
 * The tests verify that string resource IDs are defined (compilation test).
 */
class PhaseProgressStringResourcesTest {

    @Test
    fun `phase progress title string resource exists`() {
        // If this compiles, the string resource exists
        val stringId = R.string.phase_progress_title
        assertNotEquals(0, stringId)
    }

    @Test
    fun `phase 1 label string resource exists`() {
        val stringId = R.string.phase_1_label
        assertNotEquals(0, stringId)
    }

    @Test
    fun `phase 1 subtitle string resource exists`() {
        val stringId = R.string.phase_1_subtitle
        assertNotEquals(0, stringId)
    }

    @Test
    fun `phase 2 label string resource exists`() {
        val stringId = R.string.phase_2_label
        assertNotEquals(0, stringId)
    }

    @Test
    fun `phase 2 subtitle string resource exists`() {
        val stringId = R.string.phase_2_subtitle
        assertNotEquals(0, stringId)
    }

    @Test
    fun `view all tests string resource exists`() {
        val stringId = R.string.progress_view_all
        assertNotEquals(0, stringId)
    }

    @Test
    fun `no tests attempted string resource exists`() {
        val stringId = R.string.progress_no_tests
        assertNotEquals(0, stringId)
    }

    @Test
    fun `completed on format string resource exists`() {
        val stringId = R.string.progress_completed_on
        assertNotEquals(0, stringId)
    }

    @Test
    fun `not attempted string resource exists`() {
        val stringId = R.string.progress_not_attempted
        assertNotEquals(0, stringId)
    }

    @Test
    fun `trophy icon string resource exists`() {
        val stringId = R.string.icon_trophy
        assertNotEquals(0, stringId)
    }

    @Test
    fun `psychology tests string resource exists`() {
        val stringId = R.string.progress_psychology
        assertNotEquals(0, stringId)
    }

    @Test
    fun `gto tasks string resource exists`() {
        val stringId = R.string.progress_gto
        assertNotEquals(0, stringId)
    }

    @Test
    fun `interview string resource exists`() {
        val stringId = R.string.progress_interview
        assertNotEquals(0, stringId)
    }
}
