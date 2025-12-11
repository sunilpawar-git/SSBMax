# Integration Tests - Quick Start Guide

## 🚀 Running Phase 3 Integration Tests

### Prerequisites Checklist

- [ ] Node.js and npm installed
- [ ] Firebase CLI installed (`npm install -g firebase-tools`)
- [ ] Android Emulator or physical device connected
- [ ] Android SDK and tools configured

---

## Step 1: Install Firebase Emulator (First Time Only)

```bash
# Install Firebase CLI globally
npm install -g firebase-tools

# Login to Firebase (if needed)
firebase login

# Initialize Firebase in project (if not already done)
cd /Users/sunil/Downloads/SSBMax
firebase init emulators
```

**Select these emulators when prompted**:
- [x] Authentication Emulator
- [x] Firestore Emulator
- [x] Storage Emulator

Use default ports:
- Authentication: 9099
- Firestore: 8080
- Storage: 9199

---

## Step 2: Start Firebase Emulator

```bash
cd /Users/sunil/Downloads/SSBMax

# Start emulators (keeps running in terminal)
firebase emulators:start

# OR start in background
firebase emulators:start &
```

**Expected Output**:
```
┌─────────────────────────────────────────────────────────────┐
│ ✔  All emulators ready! It is now safe to connect your app │
│ View Emulator UI at http://127.0.0.1:4000                  │
└─────────────────────────────────────────────────────────────┘

┌────────────┬────────────────┬─────────────────────────────────┐
│ Emulator   │ Host:Port      │ View in Emulator UI             │
├────────────┼────────────────┼─────────────────────────────────┤
│ Auth       │ 127.0.0.1:9099 │ http://127.0.0.1:4000/auth      │
│ Firestore  │ 127.0.0.1:8080 │ http://127.0.0.1:4000/firestore │
│ Storage    │ 127.0.0.1:9199 │ http://127.0.0.1:4000/storage   │
└────────────┴────────────────┴─────────────────────────────────┘
```

**Keep this terminal open** - emulators must run while tests execute.

---

## Step 3: Start Android Emulator

**Option A: Android Studio**
1. Open Android Studio
2. Tools → Device Manager
3. Start your Android emulator (e.g., Pixel 9 API 35)

**Option B: Command Line**
```bash
# List available AVDs
emulator -list-avds

# Start emulator
emulator -avd Pixel_9_API_35
```

**Verify device connected**:
```bash
adb devices
# Should show: 
# emulator-5554    device
```

---

## Step 4: Run Integration Tests

### Run All Repository Integration Tests

```bash
cd /Users/sunil/Downloads/SSBMax

# Run all integration tests
bash gradle.sh connectedAndroidTest

# OR run only core:data module tests
bash gradle.sh :core:data:connectedDebugAndroidTest
```

### Run Specific Test Class

```bash
# Run UserProfileRepository tests only
bash gradle.sh :core:data:connectedDebugAndroidTest \
  --tests "*UserProfileRepositoryImplTest"

# Run TestProgressRepository tests only
bash gradle.sh :core:data:connectedDebugAndroidTest \
  --tests "*TestProgressRepositoryImplTest"

# Run TestContentRepository tests only
bash gradle.sh :core:data:connectedDebugAndroidTest \
  --tests "*TestContentRepositoryImplTest"

# Run TestSubmissionRepository tests only
bash gradle.sh :core:data:connectedDebugAndroidTest \
  --tests "*TestSubmissionRepositoryImplTest"
```

### Run Specific Test Method

```bash
bash gradle.sh :core:data:connectedDebugAndroidTest \
  --tests "*.UserProfileRepositoryImplTest.getUserProfile_emits_realtime_updates"
```

---

## Step 5: View Test Results

### Terminal Output

```
> Task :core:data:connectedDebugAndroidTest
Starting 86 tests on Pixel_9_API_35(AVD) - 16

com.ssbmax.core.data.repository.UserProfileRepositoryImplTest > saveUserProfile_successfully_persists_to_firestore PASSED
com.ssbmax.core.data.repository.UserProfileRepositoryImplTest > getUserProfile_returns_saved_profile PASSED
... (all tests)

BUILD SUCCESSFUL in 3m 42s
86 tests completed, 0 failed, 0 skipped
```

### HTML Report

After tests complete, view detailed HTML report:

```bash
# Open HTML test report
open core/data/build/reports/androidTests/connected/index.html
```

### Logcat (Real-time)

Watch test execution in real-time:

```bash
# In separate terminal
adb logcat -s TestRunner:* TestProgress:* TestContent:*
```

---

## 📊 Expected Test Execution Time

| Test Suite | Test Count | Avg Time | Notes |
|------------|------------|----------|-------|
| UserProfile | 15 tests | ~45s | Real-time updates |
| TestProgress | 19 tests | ~55s | Progress calculations |
| TestContent | 28 tests | ~1m 20s | Caching, sessions |
| TestSubmission | 24 tests | ~1m 10s | Complete workflows |
| **TOTAL** | **86 tests** | **~3m 40s** | Firebase Emulator |

