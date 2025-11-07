package com.ssbmax.ui.settings

import android.util.Log
import app.cash.turbine.test
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.ssbmax.testing.BaseViewModelTest
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Ignore
import org.junit.Test

/**
 * Unit tests for SubscriptionManagementViewModel
 * 
 * Tests subscription tier loading, usage tracking, error handling,
 * and UI state management.
 * 
 * Tests use MockK to properly mock Firebase Task responses.
 * Firebase emulator configuration is available in core/data/build.gradle.kts.
 */
class SubscriptionManagementViewModelTest : BaseViewModelTest() {
    
    companion object {
        @BeforeClass
        @JvmStatic
        fun setupClass() {
            // Mock android.util.Log for all tests
            mockkStatic(Log::class)
            every { Log.d(any(), any()) } returns 0
            every { Log.e(any(), any()) } returns 0
            every { Log.e(any(), any(), any()) } returns 0
            every { Log.w(any(), any<String>()) } returns 0
            every { Log.w(any(), any(), any()) } returns 0
            every { Log.i(any(), any()) } returns 0
            every { Log.v(any(), any()) } returns 0
        }
    }
    
    private lateinit var viewModel: SubscriptionManagementViewModel
    private lateinit var mockFirebaseAuth: FirebaseAuth
    private lateinit var mockFirestore: FirebaseFirestore
    private lateinit var mockUser: FirebaseUser
    
    @Before
    fun setup() {
        mockFirebaseAuth = mockk(relaxed = true)
        mockFirestore = mockk(relaxed = true)
        mockUser = mockk(relaxed = true)
        
        // Default: user is authenticated
        every { mockFirebaseAuth.currentUser } returns mockUser
        every { mockUser.uid } returns "test-user-123"
    }
    
    @After
    fun tearDown() {
        unmockkAll()
    }
    
    // ==================== Subscription Tier Tests ====================
    
