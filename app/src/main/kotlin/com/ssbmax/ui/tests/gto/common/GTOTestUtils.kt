package com.ssbmax.ui.tests.gto.common

/**
 * Utility functions for GTO tests
 */
object GTOTestUtils {
    /**
     * Count words in text (split by whitespace)
     */
    fun countWords(text: String): Int {
        if (text.isBlank()) return 0
        return text.trim().split(Regex("\\s+")).size
    }
}
