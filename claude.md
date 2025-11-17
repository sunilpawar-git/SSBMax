# SSBMax - World-Class Android App Cursor Rules
# Specialized rules for building an exceptional SSB (Services Selection Board) preparation app

## 🎯 PROJECT OVERVIEW
You are building SSBMax, a comprehensive Android app for SSB preparation that helps candidates:
- Access structured study materials for Psychology, GTO, Interview, and Conference stages
- Practice authentic SSB tests (TAT, WAT, SRT, SDT, GTO tasks)
- Track progress and performance analytics
- Get personalized tips and guidance
- Simulate real SSB testing conditions

## 🏗️ ARCHITECTURE & DESIGN PATTERNS

### Core Architecture
- **Pattern**: MVVM (Model-View-ViewModel) with Repository pattern
- **Navigation**: Single Activity with Navigation Component
- **UI**: Material Design 3 with consistent theming
- **Data Binding**: Use ViewBinding for all layouts
- **Dependency Injection**: Use Hilt/Dagger for clean architecture

### Package Structure
```
com.example.ssbmax/
├── data/
│   ├── local/          # Room database, SharedPreferences
│   ├── remote/         # API services, DTOs
│   ├── repository/     # Data repositories
│   └── models/         # Data classes, entities
├── domain/
│   ├── usecase/        # Business logic use cases
│   └── repository/     # Repository interfaces
├── presentation/
│   ├── ui/
│   │   ├── auth/       # Login, registration
│   │   ├── dashboard/  # Home, progress tracking
│   │   ├── tests/      # All SSB test modules
│   │   ├── study/      # Study materials
│   │   ├── practice/   # Practice sessions
│   │   └── profile/    # User profile, settings
│   ├── viewmodel/      # ViewModels
│   └── adapter/        # RecyclerView adapters
├── utils/
│   ├── extensions/     # Kotlin extensions
│   ├── constants/      # App constants
│   └── helpers/        # Utility classes
└── di/                 # Dependency injection modules
```

## 🚦 NAVIGATION & STATE MANAGEMENT RULES

### **CRITICAL: ID-Based Navigation Pattern**

**ALL test screens MUST follow this pattern. NO EXCEPTIONS.**

#### ✅ CORRECT Pattern (Required)
```kotlin
@Composable
fun MyTestScreen(
    onTestComplete: (submissionId: String, subscriptionType: SubscriptionType) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: MyTestViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isCompleted) {
        if (uiState.isCompleted && uiState.submissionId != null) {
            // Pass ONLY the ID
            onTestComplete(uiState.submissionId!!, uiState.subscriptionType!!)
        }
    }
}

// Result Screen - Fetches data via ViewModel
@Composable
fun MyTestResultScreen(
    submissionId: String,
    viewModel: MyTestResultViewModel = hiltViewModel()
) {
    LaunchedEffect(submissionId) {
        viewModel.loadSubmission(submissionId)  // Fetch from Firestore
    }
}

// ViewModel fetches from Repository
@HiltViewModel
class MyTestResultViewModel @Inject constructor(
    private val repository: SubmissionRepository
) : ViewModel() {
    fun loadSubmission(submissionId: String) {
        viewModelScope.launch {
            repository.getSubmission(submissionId)
        }
    }
}
```

#### ❌ WRONG - Anti-Pattern (DO NOT USE)
```kotlin
// ANTI-PATTERN: Passing complex object
@Composable
fun MyTestScreen(
    onTestComplete: (TestResult) -> Unit  // ❌ WRONG!
) {
    onTestComplete(complexResultObject)  // Not process-death safe
}

// ANTI-PATTERN: Singleton state holder
object TestResultHolder {  // ❌ WRONG!
    var result: TestResult? = null  // Memory leak, state pollution
}
```

### **Why ID-Based Navigation?**

1. **Process-Death Safe**: Configuration changes don't lose data
2. **Deep Linking**: Can create deep links with IDs
3. **Size Limits**: Avoids 1MB Parcelable transaction limit
4. **Single Source of Truth**: Firestore/database is authority
5. **Testable**: Easy to mock repository in tests

### **Forbidden Anti-Patterns**

#### ❌ 1. Singleton State Holders
**NEVER create singleton objects with mutable state:**

```kotlin
// ❌ FORBIDDEN
object MyHolder {
    private var data: MyData? = null
    fun setData(d: MyData) { data = d }
}
```

**Why forbidden:**
- Memory leaks (singleton lives forever)
- State pollution between tests
- Not thread-safe
- Not process-death safe
- Difficult to test

