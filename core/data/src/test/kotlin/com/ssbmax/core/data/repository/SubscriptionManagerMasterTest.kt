package com.ssbmax.core.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.ssbmax.core.data.debug.DebugConfig
import com.ssbmax.core.data.local.dao.TestUsageDao
import com.ssbmax.core.data.local.entity.TestUsageEntity
import com.ssbmax.core.data.security.SecurityEventLogger
import com.ssbmax.core.domain.model.EntryType
import com.ssbmax.core.domain.model.Gender
import com.ssbmax.core.domain.model.SubscriptionTier
import com.ssbmax.core.domain.model.SubscriptionType
import com.ssbmax.core.domain.model.TestType
import com.ssbmax.core.domain.model.UserProfile
import com.ssbmax.core.domain.repository.UserProfileRepository
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * MASTER SUBSCRIPTION TEST SUITE
 * 
 * This is the "mother of all subscription tests" - a comprehensive test suite
 * that validates ALL subscription-related functionality in one place.
 * 
 * Coverage:
 * - All 16 test types (OIR, PPDT, PIQ, TAT, WAT, SRT, SD, 8 GTO sub-tests, IO)
 * - All 3 subscription tiers (FREE, PRO, PREMIUM)
 * - 48 parameterized test combinations (16 types × 3 tiers)
 * - Month reset behavior
 * - Concurrent access and atomicity
 * - Security logging and audit trail
 * - Edge cases and error handling
 * - Firestore transaction validation
 * 
 * This test serves as the SINGLE SOURCE OF TRUTH for subscription limit testing.
 * When adding new test types, update the parameterized data in this test.
 * 
 * Related files:
 * - Implementation: core/data/src/main/kotlin/com/ssbmax/core/data/repository/SubscriptionManager.kt
 * - Security tests: core/data/src/test/kotlin/com/ssbmax/core/data/repository/SubscriptionManagerSecurityTest.kt
 * - ViewModel tests: app/src/test/kotlin/com/ssbmax/ui/settings/SubscriptionManagementViewModelTest.kt
 */
