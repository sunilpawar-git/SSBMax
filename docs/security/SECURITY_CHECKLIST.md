# SSBMax Security Implementation Checklist

> **Last Updated**: Nov 6, 2025  
> **Purpose**: Track security implementation status across all test types to ensure no ViewModels are left unprotected

---

## 📋 Test ViewModel Security Status

### ✅ **FULLY IMPLEMENTED** (5/8)

| Test Type | ViewModel | Auth Guard | Subscription Check | Security Logging | Usage Recording | Unit Tests |
|-----------|-----------|:----------:|:------------------:|:----------------:|:---------------:|:----------:|
| **OIR** | `OIRTestViewModel` | ✅ | ✅ | ✅ | ✅ | ✅ 18 tests |
| **WAT** | `WATTestViewModel` | ✅ | ✅ | ✅ | ✅ | ✅ 17 tests |
| **SRT** | `SRTTestViewModel` | ✅ | ✅ | ✅ | ✅ | ✅ 17 tests |
| **TAT** | `TATTestViewModel` | ✅ | ✅ | ✅ | ✅ | ✅ 17 tests |
| **PPDT** | `PPDTTestViewModel` | ✅ | ✅ | ✅ | ✅ | ✅ 18 tests |

---

### ⚠️ **PENDING IMPLEMENTATION** (3/8)

| Test Type | Status | ViewModel File | Priority | Notes |
|-----------|--------|----------------|----------|-------|
| **SD** (Self Description) | 🔴 **NOT CREATED** | `app/.../tests/sd/SDTestViewModel.kt` | HIGH | Core Phase 2 test |
| **GTO** (Group Testing) | 🔴 **NOT CREATED** | `app/.../tests/gto/GTOTestViewModel.kt` | HIGH | Core Phase 2 test |
| **IO** (Interview Officer) | 🔴 **NOT CREATED** | `app/.../tests/io/IOTestViewModel.kt` | MEDIUM | Phase 2 test |

---

## 🛡️ Required Security Components

When creating a new test ViewModel, **ALL** of these components are **MANDATORY**:

### 1. **Authentication Guard** (Critical)
```kotlin
// In loadTest() function
val user = observeCurrentUser().first()
val userId = user?.id ?: run {
    Log.e(TAG, "🚨 SECURITY: Unauthenticated test access blocked")
    securityLogger.logUnauthenticatedAccess(
        testType = TestType.[YOUR_TYPE],
        context = "[YourTest]ViewModel.loadTest"
    )
    _uiState.update { it.copy(error = "Authentication required...") }
    return@launch
}
```

### 2. **Subscription Limit Check** (Critical)
```kotlin
// After authentication, before loading test
val eligibility = subscriptionManager.canTakeTest(userId, TestType.[YOUR_TYPE])
when (eligibility) {
    is TestEligibility.LimitReached -> {
        _uiState.update { it.copy(
            isLimitReached = true,
            subscriptionTier = eligibility.subscriptionTier,
            testsLimit = eligibility.testsLimit,
            testsUsed = eligibility.testsUsed,
            resetsAt = eligibility.resetsAt
        ) }
        return@launch
    }
    is TestEligibility.Allowed -> { /* proceed */ }
}
```

### 3. **Performance Recording** (Required for Analytics)
```kotlin
// In submitTest() function, after calculating score
difficultyManager.recordPerformance(
    userId = userId,
    testType = TestType.[YOUR_TYPE],
    score = calculatedScore,
    timeSpent = timeSpentMillis,
    submissionId = submissionId
)
```

### 4. **Usage Recording** (Required for Subscription)
```kotlin
// In submitTest() function, after recording performance
subscriptionManager.recordTestUsage(
    userId = userId,
    testType = TestType.[YOUR_TYPE],
    submissionId = submissionId
)
```

### 5. **Constructor Dependencies** (Required)
```kotlin
@HiltViewModel
class YourTestViewModel @Inject constructor(
    private val observeCurrentUser: ObserveCurrentUserUseCase,    // Auth
    private val subscriptionManager: SubscriptionManager,         // Limits
    private val difficultyManager: DifficultyProgressionManager,  // Analytics
    private val securityLogger: SecurityEventLogger,              // Logging
    // ... other dependencies
) : ViewModel()
```

### 6. **UI State Fields** (Required)
```kotlin
data class YourTestUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    
    // Subscription fields (REQUIRED)
    val isLimitReached: Boolean = false,
    val subscriptionTier: String = "",
    val testsLimit: Int = 0,
    val testsUsed: Int = 0,
    val resetsAt: String = "",
    
    // ... test-specific fields
)
```

---

## 🧪 Required Unit Tests

For each ViewModel, create tests covering:

### Security Tests (MANDATORY)
- ✅ **Unauthenticated access blocked** - Verify `observeCurrentUser()` returns null → test blocked
- ✅ **Security logging called** - Verify `securityLogger.logUnauthenticatedAccess()` invoked
- ✅ **Limit enforcement** - Verify subscription limit blocks test when reached
- ✅ **Limit dialog shown** - Verify UI state updated with `isLimitReached = true`