**✅ Use instead:**
- ViewModel for UI state
- Repository for data management
- Pass IDs via navigation

#### ❌ 2. Complex Object Navigation
**NEVER pass complex objects via navigation:**

```kotlin
// ❌ FORBIDDEN
navController.navigate("result") {
    putExtra("result", complexObject)  // Will fail on process death
}

onComplete: (User, Settings, Data) -> Unit  // Too complex
```

**✅ Use instead:**
```kotlin
navController.navigate("result/$submissionId")

onComplete: (submissionId: String, subscriptionType: SubscriptionType) -> Unit
```

#### ❌ 3. *Holder File Naming
**NEVER create files ending in `*Holder.kt`:**

```kotlin
// ❌ FORBIDDEN FILE NAMES
UserHolder.kt
TestResultHolder.kt
DataHolder.kt
```

These names signal anti-patterns. Use:
- `*ViewModel.kt` for UI state
- `*Repository.kt` for data

### **Architecture Enforcement**

The following systems prevent anti-patterns:

1. **Custom Lint Rules** (ERROR severity):
   - `SingletonMutableStateDetector` - Catches mutable singletons
   - `NavigationComplexObjectDetector` - Catches complex object navigation
   - `ViewModelLifecycleDetector` - Ensures proper cleanup

2. **Pre-Commit Hooks**:
   - Blocks commit if *Holder.kt files added
   - Blocks commit if singleton with mutable state detected
   - Blocks commit if complex object navigation detected

3. **Architecture Tests** (`ArchitectureTest.kt`):
   - Validates all test screens use ID-based navigation
   - Ensures no singleton mutable state exists
   - Verifies all result screens have ViewModels

4. **CI/CD Pipeline**:
   - Runs lint checks (fails build on violations)
   - Runs architecture tests (fails build on violations)
   - No `continue-on-error` - violations block merges

### **Standard Test Flow Pattern**

Every test module MUST follow this exact flow:

```
User completes test
    ↓
TestScreen: submitTest()
    ↓
ViewModel: Creates OIRSubmission with result data
    ↓
Repository: Saves to Firestore (submitOIR)
    ↓
ViewModel: Updates UI state with submissionId
    ↓
TestScreen: onTestComplete(submissionId, subscriptionType)
    ↓
Navigation: TestResultHandler.handleTestSubmission()
    ↓
Navigate to: ResultScreen(submissionId)
    ↓
ResultViewModel: loadSubmission(submissionId)
    ↓
Repository: Fetches from Firestore (getSubmission)
    ↓
ResultViewModel: Parses and updates UI state
    ↓
ResultScreen: Displays result with loading/error/success states
```

### **Reference Implementation**

See these files for correct patterns:
- **Test Screen**: `app/src/main/kotlin/com/ssbmax/ui/tests/tat/TATTestScreen.kt`
- **Test ViewModel**: `app/src/main/kotlin/com/ssbmax/ui/tests/tat/TATTestViewModel.kt`
- **Result Screen**: `app/src/main/kotlin/com/ssbmax/ui/tests/tat/TATSubmissionResultScreen.kt`
- **Result ViewModel**: `app/src/main/kotlin/com/ssbmax/ui/tests/tat/TATSubmissionResultViewModel.kt`
- **Navigation**: `app/src/main/kotlin/com/ssbmax/navigation/SharedNavGraph.kt`

All 7 test modules (TAT, WAT, SRT, OIR, PPDT, SDT, PIQ) follow this pattern.

## 📱 UI/UX EXCELLENCE

### Material Design 3 Implementation
- Use Material You dynamic theming
- Implement proper elevation and shadows
- Follow 8dp grid system for spacing
- Use appropriate typography scale (headlineLarge, bodyMedium, etc.)
- Implement proper color contrast ratios (WCAG AA compliant)

### SSB-Specific UI Components
```kotlin
// Custom components for SSB tests
class TimerView : View {
    // Countdown timer for timed tests
    // Visual progress indicator
    // Audio/vibration alerts
}

class TestProgressBar : LinearProgressIndicator {
    // Shows test completion progress
    // Color-coded based on performance
}

class SSBCardView : MaterialCardView {
    // Consistent card styling across app
    // Elevation animations
    // Ripple effects
}
```

### Navigation & Flow
- Implement bottom navigation for main sections
- Use navigation drawer for secondary features
- Deep linking for specific tests/materials
- Proper back stack management
- Smooth transitions between screens

## 🧠 SSB TEST IMPLEMENTATION

