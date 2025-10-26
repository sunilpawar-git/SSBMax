# SSBMax Testing Setup Guide

## Overview
This document describes the testing infrastructure for SSBMax, including setup instructions for Firebase Emulator Suite and test execution.

## Testing Architecture

### Test Pyramid
- **Unit Tests (60%)**: Fast, isolated tests for ViewModels, Use Cases, and Domain Models
- **Integration Tests (25%)**: Repository tests with Firebase Emulator
- **UI Tests (15%)**: End-to-end user journey tests

### Tools & Libraries
- **JUnit 4**: Test framework
- **MockK**: Mocking library for Kotlin
- **Turbine**: Flow testing
- **Coroutines Test**: Async testing utilities
- **Hilt Testing**: Dependency injection for tests
- **Compose UI Test**: UI component testing
- **Firebase Emulator Suite**: Local Firebase services for integration testing

## Prerequisites

### 1. Install Firebase CLI
```bash
# Install Firebase CLI globally
npm install -g firebase-tools

# Verify installation
firebase --version
```

### 2. Install Java JDK
Firebase Emulator requires Java 11 or higher:
```bash
# Check Java version
java -version

# If needed, install Java 11+
# macOS: brew install openjdk@11
# Ubuntu: sudo apt install openjdk-11-jdk
```

## Firebase Emulator Setup

### 1. Start Emulator Suite
```bash
# Navigate to project root
cd /Users/sunil/Downloads/SSBMax

# Start emulators (first time will download emulator binaries)
firebase emulators:start

# Or run in background
firebase emulators:start &
```

### 2. Emulator Ports
- **Auth Emulator**: http://localhost:9099
- **Firestore Emulator**: http://localhost:8080
- **Storage Emulator**: http://localhost:9199
- **Emulator UI**: http://localhost:4000

### 3. Emulator UI
Open http://localhost:4000 to view:
- Firestore data in real-time
- Auth users
- Storage files
- Test data seeded during tests

## Running Tests

### Unit Tests (Local)
```bash
# Run all unit tests
./gradlew test

# Run specific module tests
./gradlew :app:testDebugUnitTest
./gradlew :core:domain:test
./gradlew :core:data:test

# Run with coverage report
./gradlew testDebugUnitTestCoverage

# View coverage report
open app/build/reports/coverage/test/debug/index.html
```

### Integration Tests (Requires Emulator)
```bash
# Ensure Firebase Emulator is running
firebase emulators:start &

# Run instrumented tests on connected device/emulator
./gradlew connectedAndroidTest

# Run specific test class
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.ssbmax.core.data.repository.AuthRepositoryImplTest
```

### UI Tests (Requires Device/Emulator)
```bash
# Start Android emulator or connect physical device
adb devices

# Run UI tests
./gradlew connectedDebugAndroidTest

# Run specific screen test
./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.ssbmax.ui.auth.LoginScreenTest
```

## Test Utilities

### 1. BaseViewModelTest
Base class for ViewModel unit tests with coroutine setup:
```kotlin
class MyViewModelTest : BaseViewModelTest() {
    @Test
    fun `test ViewModel state`() = runTest {
        // Test implementation
    }
}
```

### 2. BaseRepositoryTest
Base class for repository integration tests with Firebase Emulator:
```kotlin
class MyRepositoryTest : BaseRepositoryTest() {
    @Test
    fun `test repository with Firestore`() = runTest {
        // Firestore and Auth are automatically connected to emulator
        val userId = createTestUser()
        seedFirestoreData("users", userId, mapOf("name" to "Test"))
    }
}
```

### 3. MockDataFactory
Factory for creating consistent test data:
```kotlin
val mockUser = MockDataFactory.createMockUser()
val mockProfile = MockDataFactory.createMockUserProfile()
val mockQuestions = MockDataFactory.createMockTATQuestions(count = 12)
```

### 4. TestDispatcherRule
JUnit rule for coroutine testing:
```kotlin
class MyTest {
    @get:Rule
    val dispatcherRule = TestDispatcherRule()
    
    @Test
    fun `test with coroutines`() = runTest {
        // Dispatchers.Main is properly set up
    }
}
```

## Test Data Management

### Seeding Test Data
```kotlin
@Before
fun setUp() {
    // Seed Firestore with test data
    seedFirestoreData(
        collection = "users",
        documentId = "test-user-123",
        data = mapOf(
            "email" to "test@ssbmax.com",
            "displayName" to "Test User"
        )
    )
}
```

### Cleaning Up After Tests
```kotlin
@After
fun tearDown() {
    // Clear test data
    clearFirestoreCollection("users")
    auth.signOut()
}
```

## Troubleshooting

### Emulator Connection Issues
If tests fail to connect to emulator:
1. Verify emulator is running: `curl http://localhost:8080`
2. Check emulator logs: `firebase emulators:start --debug`
3. For Android emulator, use `10.0.2.2` instead of `localhost`
4. Restart emulators: `firebase emulators:start --clear`

### Test Flakiness
If tests are flaky:
1. Use `StandardTestDispatcher` instead of `UnconfinedTestDispatcher` for timing-sensitive tests
2. Add `advanceUntilIdle()` or `advanceTimeBy()` to control coroutine execution
3. Use Turbine's `awaitItem()` with timeout for Flow testing
4. Ensure proper cleanup in `@After` methods

### Hilt Injection Issues
If DI fails in tests:
1. Ensure `HiltTestRunner` is configured in `build.gradle.kts`
2. Annotate test class with `@HiltAndroidTest`
3. Add `@get:Rule val hiltRule = HiltAndroidRule(this)` to test class
4. Call `hiltRule.inject()` in `@Before` setup

## CI/CD Integration

### GitHub Actions Example
```yaml
name: Run Tests

on: [push, pull_request]

jobs:
  unit-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
      - name: Run unit tests
        run: ./gradlew test
      
  integration-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Install Firebase CLI
        run: npm install -g firebase-tools
      - name: Start Firebase Emulators
        run: firebase emulators:start --only auth,firestore &
      - name: Wait for emulators
        run: sleep 10
      - name: Run integration tests
        run: ./gradlew connectedAndroidTest
```

## Coverage Goals

- **Unit Tests**: 85%+ on domain and ViewModel layers
- **Integration Tests**: 80%+ on repositories
- **UI Tests**: 70%+ on critical screens
- **Overall**: 80%+ across entire project

## Best Practices

1. **Write tests first** for new features (TDD approach)
2. **Keep tests fast** - unit tests should run in < 5 minutes
3. **Isolate tests** - no shared state between tests
4. **Use descriptive names** - backtick style: `` `test something specific` ``
5. **Test behavior, not implementation** - focus on what, not how
6. **Mock external dependencies** - only use real Firebase in integration tests
7. **Clean up resources** - always sign out, clear data in `@After`
8. **Use factories** - MockDataFactory for consistent test data
9. **Test edge cases** - null values, empty lists, errors
10. **Monitor flakiness** - refactor flaky tests immediately

## Next Steps

1. Complete Phase 1: Foundation setup ✓
2. Phase 2: Domain layer unit tests
3. Phase 3: Repository integration tests
4. Phase 4: ViewModel unit tests
5. Phase 5: UI component tests
6. Phase 6: E2E integration tests
7. Phase 7: Performance & edge case tests

## Support

For questions or issues with testing setup:
1. Check this documentation
2. Review existing tests for examples
3. Check Firebase Emulator logs: `firebase emulators:start --debug`
4. Review test execution logs in Android Studio

