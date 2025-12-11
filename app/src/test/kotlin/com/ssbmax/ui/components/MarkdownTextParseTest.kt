package com.ssbmax.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class MarkdownTextParseTest {

    @Test
    fun parseInlineBold_appliesBoldSpan() {
        val annotated = parseInlineBold("This has **bold** text.")

        // Plain text content
        assertEquals("This has bold text.", annotated.text)

        // Expect one bold span covering "bold"
        val spans = annotated.spanStyles
        assertEquals(1, spans.size)
        val span = spans.first()
        val boldStart = "This has ".length
        val boldEnd = boldStart + "bold".length
        assertEquals(boldStart, span.start)
        assertEquals(boldEnd, span.end)
    }
}
