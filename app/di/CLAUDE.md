# app/di/CLAUDE.md — Dependency Injection Setup

**Scope:** Hilt modules, @Inject constructors, scopes (Singleton, Activity, Fragment). Inherits [app/CLAUDE.md](../CLAUDE.md). This file adds DI-specific patterns for the app module.

**Key Addition:** Hilt setup + common scopes + mocking patterns for testing.

---

## Hilt Module Structure (SSBMaxApp Setup)

**Application Setup:**
```kotlin
// In app/src/main/kotlin/com/ssbmax/SSBMaxApp.kt
@HiltAndroidApp
class SSBMaxApp : Application() {
  override fun onCreate() {
    super.onCreate()
    // Hilt auto-initializes all modules
  }
}

// In AndroidManifest.xml
<application android:name=".SSBMaxApp" ... />
```

**Global Modules (Application-scoped):**
```kotlin
// In app/src/main/kotlin/com/ssbmax/di/AppModule.kt
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
  
  @Provides
  @Singleton
  fun provideErrorLogger(context: Context): ErrorLogger {
    return ErrorLogger(context)
  }
  
  @Provides
  @Singleton
  fun provideSubscriptionManager(
    firestore: FirebaseFirestore
  ): SubscriptionManager {
    return SubscriptionManager(firestore)
  }
}

// ✅ Singleton scope (one instance app-wide)
// ✅ Auto-injected into ViewModels, Repositories
// ✅ Thread-safe (Hilt handles)
```

**Repository Module (provide interfaces, not implementations):**
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
  
  @Provides
  @Singleton
  fun provideTATRepository(
    firestore: FirebaseFirestore,
    tatDao: TATDAO
  ): TATRepository {
    return TATRepositoryImpl(firestore, tatDao)
  }
  
  @Provides
  @Singleton
  fun provideOIRRepository(
    firestore: FirebaseFirestore,
    oirDao: OIRDAO
  ): OIRRepository {
    return OIRRepositoryImpl(firestore, oirDao)
  }
}

// ✅ Provides interface (TATRepository), not implementation
// ✅ ViewModels inject interface, not implementation
// ✅ Easy to swap implementations for testing
```

**Use Case Module:**
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {
  
  @Provides
  fun provideGetTATQuestionsUseCase(
    repository: TATRepository
  ): GetTATQuestionsUseCase {
    return GetTATQuestionsUseCase(repository)
  }
  
  @Provides
  fun provideSubmitTATResponseUseCase(
    repository: TATRepository,
    errorLogger: ErrorLogger
  ): SubmitTATResponseUseCase {
    return SubmitTATResponseUseCase(repository, errorLogger)
  }
  
  @Provides
  fun provideCheckSubscriptionUseCase(
    subscriptionManager: SubscriptionManager
  ): CheckSubscriptionUseCase {
    return CheckSubscriptionUseCase(subscriptionManager)
  }
}

// ✅ Create use case instances with dependencies
// ✅ Keep use cases stateless (Singleton safe)
```

---

## @HiltViewModel Pattern

**ViewModel Injection (in Composables):**
```kotlin
@Composable
fun TATScreen(
  viewModel: TATTestViewModel = hiltViewModel()
) {
  // ✅ Hilt automatically creates ViewModel with dependencies
  val uiState by viewModel.uiState.collectAsState()
  // ...
}

// ✅ Never do: MyViewModel(useCase) — use @HiltViewModel + hiltViewModel()
// ✅ Hilt scopes ViewModel to Compose NavGraph lifetime
```

**ViewModel Definition:**
```kotlin
@HiltViewModel
class TATTestViewModel @Inject constructor(
  private val getTATQuestionsUseCase: GetTATQuestionsUseCase,
  private val submitTATResponseUseCase: SubmitTATResponseUseCase,
  private val subscriptionManager: SubscriptionManager,
  private val errorLogger: ErrorLogger
) : ViewModel() {
  // Implementation
}

// ✅ @Inject on constructor (not on properties)
// ✅ All dependencies declared as constructor parameters
// ✅ Hilt provides dependencies automatically
```

---

## Scopes Explained (Singleton, Activity, Fragment)

**Singleton (App-wide, one instance):**
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object SingletonModule {
  @Provides
  @Singleton
  fun provideErrorLogger(): ErrorLogger = ErrorLogger()
  
  // One instance for entire app lifetime
  // ✅ Use for: stateless utilities, database, network client
  // ❌ Never store mutable state here
}
```

**Activity (Activity-scoped, one per Activity):**
```kotlin
@Module
@InstallIn(ActivityComponent::class)
object ActivityModule {
  @Provides
  fun provideActivityTracker(activity: Activity): ActivityTracker {
    return ActivityTracker(activity)
  }
  
  // One instance per Activity
  // Destroyed when Activity is destroyed
  // ✅ Use for: Activity-specific services
  // ✅ Inject Activity directly if needed
}
```

**Fragment (Fragment-scoped, one per Fragment):**
```kotlin
@Module
@InstallIn(FragmentComponent::class)
object FragmentModule {
  @Provides
  fun provideFragmentAnalytics(fragment: Fragment): FragmentAnalytics {
    return FragmentAnalytics(fragment)
  }
}
```

**NavGraph (for Compose Navigation):**
```kotlin
// Modern approach: use MultiBindingMap for navigation scopes
@Module
@InstallIn(SingletonComponent::class)
object AppNavModule {
  @Provides
  @Singleton
  fun provideNavController(): NavController = NavController()
}
```

---

## Testing with Hilt (@HiltAndroidTest)

**Test Setup (Unit + Android Tests):**
```kotlin
@HiltAndroidTest
class TATTestViewModelTest {
  @get:Rule
  val hiltRule = HiltAndroidRule(this)
  
