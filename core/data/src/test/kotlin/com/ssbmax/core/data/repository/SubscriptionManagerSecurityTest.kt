package com.ssbmax.core.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.ssbmax.core.data.local.dao.TestUsageDao
import com.ssbmax.core.data.local.entity.TestUsageEntity
import com.ssbmax.core.domain.model.SubscriptionTier
import com.ssbmax.core.domain.model.SubscriptionType
import com.ssbmax.core.domain.model.TestType
import com.ssbmax.core.domain.model.UserProfile
import com.ssbmax.core.domain.repository.UserProfileRepository
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Security-focused tests for SubscriptionManager
 * 
 * Tests critical security scenarios:
 * - Limit enforcement
 * - Firestore integration
 * - Race condition prevention
 * - Error handling (fail-secure)
 */
class SubscriptionManagerSecurityTest {
    
    private lateinit var subscriptionManager: SubscriptionManager
    private lateinit var mockTestUsageDao: TestUsageDao
    private lateinit var mockUserProfileRepository: UserProfileRepository
    private lateinit var mockFirestore: FirebaseFirestore
    
    private val testUserId = "test-user-123"
    private val currentMonth = "2025-11"
    
    @Before
    fun setup() {
        // Mock Android Log
        mockkStatic(Log::class)
        every { Log.v(any(), any()) } returns 0
        every { Log.d(any(), any()) } returns 0
        every { Log.i(any(), any()) } returns 0
        every { Log.w(any<String>(), any<String>()) } returns 0
        every { Log.e(any<String>(), any<String>()) } returns 0
        every { Log.e(any<String>(), any<String>(), any<Throwable>()) } returns 0
        
        mockTestUsageDao = mockk(relaxed = true)
        mockUserProfileRepository = mockk()
        mockFirestore = mockk()
        
        subscriptionManager = SubscriptionManager(
            testUsageDao = mockTestUsageDao,
            userProfileRepository = mockUserProfileRepository,
            firestore = mockFirestore
        )
    }
    
    @After
    fun tearDown() {
        unmockkAll()
    }
    
    /**
     * SECURITY TEST: Verify FREE tier is blocked after 1 test
     */
    @Test
    fun `canTakeTest blocks FREE tier after limit reached`() = runTest {
        // Given: FREE tier user with 1 test already used
        val mockProfile = UserProfile(
            userId = testUserId,
            fullName = "Test User",
            age = 25,
            gender = com.ssbmax.core.domain.model.Gender.MALE,
            entryType = com.ssbmax.core.domain.model.EntryType.GRADUATE,
            subscriptionType = SubscriptionType.FREE
        )
        coEvery { mockUserProfileRepository.getUserProfile(testUserId) } returns flowOf(Result.success(mockProfile))
        
        val mockUsage = TestUsageEntity(
            id = "${testUserId}_$currentMonth",
            userId = testUserId,
            month = currentMonth,
            oirTestsUsed = 1,
            tatTestsUsed = 0,
            watTestsUsed = 0,
            srtTestsUsed = 0,
            ppdtTestsUsed = 0,
            gtoTestsUsed = 0,
            interviewTestsUsed = 0
        )
        
        // Mock Firestore to return usage
        val mockDocRef = mockk<com.google.firebase.firestore.DocumentReference>()
        val mockDocSnapshot = mockk<com.google.firebase.firestore.DocumentSnapshot>()
        val mockCollectionRef = mockk<com.google.firebase.firestore.CollectionReference>()
        val mockUserDocRef = mockk<com.google.firebase.firestore.DocumentReference>()
        val mockSubCollectionRef = mockk<com.google.firebase.firestore.CollectionReference>()
        
        every { mockFirestore.collection("users") } returns mockCollectionRef
        every { mockCollectionRef.document(testUserId) } returns mockUserDocRef
        every { mockUserDocRef.collection("subscription") } returns mockSubCollectionRef
        every { mockSubCollectionRef.document(any()) } returns mockDocRef
        every { mockDocRef.get() } returns mockk {
            coEvery { await() } returns mockDocSnapshot
        }
        every { mockDocSnapshot.exists() } returns true
        every { mockDocSnapshot.getLong("oirTestsUsed") } returns 1
        every { mockDocSnapshot.getLong("tatTestsUsed") } returns 0
        every { mockDocSnapshot.getLong("watTestsUsed") } returns 0
        every { mockDocSnapshot.getLong("srtTestsUsed") } returns 0
        every { mockDocSnapshot.getLong("ppdtTestsUsed") } returns 0
        every { mockDocSnapshot.getLong("gtoTestsUsed") } returns 0
        every { mockDocSnapshot.getLong("interviewTestsUsed") } returns 0
        every { mockDocSnapshot.getLong("lastUpdated") } returns System.currentTimeMillis()
        
        coEvery { mockTestUsageDao.insertOrReplace(any()) } just Runs
        
        // When: Check if user can take another test
        val result = subscriptionManager.canTakeTest(TestType.WAT, testUserId)
        
        // Then: Should be blocked (limit reached)
        assertTrue(result is TestEligibility.LimitReached)
        assertEquals(SubscriptionTier.FREE, (result as TestEligibility.LimitReached).tier)
        assertEquals(1, result.limit)
        assertEquals(1, result.usedCount)
    }
    
