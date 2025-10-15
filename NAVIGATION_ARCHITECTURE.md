# SSBMax Navigation Architecture

## Visual Component Hierarchy

```
MainActivity
    │
    └── SSBMaxTheme
            │
            └── SSBMaxApp ──────────────────┐
                    │                       │
                    ├── Current User State  │
                    │   (mock or real)      │
                    │                       │
                    └── Route Detection ────┤
                            │               │
                            ├─── Auth Screens (no scaffold)
                            │       ├── SplashScreen
                            │       ├── LoginScreen
                            │       └── RoleSelectionScreen
                            │
                            └─── Main Screens (with scaffold)
                                    │
                                    └── SSBMaxScaffold
                                            │
                                            ├── ModalNavigationDrawer ─────┐
                                            │       │                      │
                                            │       └── SSBMaxDrawer       │
                                            │               │              │
                                            │               ├── Header     │
                                            │               ├── Menu Items │
                                            │               └── Actions    │
                                            │                              │
                                            └── Scaffold ──────────────────┤
                                                    │                      │
                                                    ├── Bottom Bar         │
                                                    │   (SSBMaxBottomBar)  │
                                                    │                      │
                                                    └── Content            │
                                                            │              │
                                                            └── NavHost ───┤
                                                                    │      │
                                                                    └── Screens
                                                                        (receives drawer state)
```

---

## Navigation Flow Chart

### Student Navigation Map

```
┌─────────────────────────────────────────────────────────────────┐
│                        SSBMax Student                            │
│                                                                  │
│  ╔══════════════════════════════════════════════════════════╗   │
│  ║  Navigation Drawer (☰)                                    ║   │
│  ║                                                            ║   │
│  ║  👤 SSB Aspirant                                          ║   │
│  ║     user@example.com                                      ║   │
│  ║     [Student]                                             ║   │
│  ║  ──────────────────────────────────────────────────────   ║   │
│  ║  SSB TESTS                                                ║   │
│  ║    📋 Phase 1 - Screening  ──────────┐                   ║   │
│  ║       OIR & PPDT                     │                    ║   │
│  ║    🧠 Phase 2 - Assessment ──────────┤                   ║   │
│  ║       Psychology, GTO, IO            │                    ║   │
│  ║                                       │                    ║   │
│  ║  QUICK ACCESS                         │                    ║   │
│  ║    📝 OIR Test ────────────────────┐  │                   ║   │
│  ║    🖼️  PPDT ───────────────────────┤  │                   ║   │
│  ║    ✏️  TAT ────────────────────────┤  │                   ║   │
│  ║    💭 WAT ────────────────────────┤  │                   ║   │
│  ║    🎯 SRT ────────────────────────┘  │                   ║   │
│  ║                                       │                    ║   │
│  ║  👥 My Batches ───────────────────────┤                   ║   │
│  ║                                       │                    ║   │
│  ║  ACCOUNT                              │                    ║   │
│  ║    ⚙️  Settings ───────────────────┐  │                   ║   │
│  ║    🚪 Sign Out ────────────────────┘  │                   ║   │
│  ╚═══════════════════════════════════════╩═══════════════════╝   │
│                                          │                        │
│  ┌───────────────────────────────────────┼──────────────────────┐│
│  │ Main Content Area                     │                      ││
│  │                                       ▼                      ││
│  │  [Current Screen - e.g., StudentHomeScreen]                 ││
│  │                                                              ││
│  │  ┌─────────────────────────────────────────────────────┐   ││
│  │  │  YOUR PROGRESS                                       │   ││
│  │  │  ┌──────────────────┬──────────────────┐            │   ││
│  │  │  │ PHASE 1 - 60%    │ PHASE 2 - 30%    │            │   ││
│  │  │  │ ✓ OIR: 85%       │ □ Psychology     │            │   ││
│  │  │  │ ✓ PPDT: 72%      │ □ GTO Tests      │            │   ││
│  │  │  └──────────────────┴──────────────────┘            │   ││
│  │  └─────────────────────────────────────────────────────┘   ││
│  │                                                              ││
│  └──────────────────────────────────────────────────────────────┘│
│                                                                  │
│  ┌──────────────────────────────────────────────────────────────┐│
│  │ Bottom Navigation                                            ││
│  │  [🏠 Home] [📝 Tests] [📚 Study] [👤 Profile]               ││
│  └──────────────────────────────────────────────────────────────┘│
└───────────────────────────────────────────────────────────────────┘
```

