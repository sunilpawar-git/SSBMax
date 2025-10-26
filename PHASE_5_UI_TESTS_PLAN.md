# Phase 5: UI Component Tests - Implementation Plan

## 🎯 **Overview**

Test user-facing components and critical user journeys using Jetpack Compose Testing and Espresso.

**Goal**: Ensure UI works correctly from user perspective  
**Estimated Tests**: 40-50 UI tests  
**Estimated Time**: 3-4 hours  

---

## 📊 **Test Categories**

### **5.1: Authentication Flow Tests (8 tests)**
**Priority**: HIGH - First user experience

- ✅ Login screen displays correctly
- ✅ Login with valid credentials succeeds
- ✅ Login with invalid credentials shows error
- ✅ Registration flow completes successfully
- ✅ Password visibility toggle works
- ✅ Forgot password navigation works
- ✅ Google Sign-In button appears
- ✅ Error messages display properly

**File**: `LoginScreenTest.kt`, `RegistrationScreenTest.kt`

---

### **5.2: Test Taking Flow Tests (12 tests)**
**Priority**: HIGH - Core functionality

#### TAT Test Flow (3 tests)
- ✅ TAT test starts and displays image
- ✅ Story input works and saves
- ✅ Timer counts down and auto-advances
- ✅ Test completes and shows results

#### WAT Test Flow (2 tests)
- ✅ Words display rapidly (15s each)
- ✅ Responses are recorded
- ✅ Test completes after all words

#### OIR Test Flow (3 tests)
- ✅ Questions display with options
- ✅ Selecting answer shows feedback
- ✅ Navigation between questions works
- ✅ Test submits successfully

#### SRT Test Flow (2 tests)
- ✅ Situations display correctly
- ✅ Response input and submission works

**Files**: `TATTestScreenTest.kt`, `WATTestScreenTest.kt`, `OIRTestScreenTest.kt`, `SRTTestScreenTest.kt`

---

### **5.3: Navigation Tests (8 tests)**
**Priority**: HIGH - User journey

- ✅ Bottom navigation switches screens
- ✅ Back button navigation works
- ✅ Deep linking to tests works
- ✅ Navigation drawer opens/closes
- ✅ Test selection navigates correctly
- ✅ Profile screen navigation works
- ✅ Study materials navigation works
- ✅ Back stack is maintained correctly

**File**: `NavigationTest.kt`

---

### **5.4: Dashboard/Home Screen Tests (6 tests)**
**Priority**: MEDIUM - User engagement

- ✅ Phase progress ribbons display
- ✅ Test cards are clickable
- ✅ Progress percentages show correctly
- ✅ User name displays
- ✅ Recent activity shows
- ✅ Quick actions work

**File**: `StudentHomeScreenTest.kt`

---

### **5.5: Study Materials Tests (5 tests)**
**Priority**: MEDIUM - Learning experience

- ✅ Topic list displays
- ✅ Topic detail screen loads
- ✅ Markdown content renders
- ✅ Images load correctly
- ✅ Bookmarking works

**File**: `StudyMaterialsScreenTest.kt`, `TopicScreenTest.kt`

---

### **5.6: Profile & Settings Tests (5 tests)**
**Priority**: MEDIUM - User management

- ✅ Profile screen displays user data
- ✅ Profile editing works
- ✅ Statistics display correctly
- ✅ Recent tests show
- ✅ Logout works

**File**: `StudentProfileScreenTest.kt`

---

### **5.7: UI Component Tests (6 tests)**
**Priority**: MEDIUM - Reusable components

- ✅ Phase progress ribbon renders
- ✅ Test card component displays
- ✅ Timer component counts down
- ✅ Loading states show spinner
- ✅ Error states show messages
- ✅ Empty states show properly

**File**: `ComponentsTest.kt`

---

## 🛠️ **Testing Infrastructure Needed**

### Dependencies (add to `app/build.gradle.kts`):
```kotlin
androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.5.4")
androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
androidTestImplementation("androidx.test:runner:1.5.2")
androidTestImplementation("androidx.test:rules:1.5.0")
androidTestImplementation("io.mockk:mockk-android:1.13.8")
debugImplementation("androidx.compose.ui:ui-test-manifest:1.5.4")
```