    @Test
    fun `loads FREE tier correctly`() = runTest {
        // Given - user has FREE tier
        setupMockFirestore(tier = "FREE", oirUsed = 0)
        
        viewModel = SubscriptionManagementViewModel(mockFirebaseAuth, mockFirestore)
        
        // When
        viewModel.loadSubscriptionData()
        
        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            
            assertFalse("Should not be loading", state.isLoading)
            assertNull("Should have no error", state.error)
            assertEquals("Should be FREE tier", SubscriptionTierModel.FREE, state.currentTier)
            assertEquals("Should have OIR usage", 0, state.monthlyUsage["OIR Tests"]?.used)
            assertEquals("Should have OIR limit", 1, state.monthlyUsage["OIR Tests"]?.limit)
            assertNull("FREE tier should have no expiry", state.subscriptionExpiresAt)
        }
    }
    
    @Test
    fun `loads PRO tier correctly with usage data`() = runTest {
        // Given - user has PRO tier with some usage
        setupMockFirestore(tier = "PRO", oirUsed = 2, tatUsed = 1)
        
        viewModel = SubscriptionManagementViewModel(mockFirebaseAuth, mockFirestore)
        
        // When
        viewModel.loadSubscriptionData()
        
        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            
            assertEquals("Should be PRO tier", SubscriptionTierModel.PRO, state.currentTier)
            assertEquals("OIR used should be 2", 2, state.monthlyUsage["OIR Tests"]?.used)
            assertEquals("OIR limit should be 5", 5, state.monthlyUsage["OIR Tests"]?.limit)
            assertEquals("TAT used should be 1", 1, state.monthlyUsage["TAT Tests"]?.used)
            assertEquals("TAT limit should be 3", 3, state.monthlyUsage["TAT Tests"]?.limit)
            assertNotNull("PRO tier should have expiry date", state.subscriptionExpiresAt)
        }
    }
    
    @Test
    fun `loads PREMIUM tier correctly with unlimited access`() = runTest {
        // Given - user has PREMIUM tier
        setupMockFirestore(tier = "PREMIUM", oirUsed = 10, tatUsed = 8)
        
        viewModel = SubscriptionManagementViewModel(mockFirebaseAuth, mockFirestore)
        
        // When
        viewModel.loadSubscriptionData()
        
        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            
            assertEquals("Should be PREMIUM tier", SubscriptionTierModel.PREMIUM, state.currentTier)
            assertEquals("OIR limit should be unlimited (-1)", -1, state.monthlyUsage["OIR Tests"]?.limit)
            assertEquals("TAT limit should be unlimited (-1)", -1, state.monthlyUsage["TAT Tests"]?.limit)
            assertNotNull("PREMIUM tier should have expiry date", state.subscriptionExpiresAt)
        }
    }
    
    @Test
    fun `defaults to FREE tier when subscription document not found`() = runTest {
        // Given - subscription document doesn't exist
        setupMockFirestore(tier = null, oirUsed = 0)
        
        viewModel = SubscriptionManagementViewModel(mockFirebaseAuth, mockFirestore)
        
        // When
        viewModel.loadSubscriptionData()
        
        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            
            assertEquals("Should default to FREE tier", SubscriptionTierModel.FREE, state.currentTier)
            assertFalse("Should not be loading", state.isLoading)
        }
    }
    
    // ==================== Usage Calculation Tests ====================
    
    @Test
    fun `calculates usage percentages correctly for FREE tier`() = runTest {
        // Given - FREE user used 1/1 OIR test
        setupMockFirestore(tier = "FREE", oirUsed = 1)
        
        viewModel = SubscriptionManagementViewModel(mockFirebaseAuth, mockFirestore)
        viewModel.loadSubscriptionData()
        
        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            val oirUsage = state.monthlyUsage["OIR Tests"]!!
            
            assertEquals("Used 1 test", 1, oirUsage.used)
            assertEquals("Limit is 1", 1, oirUsage.limit)
            
            // Calculate percentage
            val percentage = (oirUsage.used.toFloat() / oirUsage.limit * 100).toInt()
            assertEquals("Should be 100% used", 100, percentage)
        }
    }
    
    @Test
    fun `calculates usage percentages correctly for PRO tier`() = runTest {
        // Given - PRO user used 2/5 OIR tests
        setupMockFirestore(tier = "PRO", oirUsed = 2)
        
        viewModel = SubscriptionManagementViewModel(mockFirebaseAuth, mockFirestore)
        viewModel.loadSubscriptionData()
        
        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            val oirUsage = state.monthlyUsage["OIR Tests"]!!
            
            val percentage = (oirUsage.used.toFloat() / oirUsage.limit * 100).toInt()
            assertEquals("Should be 40% used", 40, percentage)
        }
    }
    
    @Test
    fun `handles unlimited usage for PREMIUM tier`() = runTest {
        // Given - PREMIUM user with high usage
        setupMockFirestore(tier = "PREMIUM", oirUsed = 50)
        
        viewModel = SubscriptionManagementViewModel(mockFirebaseAuth, mockFirestore)
        viewModel.loadSubscriptionData()
        
        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            val oirUsage = state.monthlyUsage["OIR Tests"]!!
            
            assertEquals("Used 50 tests", 50, oirUsage.used)
            assertEquals("Limit should be -1 (unlimited)", -1, oirUsage.limit)
            
            // For unlimited, we don't show percentage
            assertTrue("PREMIUM has unlimited access", oirUsage.limit == -1)
        }
    }
    
    // ==================== Error Handling Tests ====================
    
    @Test
    fun `handles authentication error gracefully`() = runTest {
        // Given - user not authenticated
        every { mockFirebaseAuth.currentUser } returns null
        
        viewModel = SubscriptionManagementViewModel(mockFirebaseAuth, mockFirestore)
        
        // When
        viewModel.loadSubscriptionData()
        
        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            
            assertFalse("Should not be loading", state.isLoading)
            assertNotNull("Should have error", state.error)
            assertTrue("Error should mention authentication", 
                state.error!!.contains("not authenticated"))
        }
    }
    
    @Test
    fun `handles Firestore error gracefully`() = runTest {
        // Given - Firestore throws exception
        every { mockFirebaseAuth.currentUser } returns mockUser
        every { mockUser.uid } returns "test-user-123"
        
        val mockUsersCollection = mockk<com.google.firebase.firestore.CollectionReference>(relaxed = true)
        val mockUserDocRef = mockk<DocumentReference>(relaxed = true)
        val mockDataCollection = mockk<com.google.firebase.firestore.CollectionReference>(relaxed = true)
        val mockSubscriptionDocRef = mockk<DocumentReference>(relaxed = true)
        val mockTask = mockk<Task<DocumentSnapshot>>(relaxed = true)
        
        val testException = Exception("Network error")
        
        every { mockFirestore.collection("users") } returns mockUsersCollection
        every { mockUsersCollection.document(any()) } returns mockUserDocRef
        every { mockUserDocRef.collection("data") } returns mockDataCollection
        every { mockDataCollection.document("subscription") } returns mockSubscriptionDocRef
        every { mockSubscriptionDocRef.get() } returns mockTask
        
        // Mock Task to fail
        every { mockTask.isSuccessful } returns false
        every { mockTask.isComplete } returns true
        every { mockTask.exception } returns testException
        
        // Mock failure callback for await()
        every { mockTask.addOnSuccessListener(any()) } returns mockTask
        every { 
            mockTask.addOnFailureListener(any())
        } answers {
            val listener = firstArg<com.google.android.gms.tasks.OnFailureListener>()
            listener.onFailure(testException)
            mockTask
        }
        every { mockTask.addOnCompleteListener(any()) } returns mockTask
        
        viewModel = SubscriptionManagementViewModel(mockFirebaseAuth, mockFirestore)
        
        // When
        viewModel.loadSubscriptionData()
        
        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            
            assertFalse("Should not be loading", state.isLoading)
            assertNotNull("Should have error", state.error)
            assertTrue("Error should mention failure", 
                state.error!!.contains("Failed to load"))
        }
    }
    
    @Test
    fun `loads successfully even when usage document is missing`() = runTest {
        // Given - tier exists but usage document returns nulls
        val mockUsersCollection = mockk<com.google.firebase.firestore.CollectionReference>(relaxed = true)
        val mockUserDocRef = mockk<DocumentReference>(relaxed = true)
        val mockDataCollection = mockk<com.google.firebase.firestore.CollectionReference>(relaxed = true)
        val mockUsageCollection = mockk<com.google.firebase.firestore.CollectionReference>(relaxed = true)
        val mockSubscriptionDocRef = mockk<DocumentReference>(relaxed = true)
        val mockUsageDocRef = mockk<DocumentReference>(relaxed = true)
        
        val mockTierTask = mockk<Task<DocumentSnapshot>>(relaxed = true)
        val mockUsageTask = mockk<Task<DocumentSnapshot>>(relaxed = true)
        val mockTierDoc = mockk<DocumentSnapshot>(relaxed = true)
        val mockUsageDoc = mockk<DocumentSnapshot>(relaxed = true)
        
        every { mockFirebaseAuth.currentUser } returns mockUser
        every { mockUser.uid } returns "test-user-123"
        
        // Mock Firestore navigation
        every { mockFirestore.collection("users") } returns mockUsersCollection
        every { mockUsersCollection.document(any()) } returns mockUserDocRef
        every { mockUserDocRef.collection("data") } returns mockDataCollection
        every { mockDataCollection.document("subscription") } returns mockSubscriptionDocRef
        every { mockSubscriptionDocRef.get() } returns mockTierTask
        every { mockUserDocRef.collection("test_usage") } returns mockUsageCollection
        every { mockUsageCollection.document(any()) } returns mockUsageDocRef
        every { mockUsageDocRef.get() } returns mockUsageTask
        
        // Tier document exists
        every { mockTierTask.isSuccessful } returns true
        every { mockTierTask.isComplete } returns true
        every { mockTierTask.result } returns mockTierDoc
        every { mockTierTask.exception } returns null
        every { mockTierDoc.getString("tier") } returns "PRO"
        every { mockTierDoc.exists() } returns true
        
        // Usage document doesn't exist (returns nulls)
        every { mockUsageTask.isSuccessful } returns true
        every { mockUsageTask.isComplete } returns true
        every { mockUsageTask.result } returns mockUsageDoc
        every { mockUsageTask.exception } returns null
        every { mockUsageDoc.getLong(any()) } returns null
        every { mockUsageDoc.exists() } returns false
        
        // Mock callbacks for await()
        every { 
            mockTierTask.addOnSuccessListener(any())
        } answers {
            val listener = firstArg<com.google.android.gms.tasks.OnSuccessListener<DocumentSnapshot>>()
            listener.onSuccess(mockTierDoc)
            mockTierTask
        }
        every { 
            mockUsageTask.addOnSuccessListener(any())
        } answers {
            val listener = firstArg<com.google.android.gms.tasks.OnSuccessListener<DocumentSnapshot>>()
            listener.onSuccess(mockUsageDoc)
            mockUsageTask
        }
        every { mockTierTask.addOnFailureListener(any()) } returns mockTierTask
        every { mockTierTask.addOnCompleteListener(any()) } returns mockTierTask
        every { mockUsageTask.addOnFailureListener(any()) } returns mockUsageTask
        every { mockUsageTask.addOnCompleteListener(any()) } returns mockUsageTask
        
        viewModel = SubscriptionManagementViewModel(mockFirebaseAuth, mockFirestore)
        
        // When
        viewModel.loadSubscriptionData()
        
        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            
            assertFalse("Should not be loading", state.isLoading)
            assertNull("Should have no error", state.error)
            assertEquals("Should be PRO tier", SubscriptionTierModel.PRO, state.currentTier)
            // Usage should default to 0
            assertEquals("OIR used should default to 0", 0, state.monthlyUsage["OIR Tests"]?.used)
        }
    }
    
    // ==================== Helper Methods ====================
    
    private fun setupMockFirestore(
        tier: String?,
        oirUsed: Int = 0,
        tatUsed: Int = 0,
        watUsed: Int = 0,
        srtUsed: Int = 0,
        ppdtUsed: Int = 0
    ) {
        val mockUsersCollection = mockk<com.google.firebase.firestore.CollectionReference>(relaxed = true)
        val mockUserDocRef = mockk<DocumentReference>(relaxed = true)
        val mockDataCollection = mockk<com.google.firebase.firestore.CollectionReference>(relaxed = true)
        val mockUsageCollection = mockk<com.google.firebase.firestore.CollectionReference>(relaxed = true)
        val mockSubscriptionDocRef = mockk<DocumentReference>(relaxed = true)
        val mockUsageDocRef = mockk<DocumentReference>(relaxed = true)
        
        val mockTierTask = mockk<Task<DocumentSnapshot>>(relaxed = true)
        val mockUsageTask = mockk<Task<DocumentSnapshot>>(relaxed = true)
        val mockTierDoc = mockk<DocumentSnapshot>(relaxed = true)
        val mockUsageDoc = mockk<DocumentSnapshot>(relaxed = true)
        
        // Mock Firestore collection navigation
        every { mockFirestore.collection("users") } returns mockUsersCollection
        every { mockUsersCollection.document(any()) } returns mockUserDocRef
        
        // Mock data subcollection for subscription tier
        every { mockUserDocRef.collection("data") } returns mockDataCollection
        every { mockDataCollection.document("subscription") } returns mockSubscriptionDocRef
        every { mockSubscriptionDocRef.get() } returns mockTierTask
        
        // Mock test_usage subcollection
        every { mockUserDocRef.collection("test_usage") } returns mockUsageCollection
        every { mockUsageCollection.document(any()) } returns mockUsageDocRef
        every { mockUsageDocRef.get() } returns mockUsageTask
        
        // Setup tier document mock with await() support
        every { mockTierTask.isSuccessful } returns true
        every { mockTierTask.isComplete } returns true
        every { mockTierTask.result } returns mockTierDoc
        every { mockTierTask.exception } returns null
        every { mockTierDoc.getString("tier") } returns tier
        every { mockTierDoc.exists() } returns (tier != null)
        
        // Setup usage document mock with await() support
        every { mockUsageTask.isSuccessful } returns true
        every { mockUsageTask.isComplete } returns true
        every { mockUsageTask.result } returns mockUsageDoc
        every { mockUsageTask.exception } returns null
        every { mockUsageDoc.getLong("oirTestsUsed") } returns oirUsed.toLong()
        every { mockUsageDoc.getLong("tatTestsUsed") } returns tatUsed.toLong()
        every { mockUsageDoc.getLong("watTestsUsed") } returns watUsed.toLong()
        every { mockUsageDoc.getLong("srtTestsUsed") } returns srtUsed.toLong()
        every { mockUsageDoc.getLong("ppdtTestsUsed") } returns ppdtUsed.toLong()
        every { mockUsageDoc.exists() } returns true
        
        // Mock Task callback registration for await() support
        every { 
            mockTierTask.addOnSuccessListener(any())
        } answers {
            val listener = firstArg<com.google.android.gms.tasks.OnSuccessListener<DocumentSnapshot>>()
            listener.onSuccess(mockTierDoc)
            mockTierTask
        }
        
        every { 
            mockUsageTask.addOnSuccessListener(any())
        } answers {
            val listener = firstArg<com.google.android.gms.tasks.OnSuccessListener<DocumentSnapshot>>()
            listener.onSuccess(mockUsageDoc)
            mockUsageTask
        }
        
        every { mockTierTask.addOnFailureListener(any()) } returns mockTierTask
        every { mockTierTask.addOnCompleteListener(any()) } returns mockTierTask
        every { mockUsageTask.addOnFailureListener(any()) } returns mockUsageTask
        every { mockUsageTask.addOnCompleteListener(any()) } returns mockUsageTask
    }
}