    /**
     * SECURITY TEST: Verify PRO tier is blocked after 5 tests
     */
    @Test
    fun `canTakeTest blocks PRO tier after 5 tests`() = runTest {
        // Given: PRO tier user with 5 tests used
        val mockProfile = UserProfile(
            userId = testUserId,
            fullName = "Test User",
            age = 25,
            gender = com.ssbmax.core.domain.model.Gender.MALE,
            entryType = com.ssbmax.core.domain.model.EntryType.GRADUATE,
            subscriptionType = SubscriptionType.PRO
        )
        coEvery { mockUserProfileRepository.getUserProfile(testUserId) } returns flowOf(Result.success(mockProfile))
        
        // Mock Firestore with 5 tests used
        val mockDocRef = mockk<com.google.firebase.firestore.DocumentReference>()
        val mockDocSnapshot = mockk<com.google.firebase.firestore.DocumentSnapshot>()
        val mockCollectionRef = mockk<com.google.firebase.firestore.CollectionReference>()
        val mockUserDocRef = mockk<com.google.firebase.firestore.DocumentReference>()
        val mockSubCollectionRef = mockk<com.google.firebase.firestore.CollectionReference>()
        
        every { mockFirestore.collection("users") } returns mockCollectionRef
        every { mockCollectionRef.document(testUserId) } returns mockUserDocRef
        every { mockUserDocRef.collection("subscription") } returns mockSubCollectionRef
        every { mockSubCollectionRef.document(any()) } returns mockDocRef
        every { mockDocRef.get() } returns mockk {
            coEvery { await() } returns mockDocSnapshot
        }
        every { mockDocSnapshot.exists() } returns true
        every { mockDocSnapshot.getLong("oirTestsUsed") } returns 2
        every { mockDocSnapshot.getLong("tatTestsUsed") } returns 1
        every { mockDocSnapshot.getLong("watTestsUsed") } returns 1
        every { mockDocSnapshot.getLong("srtTestsUsed") } returns 1
        every { mockDocSnapshot.getLong("ppdtTestsUsed") } returns 0
        every { mockDocSnapshot.getLong("gtoTestsUsed") } returns 0
        every { mockDocSnapshot.getLong("interviewTestsUsed") } returns 0
        every { mockDocSnapshot.getLong("lastUpdated") } returns System.currentTimeMillis()
        
        coEvery { mockTestUsageDao.insertOrReplace(any()) } just Runs
        
        // When
        val result = subscriptionManager.canTakeTest(TestType.PPDT, testUserId)
        
        // Then: Should be blocked
        assertTrue(result is TestEligibility.LimitReached)
        assertEquals(SubscriptionTier.PRO, (result as TestEligibility.LimitReached).tier)
        assertEquals(5, result.limit)
        assertEquals(5, result.usedCount)
    }
    
    /**
     * SECURITY TEST: Verify PREMIUM tier is never blocked
     */
    @Test
    fun `canTakeTest never blocks PREMIUM tier`() = runTest {
        // Given: PREMIUM tier user with many tests used
        val mockProfile = UserProfile(
            userId = testUserId,
            fullName = "Test User",
            age = 25,
            gender = com.ssbmax.core.domain.model.Gender.MALE,
            entryType = com.ssbmax.core.domain.model.EntryType.GRADUATE,
            subscriptionType = SubscriptionType.PREMIUM
        )
        coEvery { mockUserProfileRepository.getUserProfile(testUserId) } returns flowOf(Result.success(mockProfile))
        
        // Mock Firestore with 100 tests used
        val mockDocRef = mockk<com.google.firebase.firestore.DocumentReference>()
        val mockDocSnapshot = mockk<com.google.firebase.firestore.DocumentSnapshot>()
        val mockCollectionRef = mockk<com.google.firebase.firestore.CollectionReference>()
        val mockUserDocRef = mockk<com.google.firebase.firestore.DocumentReference>()
        val mockSubCollectionRef = mockk<com.google.firebase.firestore.CollectionReference>()
        
        every { mockFirestore.collection("users") } returns mockCollectionRef
        every { mockCollectionRef.document(testUserId) } returns mockUserDocRef
        every { mockUserDocRef.collection("subscription") } returns mockSubCollectionRef
        every { mockSubCollectionRef.document(any()) } returns mockDocRef
        every { mockDocRef.get() } returns mockk {
            coEvery { await() } returns mockDocSnapshot
        }
        every { mockDocSnapshot.exists() } returns true
        every { mockDocSnapshot.getLong(any()) } returns 100
        every { mockDocSnapshot.getLong("lastUpdated") } returns System.currentTimeMillis()
        
        coEvery { mockTestUsageDao.insertOrReplace(any()) } just Runs
        
        // When
        val result = subscriptionManager.canTakeTest(TestType.TAT, testUserId)
        
        // Then: Should be eligible (unlimited)
        assertTrue(result is TestEligibility.Eligible)
        assertTrue((result as TestEligibility.Eligible).remainingTests > 1000)
    }
    
    /**
     * SECURITY TEST: Fail-secure on Firestore error
     */
    @Test
    fun `canTakeTest fails secure on Firestore error`() = runTest {
        // Given: Firestore throws exception
        val mockProfile = UserProfile(
            userId = testUserId,
            fullName = "Test User",
            age = 25,
            gender = com.ssbmax.core.domain.model.Gender.MALE,
            entryType = com.ssbmax.core.domain.model.EntryType.GRADUATE,
            subscriptionType = SubscriptionType.FREE
        )
        coEvery { mockUserProfileRepository.getUserProfile(testUserId) } returns flowOf(Result.success(mockProfile))
        
        every { mockFirestore.collection(any()) } throws Exception("Network error")
        
        // When
        val result = subscriptionManager.canTakeTest(TestType.OIR, testUserId)
        
        // Then: Should block access (fail-secure)
        assertTrue(result is TestEligibility.LimitReached)
    }
}