---

## 🔍 Troubleshooting

### Problem: Tests fail with "Connection refused"

**Cause**: Firebase Emulator not running

**Solution**:
```bash
# In separate terminal
firebase emulators:start
```

### Problem: Tests fail with "No connected devices"

**Cause**: Android emulator not running

**Solution**:
```bash
# Check devices
adb devices

# Start emulator
emulator -avd Pixel_9_API_35
```

### Problem: Tests hang or timeout

**Cause**: Network issues or emulator performance

**Solution**:
```bash
# Restart ADB
adb kill-server
adb start-server

# Cold boot emulator
emulator -avd Pixel_9_API_35 -no-snapshot-load
```

### Problem: Firebase Emulator UI not accessible

**Cause**: Port conflict

**Solution**:
```bash
# Check if port 4000 is in use
lsof -i :4000

# Kill process using port
kill -9 <PID>

# Restart emulator
firebase emulators:start
```

### Problem: "PERMISSION_DENIED" errors

**Cause**: Security rules not deployed to emulator

**Solution**:
```bash
# Deploy rules to emulator
firebase deploy --only firestore:rules --project demo-test
```

### Problem: Tests pass locally but fail in CI

**Cause**: Firebase Emulator not started in CI

**Solution**: Ensure CI pipeline starts emulators before tests:
```yaml
# .github/workflows/android.yml
- name: Start Firebase Emulator
  run: firebase emulators:start --only auth,firestore --project demo-test &
  
- name: Wait for Emulators
  run: sleep 10
  
- name: Run Integration Tests
  run: ./gradlew connectedAndroidTest
```

---

## 🧹 Cleanup

### After Testing

```bash
# Stop Firebase Emulator
# Press Ctrl+C in terminal running emulators

# OR if running in background
firebase emulators:stop

# Stop Android Emulator
adb emu kill
```

### Clear Emulator Data (if needed)

```bash
# Clear Firestore emulator data
curl -X DELETE http://localhost:8080/emulator/v1/projects/demo-test/databases/(default)/documents

# Clear Auth emulator data
curl -X DELETE http://localhost:9099/emulator/v1/projects/demo-test/accounts
```

---

## 📈 Continuous Integration Setup

### GitHub Actions Example

```yaml
name: Android Integration Tests

on: [push, pull_request]

jobs:
  integration-tests:
    runs-on: macos-latest
    
    steps:
      - uses: actions/checkout@v3
      
      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'
      
      - name: Install Node.js
        uses: actions/setup-node@v3
        with:
          node-version: '18'
      
      - name: Install Firebase CLI
        run: npm install -g firebase-tools
      
      - name: Start Firebase Emulator
        run: |
          firebase emulators:start --only auth,firestore --project demo-test &
          sleep 10
      
      - name: Run Integration Tests
        uses: reactivecircus/android-emulator-runner@v2
        with:
          api-level: 35
          target: google_apis
          arch: x86_64
          script: ./gradlew connectedAndroidTest
      
      - name: Upload Test Reports
        uses: actions/upload-artifact@v3
        if: always()
        with:
          name: test-reports
          path: |
            **/build/reports/androidTests/
            **/build/test-results/
```

---

## 📚 Additional Resources

### View Emulator UI
- **URL**: http://localhost:4000
- **Firestore Data**: http://localhost:4000/firestore
- **Auth Users**: http://localhost:4000/auth

### Test Documentation
- [Phase 3 Complete Guide](PHASE_3_REPOSITORY_INTEGRATION_TESTS_COMPLETE.md)
- [Testing Plan](incremental-testing-plan.plan.md)
- [Firebase Emulator Docs](https://firebase.google.com/docs/emulator-suite)

---

## ✅ Success Checklist

Before reporting test issues, verify:

- [ ] Firebase Emulator is running (`firebase emulators:start`)
- [ ] Android Emulator or device is connected (`adb devices`)
- [ ] Internet connection is available (for downloading dependencies)
- [ ] Correct emulator ports configured (Firestore: 8080, Auth: 9099)
- [ ] No other apps using emulator ports
- [ ] `firestore.rules` file exists in project root
- [ ] `firebase.json` file exists in project root

---

## 🎯 Quick Command Reference

```bash
# Start emulators
firebase emulators:start

# Run all integration tests
bash gradle.sh connectedAndroidTest

# Run specific test class
bash gradle.sh :core:data:connectedDebugAndroidTest --tests "*UserProfileRepositoryImplTest"

# View HTML report
open core/data/build/reports/androidTests/connected/index.html

# Check connected devices
adb devices

# Watch test logs
adb logcat -s TestRunner:*

# Stop emulators
# Press Ctrl+C or:
firebase emulators:stop
```

---

**Happy Testing! 🚀**

For issues or questions, refer to [PHASE_3_REPOSITORY_INTEGRATION_TESTS_COMPLETE.md](PHASE_3_REPOSITORY_INTEGRATION_TESTS_COMPLETE.md)