### Psychology Tests Module
```kotlin
// TAT (Thematic Apperception Test)
class TATTestActivity : AppCompatActivity() {
    // Image display with zoom capabilities
    // Story input with character count
    // Timer with visual/audio cues
    // Auto-save functionality
    // Offline capability
}

// WAT (Word Association Test)
class WATTestFragment : Fragment() {
    // Rapid word display (15 seconds each)
    // Quick response input
    // Automatic progression
    // Response validation
}

// SRT (Situation Reaction Test)
class SRTTestFragment : Fragment() {
    // Scenario presentation
    // Multiple choice or text input
    // Time management per question
    // Progress tracking
}
```

### GTO Tests Module
```kotlin
class GTOTestSuite {
    // Group Discussion simulation
    // Planning exercises with visual aids
    // Command task scenarios
    // Lecturette timer and topics
    // Performance evaluation metrics
}
```

### Test Engine Core
```kotlin
data class SSBTest(
    val id: String,
    val type: TestType,
    val category: SSBCategory,
    val questions: List<TestQuestion>,
    val timeLimit: Duration,
    val instructions: String,
    val scoringCriteria: ScoringCriteria
)

class TestEngine {
    fun startTest(test: SSBTest): TestSession
    fun pauseTest(sessionId: String)
    fun resumeTest(sessionId: String)
    fun submitTest(sessionId: String): TestResult
    fun calculateScore(responses: List<TestResponse>): Score
}
```

## 📊 PROGRESS TRACKING & ANALYTICS

### Performance Metrics
```kotlin
data class UserProgress(
    val testsCompleted: Int,
    val studyHours: Float,
    val streakDays: Int,
    val categoryScores: Map<SSBCategory, Float>,
    val weakAreas: List<String>,
    val recommendations: List<String>
)

class AnalyticsRepository {
    suspend fun trackTestCompletion(testId: String, score: Float)
    suspend fun trackStudyTime(duration: Duration, category: SSBCategory)
    suspend fun generateProgressReport(): ProgressReport
    suspend fun getPersonalizedRecommendations(): List<Recommendation>
}
```

### Gamification Elements
- Achievement badges for milestones
- Daily/weekly challenges
- Leaderboards (optional, privacy-focused)
- Streak counters and rewards
- Progress visualization with charts

## 🎓 STUDY MATERIALS SYSTEM

### Content Management
```kotlin
sealed class StudyContent {
    data class TextMaterial(
        val title: String,
        val content: String,
        val category: SSBCategory,
        val difficulty: DifficultyLevel,
        val estimatedReadTime: Duration
    ) : StudyContent()
    
    data class VideoMaterial(
        val title: String,
        val videoUrl: String,
        val transcript: String?,
        val duration: Duration
    ) : StudyContent()
    
    data class InteractiveMaterial(
        val title: String,
        val exercises: List<Exercise>,
        val feedback: FeedbackSystem
    ) : StudyContent()
}
```

### Offline Capability
- Download materials for offline access
- Sync progress when online
- Cache frequently accessed content
- Efficient storage management

## 🔒 DATA MANAGEMENT & SECURITY

### Local Database (Room)
```kotlin
@Entity(tableName = "test_results")
data class TestResultEntity(
    @PrimaryKey val id: String,
    val testType: String,
    val score: Float,
    val completedAt: Long,
    val timeSpent: Long,
    val responses: String // JSON serialized
)

@Entity(tableName = "study_progress")
data class StudyProgressEntity(
    @PrimaryKey val materialId: String,
    val progress: Float,
    val lastAccessed: Long,
    val bookmarked: Boolean,
    val notes: String?
)
```

### Privacy & Security
- Encrypt sensitive user data
- Implement proper authentication
- GDPR compliance for data handling
- Secure API communication (HTTPS, certificate pinning)
- Local data encryption for test results

## ⚡ PERFORMANCE OPTIMIZATION

### Memory Management
```kotlin
class ImageCache {
    private val lruCache = LruCache<String, Bitmap>(maxSize)
    
    fun loadImage(url: String, imageView: ImageView) {
        // Efficient image loading with caching
        // Proper bitmap recycling
        // Memory leak prevention
    }
}
```

### Background Processing
- Use WorkManager for sync operations
- Implement proper coroutine scoping
- Background test result processing
- Efficient data synchronization

### Battery Optimization
- Minimize background processing
- Efficient timer implementations
- Proper wake lock management
- Doze mode compatibility

## 🧪 TESTING STRATEGY

