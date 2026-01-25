# SSBMax - World-Class Android App Development Guide
# Specialized rules for building an exceptional SSB (Services Selection Board) preparation app
# Last Updated: January 25, 2026

## 🎯 PROJECT OVERVIEW

SSBMax is a comprehensive Android app for SSB preparation that helps candidates:
- Access structured study materials for Psychology, GTO, Interview, and Conference stages
- Practice authentic SSB tests (TAT, WAT, SRT, SDT, PIQ, OIR, PPDT, GTO tasks)
- Take AI-powered interview simulations with TTS support
- Track progress and performance analytics with OLQ (Officer Like Qualities) insights
- Get personalized tips and guidance
- Simulate real SSB testing conditions

## 🏗️ ARCHITECTURE & DESIGN PATTERNS

### Core Architecture
- **Pattern**: MVVM (Model-View-ViewModel) with Repository pattern
- **UI Framework**: Jetpack Compose (100% Compose, no XML layouts)
- **Navigation**: Jetpack Navigation Compose with type-safe navigation
- **State Management**: StateFlow & Compose State with proper hoisting
- **Dependency Injection**: Hilt/Dagger for clean architecture
- **Async Operations**: Kotlin Coroutines with structured concurrency
- **Local Storage**: Room Database + DataStore Preferences
- **Backend**: Firebase (Auth, Firestore, Storage, Messaging, Analytics, Crashlytics)
- **AI Integration**: Google Gemini for interview questions and test analysis

### Tech Stack (Current Versions - Jan 2026)
```kotlin
// Core
Kotlin: 2.1.0
AGP: 8.7.3
Compose BOM: 2024.05.00
compileSdk: 35
targetSdk: 35
minSdk: 26
JVM Target: 21

// Key Libraries
Hilt: 2.54
Room: 2.6.1
Coroutines: 1.9.0
Coil: 2.5.0
Firebase BOM: 33.7.0
Navigation Compose: 2.8.5
Ktor: 3.0.2
Generative AI (Gemini): 0.9.0
Google Billing: 7.1.1
WorkManager: 2.9.0
```

### Multi-Module Architecture
```
SSBMax/
├── app/                          # Main application module
│   ├── ui/                       # Compose UI screens & components
│   │   ├── auth/                # Login, role selection
│   │   ├── home/                # Student & instructor dashboards
│   │   ├── tests/               # All test modules (TAT, WAT, SRT, OIR, PPDT, SDT, PIQ, GTO)
│   │   ├── interview/           # AI-powered interview (voice/text modes)
│   │   ├── study/               # Study materials
│   │   ├── profile/             # User profiles
│   │   ├── premium/             # Subscription & upgrades
│   │   └── settings/            # App settings
│   ├── navigation/               # Navigation graphs (Auth, Student, Instructor, Shared)
│   ├── notifications/            # FCM messaging service
│   └── di/                       # App-level DI modules
├── core/
│   ├── common/                  # Shared utilities across modules
│   ├── designsystem/            # Reusable Compose components, theme
│   ├── domain/                  # Business logic, use cases, models, repository interfaces
│   │   ├── model/               # Domain models (TestType, User, Subscription, Interview, GTO)
│   │   ├── usecase/             # Use cases organized by feature
│   │   ├── repository/          # Repository interfaces
│   │   └── service/             # Service interfaces (AIService)
│   └── data/                    # Repositories, data sources, Room DB
│       ├── ai/                  # Gemini AI integration, prompts
│       ├── local/               # Room database, DAOs, entities
│       ├── remote/              # Firebase repositories
│       └── repository/          # Repository implementations
├── lint/                         # Custom lint rules (StateFlow, Hardcoded strings, etc.)
└── gradle/
    └── libs.versions.toml       # Centralized version catalog
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
        viewModel.loadSubmission(submissionId) // Fetch from Firestore
    }
}
```

#### ❌ WRONG - Anti-Patterns (DO NOT USE)
```kotlin
// ANTI-PATTERN: Passing complex object
onComplete: (TestResult) -> Unit // ❌ Not process-death safe

// ANTI-PATTERN: Singleton state holder
object TestResultHolder { // ❌ Memory leak, state pollution
    var result: TestResult? = null
}
```

**Why ID-Based Navigation?**
1. **Process-Death Safe**: Configuration changes don't lose data
2. **Deep Linking**: Can create deep links with IDs
3. **Size Limits**: Avoids 1MB Parcelable transaction limit
4. **Single Source of Truth**: Firestore/database is authority
5. **Testable**: Easy to mock repository in tests

### **Forbidden Anti-Patterns**

1. **Singleton State Holders** - NEVER create singleton objects with mutable state
2. **Complex Object Navigation** - NEVER pass complex objects via navigation
3. ***Holder File Naming** - NEVER create files ending in `*Holder.kt`

## 📱 TEST TYPES & SUBSCRIPTION MODEL

### TestType Enum (core/domain/model/SSBPhase.kt)
```kotlin
enum class TestType {
    // Phase 1
    OIR,        // Officer Intelligence Rating
    PPDT,       // Picture Perception & Description Test
    
    // Phase 2
    PIQ,        // Personal Information Questionnaire
    TAT,        // Thematic Apperception Test
    WAT,        // Word Association Test
    SRT,        // Situation Reaction Test
    SD,         // Self Description
    
    // GTO Tasks (8 individual tests)
    GTO_GD,     // Group Discussion
    GTO_GPE,    // Group Planning Exercise
    GTO_PGT,    // Progressive Group Task
    GTO_GOR,    // Group Obstacle Race
    GTO_HGT,    // Half Group Task
    GTO_LECTURETTE, // Lecturette
    GTO_IO,     // Individual Obstacles
    GTO_CT,     // Command Task
    
    IO;         // Interview Test - AI-powered SSB interview
}
```

