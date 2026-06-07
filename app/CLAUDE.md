# app/CLAUDE.md — UI Layer Guidance

**Scope:** Compose screens, ViewModels, navigation, Hilt dependency injection. This file specializes the root [claude.md](../claude.md) and [core/domain/CLAUDE.md](../core/domain/CLAUDE.md) for UI layer patterns.

**Key Rule:** UI layer is presentation-only. Business logic, data fetching, and repository calls LIVE in ViewModels backed by use cases, never in Composables. See [Rule #5](../claude.md#L5) (use LLM for judgment, not routing).

---

## ViewModel Architecture (MVVM Pattern)

**Contract:**
```kotlin
@HiltViewModel
class MyFeatureViewModel @Inject constructor(
  private val myUseCase: MyUseCase,
  private val errorLogger: ErrorLogger
) : ViewModel() {
  private val _uiState = MutableStateFlow<MyUiState>(MyUiState.Loading)
  val uiState: StateFlow<MyUiState> = _uiState.asStateFlow()
  
  init { loadData() }
  
  private fun loadData() {
    viewModelScope.launch {
      _uiState.update { it.copy(isLoading = true) }
      val result = myUseCase.getData()
      _uiState.value = when (result) {
        is Result.Success -> MyUiState.Content(result.data)
        is Result.Failure -> MyUiState.Error(result.exception.message ?: "Unknown error")
      }
    }
  }
}

// ❌ NEVER expose MutableStateFlow to UI
// ❌ NEVER call Firebase, Firestore, or repository directly
// ✅ ONLY expose StateFlow<UiState> (read-only)
// ✅ ONLY use use cases (injected via Hilt)
```

**Key Patterns:**
- **Thread-safe updates:** Use `_uiState.update { it.copy(...) }` (Rule #4 enforced by lint)
- **Lifecycle awareness:** Use `viewModelScope` for coroutines (auto-cancelled on ViewModel clear)
- **Timer restoration:** Store timer start time, not Job references (survives config changes)
- **Security checks:** Authentication guard + subscription limit check BEFORE loading data
- **Never expose MutableStateFlow:** Always wrap with `.asStateFlow()` before exposing

**Anti-patterns:**
- ❌ `_uiState.value = MyUiState.Content(data)` — use `.update {}` for thread safety
- ❌ `Firebase.firestore.collection("data").get()` — use repositories + use cases
- ❌ `public val job: Job` — no singleton mutable state (Rule #5)
- ❌ Creating ViewModel with `MyViewModel()` — use `@HiltViewModel` + DI

**Testing Strategy:**
```bash
# Test ViewModel state transitions
./gradlew :app:testDebugUnitTest -k "MyFeatureViewModelTest"
```

---

## Composable Decomposition (50-Line Limit)

**Rule:** No Composable > 50 lines. Break down into sub-components.

**Template:**
```kotlin
@Composable
fun MyScreen(viewModel: MyFeatureViewModel = hiltViewModel()) {
  val uiState by viewModel.uiState.collectAsState()
  
  when (uiState) {
    is MyUiState.Loading -> LoadingContent()
    is MyUiState.Content -> ContentView((uiState as MyUiState.Content).data)
    is MyUiState.Error -> ErrorView((uiState as MyUiState.Error).message)
  }
}

@Composable
fun ContentView(data: MyData) {
  Column {
    MyCard(data.title)
    MyButton(data.description) { /* handle action */ }
  }
}

// Each extracted Composable is independently testable + previewable
// Reduces cognitive load + enables parallel UI development
```

**When to extract:**
- Reusable across multiple screens → `core/designsystem/components/`
- Feature-specific sub-component → same file or `feature/components/`
- Complex state management → consider promoting to ViewModel

**Lifecycle Pattern:**
- Screen Composable: State collection, branching, overall layout
- Feature Composable: Sub-section with local state (if simple)
- Reusable Component: No dependencies on feature logic

---

## Navigation: Type-Safe Routing

**SSOT:** [SSBMaxDestinations.kt](src/main/kotlin/com/ssbmax/navigation/SSBMaxDestinations.kt) is the ONLY place to define routes.

**Pattern:**
```kotlin
sealed class SSBMaxDestinations {
  data object Home : SSBMaxDestinations()
  data class TestDetail(val testId: String) : SSBMaxDestinations()
  data object Settings : SSBMaxDestinations()
}

// Navigation graph uses destinations
NavHost(navController, SSBMaxDestinations.Home) {
  composable<SSBMaxDestinations.Home> {
    HomeScreen()
  }
  composable<SSBMaxDestinations.TestDetail> { backStackEntry ->
    val testId = backStackEntry.arguments?.getString("testId") ?: return@composable
    TestDetailScreen(testId)
  }
}

// Navigate: navController.navigate(SSBMaxDestinations.TestDetail("tat-1"))
// ❌ NEVER use string routes: navController.navigate("test/tat-1")
```

**Events via Channel (for one-off navigation):**
```kotlin
class MyFeatureViewModel(private val errorLogger: ErrorLogger) : ViewModel() {
  private val _navigationEvents = Channel<TestNavigationEvent>()
  val navigationEvents: Flow<TestNavigationEvent> = _navigationEvents.receiveAsFlow()
  
  fun submitTest() {
    // After success: signal navigation event
    viewModelScope.launch {
      _navigationEvents.send(TestNavigationEvent.NavigateToResults(testId = "123"))
    }
  }
}

// In Screen: LaunchedEffect to observe navigation events
LaunchedEffect(Unit) {
  viewModel.navigationEvents.collect { event ->
    when (event) {
      is TestNavigationEvent.NavigateToResults -> navController.navigate(
        SSBMaxDestinations.ResultsDetail(event.testId)
      )
    }
  }
}

// ✅ Separates state (StateFlow) from events (Channel)
// ✅ Events fire exactly once, not replayed on config change
```

---

## Configuration Changes & Memory Leak Prevention

**Problem:** Timers, coroutines leak when config changes (device rotation).

**Solution Pattern (from [PPDTTestViewModel](src/main/kotlin/com/ssbmax/ui/tests/ppdt/PPDTTestViewModel.kt)):**
```kotlin
@HiltViewModel
class PPDTTestViewModel(
  private val checkSubscriptionUseCase: CheckSubscriptionUseCase,
  private val submitTestUseCase: SubmitTestUseCase
) : ViewModel() {
  private var timerStartTime: Long? = null  // ✅ Restore from savedState
  
  fun startTimer() {
    if (timerStartTime == null) {
      timerStartTime = System.currentTimeMillis()
    }
    launchTimerCoroutine()
  }
  
  private fun launchTimerCoroutine() {
    viewModelScope.launch {
      while (isActive) {
        val elapsed = System.currentTimeMillis() - (timerStartTime ?: 0)
        _uiState.update { it.copy(timeRemaining = 600_000 - elapsed) }
        delay(1000)
      }
    }
  }
  
  // ✅ viewModelScope auto-cancels on ViewModel clear (no manual cleanup needed)
  // ✅ timerStartTime survives config change (stored as primitive in savedState)
}
```

**Memory Leak Prevention Checklist:**
- ✅ All async operations use `viewModelScope` (auto-cancelled)
- ✅ No Job/Listener references stored as public properties
- ✅ Timers restart from saved start time (not resumed from Job)
- ✅ Firestore listeners removed in `onCleared()` if needed
- ✅ No bitmap references or context leaks

**Testing Memory Leaks:**
```bash
# Run with memory leak detection (if integrated)
./gradlew :app:testDebugUnitTest --continue
# Monitor for LiveData/StateFlow not cleared in Mockito verify
```

---

## Security Checks in ViewModels

**Before loading test data:**
```kotlin
@HiltViewModel
class TATTestViewModel @Inject constructor(
  private val checkAuthUseCase: CheckAuthUseCase,
  private val checkSubscriptionUseCase: CheckSubscriptionUseCase,
  private val getTATQuestionsUseCase: GetTATQuestionsUseCase
) : ViewModel() {
  
  fun loadTest() {
    viewModelScope.launch {
      // 1. Check authentication (from security annotation in domain layer)
      val authResult = checkAuthUseCase.execute()
      if (authResult is Result.Failure) {
        _uiState.update { it.copy(error = "Not authenticated") }
        return@launch
      }
      
      // 2. Check subscription eligibility
      val subResult = checkSubscriptionUseCase.execute(TestType.TAT)
      if (subResult is Result.Failure) {
        _uiState.update { it.copy(limitReached = true) }
        return@launch
      }
      
      // 3. Load data only after all checks pass
      val questionsResult = getTATQuestionsUseCase.execute()
      _uiState.value = when (questionsResult) {
        is Result.Success -> MyUiState.Content(questionsResult.data)
        is Result.Failure -> MyUiState.Error(questionsResult.exception.message ?: "Failed")
      }
    }
  }
}
```

**Referenced:** See [core/domain/CLAUDE.md](../core/domain/CLAUDE.md) for security annotation patterns.

---

## Testing Strategies for UI Layer

**1. ViewModel Unit Tests (JUnit 4 + MockK + Turbine):**
```bash
./gradlew :app:testDebugUnitTest -k "MyFeatureViewModelTest"
```

**2. Composable Previews (for visual validation):**
```kotlin
@Preview(showBackground = true)
@Composable
fun MyScreenPreview() {
  SSBMaxTheme {
    MyScreen() // Preview with default/mock ViewModel
  }
}
```

**3. UI Integration Tests (Compose test framework):**
```bash
./gradlew :app:connectedDebugAndroidTest -k "MyScreenTest"
```

---

## Hilt Dependency Injection (UI Layer)

**For ViewModels:**
```kotlin
@HiltViewModel
class MyViewModel @Inject constructor(
  private val useCase: MyUseCase,
  private val errorLogger: ErrorLogger
) : ViewModel()
```

**In Composables:**
```kotlin
@Composable
fun MyScreen(viewModel: MyViewModel = hiltViewModel()) {
  // ✅ Hilt auto-creates/provides ViewModel
}
```

**Anti-patterns:**
- ❌ `MyViewModel()` — use `@HiltViewModel` + `hiltViewModel()`
- ❌ Manual dependency passing — use Hilt @Inject
- ❌ Singleton viewModels — each screen gets its own instance

See [app/di/CLAUDE.md](di/CLAUDE.md) for DI patterns.

---

## References

- **Root guidance:** [claude.md](../claude.md) (12 core rules, quality limits)
- **Domain layer:** [core/domain/CLAUDE.md](../core/domain/CLAUDE.md) (use cases, models)
- **Data layer:** [core/data/CLAUDE.md](../core/data/CLAUDE.md) (repositories, error handling)
- **Navigation:** [app/navigation/CLAUDE.md](navigation/CLAUDE.md)
- **DI patterns:** [app/di/CLAUDE.md](di/CLAUDE.md)
- **Feature screens:** [app/ui/CLAUDE.md](ui/CLAUDE.md)

---

**Last Updated:** June 2026 | **Maintainer:** Sunil Pawar
