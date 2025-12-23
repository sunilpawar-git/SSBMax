package com.ssbmax.billing

import android.app.Activity
import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import io.mockk.mockk

/**
 * BillingRepositoryTest - Unit tests for BillingRepository
 * Uses mockk for Context instead of Robolectric to avoid SDK version issues
 */
class BillingRepositoryTest {

    private lateinit var context: Context
    private lateinit var repository: BillingRepository
    
    @Before
    fun setup() {
        context = mockk(relaxed = true)
        repository = BillingRepository(context)
    }

    @Test
    fun initialize_populatesMockProducts() {
        assertTrue(repository.products.value.isEmpty())

        repository.initialize()

        val products = repository.products.value
        assertEquals(2, products.size)
        assertEquals("premium_monthly", products[0].id)
        assertEquals("premium_yearly", products[1].id)
    }

    @Test
    fun purchaseProduct_setsPremiumStatus() {
        repository.initialize()

        assertFalse(repository.premiumStatus.value)

        val activity = mockk<Activity>(relaxed = true)
        repository.purchaseProduct(activity = activity, productId = "premium_monthly")

        assertTrue(repository.premiumStatus.value)
    }
}

