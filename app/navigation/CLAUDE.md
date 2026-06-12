# app/navigation/CLAUDE.md — Type-Safe Routing

**Scope:** Navigation destinations, route parameters, nested graphs, navigation events. Inherits [app/CLAUDE.md](../CLAUDE.md). This file adds routing-specific patterns.

**Key Addition:** SSBMaxDestinations sealed class pattern + NavGraph organization + event-based navigation.

---

## Navigation SSOT: SSBMaxDestinations

**Single Source of Truth for ALL Routes:**
```kotlin
// In app/src/main/kotlin/com/ssbmax/navigation/SSBMaxDestinations.kt

sealed class SSBMaxDestinations {
  // Home & Authentication
  data object Home : SSBMaxDestinations()
  data object AuthenticationFlow : SSBMaxDestinations()
  data object Login : SSBMaxDestinations()
  data object SignUp : SSBMaxDestinations()
  
  // Test Selection & Details
  data object TestSelection : SSBMaxDestinations()
  data class TestDetail(val testId: String) : SSBMaxDestinations()
  data class TATTest(val testId: String) : SSBMaxDestinations()
  data class WATTest(val testId: String) : SSBMaxDestinations()
  data class InterviewTest(val interviewId: String) : SSBMaxDestinations()
  
  // Results & Review
  data class TestResults(val testId: String) : SSBMaxDestinations()
  data class ResponseReview(val responseId: String) : SSBMaxDestinations()
  
  // Settings
  data object SettingsScreen : SSBMaxDestinations()
  data object ProfileSettings : SSBMaxDestinations()
  
  // Errors
  data object Error : SSBMaxDestinations()
}

// ✅ ONLY place routes are defined
// ✅ Type-safe (compiler checks route names)
// ✅ Easy to add routes (single file edit)
// ✅ Easy to refactor (change all references in one place)

// ❌ NEVER define routes as strings: "home", "tests", etc.
// ❌ NEVER hardcode route names in screens
```

**Helper Extension (create route from destination):**
```kotlin
fun SSBMaxDestinations.toRoute(): String = when (this) {
  is SSBMaxDestinations.Home -> "home"
  is SSBMaxDestinations.TestDetail -> "test/${this.testId}"
  is SSBMaxDestinations.TATTest -> "tat/${this.testId}"
  // ...
}

// Or use built-in:
navController.navigate(SSBMaxDestinations.TATTest(testId = "tat_001"))
```

---

## NavHost Setup

**Application-Level Navigation:**
```kotlin
@Composable
fun SSBMaxApp() {
  val navController = rememberNavController()
  
  // Determine start destination based on auth state
  val startDestination = if (isUserLoggedIn()) {
    SSBMaxDestinations.Home
  } else {
    SSBMaxDestinations.AuthenticationFlow
  }
  
  NavHost(
    navController = navController,
    startDestination = startDestination
  ) {
    // Include all navigation graphs (see next section)
    authNavGraph(navController)
    testNavGraph(navController)
    settingsNavGraph(navController)
  }
}

// ✅ NavHost is root of navigation
// ✅ startDestination from auth state
```

---

## Nested Navigation Graphs (Feature-Level)

**Pattern: Organize routes by feature**

