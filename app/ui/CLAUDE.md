# app/ui/CLAUDE.md — Feature Screens & State Management

**Scope:** Feature-specific screens (TAT, WAT, Interview, etc.). Inherits [app/CLAUDE.md](../CLAUDE.md) + [claude.md](../../claude.md). This file adds patterns for building individual feature screens.

**Key Addition:** Screen template + feature state management + testing patterns specific to exam screens.

---

## Screen Structure Template

**Anatomy of a Feature Screen:**
```kotlin
@Composable
fun TATTestScreen(
  testId: String,
  viewModel: TATTestViewModel = hiltViewModel()
) {
  val uiState by viewModel.uiState.collectAsState()
  
  // 1. Observe navigation events
  LaunchedEffect(Unit) {
    viewModel.navigationEvents.collect { event ->
      when (event) {
        is TATNavigationEvent.NavigateToResults -> 
          navController.navigate(SSBMaxDestinations.ResultsDetail(event.testId))
      }
    }
  }
  
  // 2. Handle screen state
  when (val state = uiState) {
    is TATUiState.Loading -> TATLoadingContent()
    is TATUiState.Content -> TATContent(state.questions, viewModel::submitAnswer)
    is TATUiState.TimerExpired -> TATSubmissionContent(state.answers, viewModel::submit)
    is TATUiState.Error -> ErrorContent(state.message)
    is TATUiState.LimitReached -> UpgradeContent()
  }
}

// Sub-components
@Composable
fun TATContent(questions: List<TATQuestion>, onAnswer: (String, String) -> Unit) {
  // Individual question UI
}

// ✅ Screen observes state, sub-components render UI only
// ✅ Navigation via LaunchedEffect + events
// ✅ Each sub-component < 50 lines
```

---

## Feature State Definition (UiState Sealed Class)

**Pattern:**
```kotlin
sealed class TATUiState {
  data object Loading : TATUiState()
  
  data class Content(
    val questions: List<TATQuestion>,
    val currentIndex: Int = 0,
    val selectedAnswers: Map<String, String> = emptyMap(),
    val timeRemaining: Int = 600 // 10 minutes in seconds
  ) : TATUiState()
  
  data class TimerExpired(
    val answers: Map<String, String>
  ) : TATUiState()
  
  data class Error(
    val message: String
  ) : TATUiState()
  
  data object LimitReached : TATUiState()
  
  data object Submitted : TATUiState()
}

// ✅ Each state is a complete snapshot
// ✅ No null values (use sealed class variants)
// ✅ All data needed to render screen included
```

**Update Pattern (thread-safe):**
```kotlin
// In ViewModel
fun selectAnswer(questionId: String, optionId: String) {
  _uiState.update { state ->
    if (state !is TATUiState.Content) return@update state
    
    state.copy(
      selectedAnswers = state.selectedAnswers + (questionId to optionId)
    )
  }
}

// ✅ Always use `.update { }` for thread safety
// ✅ Copy immutable data, don't mutate directly
```

---

## Feature ViewModel Pattern (Test Submission Flow)

