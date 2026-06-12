# core/data/remote/CLAUDE.md — Firebase Integration

**Scope:** Firestore document structure, security rules, real-time listeners, batch operations, caching. Inherits [core/data/CLAUDE.md](../CLAUDE.md). This file adds Firebase/Firestore-specific patterns.

**Key Addition:** Firestore structure + security rules (SSOT) + transaction patterns + caching strategy.

---

## Firestore Document Structure (SSOT for Data Model)

**Collection Architecture:**
```
Firebase Project: SSBMax
├── users/{userId}
│   ├── name: String
│   ├── email: String
│   ├── subscriptionTier: String ("FREE", "PREMIUM", "LIFETIME")
│   ├── createdAt: Timestamp
│   └── lastActiveAt: Timestamp
│
├── tat_questions/{questionId}
│   ├── questionText: String
│   ├── imageUrl: String
│   ├── options: [{ id, text, imageUrl }]
│   ├── correctAnswerId: String
│   ├── difficulty: String
│   ├── category: String
│   └── createdAt: Timestamp
│
├── interviews/{interviewId}
│   ├── userId: String (owner)
│   ├── title: String
│   ├── questions: [{ id, text, timeLimit }]
│   ├── status: String ("draft", "active", "completed")
│   ├── createdAt: Timestamp
│   └── responses/{responseId}
│       ├── questionId: String
│       ├── userResponse: String
│       ├── audioUrl: String
│       ├── evaluationScore: Number (1-10)
│       └── evaluationFeedback: String
│
└── userStats/{userId}
    ├── testsCompleted: Number
    ├── averageScore: Number
    ├── lastTestDate: Timestamp
    └── totalTimeSpent: Number (minutes)
```

**Firestore Best Practices:**
- ✅ Denormalize frequently-accessed data (avoid excessive joins)
- ✅ Use subcollections for unbounded data (responses under interviews)
- ✅ Store timestamps as server-side (prevents clock skew)
- ✅ Use document references for relationships
- ✅ Index compound queries early

---

## Firebase Security Rules (SSOT for Access Control)

**Rule Template:**
```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    
    // Helper functions
    function isAuthenticated() {
      return request.auth != null;
    }
    
    function isUserOwner(userId) {
      return request.auth.uid == userId;
    }
    
    function hasSubscription(tier) {
      return get(/databases/$(database)/documents/users/$(request.auth.uid)).data.subscriptionTier == tier;
    }
    
    // Users can read/write own data only
    match /users/{userId} {
      allow read: if isUserOwner(userId);
      allow write: if isUserOwner(userId) && request.resource.data.keys().hasAll(['name', 'email']);
      allow delete: if false; // Never delete
    }
    
    // Questions: public read, admin write
    match /tat_questions/{questionId} {
      allow read: if isAuthenticated();
      allow write: if request.auth.token.admin == true;
    }
    
    // Interviews: owner read/write, PREMIUM only
    match /interviews/{interviewId} {
      allow read: if isUserOwner(resource.data.userId) && hasSubscription("PREMIUM");
      allow create: if isUserOwner(request.resource.data.userId) && hasSubscription("PREMIUM");
      allow update: if isUserOwner(resource.data.userId) && hasSubscription("PREMIUM");
      
      // Responses: user can write own, read own
      match /responses/{responseId} {
        allow read: if isUserOwner(resource.data.userId);
        allow create: if isUserOwner(request.resource.data.userId);
        allow update, delete: if false; // Immutable
      }
    }
    
    // User stats: user can read own
    match /userStats/{userId} {
      allow read: if isUserOwner(userId);
      allow write: if false; // Server-side only (Cloud Functions)
    }
  }
}

// ✅ Rules are SSOT for access control
// ✅ Never trust client-side permission checks alone
// ✅ Server-side increment (userStats) via Cloud Functions
```

**Deploy Rules:**
```bash
firebase deploy --only firestore:rules
```

---

## Repository Pattern with Firestore

