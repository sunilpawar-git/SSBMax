# Firebase Emulator Setup - Complete Guide

## ✅ Current Status

### Completed:
1. ✅ Firebase CLI installed (v14.22.0)
2. ✅ Java configured (Android Studio JDK 21)
3. ✅ Firebase emulators downloaded and running
4. ✅ Test infrastructure created:
   - `FirebaseTestHelper` for emulator configuration
   - `TestApplication` for Firebase initialization
   - Test runner configuration
   - Test resources with Firebase config

### Issue:
Tests are **timing out** because the Android emulator cannot connect to Firebase emulators running on `127.0.0.1`.

## 🔧 Solution: Configure Emulators to Listen on All Interfaces

The Firebase emulators need to listen on `0.0.0.0` (all network interfaces) so the Android emulator can connect via `10.0.2.2`.

### Step 1: Update `firebase.json`

Add host configuration to bind emulators to all interfaces:

```json
{
  "emulators": {
    "auth": {
      "host": "0.0.0.0",
      "port": 9099
    },
    "firestore": {
      "host": "0.0.0.0",
      "port": 8080
    },
    "ui": {
      "enabled": true,
      "host": "0.0.0.0",
      "port": 4000
    }
  }
}
```

### Step 2: Restart Emulators

```bash
# Stop current emulators (Ctrl+C)

# Set JAVA_HOME
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"

# Start emulators with new configuration
cd /Users/sunil/Downloads/SSBMax
firebase emulators:start --only auth,firestore --project demo-test
```

### Step 3: Run Integration Tests

```bash
bash gradle.sh :core:data:connectedDebugAndroidTest
```

## 📁 Files Created

### 1. Firebase Test Helper
**Location**: `core/data/src/androidTest/kotlin/com/ssbmax/core/data/FirebaseTestHelper.kt`

Provides singleton Firestore instance configured for emulator:
```kotlin
// Usage in tests:
firestore = FirebaseTestHelper.getEmulatorFirestore()
```

### 2. Test Application
**Location**: `core/data/src/androidTest/kotlin/com/ssbmax/core/data/FirebaseTestRunner.kt`

Custom test runner and application that initializes Firebase.

### 3. Test Manifest
**Location**: `core/data/src/androidTest/AndroidManifest.xml`

Configures test application and permissions.

### 4. Test Resources
**Location**: `core/data/src/androidTest/res/values/google_services_test.xml`

Provides Firebase configuration values for tests.

## 🧪 Test Architecture

### Integration Test Structure:
```
core/data/src/androidTest/kotlin/com/ssbmax/core/data/repository/
├── TestContentRepositoryImplTest.kt (24 tests)
├── TestProgressRepositoryImplTest.kt (15 tests)
├── TestSubmissionRepositoryImplTest.kt (19 tests)
└── UserProfileRepositoryImplTest.kt (15 tests)

Total: 77 integration tests
```

### Test Features:
- ✅ Real Firebase Firestore operations against emulator
- ✅ Real-time updates with Flow/Turbine testing
- ✅ Automatic test data cleanup
- ✅ Isolated test environment (no persistence)
- ✅ Support for all test types (TAT, WAT, SRT, PPDT, OIR)
- ✅ User profile CRUD operations
- ✅ Submission tracking and grading workflow
- ✅ Progress calculation across SSB phases

## 🎯 Next Steps

1. **Update firebase.json** with host configuration
2. **Restart Firebase emulators**
3. **Run tests** - they should connect successfully
4. **Monitor Emulator UI** at http://127.0.0.1:4000/ to see test data

## 🔍 Debugging

### Check Emulator Connection:
```bash
# From Android emulator, test connectivity:
adb shell ping -c 3 10.0.2.2
```

### View Emulator Logs:
```bash
# Check firestore-debug.log for connection attempts
tail -f firestore-debug.log
```

### Test Individual Repository:
```bash
# Run specific test class
bash gradle.sh :core:data:connectedDebugAndroidTest \
  --tests "com.ssbmax.core.data.repository.UserProfileRepositoryImplTest"
```

## 📊 Expected Test Results

Once emulators are properly configured:
- **77 tests** should run
- Tests validate:
  - Firestore CRUD operations
  - Real-time Flow updates  
  - Data caching behavior
  - Mock data fallbacks
  - Session management
  - Progress tracking
  - Submission workflows

## ✨ Benefits

1. **Fast Feedback**: No need for real Firebase project
2. **Isolated Testing**: Each test run is independent
3. **Offline Development**: Work without internet
4. **Cost-Free**: No Firebase billing for test operations
5. **Realistic Testing**: Uses actual Firebase SDK, not mocks

---

**Status**: Ready to run once `firebase.json` is updated and emulators restarted with `0.0.0.0` binding.