### Unit Tests
```kotlin
class TestEngineTest {
    @Test
    fun `should calculate correct score for TAT test`() {
        // Test scoring algorithms
        // Validate test logic
        // Edge case handling
    }
}
```

### UI Tests
```kotlin
@RunWith(AndroidJUnit4::class)
class TATTestActivityTest {
    @Test
    fun testTimerFunctionality() {
        // Test timer accuracy
        // UI state changes
        // User interaction flows
    }
}
```

### Integration Tests
- Test complete user flows
- Database operations
- Network connectivity scenarios
- Offline/online transitions

## 🌐 ACCESSIBILITY & INCLUSIVITY

### Accessibility Features
- Screen reader support (TalkBack)
- High contrast mode support
- Font size scaling
- Voice input for tests (where appropriate)
- Keyboard navigation support

### Internationalization
- **English-only** (SSB exam is conducted entirely in English)
- All UI strings MUST use string resources from `app/src/main/res/values/strings.xml`
- NO hardcoded strings in Kotlin/Compose code
- String resources allow future language support via `values-{lang}/strings.xml` if needed
- RTL layout support: Not required (English is LTR)

**Rationale for English-Only:**
- SSB (Services Selection Board) tests are conducted entirely in English
- Officer candidates must demonstrate English proficiency for the armed forces
- Study materials should match real exam conditions to prepare candidates effectively
- Supporting Hindi UI could create dependency that hurts exam performance
- All test content, instructions, and materials remain in English only

## 📈 ANALYTICS & MONITORING

### User Analytics
```kotlin
class AnalyticsManager {
    fun trackScreenView(screenName: String)
    fun trackTestStart(testType: String)
    fun trackTestCompletion(testType: String, score: Float)
    fun trackStudyMaterialAccess(materialId: String)
    fun trackUserEngagement(sessionDuration: Long)
}
```

### Performance Monitoring
- Crash reporting (Firebase Crashlytics)
- Performance metrics (app startup time, test loading time)
- Network request monitoring
- Memory usage tracking

## 🚀 DEPLOYMENT & MAINTENANCE

### Build Configuration
```kotlin
// build.gradle.kts (app level)
android {
    buildTypes {
        debug {
            isDebuggable = true
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}
```

### CI/CD Pipeline
- Automated testing on pull requests
- Code quality checks (detekt, ktlint)
- Automated builds for different environments
- Play Store deployment automation

## 📋 CODING STANDARDS

### **CRITICAL: String Resources Policy**

**ALL user-facing strings MUST be defined in XML string resources. NO EXCEPTIONS.**

#### ✅ CORRECT Pattern (Required)
```kotlin
// ✅ In strings.xml
<resources>
    <string name="test_completed">Test Completed Successfully</string>
    <string name="error_network">Network error occurred</string>
    <string name="button_submit">Submit</string>
</resources>

// ✅ In Kotlin/Compose code
Text(text = stringResource(R.string.test_completed))
Toast.makeText(context, getString(R.string.error_network), Toast.LENGTH_SHORT).show()
Button(onClick = {}) { Text(stringResource(R.string.button_submit)) }
```

#### ❌ WRONG - Anti-Pattern (DO NOT USE)
```kotlin
// ❌ FORBIDDEN: Hardcoded strings in code
Text(text = "Test Completed Successfully")  // Build will fail
Toast.makeText(context, "Network error", Toast.LENGTH_SHORT).show()  // Build will fail
Button(onClick = {}) { Text("Submit") }  // Build will fail
```

**Why String Resources Are Mandatory:**
1. **Consistency**: Ensures uniform language across the app
2. **Maintainability**: Centralized location for all user-facing text
3. **Future-Proofing**: Easy to add translations later if needed
4. **Professional Standards**: Industry best practice for Android development
5. **Content Management**: Non-developers can update text without code changes

**Enforcement:**
- `HardcodedTextDetector` lint rule (ERROR severity - fails build)
- Pre-commit hook blocks hardcoded strings
- CI/CD pipeline enforces string resource usage
- All PRs reviewed for compliance

**String Resource Naming Convention:**
```xml
<!-- Screen-specific strings -->
<string name="home_welcome">Welcome to SSBMax</string>
<string name="profile_edit_title">Edit Profile</string>

<!-- Common UI elements -->
<string name="button_continue">Continue</string>
<string name="button_cancel">Cancel</string>
<string name="label_email">Email Address</string>

<!-- Error messages -->
<string name="error_network">Network connection failed</string>
<string name="error_invalid_input">Please enter valid information</string>

<!-- Test-specific strings -->
<string name="tat_instructions">Write a story based on the image</string>
<string name="wat_timer_label">Time Remaining: %1$s</string>
</xml>
```

