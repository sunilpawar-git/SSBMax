# core:data/CLAUDE.md — Data Layer Guidance

**Scope:** Module-specific patterns for Firebase, Room, repositories, and secrets. For general SSBMax rules, see root [`claude.md`](../../claude.md).

## Quick Reference: Data Layer Patterns

### 1. Repository Pattern (SSOT for data access)

**Interface (core:domain):**
```kotlin
// core/domain/src/main/kotlin/com/ssbmax/core/domain/repository/OIRRepository.kt
interface OIRRepository {
    suspend fun getQuestion(id: String): Result<OIRQuestion>
    suspend fun submitAnswer(id: String, answer: String): Result<Unit>
}
```

**Implementation (core:data):**
```kotlin
// core/data/src/main/kotlin/com/ssbmax/core/data/repository/OIRRepositoryImpl.kt
@Singleton
class OIRRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val cache: OIRDatabase
) : OIRRepository {
    override suspend fun getQuestion(id: String): Result<OIRQuestion> = withContext(Dispatchers.IO) {
        try {
            val docSnapshot = firestore.collection("oir_questions").document(id).get().await()
            Result.success(docSnapshot.toObject(OIRQuestion::class.java) ?: throw Exception("Null"))
        } catch (e: Exception) {
            ErrorLogger.log(e, "Failed to fetch OIR question $id")
            Result.failure(e)
        }
    }
}
```

**Usage (app/ViewModels):**
```kotlin
class OIRViewModel @Inject constructor(
    private val oirRepository: OIRRepository  // Injected interface, NOT impl
) : ViewModel() {
    fun loadQuestion(id: String) {
        viewModelScope.launch {
            oirRepository.getQuestion(id).onSuccess { question ->
                _uiState.update { it.copy(question = question) }
            }
        }
    }
}
```

**Key Rule:** ViewModel/Compose NEVER calls Firebase directly. Always route through repository.

---

### 2. Secret/Config Management (Never hardcoded)

**BuildConfig approach (Local development):**
```kotlin
// app/build.gradle.kts
debug {
    buildConfigField("String", "GEMINI_API_KEY", "\"${System.getenv("GEMINI_API_KEY") ?: ""}\"")
}

// Access in code:
val apiKey = BuildConfig.GEMINI_API_KEY

// In local.properties:
GEMINI_API_KEY=sk-proj-...
```

**Firebase Remote Config (Production):**
```kotlin
@Singleton
class ConfigProvider @Inject constructor(
    private val remoteConfig: FirebaseRemoteConfig
) {
    suspend fun getApiKey(name: String): String = withContext(Dispatchers.Default) {
        remoteConfig.getString(name)  // Fetched from Firebase Console
    }
}

// Usage:
val apiKey = configProvider.getApiKey("gemini_api_key")
```

**Lint enforcement:** HardcodedApiKeyDetector flags any `const val API_KEY = "sk_..."`. Use lint to prevent mistakes.

---

### 3. Room Database (Local persistence)

**Entity + DAO:**
```kotlin
@Entity(tableName = "oir_questions", indices = [Index("questionId", unique = true)])
data class CachedOIRQuestion(
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "questionId") val questionId: String,
    val questionText: String,
    val options: String,  // JSON serialized
    val correctAnswerId: String,
    @ColumnInfo(defaultValue = "CURRENT_TIMESTAMP") val cachedAt: Long
)

@Dao
interface OIRQuestionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestion(question: CachedOIRQuestion)

    @Query("SELECT * FROM oir_questions WHERE questionId = :id")
    suspend fun getQuestion(id: String): CachedOIRQuestion?

    @Query("DELETE FROM oir_questions WHERE cachedAt < :expiryTime")
    suspend fun evictStale(expiryTime: Long)
}
```

**Migration pattern:**
```kotlin
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE oir_questions_new (
                id INTEGER PRIMARY KEY,
                questionId TEXT NOT NULL UNIQUE,
                ...
            )
        """)
        db.execSQL("INSERT INTO oir_questions_new SELECT * FROM oir_questions")
        db.execSQL("DROP TABLE oir_questions")
        db.execSQL("ALTER TABLE oir_questions_new RENAME TO oir_questions")
    }
}

// In database:
@Database(entities = [CachedOIRQuestion::class, ...], version = 2)
abstract class SSBDatabase : RoomDatabase() {
    companion object {
        val ALL_MIGRATIONS = arrayOf(MIGRATION_1_2, ...)
    }
}
```

