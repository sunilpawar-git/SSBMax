# core/data/local/CLAUDE.md — Room Database Patterns

**Scope:** SQLite via Room, entity design, DAO queries, migrations, schema versioning. Inherits [core/data/CLAUDE.md](../CLAUDE.md). This file adds local database-specific patterns.

**Key Addition:** Entity design + migration patterns + caching strategies + testing with InMemoryDb.

---

## Room Database Setup

**Database Definition:**
```kotlin
// In core/data/src/main/kotlin/com/ssbmax/core/data/local/SSBMaxDatabase.kt

@Database(
  entities = [
    TATQuestion::class,
    OIRQuestion::class,
    InterviewResponse::class,
    UserPreferences::class
  ],
  version = 2,
  autoMigrations = [] // Explicit migrations below
)
abstract class SSBMaxDatabase : RoomDatabase() {
  abstract fun tatQuestionDao(): TATQuestionDAO
  abstract fun oirQuestionDao(): OIRQuestionDAO
  abstract fun interviewResponseDao(): InterviewResponseDAO
  abstract fun userPreferencesDao(): UserPreferencesDAO
}

// Hilt provider
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
  
  @Provides
  @Singleton
  fun provideDatabase(context: Context): SSBMaxDatabase {
    return Room.databaseBuilder(
      context,
      SSBMaxDatabase::class.java,
      "ssbmax.db"
    )
      .addMigrations(MIGRATION_1_2)
      .build()
  }
  
  @Provides
  fun provideTATQuestionDAO(db: SSBMaxDatabase): TATQuestionDAO {
    return db.tatQuestionDao()
  }
}

// ✅ Version increments on schema changes
// ✅ Migrations defined explicitly (no auto-migrations for safety)
// ✅ Singleton instance (reused across app)
```

---

## Entity Design (Data Models)

**Pattern: Annotated data classes**

```kotlin
@Entity(
  tableName = "tat_questions",
  indices = [
    Index("id", unique = true),
    Index("difficulty", "category") // For filtering queries
  ]
)
data class TATQuestion(
  @PrimaryKey
  val id: String,
  
  @ColumnInfo(name = "question_text")
  val questionText: String,
  
  @ColumnInfo(name = "image_url")
  val imageUrl: String?,
  
  @ColumnInfo(name = "time_limit")
  val timeLimit: Int, // in seconds
  
  @ColumnInfo(name = "difficulty")
  val difficulty: String, // "easy", "medium", "hard"
  
  @ColumnInfo(name = "category")
  val category: String?,
  
  @ColumnInfo(name = "created_at")
  val createdAt: Long, // timestamp
  
  @ColumnInfo(name = "last_updated")
  val lastUpdated: Long, // timestamp
  
  @Ignore // Not stored in DB
  val options: List<Option> = emptyList()
) {
  data class Option(
    val id: String,
    val text: String,
    val imageUrl: String? = null
  )
}

// ✅ @Entity marks class as database table
// ✅ @PrimaryKey on unique identifier
// ✅ @ColumnInfo renames columns (snake_case)
// ✅ @Ignore excludes fields from storage
// ✅ Indices on frequently-queried columns
```

**Related Entities (Foreign Keys):**
```kotlin
@Entity(
  tableName = "interview_responses",
  foreignKeys = [
    ForeignKey(
      entity = InterviewQuestion::class,
      parentColumns = ["id"],
      childColumns = ["question_id"],
      onDelete = ForeignKey.CASCADE // Auto-delete responses if question deleted
    )
  ]
)
data class InterviewResponse(
  @PrimaryKey
  val responseId: String,
  
  @ColumnInfo(name = "question_id")
  val questionId: String,
  
  @ColumnInfo(name = "user_response")
  val userResponse: String,
  
  @ColumnInfo(name = "audio_url")
  val audioUrl: String?,
  
  @ColumnInfo(name = "timestamp")
  val timestamp: Long
)

// ✅ Foreign key ensures referential integrity
// ✅ CASCADE delete propagates
```

---

## DAO (Data Access Objects) Queries

**Pattern: Type-safe queries**