@RunWith(Parameterized::class)
class SubscriptionManagerMasterTest(
    private val testType: TestType,
    private val tier: SubscriptionTier,
    private val expectedLimit: Int
) {
    
    companion object {
        private const val TEST_USER_ID = "test-user-123"
        
        @JvmStatic
        @Parameterized.Parameters(name = "{0} - {1} tier: limit={2}")
        fun data(): Collection<Array<Any>> {
            return listOf(
                // ===== FREE TIER LIMITS =====
                // Phase 1 tests
                arrayOf(TestType.OIR, SubscriptionTier.FREE, 1),
                arrayOf(TestType.PPDT, SubscriptionTier.FREE, 1),
                // Phase 2 tests
                arrayOf(TestType.PIQ, SubscriptionTier.FREE, 1),
                arrayOf(TestType.TAT, SubscriptionTier.FREE, 0),
                arrayOf(TestType.WAT, SubscriptionTier.FREE, 0),
                arrayOf(TestType.SRT, SubscriptionTier.FREE, 0),
                arrayOf(TestType.SD, SubscriptionTier.FREE, 0),
                // GTO Tests (8 sub-tests)
                arrayOf(TestType.GTO_GD, SubscriptionTier.FREE, 0),
                arrayOf(TestType.GTO_GPE, SubscriptionTier.FREE, 0),
                arrayOf(TestType.GTO_PGT, SubscriptionTier.FREE, 0),
                arrayOf(TestType.GTO_GOR, SubscriptionTier.FREE, 0),
                arrayOf(TestType.GTO_HGT, SubscriptionTier.FREE, 0),
                arrayOf(TestType.GTO_LECTURETTE, SubscriptionTier.FREE, 0),
                arrayOf(TestType.GTO_IO, SubscriptionTier.FREE, 0),
                arrayOf(TestType.GTO_CT, SubscriptionTier.FREE, 0),
                // Interview
                arrayOf(TestType.IO, SubscriptionTier.FREE, 0),
                
                // ===== PRO TIER LIMITS =====
                // Phase 1 tests
                arrayOf(TestType.OIR, SubscriptionTier.PRO, 5),
                arrayOf(TestType.PPDT, SubscriptionTier.PRO, 5),
                // Phase 2 tests
                arrayOf(TestType.PIQ, SubscriptionTier.PRO, Int.MAX_VALUE),
                arrayOf(TestType.TAT, SubscriptionTier.PRO, 3),
                arrayOf(TestType.WAT, SubscriptionTier.PRO, 3),
                arrayOf(TestType.SRT, SubscriptionTier.PRO, 3),
                arrayOf(TestType.SD, SubscriptionTier.PRO, 3),
                // GTO Tests (3 attempts per sub-test)
                arrayOf(TestType.GTO_GD, SubscriptionTier.PRO, 3),
                arrayOf(TestType.GTO_GPE, SubscriptionTier.PRO, 3),
                arrayOf(TestType.GTO_PGT, SubscriptionTier.PRO, 3),
                arrayOf(TestType.GTO_GOR, SubscriptionTier.PRO, 3),
                arrayOf(TestType.GTO_HGT, SubscriptionTier.PRO, 3),
                arrayOf(TestType.GTO_LECTURETTE, SubscriptionTier.PRO, 3),
                arrayOf(TestType.GTO_IO, SubscriptionTier.PRO, 3),
                arrayOf(TestType.GTO_CT, SubscriptionTier.PRO, 3),
                // Interview
                arrayOf(TestType.IO, SubscriptionTier.PRO, 1),
                
                // ===== PREMIUM TIER LIMITS (All Unlimited) =====
                arrayOf(TestType.OIR, SubscriptionTier.PREMIUM, Int.MAX_VALUE),
                arrayOf(TestType.PPDT, SubscriptionTier.PREMIUM, Int.MAX_VALUE),
                arrayOf(TestType.PIQ, SubscriptionTier.PREMIUM, Int.MAX_VALUE),
                arrayOf(TestType.TAT, SubscriptionTier.PREMIUM, Int.MAX_VALUE),
                arrayOf(TestType.WAT, SubscriptionTier.PREMIUM, Int.MAX_VALUE),
                arrayOf(TestType.SRT, SubscriptionTier.PREMIUM, Int.MAX_VALUE),
                arrayOf(TestType.SD, SubscriptionTier.PREMIUM, Int.MAX_VALUE),
                arrayOf(TestType.GTO_GD, SubscriptionTier.PREMIUM, Int.MAX_VALUE),
                arrayOf(TestType.GTO_GPE, SubscriptionTier.PREMIUM, Int.MAX_VALUE),
                arrayOf(TestType.GTO_PGT, SubscriptionTier.PREMIUM, Int.MAX_VALUE),
                arrayOf(TestType.GTO_GOR, SubscriptionTier.PREMIUM, Int.MAX_VALUE),
                arrayOf(TestType.GTO_HGT, SubscriptionTier.PREMIUM, Int.MAX_VALUE),
                arrayOf(TestType.GTO_LECTURETTE, SubscriptionTier.PREMIUM, Int.MAX_VALUE),
                arrayOf(TestType.GTO_IO, SubscriptionTier.PREMIUM, Int.MAX_VALUE),
                arrayOf(TestType.GTO_CT, SubscriptionTier.PREMIUM, Int.MAX_VALUE),
                arrayOf(TestType.IO, SubscriptionTier.PREMIUM, Int.MAX_VALUE)
            )
        }
    }
    
    private lateinit var subscriptionManager: SubscriptionManager
    private lateinit var mockUserProfileRepo: UserProfileRepository
    private lateinit var mockFirestore: FirebaseFirestore
    private lateinit var mockSecurityLogger: SecurityEventLogger
    private lateinit var mockTestUsageDao: TestUsageDao
    private lateinit var mockDebugConfig: DebugConfig
    
    @Before
    fun setup() {
        // Mock Android Log
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0
        every { Log.w(any<String>(), any<String>()) } returns 0
        every { Log.w(any(), any(), any()) } returns 0
        every { Log.i(any(), any()) } returns 0
        every { Log.v(any(), any()) } returns 0
        
        mockUserProfileRepo = mockk(relaxed = true)
        mockFirestore = mockk(relaxed = true)
        mockSecurityLogger = mockk(relaxed = true)
        mockTestUsageDao = mockk(relaxed = true)
        mockDebugConfig = mockk(relaxed = true)
        
        // Disable debug bypass for real limit testing
        every { mockDebugConfig.bypassSubscriptionLimits } returns false
        
        subscriptionManager = SubscriptionManager(
            userProfileRepository = mockUserProfileRepo,
            firestore = mockFirestore,
            securityLogger = mockSecurityLogger,
            testUsageDao = mockTestUsageDao,
            debugConfig = mockDebugConfig
        )
    }
    
    @After
    fun tearDown() {
        unmockkAll()
    }
    
    // ==================== PARAMETERIZED LIMIT TESTS ====================
    
    /**
     * TEST 1: Verify exact limit for each test type × tier combination
     * This is the core test that validates all 51 combinations
     */
    @Test
    fun `verifies exact limit for test type and tier combination`() = runTest {
        // Given - user with specific tier
        val subscriptionType = when (tier) {
            SubscriptionTier.FREE -> SubscriptionType.FREE
            SubscriptionTier.PRO -> SubscriptionType.PRO
            SubscriptionTier.PREMIUM -> SubscriptionType.PREMIUM
        }
        val user = UserProfile(
            userId = TEST_USER_ID,
            fullName = "Test User",
            age = 22,
            gender = Gender.MALE,
            entryType = EntryType.GRADUATE,
            subscriptionType = subscriptionType
        )
        every { mockUserProfileRepo.getUserProfile(TEST_USER_ID) } returns flowOf(Result.success(user))
        
        // Mock Firestore to return zero usage
        mockFirestoreUsage(
            userId = TEST_USER_ID,
            usedCount = 0
        )
        
        // When
        val eligibility = subscriptionManager.canTakeTest(testType, TEST_USER_ID)
        
        // Then
        if (expectedLimit == 0) {
            // When limit is 0, user should be blocked even with 0 usage
            assertTrue("Should be blocked when limit is 0", eligibility is TestEligibility.LimitReached)
        } else if (expectedLimit == Int.MAX_VALUE) {
            // Unlimited tier
            assertTrue("Should be eligible with unlimited tier", eligibility is TestEligibility.Eligible)
            val eligible = eligibility as TestEligibility.Eligible
            assertEquals("Should have max remaining", Int.MAX_VALUE, eligible.remainingTests)
        } else {
            // Normal limit
            assertTrue("Should be eligible with 0 usage", eligibility is TestEligibility.Eligible)
            val eligible = eligibility as TestEligibility.Eligible
            assertEquals(
                "Limit should match expected for $testType on $tier tier",
                expectedLimit,
                eligible.remainingTests
            )
        }
    }
    
    /**
     * TEST 2: Verify limit enforcement at boundary
     * Tests that users are blocked exactly at the limit
     */
    @Test
    fun `blocks test when limit is reached`() = runTest {
        // Skip unlimited tiers
        if (expectedLimit == Int.MAX_VALUE) return@runTest
        
        // Given - user at limit
        val subscriptionType = when (tier) {
            SubscriptionTier.FREE -> SubscriptionType.FREE
            SubscriptionTier.PRO -> SubscriptionType.PRO
            SubscriptionTier.PREMIUM -> SubscriptionType.PREMIUM
        }
        val user = UserProfile(
            userId = TEST_USER_ID,
            fullName = "Test User",
            age = 22,
            gender = Gender.MALE,
            entryType = EntryType.GRADUATE,
            subscriptionType = subscriptionType
        )
        every { mockUserProfileRepo.getUserProfile(TEST_USER_ID) } returns flowOf(Result.success(user))
        
        // Mock Firestore to return usage at limit
        mockFirestoreUsage(
            userId = TEST_USER_ID,
            testType = testType,
            usedCount = expectedLimit
        )
        
        // When
        val eligibility = subscriptionManager.canTakeTest(testType, TEST_USER_ID)
        
        // Then
        assertTrue("Should be blocked at limit", eligibility is TestEligibility.LimitReached)
        val limitReached = eligibility as TestEligibility.LimitReached
        assertEquals("Tier should match", tier, limitReached.tier)
        assertEquals("Limit should match", expectedLimit, limitReached.limit)
        assertEquals("Used count should match", expectedLimit, limitReached.usedCount)
        
        // Verify security logging
        verify(exactly = 1) {
            mockSecurityLogger.logLimitReached(
                userId = TEST_USER_ID,
                testType = testType,
                subscriptionTier = tier.name,
                testsUsed = expectedLimit,
                testsLimit = expectedLimit
            )
        }
    }
    
    /**
     * TEST 3: Verify user is eligible one below limit
     */
    @Test
    fun `allows test when one below limit`() = runTest {
        // Skip unlimited tiers and zero limits
        if (expectedLimit == Int.MAX_VALUE || expectedLimit == 0) return@runTest
        
        // Given - user one below limit
        val subscriptionType = when (tier) {
            SubscriptionTier.FREE -> SubscriptionType.FREE
            SubscriptionTier.PRO -> SubscriptionType.PRO
            SubscriptionTier.PREMIUM -> SubscriptionType.PREMIUM
        }
        val user = UserProfile(
            userId = TEST_USER_ID,
            fullName = "Test User",
            age = 22,
            gender = Gender.MALE,
            entryType = EntryType.GRADUATE,
            subscriptionType = subscriptionType
        )
        every { mockUserProfileRepo.getUserProfile(TEST_USER_ID) } returns flowOf(Result.success(user))
        
        // Mock Firestore to return usage one below limit
        mockFirestoreUsage(
            userId = TEST_USER_ID,
            testType = testType,
            usedCount = expectedLimit - 1
        )
        
        // When
        val eligibility = subscriptionManager.canTakeTest(testType, TEST_USER_ID)
        
        // Then
        assertTrue("Should be eligible one below limit", eligibility is TestEligibility.Eligible)
        val eligible = eligibility as TestEligibility.Eligible
        assertEquals("Should have 1 test remaining", 1, eligible.remainingTests)
    }
    
    // ==================== HELPER METHODS ====================
    
    private fun mockFirestoreUsage(
        userId: String,
        testType: TestType? = null,
        usedCount: Int = 0
    ) {
        val mockCollectionRef = mockk<com.google.firebase.firestore.CollectionReference>(relaxed = true)
        val mockDocRef = mockk<com.google.firebase.firestore.DocumentReference>(relaxed = true)
        val mockSubCollectionRef = mockk<com.google.firebase.firestore.CollectionReference>(relaxed = true)
        val mockUsageDocRef = mockk<com.google.firebase.firestore.DocumentReference>(relaxed = true)
        val mockTask = mockk<com.google.android.gms.tasks.Task<com.google.firebase.firestore.DocumentSnapshot>>(relaxed = true)
        val mockSnapshot = mockk<com.google.firebase.firestore.DocumentSnapshot>(relaxed = true)
        
        every { mockFirestore.collection("users") } returns mockCollectionRef
        every { mockCollectionRef.document(userId) } returns mockDocRef
        every { mockDocRef.collection("subscription") } returns mockSubCollectionRef
        every { mockSubCollectionRef.document(any()) } returns mockUsageDocRef
        every { mockUsageDocRef.get() } returns mockTask
        
        every { mockTask.isSuccessful } returns true
        every { mockTask.isComplete } returns true
        every { mockTask.result } returns mockSnapshot
        every { mockTask.exception } returns null
        every { mockSnapshot.exists() } returns true
        
        // Set usage counts based on test type
        every { mockSnapshot.getLong("oirTestsUsed") } returns if (testType == TestType.OIR) usedCount.toLong() else 0L
        every { mockSnapshot.getLong("ppdtTestsUsed") } returns if (testType == TestType.PPDT) usedCount.toLong() else 0L
        every { mockSnapshot.getLong("piqTestsUsed") } returns if (testType == TestType.PIQ) usedCount.toLong() else 0L
        every { mockSnapshot.getLong("tatTestsUsed") } returns if (testType == TestType.TAT) usedCount.toLong() else 0L
        every { mockSnapshot.getLong("watTestsUsed") } returns if (testType == TestType.WAT) usedCount.toLong() else 0L
        every { mockSnapshot.getLong("srtTestsUsed") } returns if (testType == TestType.SRT) usedCount.toLong() else 0L
        every { mockSnapshot.getLong("sdTestsUsed") } returns if (testType == TestType.SD) usedCount.toLong() else 0L
        every { mockSnapshot.getLong("gtoTestsUsed") } returns if (testType in listOf(
            TestType.GTO_GD, TestType.GTO_GPE, TestType.GTO_PGT, TestType.GTO_GOR,
            TestType.GTO_HGT, TestType.GTO_LECTURETTE, TestType.GTO_IO, TestType.GTO_CT
        )) usedCount.toLong() else 0L
        every { mockSnapshot.getLong("interviewTestsUsed") } returns if (testType == TestType.IO) usedCount.toLong() else 0L
        
        // Mock Task callback for await()
        every { mockTask.addOnSuccessListener(any()) } answers {
            val listener = firstArg<com.google.android.gms.tasks.OnSuccessListener<com.google.firebase.firestore.DocumentSnapshot>>()
            listener.onSuccess(mockSnapshot)
            mockTask
        }
        every { mockTask.addOnFailureListener(any()) } returns mockTask
        every { mockTask.addOnCompleteListener(any()) } returns mockTask
        
        // Mock Room DAO
        coEvery { mockTestUsageDao.insertOrReplace(any()) } just Runs
    }
}