### Instructor Navigation Map

```
┌─────────────────────────────────────────────────────────────────┐
│                      SSBMax Instructor                           │
│                                                                  │
│  ╔══════════════════════════════════════════════════════════╗   │
│  ║  Navigation Drawer (☰)                                    ║   │
│  ║                                                            ║   │
│  ║  👤 Instructor Name                                       ║   │
│  ║     instructor@example.com                                ║   │
│  ║     [Instructor]                                          ║   │
│  ║  ──────────────────────────────────────────────────────   ║   │
│  ║  INSTRUCTOR TOOLS                                         ║   │
│  ║    ⏰ Pending Grading [12] ──────────┐                   ║   │
│  ║    👥 All Students ──────────────────┤                   ║   │
│  ║    📋 Batch Management ──────────────┤                   ║   │
│  ║    📊 Analytics Dashboard ───────────┘                   ║   │
│  ║                                       │                    ║   │
│  ║  ACCOUNT                              │                    ║   │
│  ║    ⚙️  Settings ───────────────────┐  │                   ║   │
│  ║    🚪 Sign Out ────────────────────┘  │                   ║   │
│  ╚═══════════════════════════════════════╩═══════════════════╝   │
│                                          │                        │
│  ┌───────────────────────────────────────┼──────────────────────┐│
│  │ Main Content Area                     │                      ││
│  │                                       ▼                      ││
│  │  [Current Screen - e.g., InstructorHomeScreen]              ││
│  │                                                              ││
│  │  ┌─────────────────────────────────────────────────────┐   ││
│  │  │  MY STUDENTS (12 Active)                            │   ││
│  │  │  ┌────────┬────────┬────────┬────────┐             │   ││
│  │  │  │ Student│ Student│ Student│ Student│             │   ││
│  │  │  │   1    │   2    │   3    │   4    │             │   ││
│  │  │  │ 85%    │ 72%    │ 68%    │ 91%    │             │   ││
│  │  │  └────────┴────────┴────────┴────────┘             │   ││
│  │  │                                                      │   ││
│  │  │  PENDING GRADING TASKS (12)                         │   ││
│  │  │  • Student A - TAT Test (2h ago)                    │   ││
│  │  │  • Student B - PPDT (4h ago)                        │   ││
│  │  └─────────────────────────────────────────────────────┘   ││
│  │                                                              ││
│  └──────────────────────────────────────────────────────────────┘│
│                                                                  │
│  ┌──────────────────────────────────────────────────────────────┐│
│  │ Bottom Navigation                                            ││
│  │  [📊 Home] [👥 Students] [✅ Grading] [📈 Analytics]        ││
│  └──────────────────────────────────────────────────────────────┘│
└───────────────────────────────────────────────────────────────────┘
```

---

## Navigation State Machine

### Student States
```
┌─────────────┐
│   Splash    │
└──────┬──────┘
       │ (auto-check auth)
       ▼
┌─────────────┐     ┌──────────────────┐
│   Login     │────▶│ Role Selection   │
│  (Google)   │     │ (if new user)    │
└──────┬──────┘     └────────┬─────────┘
       │                     │
       │ (authenticated)     │
       ▼                     │
       └─────────────────────┘
                 │
                 ▼
       ┌─────────────────────┐
       │   Student Home      │◀───┐
       │  (with scaffold)    │    │
       └──────┬──────────────┘    │
              │                    │
              ├─ Drawer Actions ───┤
              │  ├─ Phase 1 Detail │
              │  ├─ Phase 2 Detail │
              │  ├─ Quick Tests    │
              │  ├─ My Batches     │
              │  └─ Settings       │
              │                    │
              └─ Bottom Nav ───────┤
                 ├─ Tests Screen   │
                 ├─ Study Screen   │
                 └─ Profile Screen │
                         │          │
                         └──────────┘
```