### Test Rules Needed:
1. **ComposeTestRule** - For Compose UI testing
2. **ActivityScenarioRule** - For Activity testing
3. **HiltAndroidRule** - For DI in tests
4. **NavigationTestRule** - For navigation testing

---

## 📁 **File Structure**

```
app/src/androidTest/kotlin/com/ssbmax/
├── ui/
│   ├── auth/
│   │   ├── LoginScreenTest.kt
│   │   └── RegistrationScreenTest.kt
│   ├── tests/
│   │   ├── TATTestScreenTest.kt
│   │   ├── WATTestScreenTest.kt
│   │   ├── OIRTestScreenTest.kt
│   │   └── SRTTestScreenTest.kt
│   ├── home/
│   │   └── StudentHomeScreenTest.kt
│   ├── topic/
│   │   └── TopicScreenTest.kt
│   ├── profile/
│   │   └── StudentProfileScreenTest.kt
│   └── navigation/
│       └── NavigationTest.kt
└── testing/
    ├── ComposeTestRule.kt
    └── TestHelpers.kt
```

---

## 🎯 **Implementation Strategy**

### Phase 5.1: Setup & Auth Tests (Priority 1)
**Time**: ~1 hour  
**Tests**: 8 tests

1. Set up Compose test infrastructure
2. Create test helpers and rules
3. Implement auth flow tests

### Phase 5.2: Core Test Flow Tests (Priority 1)
**Time**: ~1.5 hours  
**Tests**: 12 tests

1. TAT test flow (most complex)
2. WAT test flow (rapid progression)
3. OIR test flow (question navigation)
4. SRT test flow

### Phase 5.3: Navigation Tests (Priority 1)
**Time**: ~45 minutes  
**Tests**: 8 tests

1. Bottom navigation
2. Back button handling
3. Deep linking

### Phase 5.4-5.7: Supporting Screens (Priority 2)
**Time**: ~1 hour  
**Tests**: 22 tests

1. Dashboard/home
2. Study materials
3. Profile
4. Reusable components

---

## 🎨 **Testing Patterns**

### Pattern 1: Compose UI Test
```kotlin
@Test
fun testButtonClick() {
    composeTestRule.setContent {
        MyScreen()
    }
    
    composeTestRule
        .onNodeWithText("Start Test")
        .performClick()
    
    composeTestRule
        .onNodeWithText("Test Started")
        .assertIsDisplayed()
}
```

### Pattern 2: Navigation Test
```kotlin
@Test
fun testNavigation() {
    val navController = TestNavHostController(context)
    
    composeTestRule.setContent {
        NavHost(navController, startDestination = "home") {
            // navigation graph
        }
    }
    
    // Perform navigation
    // Assert destination
}
```

### Pattern 3: User Flow Test
```kotlin
@Test
fun completeTestFlow() {
    // 1. Start test
    onNodeWithText("Start TAT").performClick()
    
    // 2. View image
    onNodeWithTag("tat-image").assertIsDisplayed()
    
    // 3. Enter story
    onNodeWithTag("story-input").performTextInput("Test story")
    
    // 4. Submit
    onNodeWithText("Submit").performClick()
    
    // 5. Verify completion
    onNodeWithText("Test Completed").assertIsDisplayed()
}
```

---

## ✅ **Success Criteria**

- ✅ 40+ UI tests passing
- ✅ All critical user journeys tested
- ✅ 80%+ pass rate (UI tests can be flaky)
- ✅ Tests run in < 5 minutes
- ✅ No manual intervention needed

---

## 📊 **Expected Outcomes**

| Category | Tests | Pass Rate Target |
|----------|-------|------------------|
| Auth Flow | 8 | 90%+ |
| Test Taking | 12 | 85%+ |
| Navigation | 8 | 95%+ |
| Dashboard | 6 | 90%+ |
| Study Materials | 5 | 85%+ |
| Profile | 5 | 90%+ |
| Components | 6 | 95%+ |
| **TOTAL** | **50** | **90%+** |

---

## 🚀 **Let's Start!**

Ready to implement Phase 5.1 (Setup & Auth Tests)?

This will give us the foundation for all UI testing and ensure the first user experience works perfectly!