```kotlin
@Dao
interface TATQuestionDAO {
  
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAll(questions: List<TATQuestion>)
  
  @Update
  suspend fun update(question: TATQuestion)
  
  @Delete
  suspend fun delete(question: TATQuestion)
  
  // Query all
  @Query("SELECT * FROM tat_questions ORDER BY id")
  suspend fun getAllQuestions(): List<TATQuestion>
  
  // Query by ID
  @Query("SELECT * FROM tat_questions WHERE id = :id")
  suspend fun getQuestionById(id: String): TATQuestion?
  
  // Filtered query with limit
  @Query("""
    SELECT * FROM tat_questions 
    WHERE difficulty = :difficulty 
    ORDER BY created_at DESC 
    LIMIT :limit
  """)
  suspend fun getQuestionsByDifficulty(
    difficulty: String,
    limit: Int = 20
  ): List<TATQuestion>
  
  // Observe changes (Flow)
  @Query("SELECT * FROM tat_questions ORDER BY id")
  fun observeAllQuestions(): Flow<List<TATQuestion>>
  
  // Count
  @Query("SELECT COUNT(*) FROM tat_questions")
  suspend fun count(): Int
  
  // Delete old records (cache expiration)
  @Query("DELETE FROM tat_questions WHERE last_updated < :cutoffTime")
  suspend fun deleteOlderThan(cutoffTime: Long)
}

// ✅ Suspend functions for non-blocking calls
// ✅ Flow for real-time observation
// ✅ OnConflictStrategy.REPLACE for upserts
// ✅ LIMIT for pagination
```

**Complex Queries (Multi-table):**
```kotlin
@Dao
interface InterviewResponseDAO {
  
  @Query("""
    SELECT r.* FROM interview_responses r
    INNER JOIN interview_questions q ON r.question_id = q.id
    WHERE q.interview_id = :interviewId
    ORDER BY r.timestamp DESC
  """)
  suspend fun getResponsesForInterview(interviewId: String): List<InterviewResponse>
  
  @Query("""
    SELECT r.*, q.difficulty, q.category
    FROM interview_responses r
    LEFT JOIN interview_questions q ON r.question_id = q.id
    WHERE r.user_id = :userId
    ORDER BY r.timestamp DESC
    LIMIT :limit OFFSET :offset
  """)
  suspend fun getUserResponsesPaginated(
    userId: String,
    limit: Int = 20,
    offset: Int = 0
  ): List<ResponseWithMetadata>
}

data class ResponseWithMetadata(
  val response: InterviewResponse,
  val difficulty: String?,
  val category: String?
)

// ✅ JOIN for related data
// ✅ LEFT JOIN for optional relations
// ✅ LIMIT OFFSET for pagination
```

---

## Migrations (Schema Changes)

**Pattern: Safe schema versioning**

```kotlin
// When schema changes: version 1 → version 2
// Create migration to transform old schema to new

val MIGRATION_1_2 = object : Migration(1, 2) {
  override fun migrate(database: SupportSQLiteDatabase) {
    // Step 1: Create new table with updated schema
    database.execSQL("""
      CREATE TABLE tat_questions_new (
        id TEXT PRIMARY KEY NOT NULL,
        question_text TEXT NOT NULL,
        image_url TEXT,
        time_limit INTEGER NOT NULL,
        difficulty TEXT NOT NULL,
        category TEXT,
        created_at INTEGER NOT NULL,
        last_updated INTEGER NOT NULL
      )
    """)
    
    // Step 2: Copy data from old table
    database.execSQL("""
      INSERT INTO tat_questions_new (id, question_text, image_url, time_limit, difficulty, created_at, last_updated)
      SELECT id, question_text, image_url, time_limit, difficulty, created_at, last_updated
      FROM tat_questions
    """)
    
    // Step 3: Drop old table
    database.execSQL("DROP TABLE tat_questions")
    
    // Step 4: Rename new table
    database.execSQL("ALTER TABLE tat_questions_new RENAME TO tat_questions")
  }
}

// In database builder:
Room.databaseBuilder(context, SSBMaxDatabase::class.java, "ssbmax.db")
  .addMigrations(MIGRATION_1_2)
  .build()

// ✅ Never alter table directly
// ✅ Three-step process: CREATE → COPY → RENAME
// ✅ Backward-compatible (old data preserved)
// ✅ Test migrations before release
```

**Migration Testing:**
```bash
./gradlew :core:data:testDebugUnitTest -k "MigrationTest"
```

**Test Template:**
```kotlin
class DatabaseMigrationTest {
  private val testDB = MigrationTestHelper(
    InstrumentationRegistry.getInstrumentation(),
    SSBMaxDatabase::class.java.canonicalName,
    FrameworkSQLiteOpenHelperFactory()
  )
  
  @Test
  fun migrate1To2() {
    // Create v1 database
    val db = testDB.createDatabase(DB_NAME, 1)
    db.execSQL("INSERT INTO tat_questions VALUES ('q1', 'Question?', null, 600, 'hard', 100, 100)")
    db.close()
    
    // Migrate to v2
    val migratedDB = testDB.runMigrationsAndValidate(DB_NAME, 2, true, MIGRATION_1_2)
    
    // Verify data preserved
    val cursor = migratedDB.query("SELECT * FROM tat_questions WHERE id = 'q1'")
    cursor.moveToFirst()
    assertThat(cursor.getString(0)).isEqualTo("q1") // id
    cursor.close()
  }
}
```