### Instructor States
```
┌─────────────┐
│   Splash    │
└──────┬──────┘
       │ (auto-check auth)
       ▼
┌─────────────┐     ┌──────────────────┐
│   Login     │────▶│ Role Selection   │
│  (Google)   │     │ (if new user)    │
└──────┬──────┘     └────────┬─────────┘
       │                     │
       │ (authenticated)     │
       ▼                     │
       └─────────────────────┘
                 │
                 ▼
       ┌─────────────────────┐
       │ Instructor Home     │◀───┐
       │  (with scaffold)    │    │
       └──────┬──────────────┘    │
              │                    │
              ├─ Drawer Actions ───┤
              │  ├─ Pending Grading│
              │  ├─ All Students   │
              │  ├─ Batch Mgmt     │
              │  └─ Analytics      │
              │                    │
              └─ Bottom Nav ───────┤
                 ├─ Students List  │
                 ├─ Grading Queue  │
                 └─ Analytics      │
                         │          │
                         └──────────┘
```

---

## Drawer Menu Structure (JSON-like)

### Student Drawer
```json
{
  "header": {
    "avatar": "User initials or photo",
    "name": "SSB Aspirant",
    "email": "user@example.com",
    "role": "Student"
  },
  "sections": [
    {
      "title": "SSB Tests",
      "items": [
        {
          "icon": "checklist",
          "title": "Phase 1 - Screening",
          "subtitle": "OIR & PPDT",
          "action": "navigate_to_phase_1"
        },
        {
          "icon": "psychology",
          "title": "Phase 2 - Assessment",
          "subtitle": "Psychology, GTO, IO",
          "action": "navigate_to_phase_2"
        }
      ]
    },
    {
      "title": "Quick Access",
      "items": [
        { "icon": "quiz", "title": "OIR Test", "action": "navigate_to_oir" },
        { "icon": "image", "title": "PPDT", "action": "navigate_to_ppdt" },
        { "icon": "edit_note", "title": "TAT", "action": "navigate_to_tat" },
        { "icon": "edit_note", "title": "WAT", "action": "navigate_to_wat" },
        { "icon": "edit_note", "title": "SRT", "action": "navigate_to_srt" }
      ]
    },
    {
      "title": "Other",
      "items": [
        { "icon": "groups", "title": "My Batches", "action": "navigate_to_batches" }
      ]
    },
    {
      "title": "Account",
      "items": [
        { "icon": "settings", "title": "Settings", "action": "navigate_to_settings" },
        { "icon": "logout", "title": "Sign Out", "action": "sign_out", "color": "error" }
      ]
    }
  ],
  "footer": {
    "version": "1.0.0"
  }
}
```

### Instructor Drawer
```json
{
  "header": {
    "avatar": "User initials or photo",
    "name": "Instructor Name",
    "email": "instructor@example.com",
    "role": "Instructor"
  },
  "sections": [
    {
      "title": "Instructor Tools",
      "items": [
        {
          "icon": "assignment_late",
          "title": "Pending Grading",
          "badge": "12",
          "action": "navigate_to_grading"
        },
        { "icon": "people", "title": "All Students", "action": "navigate_to_students" },
        { "icon": "groups", "title": "Batch Management", "action": "navigate_to_batches" },
        { "icon": "analytics", "title": "Analytics Dashboard", "action": "navigate_to_analytics" }
      ]
    },
    {
      "title": "Account",
      "items": [
        { "icon": "settings", "title": "Settings", "action": "navigate_to_settings" },
        { "icon": "logout", "title": "Sign Out", "action": "sign_out", "color": "error" }
      ]
    }
  ],
  "footer": {
    "version": "1.0.0"
  }
}
```

---

## Bottom Navigation Structure

### Student Bottom Nav
```json
{
  "items": [
    { "route": "student_home", "label": "Home", "icon": "home" },
    { "route": "student_tests", "label": "Tests", "icon": "quiz" },
    { "route": "student_study", "label": "Study", "icon": "menu_book" },
    { "route": "student_profile", "label": "Profile", "icon": "person" }
  ]
}
```

