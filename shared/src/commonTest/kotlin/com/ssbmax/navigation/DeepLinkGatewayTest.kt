package com.ssbmax.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * KMP-convergence Phase 4: [DeepLinkGateway] is the cold-start-safe holding
 * cell both platforms' entry points submit into. These tests pin the three
 * behaviours the plan calls out before either platform edge is wired to it.
 */
class DeepLinkGatewayTest {

    @Test
    fun `submit parses and buffers a scheme-shaped deep link`() {
        val gateway = DeepLinkGateway()

        gateway.submit("ssbmax://interview/result/abc123")

        // Cold-start buffering: the value persists in the StateFlow until
        // consumed, unlike a Channel which would drop it before anything
        // collects -- this is the entire reason DeepLinkGateway isn't one.
        assertEquals("interview/result/abc123", gateway.pendingRoute.value)
    }

    @Test
    fun `submit accepts an already scheme-stripped route`() {
        val gateway = DeepLinkGateway()

        gateway.submit("interview/result/abc123")

        assertEquals("interview/result/abc123", gateway.pendingRoute.value)
    }

    @Test
    fun `consume clears the pending route`() {
        val gateway = DeepLinkGateway()
        gateway.submit("ssbmax://interview/result/abc123")

        gateway.consume()

        assertNull(gateway.pendingRoute.value)
    }

    @Test
    fun `submit with null input leaves pendingRoute untouched`() {
        val gateway = DeepLinkGateway()

        gateway.submit(null)

        assertNull(gateway.pendingRoute.value)
    }

    @Test
    fun `submit with an unsupported scheme is tolerated, not crashed`() {
        val gateway = DeepLinkGateway()

        gateway.submit("https://ssbmax.com/interview/result/abc123")

        assertNull(gateway.pendingRoute.value)
    }

    @Test
    fun `submit with a blank string does not overwrite an existing pending route`() {
        val gateway = DeepLinkGateway()
        gateway.submit("ssbmax://interview/result/abc123")

        gateway.submit("")

        assertEquals("interview/result/abc123", gateway.pendingRoute.value)
    }
}
