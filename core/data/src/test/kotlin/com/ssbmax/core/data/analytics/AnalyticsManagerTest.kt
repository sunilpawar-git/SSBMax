package com.ssbmax.core.data.analytics

import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for AnalyticsManager
 */
class AnalyticsManagerTest {
    
    private lateinit var analyticsManager: AnalyticsManager
    private lateinit var mockFirebaseAnalytics: FirebaseAnalytics
    private lateinit var mockFirebaseAuth: FirebaseAuth
    
    @Before
    fun setup() {
        mockFirebaseAnalytics = mockk(relaxed = true)
        mockFirebaseAuth = mockk(relaxed = true)
        
        analyticsManager = AnalyticsManager(
            firebaseAnalytics = mockFirebaseAnalytics,
            firebaseAuth = mockFirebaseAuth
        )
    }
    
    @After
    fun tearDown() {
        unmockkAll()
    }
    
    @Test
    fun `trackTestStarted logs correct event`() {
        // When
        analyticsManager.trackTestStarted(
            testType = TestTypes.OIR,
            testId = "oir_standard"
        )
        
        // Then
        verify {
            mockFirebaseAnalytics.logEvent(
                name = "test_started",
                params = any()
            )
        }
    }
    
    @Test
    fun `trackTestCompleted logs event with score and time`() {
        // When
        analyticsManager.trackTestCompleted(
            testType = TestTypes.OIR,
            testId = "oir_standard",
            score = 85.5f,
            timeSpentSeconds = 1800L,
            questionsAnswered = 50
        )
        
        // Then
        verify {
            mockFirebaseAnalytics.logEvent(
                name = "test_completed",
                params = any()
            )
        }
    }
    
    @Test
    fun `trackTestLimitReached logs conversion opportunity`() {
        // When
        analyticsManager.trackTestLimitReached(
            testType = TestTypes.OIR,
            currentTier = SubscriptionTiers.FREE,
            currentUsage = 1,
            limit = 1
        )
        
        // Then
        verify {
            mockFirebaseAnalytics.logEvent(
                name = "test_limit_reached",
                params = any()
            )
        }
    }
    
    @Test
    fun `trackUpgradeCompleted logs purchase event`() {
        // When
        analyticsManager.trackUpgradeCompleted(
            fromTier = SubscriptionTiers.FREE,
            toTier = SubscriptionTiers.PRO,
            priceInRupees = 99
        )
        
        // Then
        verify {
            // Should log both upgrade_completed and purchase events
            mockFirebaseAnalytics.logEvent(
                name = "upgrade_completed",
                params = any()
            )
            
            mockFirebaseAnalytics.logEvent(
                name = "purchase",
                params = any()
            )
        }
    }
    
    @Test
    fun `trackSubscriptionView logs with source`() {
        // When
        analyticsManager.trackSubscriptionView(
            source = AnalyticsSources.TEST_LIMIT_DIALOG
        )
        
        // Then
        verify {
            mockFirebaseAnalytics.logEvent(
                name = "subscription_view",
                params = any()
            )
        }
    }
    
    @Test
    fun `trackScreenView logs screen name and class`() {
        // When
        analyticsManager.trackScreenView(
            screenName = ScreenNames.SUBSCRIPTION,
            screenClass = "SubscriptionManagementScreen"
        )
        
        // Then
        verify {
            mockFirebaseAnalytics.logEvent(
                name = "screen_view",
                params = any()
            )
        }
    }
    
    @Test
    fun `setUserProperties sets correct properties`() {
        // When
        analyticsManager.setUserProperties(
            subscriptionTier = SubscriptionTiers.PRO,
            accountAgeInDays = 30
        )
        
        // Then
        verify {
            mockFirebaseAnalytics.setUserProperty(
                name = "subscription_tier",
                value = SubscriptionTiers.PRO
            )
            
            mockFirebaseAnalytics.setUserProperty(
                name = "account_age_days",
                value = "30"
            )
        }
    }
    
    @Test
    fun `trackFeatureUsed logs feature name and parameters`() {
        // Given
        val parameters = mapOf(
            "feature_category" to "study_materials",
            "download_size" to 1024
        )
        
        // When
        analyticsManager.trackFeatureUsed(
            featureName = FeatureNames.DOWNLOAD_MATERIAL,
            parameters = parameters
        )
        
        // Then
        verify {
            mockFirebaseAnalytics.logEvent(
                name = "feature_used",
                params = any()
            )
        }
    }
}