```kotlin
// In app/src/main/kotlin/com/ssbmax/navigation/graphs/AuthNavGraph.kt

fun NavGraphBuilder.authNavGraph(navController: NavController) {
  navigation(
    startDestination = SSBMaxDestinations.Login,
    route = "auth_graph"
  ) {
    composable<SSBMaxDestinations.Login> {
      LoginScreen(
        onNavigateToSignUp = {
          navController.navigate(SSBMaxDestinations.SignUp)
        },
        onNavigateToHome = {
          navController.navigate(SSBMaxDestinations.Home) {
            popUpTo(SSBMaxDestinations.Login) { inclusive = true }
          }
        }
      )
    }
    
    composable<SSBMaxDestinations.SignUp> {
      SignUpScreen(
        onBack = { navController.popBackStack() },
        onNavigateToHome = {
          navController.navigate(SSBMaxDestinations.Home) {
            popUpTo(SSBMaxDestinations.Login) { inclusive = true }
          }
        }
      )
    }
  }
}

// In app/src/main/kotlin/com/ssbmax/navigation/graphs/TestNavGraph.kt

fun NavGraphBuilder.testNavGraph(navController: NavController) {
  navigation(
    startDestination = SSBMaxDestinations.TestSelection,
    route = "test_graph"
  ) {
    composable<SSBMaxDestinations.TestSelection> {
      TestSelectionScreen(
        onSelectTest = { testId ->
          navController.navigate(SSBMaxDestinations.TestDetail(testId))
        }
      )
    }
    
    composable<SSBMaxDestinations.TestDetail> { backStackEntry ->
      val testId = backStackEntry.arguments?.getString("testId") ?: return@composable
      TestDetailScreen(
        testId = testId,
        onNavigateToTest = { destination ->
          navController.navigate(destination)
        },
        onBack = { navController.popBackStack() }
      )
    }
    
    composable<SSBMaxDestinations.TATTest> { backStackEntry ->
      val tatTestId = backStackEntry.arguments?.getString("testId") ?: return@composable
      TATTestScreen(
        testId = tatTestId,
        onNavigateToResults = { resultId ->
          navController.navigate(SSBMaxDestinations.TestResults(resultId)) {
            popUpTo(SSBMaxDestinations.TestSelection)
          }
        }
      )
    }
  }
}

// ✅ Grouped by feature (auth, test, settings)
// ✅ Nested structure mirrors app architecture
// ✅ Each graph has its own startDestination
```

---

## Route Parameters (Type-Safe)

**Pattern: Pass data via destination parameters**

```kotlin
// Define destination with parameter
data class TestResults(val testId: String) : SSBMaxDestinations()

// In NavHost:
composable<SSBMaxDestinations.TestResults> { backStackEntry ->
  val testId = backStackEntry.arguments?.getString("testId") ?: return@composable
  
  TestResultsScreen(testId = testId)
}

// Navigate with parameter:
navController.navigate(SSBMaxDestinations.TestResults(testId = "tat_001"))

// ✅ Type-safe (compiler checks testId is String)
// ✅ No manual string conversion
// ✅ Clear what data is passed
```

**Advanced: Multiple Parameters**
```kotlin
data class ResponseReview(
  val responseId: String,
  val userId: String,
  val testType: String
) : SSBMaxDestinations()

// Navigate:
navController.navigate(
  SSBMaxDestinations.ResponseReview(
    responseId = "resp_123",
    userId = "user_456",
    testType = "TAT"
  )
)

// ✅ All data passed as destination parameters
// ✅ Avoids manual Bundle creation
```

---

## Navigation Events (Channel-Based)

**Pattern: For one-off navigation (after data operations)**

```kotlin
// In ViewModel:
private val _navigationEvents = Channel<NavigationEvent>()
val navigationEvents: Flow<NavigationEvent> = _navigationEvents.receiveAsFlow()

sealed class NavigationEvent {
  data class NavigateToResults(val testId: String) : NavigationEvent()
  data object NavigateBack : NavigationEvent()
  data class NavigateToError(val message: String) : NavigationEvent()
}

fun submitTest() {
  viewModelScope.launch {
    val result = submitUseCase.execute(testData)
    
    when (result) {
      is Result.Success -> {
        _navigationEvents.send(
          NavigationEvent.NavigateToResults(result.data.testId)
        )
      }
      is Result.Failure -> {
        _navigationEvents.send(
          NavigationEvent.NavigateToError(result.exception.message ?: "Failed")
        )
      }
    }
  }
}

// In Screen:
@Composable
fun TATScreen(viewModel: TATViewModel = hiltViewModel()) {
  LaunchedEffect(Unit) {
    viewModel.navigationEvents.collect { event ->
      when (event) {
        is NavigationEvent.NavigateToResults -> {
          navController.navigate(
            SSBMaxDestinations.TestResults(event.testId)
          )
        }
        is NavigationEvent.NavigateBack -> {
          navController.popBackStack()
        }
        is NavigationEvent.NavigateToError -> {
          showErrorDialog(event.message)
        }
      }
    }
  }
  
  val uiState by viewModel.uiState.collectAsState()
  when (uiState) {
    // Render UI based on state, not navigation events
  }
}

// ✅ Events for one-off navigation (post-submission)
// ✅ State for continuous rendering
// ✅ Events fire exactly once (not replayed)
```

