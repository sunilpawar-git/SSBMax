# SSBMax - Local Development Guide
# Quick reference for local development setup and debugging
# Last Updated: January 25, 2026

## 🚀 QUICK START

### Prerequisites
- JDK 21 (Temurin recommended)
- Android Studio Ladybug or later
- Android SDK with compileSdk 35

### First-Time Setup
```bash
# Clone and setup
git clone <repo-url>
cd SSBMax

# Create local.properties with your keys
echo "sdk.dir=/path/to/android/sdk" > local.properties
echo "GEMINI_API_KEY=your_gemini_key" >> local.properties
echo "SARVAM_API_KEY=your_sarvam_key" >> local.properties
echo "ELEVENLABS_API_KEY=your_elevenlabs_key" >> local.properties

# Build
./gradlew assembleDebug
```

### Required Firebase Setup
1. Create Firebase project
2. Download `google-services.json` to `app/` directory
3. Enable: Authentication, Firestore, Storage, Analytics, Crashlytics

## 🔧 BUILD COMMANDS

```bash
# Build debug APK
./gradlew assembleDebug

# Run unit tests
./gradlew testDebugUnitTest

# Run lint checks
./gradlew lintDebug

# Run all checks (lint + tests)
./gradlew check

# Clean build
./gradlew clean assembleDebug

# Generate code coverage report
./gradlew jacocoTestReport
```

## 🐛 DEBUG FLAGS

In `app/build.gradle.kts`:

```kotlin
debug {
    // Bypass subscription limits for testing all features
    buildConfigField("boolean", "BYPASS_SUBSCRIPTION_LIMITS", "true")
    
    // Force premium TTS (Sarvam AI) even for free users
    buildConfigField("boolean", "FORCE_PREMIUM_TTS", "true")
}
```

**To test real subscription limits:** Set `BYPASS_SUBSCRIPTION_LIMITS` to `"false"`

## 📱 TEST TYPES

| Test | Type | Implementation Status |
|------|------|----------------------|
| OIR | Phase 1 | ✅ Complete |
| PPDT | Phase 1 | ✅ Complete |
| PIQ | Phase 2 | ✅ Complete |
| TAT | Phase 2 | ✅ Complete |
| WAT | Phase 2 | ✅ Complete |
| SRT | Phase 2 | ✅ Complete |
| SD | Phase 2 | ✅ Complete |
| GTO - GD | Phase 2 | ✅ Complete |
| GTO - Lecturette | Phase 2 | ✅ Complete |
| GTO - GPE | Phase 2 | ✅ Complete |
| GTO - Others | Phase 2 | 🔄 In Progress |
| Interview (IO) | Phase 2 | ✅ Complete |

## 🎯 KEY FILES

### Configuration
- `gradle/libs.versions.toml` - Version catalog
- `app/build.gradle.kts` - App build config
- `local.properties` - API keys (not committed)

### Core Domain Models
- `core/domain/src/main/kotlin/com/ssbmax/core/domain/model/SSBPhase.kt` - TestType enum
- `core/domain/src/main/kotlin/com/ssbmax/core/domain/model/interview/` - Interview models

### Subscription Management
- `core/data/src/main/kotlin/com/ssbmax/core/data/repository/SubscriptionManager.kt` - Limits & usage

### Navigation
- `app/src/main/kotlin/com/ssbmax/navigation/SSBMaxDestinations.kt` - All routes
- `app/src/main/kotlin/com/ssbmax/navigation/SharedNavGraph.kt` - Shared screens

### AI Integration
- `core/data/src/main/kotlin/com/ssbmax/core/data/ai/` - Gemini service & prompts

## 🔍 DEBUGGING TIPS

### Subscription Issues
Check `SubscriptionManager` logs:
```
adb logcat | grep "SubscriptionManager"
```

### Firebase Issues
```
adb logcat | grep -E "(FirebaseAuth|Firestore|Firebase)"
```

### Interview TTS Issues
```
adb logcat | grep -E "(Sarvam|ElevenLabs|TTS)"
```

## 📋 COMMON ISSUES

### 1. "No google-services.json found"
- Download from Firebase Console → Project Settings → Android app
- Place in `app/google-services.json`

### 2. "Gemini API key invalid"
- Check `local.properties` has correct `GEMINI_API_KEY`
- Verify key is enabled in Google Cloud Console

### 3. "Build fails with lint errors"
- Run `./gradlew lintDebug` to see specific errors
- Check for hardcoded strings or missing string resources

### 4. "Tests timeout"
- Global timeout: 60 seconds per test
- Check for infinite loops or missing mock setup

## 🏗️ ARCHITECTURE QUICK REFERENCE

```
UI (Compose) → ViewModel → UseCase → Repository → DataSource
                  ↓            ↓           ↓
              StateFlow   Result<T>   Firebase/Room
```

**Key Patterns:**
- ViewModel exposes `StateFlow<UiState>`
- Use `.update {}` for thread-safe mutations
- Domain layer returns `Result<T>` (no Android deps)
- Repository interfaces in domain, implementations in data

---

**For full documentation, see:** `.cursorrules` and `CLAUDE.md`
