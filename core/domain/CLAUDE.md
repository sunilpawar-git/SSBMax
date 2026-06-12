# core/domain/CLAUDE.md — Business Logic Layer

**Scope:** Use cases, domain models, repository interfaces, Result<T> error handling. This file specializes [claude.md](../../claude.md) for the domain layer—where all business logic lives.

**Core Principle:** Zero Android dependencies. Pure Kotlin. Every public **use case** method returns `Result<T>`, never throws. Repository interfaces may throw; use cases catch and wrap. Domain layer is the SSOT for business rules.

---

## Use Case Structure (Template)

**Contract:**
```kotlin
class GetTATQuestionsUseCase @Inject constructor(
  private val tatRepository: TATRepository  // Interface from domain
) {
  suspend fun execute(): Result<List<TATQuestion>> {
    return try {
      val questions = tatRepository.getTATQuestions()
      Result.Success(questions)
    } catch (e: Exception) {
      Result.Failure(e)
    }
  }
}

// ✅ Single responsibility (get TAT questions only)
// ✅ Suspend function for async operations
// ✅ Returns Result<T>, never throws
// ✅ Inject repository interface (not implementation)
// ✅ No Android dependencies (no Context, no Activity, no ViewModel)
```

**Naming Convention:**
- `{Verb}{Subject}UseCase` — e.g., `GetTATQuestionsUseCase`, `SubmitInterviewResponseUseCase`
- One public method: `suspend fun execute(...): Result<T>`
- If multiple steps: break into separate use cases or use domain service

**Anti-patterns:**
- ❌ `getTATQuestionsUseCase.getAndProcess()` — breaks single responsibility
- ❌ Throwing exceptions — wrap all exceptions in `Result.Failure`
- ❌ Direct Firebase/Room calls — call repository interface only
- ❌ Storing state — use cases are stateless

---

## Result<T> Pattern (Error Handling)

**Sealed Class Definition:**
```kotlin
sealed class Result<out T> {
  data class Success<T>(val data: T) : Result<T>()
  data class Failure(val exception: Exception) : Result<Nothing>()
}

// Usage in use case:
when (val result = myUseCase.execute()) {
  is Result.Success -> handleSuccess(result.data)
  is Result.Failure -> handleError(result.exception)
}

// Alternative: extension function for mapping
inline fun <T, R> Result<T>.map(transform: (T) -> R): Result<R> {
  return when (this) {
    is Result.Success -> Result.Success(transform(data))
    is Result.Failure -> this
  }
}
```

