# SSBMax - Local Development Guide
# Quick reference for local development setup and debugging
# Last Updated: August 6, 2026

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

The old `BuildConfig.BYPASS_SUBSCRIPTION_LIMITS`/`BYPASS_INTERVIEW_PREREQUISITES` fields (Android-only,
never wired on iOS) were retired by the dev-subscription-override plan (Phase 6) and replaced by an
in-app **Developer Settings** section — same override, both platforms, no rebuild needed:

- Visible only in debug builds (`shared/.../platform/isDebugBuild()`, `expect`/`actual`).
- Settings → Developer Settings → **Subscription Override**: `Follow Real` / `Force Free` / `Force Pro` /
  `Force Premium`. Changes what every eligibility/limit read sees; `updateSubscriptionTier` always
  writes the real value regardless of the override (fail-safe — the override only changes what's *read*).
  Usage counters are **not** incremented while overridden, so toggling back to `Follow Real` doesn't
  leave you limit-locked from testing.
- Settings → Developer Settings → **Bypass Interview Prerequisites**: skips the PIQ/OIR/PPDT completion
  checks and the interview subscription gate together (mirrors the old flag's intent).
- Toggling doesn't refresh already-loaded ViewModels — re-enter the screen to see the change.

(`FORCE_PREMIUM_TTS` — the other `BuildConfig` flag previously documented here — was removed from this
doc during the same cleanup; it didn't exist anywhere in the codebase.)

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
- `shared/src/commonMain/.../data/repository/SubscriptionDtos.kt` (`SubscriptionLimits`) - the one
  monthly-limit table (per-tier, per-test-type); `core/data`'s old `SubscriptionManager` was deleted
  into `shared` in the KMP-convergence plan
- `shared/src/commonMain/.../domain/usecase/subscription/CheckTestEligibilityUseCase.kt` - enforcement
- **Interview limit changed** (dev-subscription-override plan, Phase 2): the old, contradictory
  `InterviewLimits.forSubscription` table (FREE 1 / PRO 1 / PREMIUM 3) is gone — `InterviewLimits` now
  derives its numbers from `SubscriptionLimits`'s "Interview" row: **FREE 0 / PRO 1 / PREMIUM 3**. FREE
  lost the one free interview it was inconsistently granted; PREMIUM gained an explicit 3/month cap
  instead of nominally unlimited.

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