---

## Navigation Stack Management

**Pattern: Control backstack**

```kotlin
// Navigate without adding to backstack (skip intermediate screens)
navController.navigate(SSBMaxDestinations.TestResults(testId)) {
  popUpTo(SSBMaxDestinations.TestSelection) // Pop up to this screen
  launchSingleTop = true // Don't create duplicate if already on back stack
}

// Clear all previous navigation on login success
navController.navigate(SSBMaxDestinations.Home) {
  popUpTo(SSBMaxDestinations.AuthenticationFlow) { inclusive = true } // Clear auth flow
  launchSingleTop = true
}

// Go back
navController.popBackStack()

// Go back to specific destination
navController.popBackStack(SSBMaxDestinations.TestSelection, inclusive = false)

// ✅ popUpTo: removes screens from backstack
// ✅ inclusive = true: also remove the target
// ✅ launchSingleTop: prevents duplicates
```

---

## Shared Navigation Patterns

**Pattern: Multiple features share same screen**

```kotlin
// Some screens are accessed from multiple flows
// Example: Profile screen (from Settings + from Test Results)

// In navigation:
composable<SSBMaxDestinations.ProfileSettings> {
  ProfileSettingsScreen()
}

// Navigate from Settings:
navController.navigate(SSBMaxDestinations.ProfileSettings)

// Navigate from Results:
navController.navigate(SSBMaxDestinations.ProfileSettings)

// Same screen, different navigation paths
// ✅ Single implementation, shared across features
```

---

## Deep Linking (App URLs)

**Pattern: Open app from external links**

```kotlin
// In NavHost:
composable<SSBMaxDestinations.TestResults> { backStackEntry ->
  val testId = backStackEntry.arguments?.getString("testId") ?: return@composable
  TestResultsScreen(testId)
}

// In AndroidManifest.xml:
<activity android:name=".MainActivity">
  <intent-filter>
    <action android:name="android.intent.action.VIEW" />
    <category android:name="android.intent.category.DEFAULT" />
    <category android:name="android.intent.category.BROWSABLE" />
    <data
      android:scheme="ssbmax"
      android:host="test"
      android:pathPrefix="/results" />
  </intent-filter>
</activity>

// External link: ssbmax://test/results?testId=tat_001
// Opens TestResultsScreen with testId = "tat_001"
```

---

## Testing Navigation

**Test Navigation Flow:**
```bash
./gradlew :app:connectedAndroidTest -k "NavigationTest"
```

**Example Test:**
```kotlin
@RunWith(AndroidJUnit4::class)
class NavigationTest {
  @get:Rule
  val composeTestRule = createComposeRule()
  
  @Test
  fun navigateFromLoginToHome() {
    composeTestRule.setContent {
      val navController = rememberNavController()
      SSBMaxApp(navController)
    }
    
    // Verify on login screen
    composeTestRule.onNodeWithText("Login").assertIsDisplayed()
    
    // Fill in credentials
    composeTestRule.onNodeWithTag("email_input").performTextInput("user@example.com")
    composeTestRule.onNodeWithTag("password_input").performTextInput("password123")
    
    // Click login
    composeTestRule.onNodeWithText("Login Button").performClick()
    
    // Verify navigated to home
    composeTestRule.onNodeWithText("Welcome").assertIsDisplayed()
  }
}
```

---

## Best Practices

1. **Use SSBMaxDestinations for all routes** — SSOT principle
2. **Type-safe parameters** — compiler checks route names
3. **Keep NavHost at root** — single navigation authority
4. **Use nested graphs** — organize by feature
5. **Separate state from events** — StateFlow for rendering, Channel for navigation
6. **Manage backstack carefully** — popUpTo for clear navigation
7. **Test navigation flows** — verify all paths work

---

## References

- **Parent:** [app/CLAUDE.md](../CLAUDE.md) (ViewModel + navigation events)
- **Navigation events:** [app/ui/CLAUDE.md](../ui/CLAUDE.md) (feature screens)
- **Compose Navigation:** https://developer.android.com/jetpack/compose/navigation

---

**Last Updated:** June 2026 | **Maintainer:** Sunil Pawar
