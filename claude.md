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
- Multi-language support (Hindi, English)
- RTL layout support
- Cultural considerations for content
- Regional test variations

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
fun Context.showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, duration).show()
}
```

### Code Organization
- One class per file
- Meaningful naming conventions
- Proper documentation for public APIs
- Consistent formatting (ktlint)
- No magic numbers or strings

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

### Code Review Checklist
- [ ] Follows MVVM architecture
- [ ] Proper error handling
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
