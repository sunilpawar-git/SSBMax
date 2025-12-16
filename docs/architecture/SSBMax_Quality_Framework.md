# SSBMax Quality Implementation Framework

## 🎯 SSBMax QUALITY MODE ACTIVATED

Before implementing [Phase X: Name], follow this comprehensive quality framework:

## 🚨 CRITICAL PRE-IMPLEMENTATION CHECKLIST

STOP and REFLECT before writing ANY code:

### 1. Architecture Compliance
- [ ] Domain Layer Isolation: Adding Android dependencies? (NO - domain uses Result<T>, no ErrorLogger)
- [ ] String Resources Policy: User-facing text? (If YES → use stringResource(R.string.key), never hardcoded)
- [ ] Navigation Pattern: Passing complex objects? (NO - use ID-based navigation only)
- [ ] StateFlow Updates: Modifying state? (If YES → use .update {} pattern, never .value = _state.value.copy())
- [ ] Error Handling: Which layer?
  • Domain layer → Result<T> only, no ErrorLogger
  • Data/Presentation → ErrorLogger for logging, string resources for user messages

### 2. Code Organization
- [ ] File Size: Will exceed 300 lines? (If YES → split into smaller files)
- [ ] Component Extraction: Composable >50 lines? (If YES → extract to separate component)
- [ ] Magic Numbers: Using number >2 times? (If YES → extract as named constant)
- [ ] Single Responsibility: Does this class/function do one thing well?

### 3. SSBMax-Specific Rules
- [ ] Subscription Limits: Affects test limits? (Verify against SubscriptionManager.getTestLimitForTier() - single source of truth)
- [ ] Firebase Paths: Using nested subcollections? (If YES → verify firestore.rules has nested match blocks)
- [ ] Compose Performance: Using lists? (If YES → add key = { it.id } and consider derivedStateOf)
- [ ] Test Patterns: Following ID-based navigation? (Result screens fetch data via ViewModel, not passed objects)

### 4. Quality Debt Prevention
- [ ] Commented Code: Leaving commented-out logic? (NO - delete immediately)
- [ ] Incomplete Features: Implementation complete? (NO → don't start, implement fully)
- [ ] Memory Leaks: Proper lifecycle management?
  • ViewModels use viewModelScope (not lifecycleScope)
  • Flows collected with collectAsStateWithLifecycle()
  • No static references to Context/Activity
  • Composables properly scoped to remember{}
- [ ] Accessibility: UI changes include?
  • Content descriptions for images/icons
  • Proper heading hierarchy (h1, h2, etc.)
  • Sufficient color contrast ratios
  • Screen reader friendly semantics

## ✅ IMPLEMENTATION STANDARDS

**Estimated Implementation Time: ___ hours**

### String Resources (MANDATORY)
❌ Text(text = "Test Completed Successfully")  // FORBIDDEN
✅ Text(text = stringResource(R.string.test_completed))  // REQUIRED

### StateFlow Updates (MANDATORY)
❌ _uiState.value = _uiState.value.copy(isLoading = true)  // Race condition
✅ _uiState.update { it.copy(isLoading = true) }  // Thread-safe

### Error Handling by Layer (MANDATORY)
```kotlin
// Domain Layer - STRICTLY NO Android dependencies
suspend fun invoke(): Result<Data> = try {
    val data = performOperation()
    Result.success(data)
} catch (e: Exception) {
    Result.failure(e) // NEVER use ErrorLogger here - domain is platform-independent
}

// Data/Presentation Layers - Use ErrorLogger
.onFailure { error ->
    ErrorLogger.log(error, "Failed to load data")
    _uiState.update { it.copy(error = getString(R.string.error_generic)) }
}
```

## 🚀 BUILD & CI VERIFICATION

Before pushing:
- [ ] `./gradlew lint` passes (no new lint errors)
- [ ] `./gradlew test` passes (unit tests)
- [ ] `./gradlew build` succeeds (compilation)
- [ ] No files >300 lines (count manually or use IDE metrics)
- [ ] Architecture tests pass (if implemented)

## 🔍 VERIFICATION CHECKLIST

After implementation, verify ALL pass before committing:

### Critical Architecture Checks
- [ ] Domain Isolation: No Context, ErrorLogger, or R.string in domain layer
- [ ] String Resources: All user-facing text uses stringResource() or getString()
- [ ] StateFlow Pattern: No .value = _state.value.copy() assignments
- [ ] Navigation: No complex objects passed (IDs only)
- [ ] ErrorLogger Usage: All exceptions logged via ErrorLogger (not printStackTrace)

### Lint Rule Compliance
- [ ] HardcodedTextDetector: No hardcoded strings in code (use string resources)
- [ ] PrintStackTraceDetector: No printStackTrace() calls (use ErrorLogger)
- [ ] StateFlowValueAssignmentDetector: No unsafe StateFlow assignments
- [ ] SingletonMutableStateDetector: No mutable singleton state holders

### Code Quality Checks
- [ ] File Sizes: All files ≤300 lines
- [ ] Magic Numbers: All numbers used >2 times extracted as constants
- [ ] Components: All composables >50 lines extracted
- [ ] Commented Code: Zero commented-out code blocks
- [ ] Test Coverage: All ViewModels have unit tests with proper mocking
- [ ] Repository Tests: Data operations tested with fake/test datasources
- [ ] UI Tests: Critical user flows covered (login, test completion, etc.)

## 🚨 FAILURE PROTOCOL

If ANY check fails:
1. STOP IMMEDIATELY - Do not commit or push
2. REFACTOR - Fix the failing checks
3. RE-VERIFY - Run full checklist again
4. REPEAT until ALL checks pass
5. Only then commit/push

**No exceptions. ZERO tech debt means ZERO compromises.**

## 📋 SSBMax-SPECIFIC PATTERNS

### Subscription Limits (Single Source of Truth)
```kotlin
val limit = subscriptionManager.getTestLimitForTier(TestType.SRT, user.tier)
// FREE: 1, PRO: 3, PREMIUM: unlimited
```

### Firebase Nested Subcollections
```
match /users/{userId} {
  allow read, write: if isOwner(userId);
  // CRITICAL: Nested subcollection rules
  match /data/{document} {
    allow read, write: if isOwner(userId);
  }
}
```

### Compose List Performance
```kotlin
LazyColumn {
    items(items = myList, key = { it.id }) { item ->  // ✅ REQUIRED
        MyItemCard(item)
    }
}
```

## 🎯 READY TO IMPLEMENT

With this framework, [Phase X] will be implemented with ZERO tech debt.
