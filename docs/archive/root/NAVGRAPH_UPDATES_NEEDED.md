# NavGraph Updates for Test Result Handler

## Current Status

✅ **ViewModels Updated** (All 5 tests):
- TAT, WAT, SRT, PPDT, OIR all fetch `subscriptionType` and include it in UI state

✅ **Screens Updated**:
- TAT: Updated ✅
- WAT: Updated ✅
- SRT: Needs update ⏳
- PPDT: Needs update ⏳
- OIR: Needs update ⏳

✅ **TAT NavGraph**: Already using `TestResultHandler` ✅

---

## NavGraph Updates Required

### File: `app/src/main/kotlin/com/ssbmax/navigation/NavGraph.kt`

### 1. WAT Test Route (Update)

**Current** (line ~396):
```kotlin
composable(route = SSBMaxDestinations.WATTest.route, ...) {
    WATTestScreen(
        testId = testId,
        onTestComplete = { submissionId ->
            navController.navigate(SSBMaxDestinations.WATSubmissionResult.createRoute(submissionId)) {
                popUpTo(SSBMaxDestinations.WATTest.route) { inclusive = true }
            }
        },
        onNavigateBack = { navController.navigateUp() }
    )
}
```

**Update to**:
```kotlin
composable(route = SSBMaxDestinations.WATTest.route, ...) {
    WATTestScreen(
        testId = testId,
        onTestComplete = { submissionId, subscriptionType ->
            com.ssbmax.ui.tests.common.TestResultHandler.handleTestSubmission(
                submissionId = submissionId,
                subscriptionType = subscriptionType,
                testType = com.ssbmax.core.domain.model.TestType.WAT,
                navController = navController
            )
        },
        onNavigateBack = { navController.navigateUp() }
    )
}
```

---

### 2. SRT Test Route (Update)

**Current** (line ~432):
```kotlin
composable(route = SSBMaxDestinations.SRTTest.route, ...) {
    SRTTestScreen(
        testId = testId,
        onTestComplete = { submissionId ->
            navController.navigate(SSBMaxDestinations.SRTSubmissionResult.createRoute(submissionId)) {
                popUpTo(SSBMaxDestinations.SRTTest.route) { inclusive = true }
            }
        },
        onNavigateBack = { navController.navigateUp() }
    )
}
```

**Update to**:
```kotlin
composable(route = SSBMaxDestinations.SRTTest.route, ...) {
    SRTTestScreen(
        testId = testId,
        onTestComplete = { submissionId, subscriptionType ->
            com.ssbmax.ui.tests.common.TestResultHandler.handleTestSubmission(
                submissionId = submissionId,
                subscriptionType = subscriptionType,
                testType = com.ssbmax.core.domain.model.TestType.SRT,
                navController = navController
            )
        },
        onNavigateBack = { navController.navigateUp() }
    )
}
```

---

### 3. PPDT Test Route (Update)

**Current** (line ~324):
```kotlin
composable(route = SSBMaxDestinations.PPDTTest.route, ...) {
    PPDTTestScreen(
        testId = testId,
        onTestComplete = { submissionId ->
            navController.navigate(SSBMaxDestinations.PPDTSubmissionResult.createRoute(submissionId)) {
                popUpTo(SSBMaxDestinations.PPDTTest.route) { inclusive = true }
            }
        },
        onNavigateBack = { navController.navigateUp() }
    )
}
```

**Update to**:
```kotlin
composable(route = SSBMaxDestinations.PPDTTest.route, ...) {
    PPDTTestScreen(
        testId = testId,
        onTestComplete = { submissionId, subscriptionType ->
            com.ssbmax.ui.tests.common.TestResultHandler.handleTestSubmission(
                submissionId = submissionId,
                subscriptionType = subscriptionType,
                testType = com.ssbmax.core.domain.model.TestType.PPDT,
                navController = navController
            )
        },
        onNavigateBack = { navController.navigateUp() }
    )
}
```

---

### 4. OIR Test Route (Update)

**Note**: OIR uses `onTestComplete` with `sessionId` instead of `submissionId`

**Current** (line ~284):
```kotlin
composable(route = SSBMaxDestinations.OIRTest.route, ...) {
    OIRTestScreen(
        onTestComplete = { sessionId ->
            navController.navigate(SSBMaxDestinations.OIRTestResult.createRoute(sessionId)) {
                popUpTo(SSBMaxDestinations.OIRTest.route) { inclusive = true }
            }
        },
        onNavigateBack = { navController.navigateUp() }
    )
}
```

**Update to**:
```kotlin
composable(route = SSBMaxDestinations.OIRTest.route, ...) {
    OIRTestScreen(
        onTestComplete = { sessionId, subscriptionType ->
            com.ssbmax.ui.tests.common.TestResultHandler.handleTestSubmission(
                submissionId = sessionId,
                subscriptionType = subscriptionType,
                testType = com.ssbmax.core.domain.model.TestType.OIR,
                navController = navController
            )
        },
        onNavigateBack = { navController.navigateUp() }
    )
}
```

---

## Screen Signature Updates Still Needed

### SRTTestScreen.kt
```kotlin
@Composable
fun SRTTestScreen(
    testId: String,
    onTestComplete: (String, com.ssbmax.core.domain.model.SubscriptionType) -> Unit = { _, _ -> },
    onNavigateBack: () -> Unit = {},
    viewModel: SRTTestViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // ...
    
    // Handle completion
    LaunchedEffect(uiState.isSubmitted) {
        if (uiState.isSubmitted && uiState.submissionId != null && uiState.subscriptionType != null) {
            onTestComplete(uiState.submissionId!!, uiState.subscriptionType!!)
        }
    }
}
```

### PPDTTestScreen.kt
```kotlin
@Composable
fun PPDTTestScreen(
    testId: String,
    onTestComplete: (String, com.ssbmax.core.domain.model.SubscriptionType) -> Unit = { _, _ -> },
    onNavigateBack: () -> Unit = {},
    viewModel: PPDTTestViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // ...
    
    // Handle completion
    LaunchedEffect(uiState.isSubmitted) {
        if (uiState.isSubmitted && uiState.submissionId != null && uiState.subscriptionType != null) {
            onTestComplete(uiState.submissionId!!, uiState.subscriptionType!!)
        }
    }
}
```

### OIRTestScreen.kt
**Note**: OIR uses `isCompleted` and `sessionId` instead of `isSubmitted` and `submissionId`

```kotlin
@Composable
fun OIRTestScreen(
    onTestComplete: (String, com.ssbmax.core.domain.model.SubscriptionType) -> Unit = { _, _ -> },
    onNavigateBack: () -> Unit = {},
    viewModel: OIRTestViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // ...
    
    // Handle completion
    LaunchedEffect(uiState.isCompleted) {
        if (uiState.isCompleted && uiState.sessionId != null && uiState.subscriptionType != null) {
            onTestComplete(uiState.sessionId!!, uiState.subscriptionType!!)
        }
    }
}
```

---

## Summary

**Remaining tasks for Step 10**:
1. Update SRTTestScreen.kt signature and LaunchedEffect
2. Update PPDTTestScreen.kt signature and LaunchedEffect
3. Update OIRTestScreen.kt signature and LaunchedEffect (note: isCompleted/sessionId)
4. Update NavGraph.kt for WAT, SRT, PPDT, OIR routes

**After this**, all 5 tests will route through `TestResultHandler` based on subscription type!

