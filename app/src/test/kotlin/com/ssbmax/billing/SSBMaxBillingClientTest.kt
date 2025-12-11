package com.ssbmax.billing

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class SSBMaxBillingClientTest {

    private val client = SSBMaxBillingClient()

    @Test
    fun querySubscriptions_returnsEmptySuccess() = runTest {
        val result = client.querySubscriptions()

        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow().isEmpty())
    }

    @Test
    fun verifyPurchase_returnsFalseSuccess() = runTest {
        val result = client.verifyPurchase("token")

        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow().not())
    }
}