**Implementation:**
```kotlin
class TATRepositoryImpl @Inject constructor(
  private val firestore: FirebaseFirestore,
  private val tatDao: TATQuestionDAO,
  private val errorLogger: ErrorLogger
) : TATRepository {
  
  override suspend fun getTATQuestions(): Result<List<TATQuestion>> {
    return try {
      // 1. Try local cache first
      val cached = tatDao.getAllQuestions()
      if (cached.isNotEmpty() && isCacheValid(cached)) {
        return Result.Success(cached)
      }
      
      // 2. Fetch from Firestore
      val snapshot = firestore.collection("tat_questions")
        .get()
        .await()
      
      val questions = snapshot.documents.mapNotNull { doc ->
        doc.toObject(TATQuestion::class.java)?.copy(
          id = doc.id,
          lastUpdated = System.currentTimeMillis()
        )
      }
      
      // 3. Update local cache
      tatDao.insertAll(questions)
      
      Result.Success(questions)
    } catch (e: Exception) {
      errorLogger.log(e, "Failed to fetch TAT questions from Firestore")
      Result.Failure(e)
    }
  }
  
  override suspend fun submitTATResponse(
    response: TATResponse
  ): Result<SubmitResult> {
    return try {
      val userId = FirebaseAuth.getInstance().currentUser?.uid
        ?: return Result.Failure(Exception("Not authenticated"))
      
      // 1. Score the response (in Cloud Function for security)
      val result = firestore
        .collection("interviews")
        .document(response.interviewId)
        .collection("responses")
        .add(response.copy(
          userId = userId,
          timestamp = Timestamp.now()
        ))
        .await()
      
      // 2. Update user stats (via Cloud Function trigger)
      // Function will be triggered automatically on document write
      
      Result.Success(SubmitResult(testId = result.id))
    } catch (e: Exception) {
      errorLogger.log(e, "Failed to submit TAT response")
      Result.Failure(e)
    }
  }
  
  private fun isCacheValid(questions: List<TATQuestion>): Boolean {
    val now = System.currentTimeMillis()
    val oldest = questions.minOf { it.lastUpdated }
    return now - oldest < 7 * 24 * 60 * 60 * 1000L // 7 days
  }
}

// ✅ Repository handles caching logic
// ✅ Firestore security rules enforce access
// ✅ Cloud Function handles scoring (server-side)
```

---

## Real-Time Listeners (Flow-Based)

**Pattern: Observe Firestore changes**

```kotlin
class InterviewRepositoryImpl @Inject constructor(
  private val firestore: FirebaseFirestore,
  private val errorLogger: ErrorLogger
) : InterviewRepository {
  
  override fun observeInterviewResponses(
    interviewId: String
  ): Flow<Result<List<InterviewResponse>>> = callbackFlow {
    val listener = firestore
      .collection("interviews")
      .document(interviewId)
      .collection("responses")
      .orderBy("timestamp", Query.Direction.DESCENDING)
      .addSnapshotListener { snapshot, error ->
        if (error != null) {
          errorLogger.log(error, "Failed to observe responses")
          trySend(Result.Failure(error))
          return@addSnapshotListener
        }
        
        val responses = snapshot?.documents?.mapNotNull { doc ->
          doc.toObject(InterviewResponse::class.java)?.copy(id = doc.id)
        } ?: emptyList()
        
        trySend(Result.Success(responses))
      }
    
    awaitClose {
      listener.remove()
    }
  }
}

// In ViewModel:
@HiltViewModel
class InterviewViewModel @Inject constructor(
  private val interviewRepository: InterviewRepository
) : ViewModel() {
  
  val responses: StateFlow<List<InterviewResponse>> = 
    interviewRepository.observeInterviewResponses(interviewId)
      .map { result ->
        when (result) {
          is Result.Success -> result.data
          is Result.Failure -> emptyList()
        }
      }
      .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
}

// ✅ Real-time updates without polling
// ✅ Automatic cleanup via awaitClose
// ✅ Error handling via Result<T>
```

---

## Transactions (Atomic Multi-Writes)

**Pattern: Multiple related writes or fails atomically**

```kotlin
suspend fun submitInterviewAndUpdateStats(
  interview: Interview,
  responses: List<InterviewResponse>
): Result<String> {
  return try {
    val userId = FirebaseAuth.getInstance().currentUser?.uid
      ?: return Result.Failure(Exception("Not authenticated"))
    
    val newInterviewId = firestore.runTransaction { transaction ->
      // 1. Create interview document
      val interviewRef = firestore
        .collection("interviews")
        .document()
      
      interview.copy(
        userId = userId,
        createdAt = Timestamp.now()
      ).let { transaction.set(interviewRef, it) }
      
      // 2. Add all responses
      responses.forEach { response ->
        val responseRef = interviewRef
          .collection("responses")
          .document()
        
        transaction.set(responseRef, response.copy(
          userId = userId,
          timestamp = Timestamp.now()
        ))
      }
      
      // 3. Increment user stats
      val statsRef = firestore
        .collection("userStats")
        .document(userId)
      
      transaction.update(statsRef,
        "testsCompleted", FieldValue.increment(1),
        "lastTestDate", Timestamp.now()
      )
      
      interviewRef.id // Return ID
    }.await()
    
    Result.Success(newInterviewId)
  } catch (e: Exception) {
    errorLogger.log(e, "Transaction failed")
    Result.Failure(e)
  }
}

// ✅ All operations succeed or all fail (atomic)
// ✅ No partial updates
// ✅ Automatic retry on conflicts
```