### Instructor Bottom Nav
```json
{
  "items": [
    { "route": "instructor_home", "label": "Home", "icon": "dashboard" },
    { "route": "instructor_students", "label": "Students", "icon": "people" },
    { "route": "instructor_grading", "label": "Grading", "icon": "assignment_turned_in" },
    { "route": "instructor_analytics", "label": "Analytics", "icon": "bar_chart" }
  ]
}
```

---

## Scaffold Logic Flow

```kotlin
SSBMaxScaffold(navController, user) {
    
    // 1. Determine current route
    currentRoute = navController.currentRoute
    
    // 2. Check if bottom bar should show
    showBottomBar = currentRoute in [
        student_home, student_tests, student_study, student_profile,
        instructor_home, instructor_students, instructor_grading, instructor_analytics
    ]
    
    // 3. Check if drawer should be available
    showDrawer = currentRoute NOT in [
        splash, login, role_selection
    ]
    
    // 4. Render structure
    ModalNavigationDrawer(
        enabled = showDrawer,
        drawerContent = SSBMaxDrawer(user)
    ) {
        Scaffold(
            bottomBar = if (showBottomBar) SSBMaxBottomBar()
        ) {
            NavHost(navController) {
                // All screens
            }
        }
    }
}
```

---

## Navigation Callbacks

### From Home Screens
```kotlin
StudentHomeScreen(
    onNavigateToTest = { testType -> ... },
    onNavigateToPhaseDetail = { phase -> ... },
    onNavigateToStudy = { ... },
    onOpenDrawer = { drawerState.open() }
)

InstructorHomeScreen(
    onNavigateToStudent = { studentId -> ... },
    onNavigateToGrading = { ... },
    onNavigateToBatchDetail = { batchId -> ... },
    onNavigateToCreateBatch = { ... },
    onOpenDrawer = { drawerState.open() }
)
```

### From Drawer
```kotlin
SSBMaxDrawer(
    onNavigateToPhase = { phase -> navController.navigate(...) },
    onNavigateToTest = { testType -> navController.navigate(...) },
    onNavigateToBatches = { navController.navigate(...) },
    onNavigateToSettings = { navController.navigate(...) },
    onNavigateToPendingGrading = { navController.navigate(...) },
    onNavigateToAnalytics = { navController.navigate(...) },
    onSwitchRole = { /* TODO: Implement */ },
    onSignOut = { navController.navigate(login) }
)
```

### From Bottom Bar
```kotlin
SSBMaxBottomBar(
    currentRoute = currentRoute,
    userRole = user.role,
    onNavigate = { route ->
        navController.navigate(route) {
            popUpTo(startDest) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
)
```

---

## File Organization

```
app/src/main/kotlin/com/ssbmax/
├── ui/
│   ├── SSBMaxApp.kt ..................... Global app wrapper
│   ├── components/
│   │   ├── SSBMaxBottomBar.kt ........... Bottom navigation
│   │   ├── SSBMaxDrawer.kt .............. Navigation drawer
│   │   └── SSBMaxScaffold.kt ............ Main scaffold
│   ├── home/
│   │   ├── student/
│   │   │   ├── StudentHomeScreen.kt ..... Student dashboard
│   │   │   ├── PhaseProgressRibbon.kt ... Progress display
│   │   │   └── StudentHomeViewModel.kt .. State management
│   │   └── instructor/
│   │       ├── InstructorHomeScreen.kt .. Instructor dashboard
│   │       └── InstructorHomeViewModel.kt State management
│   ├── splash/
│   │   ├── SplashScreen.kt
│   │   └── SplashViewModel.kt
│   └── auth/
│       ├── LoginScreen.kt
│       ├── RoleSelectionScreen.kt
│       └── AuthViewModel.kt
├── navigation/
│   ├── SSBMaxDestinations.kt ............ All route definitions
│   └── NavGraph.kt ...................... Navigation graph
└── MainActivity.kt ...................... Entry point
```

---

**Navigation Architecture Complete!** 🎯

All navigation flows documented and implemented.