**String Formatting:**
```kotlin
// For strings with placeholders
<string name="test_score">Your score: %1$d out of %2$d</string>

// Usage in code
Text(text = stringResource(R.string.test_score, userScore, totalScore))
```

**Location:**
- All strings: `app/src/main/res/values/strings.xml`
- Organized by feature/screen with XML comments
- Alphabetically sorted within each section

### Kotlin Best Practices
```kotlin
// Use data classes for models
data class SSBTestResult(
    val testId: String,
    val score: Float,
    val completedAt: Instant,
    val feedback: String?
) {
    init {
        require(score in 0.0..100.0) { "Score must be between 0 and 100" }
    }
}

// Use sealed classes for state management
sealed class TestState {
    object Loading : TestState()
    object Ready : TestState()
    data class InProgress(val currentQuestion: Int, val totalQuestions: Int) : TestState()
    data class Completed(val result: TestResult) : TestState()
    data class Error(val message: String) : TestState()
}

// Use extension functions for utility operations
fun Context.showToast(@StringRes messageResId: Int, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, getString(messageResId), duration).show()
}

// Usage
context.showToast(R.string.error_network)
```

### Code Organization
- One class per file
- Meaningful naming conventions
- Proper documentation for public APIs
- Consistent formatting (ktlint)
- No magic numbers
- **NO hardcoded strings** - use string resources only

## 🎯 SSB-SPECIFIC FEATURES

### Authentic Test Simulation
- Exact timing as real SSB tests
- Similar UI/UX to actual test conditions
- Proper instructions and guidelines
- Realistic test environments

### Expert Content
- Collaborate with SSB experts for content accuracy
- Regular content updates based on latest patterns
- Success stories and testimonials
- Expert tips and strategies

### Community Features
- Discussion forums for candidates
- Peer-to-peer learning
- Mentor connections
- Study groups formation

## 🔧 DEVELOPMENT WORKFLOW

### Build System
- **IMPORTANT**: Always use `./gradle.sh` wrapper script for all Gradle commands on macOS
- This project uses a custom Gradle wrapper for consistency across development environments
- Examples:
  - `./gradle.sh build` instead of `./gradlew build`
  - `./gradle.sh test` instead of `./gradlew test`
  - `./gradle.sh assembleDebug` instead of `./gradlew assembleDebug`

### Error Handling Standards

**CRITICAL: Use ErrorLogger for all error logging**

```kotlin
// ❌ WRONG - Do NOT use
try {
    riskyOperation()
} catch (e: Exception) {
    e.printStackTrace()  // ERROR: Lint will fail the build
    Log.e("Tag", "Error", e)  // Discouraged in ViewModels
}

// ✅ CORRECT - Use ErrorLogger
try {
    riskyOperation()
} catch (e: Exception) {
    ErrorLogger.log(e, "Failed to perform risky operation")
}

// ✅ BEST - With context
try {
    submitTest()
} catch (e: Exception) {
    ErrorLogger.logTestError(
        throwable = e,
        description = "Test submission failed",
        testType = "TAT",
        userId = currentUser?.id
    )
}
```

**Why ErrorLogger?**
- ✅ Integrates with Firebase Crashlytics (production monitoring)
- ✅ Logs to Android Logcat (visible in debugging)
- ✅ Supports severity levels and structured context
- ✅ Never throws exceptions itself (fail-safe)
- ✅ Automatic tag inference from call site

**Enforcement:**
- `PrintStackTraceDetector` lint rule (ERROR severity - fails build)
- Pre-commit hook blocks `printStackTrace()`
- `ErrorHandlingArchitectureTest` validates codebase
- CI/CD pipeline enforces standards

See: `app/src/main/kotlin/com/ssbmax/utils/ErrorLogger.kt`

### Code Review Checklist
- [ ] Follows MVVM architecture
- [ ] Uses ErrorLogger for error handling (no printStackTrace())
- [ ] **All user-facing strings use string resources (no hardcoded strings)**
- [ ] Memory leak prevention
- [ ] Accessibility compliance
- [ ] Performance optimization
- [ ] Security considerations
- [ ] Test coverage
- [ ] Documentation updated

### Git Workflow
- Feature branch workflow
- Meaningful commit messages
- Pull request templates
- Code review requirements
- Automated testing before merge

Remember: The goal is to create an app that genuinely helps SSB candidates succeed while providing an exceptional user experience. Focus on authenticity, accuracy, and user engagement throughout the development process.
