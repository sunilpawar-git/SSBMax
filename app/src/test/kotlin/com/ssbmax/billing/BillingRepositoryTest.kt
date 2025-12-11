package com.ssbmax.billing

import android.app.Activity
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.runner.RunWith
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.robolectric.RobolectricTestRunner
import io.mockk.mockk

@RunWith(RobolectricTestRunner::class)
class BillingRepositoryTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val repository = BillingRepository(context)

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