**Contract:**
```kotlin
@HiltViewModel
class TATTestViewModel @Inject constructor(
  private val checkSubscriptionUseCase: CheckSubscriptionUseCase,
  private val getTATQuestionsUseCase: GetTATQuestionsUseCase,
  private val submitTATResponseUseCase: SubmitTATResponseUseCase,
  private val errorLogger: ErrorLogger
) : ViewModel() {
  
  private val _uiState = MutableStateFlow<TATUiState>(TATUiState.Loading)
  val uiState: StateFlow<TATUiState> = _uiState.asStateFlow()
  
  private val _navigationEvents = Channel<TATNavigationEvent>()
  val navigationEvents: Flow<TATNavigationEvent> = _navigationEvents.receiveAsFlow()
  
  init { loadTest() }
  
  private fun loadTest() {
    viewModelScope.launch {
      _uiState.value = TATUiState.Loading
      
      // 1. Check subscription
      val subResult = checkSubscriptionUseCase.execute(SSBPhase.TAT)
      if (subResult is Result.Failure) {
        _uiState.value = TATUiState.LimitReached
        return@launch
      }
      
      // 2. Get questions
      val questionsResult = getTATQuestionsUseCase.execute()
      _uiState.value = when (questionsResult) {
        is Result.Success -> TATUiState.Content(questionsResult.data)
        is Result.Failure -> {
          errorLogger.log(questionsResult.exception, "Failed to load TAT questions")
          TATUiState.Error("Failed to load questions")
        }
      }
    }
  }
  
  fun selectAnswer(questionId: String, optionId: String) {
    _uiState.update { state ->
      if (state !is TATUiState.Content) return@update state
      state.copy(selectedAnswers = state.selectedAnswers + (questionId to optionId))
    }
  }
  
  fun submitTest() {
    val state = (_uiState.value as? TATUiState.Content) ?: return
    
    viewModelScope.launch {
      val response = TATResponse(
        questionIds = state.questions.map { it.id },
        selectedAnswers = state.selectedAnswers,
        timeSpent = 600 - state.timeRemaining
      )
      
      val result = submitTATResponseUseCase.execute(response)
      
      when (result) {
        is Result.Success -> {
          _navigationEvents.send(
            TATNavigationEvent.NavigateToResults(result.data.testId)
          )
        }
        is Result.Failure -> {
          errorLogger.log(result.exception, "Failed to submit TAT response")
          _uiState.value = TATUiState.Error("Submission failed")
        }
      }
    }
  }
}

// Navigation events
sealed class TATNavigationEvent {
  data class NavigateToResults(val testId: String) : TATNavigationEvent()
  data object NavigateBack : TATNavigationEvent()
}
```

**Flow:**
1. **Init:** Load + check subscription
2. **Select answer:** Update state (collect user responses)
3. **Submit:** Call use case (send to backend for evaluation)
4. **Navigate:** Signal navigation event (screen handles routing)

---

## Testing Feature ViewModels (JUnit 4 + Turbine)

**Test Template:**
```kotlin
class TATTestViewModelTest {
  @get:Rule
  val instantExecutorRule = InstantTaskExecutorRule()
  
  private val mockCheckSubscription = mockk<CheckSubscriptionUseCase>()
  private val mockGetQuestions = mockk<GetTATQuestionsUseCase>()
  private val mockSubmit = mockk<SubmitTATResponseUseCase>()
  private val mockErrorLogger = mockk<ErrorLogger>(relaxed = true)
  
  private lateinit var viewModel: TATTestViewModel
  
  @Before
  fun setUp() {
    viewModel = TATTestViewModel(
      mockCheckSubscription,
      mockGetQuestions,
      mockSubmit,
      mockErrorLogger
    )
  }
  
  @Test
  fun loadTestLoadsQuestionsWhenSubscriptionValid() = runTest {
    val questions = listOf(mockk<TATQuestion>())
    coEvery { mockCheckSubscription.execute(any()) } returns Result.Success(Unit)
    coEvery { mockGetQuestions.execute() } returns Result.Success(questions)
    
    // Trigger init (already runs in init {})
    viewModel.uiState.test {
      assertThat(awaitItem()).isInstanceOf(TATUiState.Loading::class.java)
      assertThat(awaitItem()).isInstanceOf(TATUiState.Content::class.java)
      cancel()
    }
  }
  
  @Test
  fun selectAnswerUpdatesState() = runTest {
    coEvery { mockCheckSubscription.execute(any()) } returns Result.Success(Unit)
    val questions = listOf(mockk<TATQuestion>(relaxed = true).apply {
      every { id } returns "q1"
    })
    coEvery { mockGetQuestions.execute() } returns Result.Success(questions)
    
    viewModel.selectAnswer("q1", "option_a")
    
    val state = (viewModel.uiState.value as TATUiState.Content)
    assertThat(state.selectedAnswers["q1"]).isEqualTo("option_a")
  }
  
  @Test
  fun submitTestNavigates() = runTest {
    // Setup
    coEvery { mockCheckSubscription.execute(any()) } returns Result.Success(Unit)
    coEvery { mockGetQuestions.execute() } returns Result.Success(listOf(mockk()))
    coEvery { mockSubmit.execute(any()) } returns Result.Success(SubmitResult(testId = "test123"))
    
    // Act
    viewModel.selectAnswer("q1", "a")
    viewModel.submitTest()
    
    // Assert navigation event
    viewModel.navigationEvents.test {
      val event = awaitItem()
      assertThat(event).isInstanceOf(TATNavigationEvent.NavigateToResults::class.java)
      assertThat((event as TATNavigationEvent.NavigateToResults).testId).isEqualTo("test123")
      cancel()
    }
  }
}
```