### Analytics Tests (MANDATORY)
- ✅ **Performance recorded** - Verify `difficultyManager.recordPerformance()` called after submission
- ✅ **Usage recorded** - Verify `subscriptionManager.recordTestUsage()` called after submission
- ✅ **Edge cases** - Test 0% score, 100% score, partial completion

### Reference Test Files
- `app/src/test/.../wat/WATTestViewModelTest.kt` (17 tests)
- `app/src/test/.../tat/TATTestViewModelTest.kt` (17 tests)
- `app/src/test/.../ppdt/PPDTTestViewModelTest.kt` (18 tests)

---

## 🔍 How to Verify Implementation

### Step 1: Check ViewModel File Exists
```bash
ls -la app/src/main/kotlin/com/ssbmax/ui/tests/[testtype]/
```

### Step 2: Verify Security Components
Run this grep command to check for auth guard:
```bash
grep -n "observeCurrentUser().first()" app/src/main/kotlin/com/ssbmax/ui/tests/[testtype]/*ViewModel.kt
```

Expected output: Should find the authentication check in `loadTest()` function.

### Step 3: Verify Security Logging
```bash
grep -n "securityLogger.logUnauthenticatedAccess" app/src/main/kotlin/com/ssbmax/ui/tests/[testtype]/*ViewModel.kt
```

Expected output: Should find the security logging call.

### Step 4: Verify Subscription Check
```bash
grep -n "subscriptionManager.canTakeTest" app/src/main/kotlin/com/ssbmax/ui/tests/[testtype]/*ViewModel.kt
```

Expected output: Should find the eligibility check.

### Step 5: Run Unit Tests
```bash
./gradle.sh :app:testDebugUnitTest --tests com.ssbmax.ui.tests.[testtype].*
```

Expected: All tests pass, including security tests.

---

## 🚨 Critical Reminders

### Before Creating New ViewModel:
1. ✅ Read this checklist completely
2. ✅ Review reference implementation: `WATTestViewModel.kt`
3. ✅ Copy security pattern from existing ViewModel
4. ✅ Add all 6 required components (see above)
5. ✅ Create comprehensive unit tests (minimum 15 tests)
6. ✅ Update this checklist to mark as ✅ IMPLEMENTED

### Before Deploying to Production:
1. ✅ All ViewModels must be marked as ✅ FULLY IMPLEMENTED
2. ✅ All unit tests must pass (no skipped/ignored tests)
3. ✅ Manual UAT testing on real device (try cache clearing)
4. ✅ Firebase Analytics configured to monitor security events
5. ✅ Firestore security rules deployed and tested

---

## 📚 Reference Documentation

### Code Files
- **Security Logger**: `core/data/.../security/SecurityEventLogger.kt` (has complete TODO in header)
- **Subscription Manager**: `core/data/.../repository/SubscriptionManager.kt`
- **Firestore Rules**: `firestore.rules` (anti-decrement validation)
- **Test Types Enum**: `core/domain/.../model/SSBPhase.kt`

### Architecture Documents
- **Security Architecture**: See `SubscriptionManager.kt` header comments
- **MVVM Pattern**: Follow existing ViewModels
- **Testing Strategy**: See `BaseViewModelTest.kt` for setup

---

## 📝 Implementation Timeline

| Date | Action | Status |
|------|--------|--------|
| Nov 5, 2025 | OIR security implemented | ✅ |
| Nov 5, 2025 | WAT security implemented | ✅ |
| Nov 5, 2025 | SRT security implemented | ✅ |
| Nov 6, 2025 | TAT security implemented | ✅ |
| Nov 6, 2025 | PPDT security implemented | ✅ |
| **TBD** | **SD security implementation** | ⏳ **PENDING** |
| **TBD** | **GTO security implementation** | ⏳ **PENDING** |
| **TBD** | **IO security implementation** | ⏳ **PENDING** |

---

## ✅ Sign-Off Checklist (Before Marking Complete)

When implementing SD/GTO/IO ViewModels, verify:

- [ ] ViewModel file created
- [ ] All 6 security components implemented (auth, limits, logging, recording)
- [ ] UI state includes subscription fields
- [ ] Unit test file created with minimum 15 tests
- [ ] Security tests pass (auth guard, logging, limits)
- [ ] Analytics tests pass (performance recording, usage recording)
- [ ] Manual testing confirms test can be taken
- [ ] Manual testing confirms limit blocks after threshold
- [ ] Manual testing confirms cache clearing doesn't bypass limit
- [ ] This checklist updated to mark test as ✅ IMPLEMENTED
- [ ] Git commit with tag: `security: Add [TestType] authentication and limits`

---

> **⚠️ IMPORTANT**: Do NOT skip any component. Every test must have identical security protection.  
> **💡 TIP**: When in doubt, copy from `WATTestViewModel.kt` - it's the reference implementation.

---

**Document maintained by**: Development Team  
**Review frequency**: On every new ViewModel creation  
**Last audit**: Nov 6, 2025 - 5/8 tests secured