**Why Result<T>?**
- ✅ Explicit error handling (no try-catch needed in callers)
- ✅ No exception throwing in data layer (Rule #8)
- ✅ Type-safe chaining with `map()` and `flatMap()`
- ✅ No hidden exceptions in logs

**Never use:**
- ❌ `fun execute(): T` — caller doesn't know failure cases
- ❌ `fun execute(): T?` — nullable return is ambiguous
- ❌ Throwing exceptions from domain layer — caught in data, wrapped in Result

---

## Repository Interfaces (SSOT for Data Access)

**Pattern:**
```kotlin
// In core/domain/repository/
interface TATRepository {
  suspend fun getTATQuestions(limit: Int = 20): List<TATQuestion>
  suspend fun submitResponse(response: TATResponse): Result<String> // returns testId
}

// In core/data/repository/ (implementation only)
@Inject
class TATRepositoryImpl(
  private val firestore: FirebaseFirestore,
  private val tatDao: TATDAO,
  private val errorLogger: ErrorLogger
) : TATRepository {
  override suspend fun getTATQuestions(limit: Int): List<TATQuestion> {
    return try {
      // 1. Try local cache (Room)
      val cached = tatDao.getTATQuestions(limit)
      if (cached.isNotEmpty()) return cached
      
      // 2. Fetch from Firestore
      val remote = firestore.collection("tat_questions")
        .limit(limit.toLong())
        .get()
        .await()
        .toObjects(TATQuestion::class.java)
      
      // 3. Cache locally
      tatDao.insertAll(remote)
      remote
    } catch (e: Exception) {
      errorLogger.log(e, "Failed to get TAT questions")
      emptyList()
    }
  }
}

// ✅ Interface in domain (contracts business logic)
// ✅ Implementation in data (handles infrastructure)
// ✅ ViewModels inject interface, use cases call it
// ✅ Implementation details hidden from business logic
```

**Key Rules:**
- Interface: NO Android imports, NO Firebase imports
- Implementation: Can use Firebase, Room, anything needed
- Use cases call interface, never implementation
- Swap implementations for testing (mock repository)

---

## Domain Models (Immutable Data Classes)

**Pattern:**
```kotlin
// Immutable, no logic
data class TATQuestion(
  val id: String,
  val questionText: String,
  val imageUrl: String,
  val timeLimit: Int,
  val difficulty: String
) {
  // Optional: validation logic (immutable)
  fun isValid(): Boolean = 
    id.isNotBlank() && questionText.isNotBlank() && timeLimit > 0
}

// Sealed class for domain concepts (not enums, for type safety)
sealed class TestType {
  data object TAT : TestType()
  data object WAT : TestType()
  data object Interview : TestType()
}

// ✅ No getters/setters (data class provides them)
// ✅ Validation methods are pure functions
// ✅ No repository calls from models
// ✅ No Android dependencies
```

**Anti-patterns:**
- ❌ `class TATQuestion { var questionText: String }` — use data class
- ❌ `enum class TestType { TAT, WAT }` with `fun subscriptionRequired()` — use sealed class instead
- ❌ Models that call repositories — separate concerns
- ❌ Storing references to services — pass via constructor

---

## Security Annotations (Domain-Level)

**Pattern (Embedded in SSBPhase):**
```kotlin
// In core/domain/model/SSBPhase.kt
enum class SSBPhase(
  val displayName: String,
  val subscriptionRequired: String? = null,
  val authenticationRequired: Boolean = true
) {
  OIR("Observation & Reading", subscriptionRequired = "FREE"),
  TAT("Thematic Apperception", subscriptionRequired = "PREMIUM"),
  INTERVIEW("Interview Simulation", subscriptionRequired = "PREMIUM", authenticationRequired = true),
  // ...
}

// Usage in use case:
class CheckSubscriptionUseCase @Inject constructor(
  private val subscriptionManager: SubscriptionManager
) {
  suspend fun execute(testType: SSBPhase): Result<Unit> {
    val tier = subscriptionManager.getUserTier()
    val required = testType.subscriptionRequired ?: return Result.Success(Unit)
    
    return if (tier == required || tier == "PREMIUM") {
      Result.Success(Unit)
    } else {
      Result.Failure(Exception("Subscription upgrade required"))
    }
  }
}

// ✅ Annotations embedded in domain models
// ✅ Use cases check before proceeding
// ✅ ViewModels prevent unauthorized access early
```

**SSOT:** [SubscriptionManager](../data/repository/SubscriptionManager.kt) is the ONLY place subscription limits are defined.

---

## Error Handling Strategy

**Rule: Never throw from domain layer. Use Result<T>.**

```kotlin
// ❌ BAD: Exception thrown
fun submitTest(): TestResult {
  val response = repository.submit(data) // throws RepositoryException
  return parseResponse(response)
}

// ✅ GOOD: Result wrapped
suspend fun submitTest(): Result<TestResult> {
  return try {
    val response = repository.submit(data)
    Result.Success(parseResponse(response))
  } catch (e: Exception) {
    Result.Failure(e)
  }
}

// ✅ BETTER: Repository returns Result
suspend fun submitTest(): Result<TestResult> {
  val response = repository.submit(data) // already returns Result<Response>
  return response.map { parseResponse(it) }
}
```

**Errors should encode:**
```kotlin
// Example: Custom domain exception
class InsufficientSubscriptionException(val requiredTier: String) : 
  Exception("Upgrade to $requiredTier required")

// Caught in data layer:
catch (e: InsufficientSubscriptionException) {
  Result.Failure(e)
}
```

---

## Testing Domain Layer

**Unit Tests (JUnit 4 + MockK):**
```bash
./gradlew :core:domain:testDebugUnitTest -k "GetTATQuestionsUseCaseTest"
```

**Test Template:**
```kotlin
class GetTATQuestionsUseCaseTest {
  private val mockRepository = mockk<TATRepository>()
  private val useCase = GetTATQuestionsUseCase(mockRepository)
  
  @Test
  fun `execute returns success when repository returns data`() = runTest {
    val questions = listOf(mockk<TATQuestion>())
    coEvery { mockRepository.getTATQuestions() } returns questions
    
    val result = useCase.execute()
    
    assertThat(result).isInstanceOf(Result.Success::class.java)
    assertThat((result as Result.Success).data).isEqualTo(questions)
  }
  
  @Test
  fun `execute returns failure when repository throws`() = runTest {
    coEvery { mockRepository.getTATQuestions() } throws Exception("Network error")
    
    val result = useCase.execute()
    
    assertThat(result).isInstanceOf(Result.Failure::class.java)
  }
}
```

**Key:**
- Mock repository interface (use `mockk<RepositoryInterface>()`)
- Test success + failure paths
- No Android dependencies needed
- Fast execution (< 100ms per test)

---

## Architecture Diagram (Domain in Context)

```
Compose Screen
    ↓ (collects StateFlow)
ViewModel (uses use cases)
    ↓ (calls suspend fun)
UseCase (domain layer) ← ← ← YOU ARE HERE
    ↓ (calls interface)
Repository Interface (domain)
    ↓ (implemented by)
RepositoryImpl (data layer)
    ↓ (calls)
Firebase / Room / Gemini
```

**Key:**
- ✅ Domain layer is the business logic "core"
- ✅ No outbound Android dependencies
- ✅ All interfaces (SSOT for contracts)
- ✅ All errors wrapped in Result<T>

---

## References

- **Root guidance:** [claude.md](../../claude.md) (12 core rules, SSOT principle)
- **Data layer implementation:** [core/data/CLAUDE.md](../data/CLAUDE.md)
- **UI layer usage:** [app/CLAUDE.md](../../app/CLAUDE.md) (ViewModel patterns)
- **Security patterns:** [docs/security/SECURITY.md](../../docs/security/SECURITY.md)

---

**Last Updated:** June 2026 | **Maintainer:** Sunil Pawar