**Test Utilities (Turbine for Flow testing):**
```bash
./gradlew :app:testDebugUnitTest -k "TATTestViewModelTest"
```

---

## Accessibility in Practice

**Exam Screen Specific:**
```kotlin
@Composable
fun TATQuestionCard(
  question: TATQuestion,
  index: Int,
  onSelect: (String) -> Unit
) {
  Card(modifier = Modifier.semantics {
    // Announce question number + total for screen readers
    contentDescription = "Question ${index + 1} of 5: ${question.questionText}"
  }) {
    Text(question.questionText)
    
    question.options.forEach { option ->
      Button(
        onClick = { onSelect(option.id) },
        modifier = Modifier.testTag("option_${option.id}")
      ) {
        Text(option.text, modifier = Modifier.semantics {
          contentDescription = "Option: ${option.text}"
        })
      }
    }
  }
}

// ✅ Screen readers announce: "Question 1 of 5: What is the capital of France?, Button"
// ✅ testTag enables UI tests to find options by ID
```

---

## Common Feature Patterns

**Pattern: Timer with State Recovery**
```kotlin
private var timerStartTime: Long? = null

fun startTimer() {
  if (timerStartTime == null) {
    timerStartTime = System.currentTimeMillis()
  }
  
  viewModelScope.launch {
    while (isActive) {
      val elapsed = System.currentTimeMillis() - (timerStartTime ?: 0)
      val remaining = 600_000 - elapsed // 10 minutes
      
      if (remaining <= 0) {
        _uiState.update { (it as? Content)?.copy(timeRemaining = 0) ?: it }
        submitTest()
        cancel()
      } else {
        _uiState.update { (it as? Content)?.copy(timeRemaining = remaining.toInt()) ?: it }
      }
      
      delay(1000)
    }
  }
}

// ✅ Timer survives config changes (startTime is primitive, not Job)
// ✅ Auto-submit when time expires
```

**Pattern: Disable UI During Submission**
```kotlin
data class Content(
  // ...existing fields
  val isSubmitting: Boolean = false
) : TATUiState()

fun submitTest() {
  _uiState.update { (it as? Content)?.copy(isSubmitting = true) ?: it }
  
  viewModelScope.launch {
    val result = submitTATResponseUseCase.execute(response)
    
    when (result) {
      is Result.Success -> {
        // Navigate (UI handled by screen)
      }
      is Result.Failure -> {
        _uiState.update { 
          (it as? Content)?.copy(
            isSubmitting = false,
            error = "Submission failed"
          ) ?: it 
        }
      }
    }
  }
}

// In Screen:
SubmitButton(
  enabled = !state.isSubmitting,
  onClick = viewModel::submitTest
)
```

---

## References

- **Parent:** [app/CLAUDE.md](../CLAUDE.md) (ViewModel lifecycle, Composable limits)
- **Root:** [claude.md](../../claude.md) (TDD, error handling)
- **Domain:** [core/domain/CLAUDE.md](../../core/domain/CLAUDE.md) (use cases, Result<T>)
- **Testing:** [docs/testing/](../../docs/testing/) for full test examples

---

**Last Updated:** June 2026 | **Maintainer:** Sunil Pawar
