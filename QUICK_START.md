# SSBMax Quick Start Guide

## Prerequisites
- Android Studio installed
- Android device or emulator
- USB debugging enabled (for physical device)

## Building the Project

### Option 1: Using Android Studio
1. Open Android Studio
2. Select "Open" and navigate to `/Users/sunil/Downloads/SSBMax`
3. Wait for Gradle sync to complete
4. Click "Build" → "Make Project" or press `Cmd + F9`
5. Click "Run" → "Run 'app'" or press `Ctrl + R`

### Option 2: Using Command Line
```bash
cd /Users/sunil/Downloads/SSBMax
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew assembleDebug
```

## Running the App

### On Emulator
```bash
# Start emulator first, then:
./gradlew installDebug
adb shell am start -n com.ssbmax/.MainActivity
```

### On Physical Device
1. Enable USB debugging on your device
2. Connect device via USB
3. Run:
```bash
adb devices  # Verify device is connected
./gradlew installDebug
```

## Common Commands

### Build Commands
```bash
# Clean build
./gradlew clean build

# Debug APK only
./gradlew assembleDebug

# Release APK (unsigned)
./gradlew assembleRelease

# Run all tests
./gradlew test

# Run lint checks
./gradlew lintDebug
```

### Testing Commands
```bash
# Run unit tests
./gradlew testDebugUnitTest

# Run specific test class
./gradlew test --tests AuthViewModelTest

# Generate test coverage report
./gradlew testDebugUnitTestCoverage
```

### ADB Commands
```bash
# Install APK
adb install app/build/outputs/apk/debug/app-debug.apk

# Uninstall app
adb uninstall com.ssbmax

# View logs
adb logcat | grep SSBMax

# Clear app data
adb shell pm clear com.ssbmax
```

## Project Structure

```
SSBMax/
├── app/                          # Main application module
│   ├── src/main/kotlin/
│   │   ├── navigation/          # Navigation setup
│   │   ├── ui/auth/            # Authentication screens
│   │   ├── ui/splash/          # Splash screen
│   │   └── SSBMaxApplication.kt
│   └── src/test/kotlin/         # Unit tests
│
├── core/
│   ├── common/                  # Shared utilities
│   ├── data/                    # Data layer & repositories
│   ├── domain/                  # Business logic & models
│   └── designsystem/           # UI components & theme
│
└── gradle/                      # Gradle configuration
```

## Development Workflow

### 1. Making Changes
1. Create a feature branch
2. Make your changes
3. Run tests: `./gradlew test`
4. Run lint: `./gradlew lintDebug`
5. Build: `./gradlew assembleDebug`

### 2. Adding New Features
- **New Screen:** Add to `app/src/main/kotlin/com/ssbmax/ui/`
- **New Model:** Add to `core/domain/src/main/kotlin/com/ssbmax/core/domain/model/`
- **New Repository:** Add to `core/data/src/main/kotlin/com/ssbmax/core/data/repository/`
- **New UI Component:** Add to `core/designsystem/`

### 3. Testing
```bash
# Test specific module
./gradlew :app:test
./gradlew :core:domain:test

# Test with coverage
./gradlew test jacocoTestReport
```

## Troubleshooting

### Build Fails with "JAVA_HOME" Error
```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
```

### Gradle Sync Issues
```bash
./gradlew clean
./gradlew --stop
./gradlew build --refresh-dependencies
```

### ADB Device Not Found
```bash
adb kill-server
adb start-server
adb devices
```

### App Crashes on Launch
```bash
# Check logs
adb logcat | grep -E "AndroidRuntime|SSBMax"

# Clear app data and reinstall
adb uninstall com.ssbmax
./gradlew installDebug
```

## Current App Features

✅ **Implemented:**
- Splash screen with app logo
- Login screen with Google Sign-In UI
- Role selection screen (Student/Instructor/Admin)
- Navigation graph with proper routing
- MVVM architecture
- Hilt dependency injection setup
- Material Design 3 theming

🔄 **In Progress:**
- Firebase Authentication integration
- Room database setup
- Repository implementations
- Test content and screens

## Key Files to Know

### Configuration
- `gradle/libs.versions.toml` - Dependency versions
- `app/build.gradle.kts` - App module configuration
- `app/google-services.json` - Firebase configuration

### Architecture
- `app/src/main/kotlin/com/ssbmax/navigation/NavGraph.kt` - Navigation routes
- `app/src/main/kotlin/com/ssbmax/ui/auth/AuthViewModel.kt` - Authentication logic
- `core/domain/src/main/kotlin/com/ssbmax/core/domain/model/` - Domain models

### Styling
- `core/designsystem/src/main/kotlin/com/ssbmax/core/designsystem/theme/` - App theme

## Next Development Steps

1. **Implement Firebase Authentication**
   - Enable Google Sign-In in Firebase Console
   - Implement `AuthRepository` in data layer
   - Connect UI to actual authentication

2. **Set Up Room Database**
   - Create database entities
   - Define DAOs
   - Implement migrations

3. **Build Test Modules**
   - Create TAT test screen
   - Implement WAT test screen
   - Add SRT test screen

4. **Add Study Materials**
   - Create content models
   - Implement content repository
   - Build study material screens

## Resources

- **Project Documentation:** `README.md`
- **Build Instructions:** `BUILD_INSTRUCTIONS.md`
- **Implementation Progress:** `IMPLEMENTATION_PROGRESS.md`
- **Phase 1 Completion:** `PHASE_1_COMPLETE.md`

## Support

For issues or questions:
1. Check `BUILD_SUCCESS_SUMMARY.md` for common solutions
2. Review error logs with `adb logcat`
3. Check Android Studio's Build Output panel

---

**Happy Coding! 🚀**