### Subscription Model (SINGLE SOURCE OF TRUTH)

**Monthly Test Limits by Tier:**

| Test Type | FREE | PRO | PREMIUM |
|-----------|------|-----|---------|
| OIR | 1 | 5 | Unlimited |
| PPDT | 1 | 5 | Unlimited |
| PIQ | 1 | Unlimited | Unlimited |
| TAT | 0 | 3 | Unlimited |
| WAT | 0 | 3 | Unlimited |
| SRT | 0 | 3 | Unlimited |
| Self Description (SD) | 0 | 3 | Unlimited |
| GTO (each of 8 tests) | 0 | 3 | Unlimited |
| Interview (IO) | 0 | 2 | Unlimited |

**Implementation Location:**
- `core/data/src/main/kotlin/com/ssbmax/core/data/repository/SubscriptionManager.kt`
- This is the SINGLE SOURCE OF TRUTH - all other displays derive from this

## 🎙️ AI-POWERED INTERVIEW SYSTEM

The app includes a complete AI-powered SSB interview simulation using Google Gemini:

**Architecture:**
```
interview/
├── start/                  # Mode selection (text/voice)
├── session/                # Active interview session
└── result/                 # Results with OLQ analysis
```

**Key Features:**
- **Text Mode**: Type responses, AI generates follow-up questions
- **Voice Mode (Premium)**: TTS reads questions aloud
  - Sarvam AI (primary premium TTS - Indian English)
  - ElevenLabs (fallback premium TTS)
  - Android TTS (free tier fallback)
- **OLQ Analysis**: AI evaluates 15 Officer Like Qualities
- **Dynamic Questions**: Generated based on user's PIQ data

**TTS Configuration (build.gradle.kts):**
```kotlin
// API Keys in local.properties:
// GEMINI_API_KEY=your_key
// SARVAM_API_KEY=your_key
// ELEVENLABS_API_KEY=your_key

// Debug: Force premium TTS for testing
buildConfigField("boolean", "FORCE_PREMIUM_TTS", "true")
```

## 🔧 DEVELOPMENT WORKFLOW

### Build System
- **IMPORTANT**: Always use `./gradlew` wrapper script for all Gradle commands
- Examples: `./gradlew build`, `./gradlew test`, `./gradlew assembleDebug`

### Error Handling Standards

**CRITICAL: Use ErrorLogger for all error logging**

```kotlin
// ❌ WRONG - Do NOT use
e.printStackTrace() // ERROR: Lint will fail the build

// ✅ CORRECT - Use ErrorLogger
ErrorLogger.log(e, "Failed to perform operation")
```

**Domain Layer Exception:**
- Domain layer (`core/domain`) must NOT use ErrorLogger (Android dependency)
- Domain layer uses `Result<T>` pattern and hardcoded business logic messages
- Presentation layer maps domain errors to string resources

### String Resources Policy

**ALL user-facing strings MUST be defined in XML string resources. NO EXCEPTIONS.**

```kotlin
// ✅ CORRECT
Text(text = stringResource(R.string.test_completed))

// ❌ WRONG - Build will fail
Text(text = "Test Completed")
```

**Exception:** Domain layer business logic messages are exempt.

### StateFlow Update Pattern

**CRITICAL: Always use .update {} for thread-safe state mutations**

```kotlin
// ❌ WRONG - Race condition risk
_uiState.value = _uiState.value.copy(isLoading = true)

// ✅ CORRECT - Thread-safe
_uiState.update { it.copy(isLoading = true) }
```

## 📏 QUALITY FRAMEWORK RULES

### Strict Limits
- **File Size**: MAX 300 lines per file. If >300, YOU MUST SPLIT IT.
- **Component Size**: MAX 50 lines per Composable. Extract sub-components.

### Strings & Resources
- **NO HARDCODED STRINGS**: Use `stringResource(R.string.key)`
- **NO HARDCODED COLORS**: Use `MaterialTheme.colorScheme.primary`
- **NO HARDCODED DIMENSIONS**: Use `dp` or `sp` resources/constants

### Architecture Integrity
- **Domain Layer**: ZERO Android dependencies (no Context, no R.string, no ErrorLogger)
- **ViewModel**: Use `_state.update { ... }` for thread-safe updates
- **Dependencies**: Injected via Hilt/Dagger

### Testing Requirements
- **ViewModels**: MUST have Unit Tests
- **Repositories**: MUST have Unit Tests
- **Protocol**: If you change logic, you MUST update/add tests
- **Failure**: If `./gradlew check` fails, fix it immediately

## ✅ CODE REVIEW CHECKLIST

Before submitting code:
- [ ] Follows MVVM architecture with proper separation of concerns
- [ ] Uses Jetpack Compose with proper state management
- [ ] Uses ErrorLogger for error handling (not printStackTrace())
- [ ] All user-facing strings use string resources
- [ ] Uses `.update {}` for StateFlow mutations
- [ ] No files exceeding 300 lines
- [ ] No memory leaks (proper ViewModel scoping)
- [ ] Test coverage for ViewModels and Repositories
- [ ] No hardcoded colors or dimensions

## 🎯 DEVELOPMENT PRIORITIES

1. **Correctness**: App must work reliably without crashes
2. **User Experience**: Smooth, intuitive, Material Design 3 compliant
3. **Performance**: Fast load times, smooth animations, efficient memory usage
4. **Security**: Proper authentication, data encryption, secure communication
5. **Maintainability**: Clean architecture, readable code, comprehensive tests
6. **Authenticity**: Match real SSB test conditions and content

---

**Remember**: The goal is to create an app that genuinely helps SSB candidates succeed while providing an exceptional, world-class user experience.