---

## Caching Strategy

**Pattern: Cache with TTL (Time To Live)**

```kotlin
class TATRepository @Inject constructor(
  private val firestore: FirebaseFirestore,
  private val tatDao: TATQuestionDAO,
  private val errorLogger: ErrorLogger
) : TATRepository {
  
  companion object {
    const val CACHE_DURATION_MILLIS = 7 * 24 * 60 * 60 * 1000L // 7 days
  }
  
  override suspend fun getTATQuestions(): Result<List<TATQuestion>> {
    return try {
      // 1. Check local cache
      val cachedQuestions = tatDao.getAllQuestions()
      if (cachedQuestions.isNotEmpty()) {
        val oldestTimestamp = cachedQuestions.minOf { it.lastUpdated }
        val now = System.currentTimeMillis()
        if (now - oldestTimestamp < CACHE_DURATION_MILLIS) {
          return Result.Success(cachedQuestions) // Cache valid
        }
      }
      
      // 2. Fetch from Firestore
      val remote = firestore.collection("tat_questions")
        .get()
        .await()
        .toObjects(TATQuestion::class.java)
      
      // 3. Update local cache
      tatDao.deleteAll() // Clear old cache
      tatDao.insertAll(remote.map { it.copy(lastUpdated = System.currentTimeMillis()) })
      
      // 4. Clean old records (>14 days)
      val cutoff = System.currentTimeMillis() - (14 * 24 * 60 * 60 * 1000L)
      tatDao.deleteOlderThan(cutoff)
      
      Result.Success(remote)
    } catch (e: Exception) {
      errorLogger.log(e, "Failed to get TAT questions")
      Result.Failure(e)
    }
  }
}

// ✅ Check cache first (fast)
// ✅ Fallback to remote (fresh data)
// ✅ Auto-update cache (transparent to caller)
// ✅ Evict old records (manage storage)
```

---

## Testing with InMemoryDb

**Unit Tests (No Android context):**
```bash
./gradlew :core:data:testDebugUnitTest -k "DAOTest"
```

**Test Template:**
```kotlin
class TATQuestionDAOTest {
  private lateinit var db: SSBMaxDatabase
  private lateinit var dao: TATQuestionDAO
  
  @Before
  fun setup() {
    db = Room.inMemoryDatabaseBuilder(
      ApplicationProvider.getApplicationContext(),
      SSBMaxDatabase::class.java
    ).build()
    dao = db.tatQuestionDao()
  }
  
  @After
  fun teardown() {
    db.close()
  }
  
  @Test
  fun insertAndRetrieveQuestion() = runTest {
    val question = TATQuestion(
      id = "q1",
      questionText = "Tell a story?",
      imageUrl = null,
      timeLimit = 600,
      difficulty = "medium",
      category = null,
      createdAt = System.currentTimeMillis(),
      lastUpdated = System.currentTimeMillis()
    )
    
    dao.insertAll(listOf(question))
    val retrieved = dao.getQuestionById("q1")
    
    assertThat(retrieved).isNotNull()
    assertThat(retrieved!!.questionText).isEqualTo("Tell a story?")
  }
  
  @Test
  fun queryByDifficultyFilters() = runTest {
    dao.insertAll(listOf(
      TATQuestion("q1", "Q1", null, 600, "easy", null, 1, 1),
      TATQuestion("q2", "Q2", null, 600, "hard", null, 2, 2),
      TATQuestion("q3", "Q3", null, 600, "hard", null, 3, 3)
    ))
    
    val hard = dao.getQuestionsByDifficulty("hard", limit = 10)
    assertThat(hard).hasSize(2)
  }
}
```

---

## Best Practices

1. **Use migrations for schema changes** — never auto-migrate in production
2. **Test migrations** — ensure data integrity
3. **Index frequently-queried columns** — improves query performance
4. **Use Flows for observation** — reactive updates
5. **Implement caching** — reduce network calls
6. **Clean old data** — manage storage (e.g., evict >14 days)
7. **Foreign keys** — maintain referential integrity

---

## References

- **Parent:** [core/data/CLAUDE.md](../CLAUDE.md) (error handling)
- **Room Docs:** https://developer.android.com/training/data-storage/room
- **Testing:** [docs/testing/](../../docs/testing/)

---

**Last Updated:** June 2026 | **Maintainer:** Sunil Pawar