**Key Rule:** Always run migrations in tests; schema changes must be backward-compatible.

---

### 4. Error Handling (Result<T> pattern)

```kotlin
// Domain layer (core:domain) defines Result<T>
sealed class Result<T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Failure<T>(val exception: Exception) : Result<T>()
}

// Data layer (core:data) returns Result
suspend fun fetchData(): Result<List<Data>> {
    return try {
        val data = firestore.collection("data").get().await()
            .toObjects(Data::class.java)
        Result.Success(data)
    } catch (e: Exception) {
        ErrorLogger.log(e, "Fetch failed")
        Result.Failure(e)
    }
}

// ViewModel (app) handles Result
when (val result = repository.fetchData()) {
    is Result.Success -> _uiState.update { it.copy(items = result.data) }
    is Result.Failure -> _uiState.update { it.copy(error = "Load failed") }
}
```

**No throwing in data layer.** Always wrap in Result<T>.

---

### 5. AI Service Integration (Gemini)

```kotlin
@Singleton
class GeminiService @Inject constructor(
    private val client: GenerativeModel,
    private val config: ConfigProvider
) {
    suspend fun evaluateResponse(prompt: String, userResponse: String): Result<String> {
        return try {
            val result = client.generateContent(buildString {
                append(prompt)
                append("\n\nUser response: $userResponse")
            })
            Result.Success(result.text ?: "")
        } catch (e: Exception) {
            ErrorLogger.log(e, "Gemini eval failed for prompt length=${prompt.length}")
            Result.Failure(e)
        }
    }
}

// Hilt: Provide GenerativeModel
@Provides
@Singleton
fun provideGenerativeModel(@ApplicationContext context: Context): GenerativeModel {
    val apiKey = BuildConfig.GEMINI_API_KEY  // or remote config
    return GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = apiKey
    )
}
```

**Key Rule:** Never pass API keys as parameters; inject them. Use DI + Hilt.

---

### 6. Firestore Security Rules (SSOT for access control)

```
// firestore.rules
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    
    // Users own their data
    match /users/{uid} {
      allow read, write: if request.auth.uid == uid;
      allow read: if request.auth.token.admin == true;
    }

    // Interviews: user creates, admin reviews
    match /interviews/{docId} {
      allow create: if request.auth != null;
      allow read: if request.auth.uid == resource.data.userId || request.auth.token.admin == true;
      allow update, delete: if request.auth.token.admin == true;
    }

    // Questions: public read, admin write
    match /oir_questions/{docId} {
      allow read: if request.auth != null;
      allow write: if request.auth.token.admin == true;
    }
  }
}
```

**Key Rule:** Rules are SSOT. Always update rules when data model changes.

---

## Lint Rules for Data Layer

| Detector | Scope | Example |
|----------|-------|---------|
| **HardcodedApiKey** | Blocks: `const val GEMINI_API_KEY = "sk_..."` | Use BuildConfig or RemoteConfig |
| **SensitiveDataInLogs** | Blocks: `Log.d("userId=$userId")` | Use ErrorLogger (auto-sanitizes) |
| **FirebaseInUILayer** | Blocks: `Compose { firestore.get() }` | Use repository only |
| **StateFlowValueAssignment** | Blocks: `_state = state.copy()` | Use `.update { }` only |

**Pre-commit hook** enforces all 4 detectors before commit. If you bypass (`--no-verify`), CI will catch it.

---

## Testing Data Layer

```bash
# Unit tests (Room, Repository logic)
./gradlew :core:data:testDebugUnitTest

# Room migrations
./gradlew :core:data:testDebugUnitTest --tests "*MigrationTest"

# Firestore integration (if emulator)
./gradlew :core:data:testDebugUnitTest --tests "*FirestoreTest"
```

**Key Rule:** Every migration must be tested. Schema changes are permanent (breaking in production).

---

## File Organization (core:data)

```
core/data/src/main/kotlin/com/ssbmax/core/data/
├── ai/                    # Gemini, embedding services
├── local/                 # Room database
│   ├── dao/               # Database access objects
│   ├── entity/            # @Entity data classes
│   ├── migration/         # Database migrations
│   └── SSBDatabase.kt     # Main DB class
├── remote/                # Firebase integration
│   ├── FirebaseService.kt
│   └── ConfigProvider.kt
├── repository/            # Repository implementations
└── util/                  # Data layer utilities
```

Each subdirectory is a SSOT for its domain (database = room, remotes = firebase, repos = business logic).
