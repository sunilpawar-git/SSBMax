# 🎯 Android Studio Testing Guide - SSBMax

**Complete guide for running tests in Android Studio**

---

## 📋 Table of Contents

1. [Quick Start](#quick-start)
2. [Run All Tests](#run-all-tests)
3. [Run Single Test](#run-single-test)
4. [Run Test Class](#run-test-class)
5. [Run Tests by Module](#run-tests-by-module)
6. [UI Tests (Instrumented)](#ui-tests-instrumented)
7. [View Test Results](#view-test-results)
8. [Debug Tests](#debug-tests)
9. [Generate Coverage Report](#generate-coverage-report)
10. [Keyboard Shortcuts](#keyboard-shortcuts)
11. [Troubleshooting](#troubleshooting)

---

## 🚀 Quick Start

### Prerequisites

✅ **Android Studio** installed (Hedgehog or newer)  
✅ **Project synced** with Gradle  
✅ **Build successful** (Build → Make Project)

---

## 1️⃣ Run ALL Tests

### Method 1: Using Project View

1. **Open Project View** (Alt+1 / Cmd+1)
2. **Right-click** on the project root folder `SSBMax`
3. Select **"Run 'All Tests'"** or **"Run Tests in 'SSBMax'"**

![Run All Tests](https://i.imgur.com/placeholder1.png)

### Method 2: Using Gradle Tool Window

1. **Open Gradle Tool Window** (View → Tool Windows → Gradle)
2. Navigate to: `SSBMax → Tasks → verification`
3. **Double-click** on:
   - `test` (for all unit tests)
   - `testDebugUnitTest` (for debug unit tests)
   - `testReleaseUnitTest` (for release unit tests)

### Method 3: Using Menu

1. **Run** → **Run...**
2. Select **"All Tests"** from the dropdown

### Method 4: Using Terminal

```bash
# From Android Studio Terminal (Alt+F12 / Cmd+F12)
./gradlew test

# For debug variant only
./gradlew testDebugUnitTest

# For release variant only
./gradlew testReleaseUnitTest
```

**Expected Output**:
```
BUILD SUCCESSFUL in 45s
171 tests passed ✅
```

---

## 2️⃣ Run Single Test

### Method 1: Click the Green Arrow (Easiest!)

1. **Open the test file** (e.g., `TATTestViewModelTest.kt`)
2. **Locate the test function** (e.g., `fun initial state is loading()`)
3. **Click the green play button** (▶️) in the gutter (left margin)
4. Select **"Run 'initial state is loading()'"**

### Method 2: Right-Click on Test Function

1. **Place cursor** inside the test function
2. **Right-click** anywhere in the function
3. Select **"Run 'initial state is loading()'"**

### Method 3: Keyboard Shortcut

1. **Place cursor** inside the test function
2. Press:
   - **Windows/Linux**: `Ctrl+Shift+F10`
   - **Mac**: `Ctrl+Shift+R`

### Visual Guide

```kotlin
@Test // ← Click the green arrow here
fun `initial state is loading`() {
    // ...
}
```

---

## 3️⃣ Run Test Class

Run all tests in a single test file.

### Method 1: Click the Green Arrow on Class

1. **Open the test file** (e.g., `TATTestViewModelTest.kt`)
2. **Click the green play button** (▶️) next to the **class name**
3. Select **"Run 'TATTestViewModelTest'"**

### Method 2: Right-Click on Class Name

1. **Right-click** on the **class name** in the file
2. Select **"Run 'TATTestViewModelTest'"**

### Method 3: Right-Click on File in Project View

1. **Open Project View** (Alt+1 / Cmd+1)
2. Navigate to the test file
3. **Right-click** on `TATTestViewModelTest.kt`
4. Select **"Run 'TATTestViewModelTest'"**

### Visual Guide

```kotlin
// ← Click the green arrow here
class TATTestViewModelTest : BaseViewModelTest() {
    
    @Test
    fun `initial state is loading`() {
        // ...
    }
}
```

---

## 4️⃣ Run Tests by Module

Run all tests in a specific module (e.g., `app`, `core:domain`).

### Method 1: Project View

1. **Open Project View** (Alt+1 / Cmd+1)
2. **Right-click** on the module folder:
   - `app` - for app module tests
   - `core/domain` - for domain tests
   - `core/data` - for data tests
3. Select **"Run 'Tests in '...''"**

### Method 2: Gradle Tool Window

1. **Open Gradle Tool Window**
2. Navigate to the module, e.g.:
   - `SSBMax → app → Tasks → verification → test`
   - `SSBMax → core → domain → Tasks → verification → test`
3. **Double-click** on `test`

### Examples

**Run all app module tests:**
```bash
./gradlew :app:test
```

**Run all core:domain tests:**
```bash
./gradlew :core:domain:test
```

**Run all core:data tests:**
```bash
./gradlew :core:data:test
```

---

## 5️⃣ UI Tests (Instrumented Tests)

UI tests require an emulator or physical device.

### Setup

1. **Start Android Emulator**:
   - Tools → Device Manager
   - Click the play button on an emulator
   - Wait for emulator to boot

   OR

2. **Connect Physical Device**:
   - Enable USB Debugging on device
   - Connect via USB
   - Accept debugging prompt

### Run UI Tests

#### Method 1: Run All UI Tests

1. **Open Project View**
2. Navigate to `app/src/androidTest/kotlin/com/ssbmax`
3. **Right-click** on the `androidTest` folder
4. Select **"Run 'All Tests'"**

#### Method 2: Run Single UI Test

1. **Open UI test file** (e.g., `LoginScreenTest.kt`)
2. **Click the green arrow** next to the test function
3. Select **"Run on [Device Name]"**

#### Method 3: Using Terminal

```bash
# Run all instrumented tests
./gradlew connectedDebugAndroidTest

# Run specific test class
./gradlew connectedDebugAndroidTest --tests "com.ssbmax.ui.auth.LoginScreenTest"
```

### 🔥 Firebase Emulator (For Repository Tests)

Repository integration tests need Firebase Emulator:

**Step 1: Start Firebase Emulator**
```bash
cd /Users/sunil/Downloads/SSBMax
firebase emulators:start
```

**Step 2: Run Repository Tests**
```bash
./gradlew :core:data:connectedDebugAndroidTest
```

---

## 6️⃣ View Test Results

### Test Results Panel

After running tests, Android Studio shows the **Run** panel at the bottom.

**Features:**
- ✅ **Green** = Passed
- ❌ **Red** = Failed
- ⏭️ **Yellow** = Skipped
- **Click on test** to see details
- **Double-click on test** to jump to test code

### Test Statistics

The Run panel shows:
```
Tests passed: 28 of 28 tests
Duration: 2.3s
```

### Test Output

Click on a test to see:
- **Console output**
- **Stack traces** (for failures)
- **Assertion errors**

---

## 7️⃣ Debug Tests

### Debug Single Test

1. **Open the test file**
2. **Set breakpoints** (click in the gutter to add red dot)
3. **Right-click** on the test
4. Select **"Debug 'test name'"**
5. Use debug controls:
   - **Step Over** (F8)
   - **Step Into** (F7)
   - **Resume** (F9)
   - **Evaluate Expression** (Alt+F8)

### Debug All Tests in Class

1. **Click the bug icon** (🐛) next to the class name
2. Select **"Debug 'ClassName'"**

### Debug Shortcuts

- **Windows/Linux**: `Shift+F9`
- **Mac**: `Ctrl+D`

---

## 8️⃣ Generate Coverage Report

See which code is tested and which isn't.

### Method 1: Run with Coverage

1. **Right-click** on test file/folder
2. Select **"Run '...' with Coverage"**
3. Wait for tests to complete
4. **Coverage Report** appears in a new panel

### Method 2: Using Gradle

```bash
# Generate coverage report
./gradlew testDebugUnitTestCoverage

# Open the report
open app/build/reports/coverage/test/debug/index.html
```

### View Coverage

- **Green highlight** = Covered by tests
- **Red highlight** = NOT covered by tests
- **Yellow highlight** = Partially covered

### Coverage Panel

Shows:
- **Class coverage** percentage
- **Method coverage** percentage
- **Line coverage** percentage

---

## 9️⃣ Keyboard Shortcuts

### Running Tests

| Action | Windows/Linux | Mac |
|--------|---------------|-----|
| Run test | `Ctrl+Shift+F10` | `Ctrl+Shift+R` |
| Debug test | `Shift+F9` | `Ctrl+D` |
| Run previous test | `Shift+F10` | `Ctrl+R` |
| Stop tests | `Ctrl+F2` | `Cmd+F2` |

### Navigation

| Action | Windows/Linux | Mac |
|--------|---------------|-----|
| Jump to test | `Ctrl+Shift+T` | `Cmd+Shift+T` |
| Open Project View | `Alt+1` | `Cmd+1` |
| Open Terminal | `Alt+F12` | `Cmd+F12` |
| Open Gradle | `Ctrl+Shift+A` → "Gradle" | `Cmd+Shift+A` → "Gradle" |

### Debugging

| Action | Windows/Linux | Mac |
|--------|---------------|-----|
| Toggle breakpoint | `Ctrl+F8` | `Cmd+F8` |
| Step over | `F8` | `F8` |
| Step into | `F7` | `F7` |
| Resume | `F9` | `F9` |
| Evaluate expression | `Alt+F8` | `Option+F8` |

---

## 🔟 Troubleshooting

### ❌ Tests Not Running

**Problem**: Click play button, nothing happens

**Solution**:
1. **File → Invalidate Caches → Invalidate and Restart**
2. Wait for Android Studio to restart
3. Try again

---

### ❌ "Cannot find symbol" errors

**Problem**: Test code shows red underlines

**Solution**:
1. **File → Sync Project with Gradle Files**
2. **Build → Clean Project**
3. **Build → Rebuild Project**

---

### ❌ Tests Fail with "Class not found"

**Problem**: Tests fail immediately with ClassNotFoundException

**Solution**:
1. Check if the test is in correct folder:
   - Unit tests: `src/test/kotlin/`
   - UI tests: `src/androidTest/kotlin/`
2. Verify package name matches folder structure
3. **File → Sync Project with Gradle Files**

---

### ❌ UI Tests Can't Find Emulator

**Problem**: "No connected devices" error

**Solution**:
1. **Tools → Device Manager**
2. Start an emulator
3. Wait for it to fully boot (home screen visible)
4. Run test again

---

### ❌ Repository Tests Fail

**Problem**: Firebase Firestore connection errors

**Solution**:
1. **Start Firebase Emulator first:**
   ```bash
   cd /Users/sunil/Downloads/SSBMax
   firebase emulators:start
   ```
2. Wait for "All emulators ready!" message
3. Run tests

---

### ❌ Tests Running Slow

**Problem**: Tests take too long

**Solution**:
1. **File → Settings → Build, Execution, Deployment → Gradle**
2. Set **"Run tests using"** to **"Gradle (Default)"** or **"IntelliJ IDEA"**
3. Try both and see which is faster for your machine

---

### ❌ Gradle Sync Failed

**Problem**: "Gradle sync failed" error

**Solution**:
1. Check internet connection
2. **File → Invalidate Caches → Invalidate and Restart**
3. In Terminal:
   ```bash
   ./gradlew --stop
   ./gradlew clean
   ./gradlew build
   ```

---

## 📚 Quick Reference Card

### Test Types in SSBMax

| Test Type | Location | Run Command | Needs Emulator? |
|-----------|----------|-------------|-----------------|
| **Unit Tests** | `src/test/` | `./gradlew test` | ❌ No |
| **UI Tests** | `src/androidTest/` | `connectedDebugAndroidTest` | ✅ Yes |
| **Repository Tests** | `src/androidTest/` | `connectedDebugAndroidTest` | ✅ Yes + Firebase |

---

### Test File Locations

```
SSBMax/
├── app/src/test/kotlin/            → Unit tests (ViewModel, etc.)
├── app/src/androidTest/kotlin/     → UI tests (Compose screens)
├── core/domain/src/test/           → Domain unit tests
├── core/data/src/test/             → Data unit tests
└── core/data/src/androidTest/      → Repository integration tests
```

---

### Run Configurations

**Create Custom Run Configuration:**

1. **Run → Edit Configurations...**
2. Click **+** → Select **Android JUnit**
3. Configure:
   - **Name**: "All Unit Tests"
   - **Test kind**: "All in package"
   - **Package**: (leave empty for all tests)
   - **Module**: app
4. **Apply → OK**

Now you can run this configuration from the toolbar dropdown!

---

## 🎯 Common Workflows

### Workflow 1: Test-Driven Development (TDD)

1. Write a failing test
2. Run the test (Ctrl+Shift+F10)
3. Watch it fail ❌
4. Implement the feature
5. Run the test again
6. Watch it pass ✅
7. Refactor code
8. Run test to ensure it still passes

### Workflow 2: Before Committing

```bash
# Run all unit tests
./gradlew test

# If all pass, commit
git add .
git commit -m "Your message"
```

### Workflow 3: Continuous Testing

**Enable Auto-Test in Android Studio:**

1. **Run → Run... → Edit Configurations**
2. Check **"Auto-test"** option
3. Tests run automatically when you save files!

### Workflow 4: Daily Testing Routine

**Morning:**
```bash
./gradlew test  # Quick unit tests (1-2 min)
```

**After major changes:**
```bash
./gradlew test connectedDebugAndroidTest  # Full suite (10-15 min)
```

**Before pushing:**
```bash
./gradlew clean test testDebugUnitTestCoverage
```

---

## 🎉 You're Ready!

You now know how to:
- ✅ Run all tests
- ✅ Run single tests
- ✅ Run tests by module
- ✅ Debug tests
- ✅ View coverage
- ✅ Troubleshoot issues

**Happy Testing! 🚀**

---

## 📖 Additional Resources

- [Android Testing Documentation](https://developer.android.com/training/testing)
- [JUnit 4 Documentation](https://junit.org/junit4/)
- [MockK Documentation](https://mockk.io/)
- [Compose Testing Guide](https://developer.android.com/jetpack/compose/testing)

---

**Created**: October 26, 2025  
**For**: SSBMax Android App  
**Version**: 1.0