  @get:Rule
  val instantExecutorRule = InstantTaskExecutorRule()
  
  @Inject
  lateinit var tatRepository: TATRepository
  
  @Inject
  lateinit var getTATQuestionsUseCase: GetTATQuestionsUseCase
  
  private lateinit var viewModel: TATTestViewModel
  
  @Before
  fun setup() {
    hiltRule.inject()
    viewModel = TATTestViewModel(
      getTATQuestionsUseCase,
      mockk(),
      mockk(),
      mockk(relaxed = true)
    )
  }
  
  @Test
  fun testViewModelLoadsQuestions() {
    // Hilt auto-provides real TATRepository
  }
}
```

**Mocking Dependencies (TestModule Override):**
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object TestRepositoryModule {
  @Provides
  @Singleton
  fun provideMockTATRepository(): TATRepository {
    return mockk<TATRepository>().apply {
      coEvery { getTATQuestions() } returns listOf(mockk())
    }
  }
}

// Run with: ./gradlew :app:connectedAndroidTest
// Hilt uses TestRepositoryModule instead of RepositoryModule
```

**ViewModel Factory for Unit Tests (without Android context):**
```kotlin
// For pure JUnit tests (no Android)
class TATTestViewModelUnitTest {
  @Test
  fun testViewModelWithMockDependencies() {
    val mockRepository = mockk<TATRepository>()
    val mockErrorLogger = mockk<ErrorLogger>(relaxed = true)
    
    val useCase = GetTATQuestionsUseCase(mockRepository)
    val viewModel = TATTestViewModel(
      useCase,
      mockk(),
      mockk(),
      mockErrorLogger
    )
    
    // Test without Hilt
  }
}
```

---

## Multi-Binding (Advanced: when you have multiple implementations)

**Example: Multiple Repository Implementations**
```kotlin
interface TestEvaluator {
  suspend fun evaluate(response: TestResponse): Result<Score>
}

class GeminiEvaluator : TestEvaluator { /* ... */ }
class LLamaEvaluator : TestEvaluator { /* ... */ }

// Provide multiple bindings
@Module
@InstallIn(SingletonComponent::class)
interface EvaluatorModule {
  
  @Binds
  @Singleton
  fun bindGeminiEvaluator(impl: GeminiEvaluator): TestEvaluator
}

// Or for multiple:
@Module
@InstallIn(SingletonComponent::class)
object MultiEvaluatorModule {
  
  @Provides
  @Singleton
  @Named("gemini")
  fun provideGeminiEvaluator(): TestEvaluator = GeminiEvaluator()
  
  @Provides
  @Singleton
  @Named("llama")
  fun provideLLamaEvaluator(): TestEvaluator = LLamaEvaluator()
}

// Inject specific one:
class MyUseCase @Inject constructor(
  @Named("gemini") private val evaluator: TestEvaluator
)
```

---

## Common Issues & Solutions

**Issue 1: Circular Dependencies**
```kotlin
// ❌ BAD: Circular dependency
class RepositoryA(private val repositoryB: RepositoryB)
class RepositoryB(private val repositoryA: RepositoryA)

// ✅ SOLUTION: Extract common interface
interface DataSource
class RepositoryA(private val dataSource: DataSource)
class RepositoryB(private val dataSource: DataSource)
```

**Issue 2: Module Not Installed**
```
Error: Hilt dependency not found: RepositoryModule not installed

// Check:
// 1. @Module present on class/object?
// 2. @InstallIn(SingletonComponent::class) present?
// 3. Module in app/ module (not in library)?

// Solution: Ensure all @Modules have @InstallIn
```

**Issue 3: ViewModel Not Injected**
```kotlin
// ❌ BAD
@Composable
fun MyScreen() {
  val viewModel = TATTestViewModel(useCase, logger) // Manual creation
}

// ✅ GOOD
@Composable
fun MyScreen(
  viewModel: TATTestViewModel = hiltViewModel() // Hilt injection
)
```

---

## Best Practices

1. **Provide interfaces, not implementations** — enables testing/swapping
2. **Use @Singleton for stateless utilities** — shared across app
3. **Use @Inject on constructor** — clearer dependency declaration
4. **Avoid circular dependencies** — extract common abstraction
5. **Test with HiltAndroidTest** — verifies DI setup
6. **Keep modules focused** — one module per concern (Repos, UseCases, etc.)
7. **Never create ViewModels manually** — always use `hiltViewModel()`

---

## References

- **Parent:** [app/CLAUDE.md](../CLAUDE.md) (ViewModel structure)
- **Hilt Docs:** https://dagger.dev/hilt/
- **Testing:** [docs/testing/](../../docs/testing/)

---

**Last Updated:** June 2026 | **Maintainer:** Sunil Pawar