---

## Batch Operations (Large Uploads)

**Pattern: Up to 500 writes per batch**

```kotlin
suspend fun importTATQuestions(
  questions: List<TATQuestion>
): Result<Int> {
  return try {
    var uploaded = 0
    var batch = firestore.batch()
    var batchSize = 0
    
    questions.forEach { question ->
      val ref = firestore
        .collection("tat_questions")
        .document(question.id)
      
      batch.set(ref, question)
      batchSize++
      uploaded++
      
      // Firestore limit: 500 writes per batch
      if (batchSize == 500) {
        batch.commit().await()
        batch = firestore.batch()
        batchSize = 0
      }
    }
    
    // Final batch
    if (batchSize > 0) {
      batch.commit().await()
    }
    
    Result.Success(uploaded)
  } catch (e: Exception) {
    errorLogger.log(e, "Batch import failed")
    Result.Failure(e)
  }
}

// ✅ Respects 500-write limit
// ✅ Single atomic batch
// ✅ Error handling via Result<T>
```

---

## Pagination (Large Result Sets)

**Pattern: Query pagination with cursors**

```kotlin
class PaginatedInterviewRepository @Inject constructor(
  private val firestore: FirebaseFirestore
) : InterviewRepository {
  
  suspend fun getInterviewsPage(
    userId: String,
    pageSize: Int = 10,
    cursor: DocumentSnapshot? = null
  ): Result<PaginatedResult<Interview>> {
    return try {
      var query: Query = firestore
        .collection("interviews")
        .whereEqualTo("userId", userId)
        .orderBy("createdAt", Query.Direction.DESCENDING)
      
      // Apply cursor (pagination)
      if (cursor != null) {
        query = query.startAfter(cursor)
      }
      
      // Fetch one extra to detect end of list
      val snapshot = query
        .limit((pageSize + 1).toLong())
        .get()
        .await()
      
      val interviews = snapshot.documents
        .take(pageSize)
        .mapNotNull { doc ->
          doc.toObject(Interview::class.java)?.copy(id = doc.id)
        }
      
      val hasMore = snapshot.documents.size > pageSize
      val nextCursor = if (hasMore) {
        snapshot.documents[pageSize]
      } else {
        null
      }
      
      Result.Success(PaginatedResult(
        data = interviews,
        nextCursor = nextCursor,
        hasMore = hasMore
      ))
    } catch (e: Exception) {
      Result.Failure(e)
    }
  }
}

data class PaginatedResult<T>(
  val data: List<T>,
  val nextCursor: DocumentSnapshot?,
  val hasMore: Boolean
)

// ✅ Efficient pagination with cursors
// ✅ No offset (expensive in Firestore)
// ✅ Detect end of list (hasMore flag)
```

---

## Testing Firebase (Emulator)

**Setup:**
```bash
firebase emulators:start --only firestore
```

**Test:**
```kotlin
@RunWith(AndroidJUnit4::class)
class FirestoreIntegrationTest {
  @get:Rule
  val hiltRule = HiltAndroidRule(this)
  
  @Inject
  lateinit var firestore: FirebaseFirestore
  
  @Inject
  lateinit var tatRepository: TATRepository
  
  @Before
  fun setup() {
    hiltRule.inject()
    // Emulator is auto-used (from TestModule)
  }
  
  @Test
  fun getQuestionsReturnsFromFirestore() = runTest {
    // Add test data
    firestore.collection("tat_questions")
      .document("test_q1")
      .set(TATQuestion(...))
      .await()
    
    // Query
    val result = tatRepository.getTATQuestions()
    assertThat(result).isInstanceOf(Result.Success::class.java)
  }
}
```

---

## Best Practices

1. **Security rules first** — design access patterns before implementation
2. **Denormalize wisely** — balance read speed vs consistency
3. **Use transactions** — for related multi-document writes
4. **Index before querying** — Firestore suggests auto-creation
5. **Batch for bulk** — respect 500-write limit
6. **Pagination with cursors** — not offset
7. **Real-time sparingly** — listeners consume quota

---

## References

- **Parent:** [core/data/CLAUDE.md](../CLAUDE.md) (error handling, Result<T>)
- **Firestore Docs:** https://firebase.google.com/docs/firestore
- **Functions:** [functions/CLAUDE.md](../../functions/CLAUDE.md) (backend logic)
- **Security Rules:** [docs/security/SECURITY.md](../../docs/security/SECURITY.md)

---

**Last Updated:** June 2026 | **Maintainer:** Sunil Pawar
