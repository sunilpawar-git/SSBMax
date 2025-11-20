# GitHub Tracking Issues for Deprecated APIs

This document contains the GitHub issues that should be created to track deprecated API removal per Phase 5 completion.

---

## Issue 1: Remove deprecated `getOIRQuestions(testId)` method

**Title:** [Phase 6] Remove deprecated `getOIRQuestions(testId)` method from TestContentRepositoryImpl

**Labels:** `tech-debt`, `breaking-change`, `phase-6`

**Milestone:** Phase 6 (2025-Q2)

**Description:**

### Summary
Remove the deprecated `getOIRQuestions(testId: String)` method from `TestContentRepositoryImpl` as part of Phase 6 tech debt cleanup.

### Migration Path
**Deprecated in:** Phase 4 (2024-Q4)
**Removal target:** Phase 6 (2025-Q2)

Developers should migrate to the new cached implementation:

```kotlin
// OLD (deprecated)
val questions = repository.getOIRQuestions(testId)

// NEW (recommended)
val questions = repository.getOIRTestQuestions(
    count = 50,
    difficulty = "MEDIUM" // optional
)
```

### Breaking Changes
- `testId` parameter removed (questions now managed by cache)
- Returns fixed count instead of all questions for testId
- Supports difficulty filtering

### Files to Update
- `core/data/src/main/kotlin/com/ssbmax/core/data/repository/TestContentRepositoryImpl.kt` (lines 42-74)
- `core/domain/src/main/kotlin/com/ssbmax/core/domain/repository/TestContentRepository.kt` (interface)
- Search codebase for any remaining usage of `getOIRQuestions(testId)`

### Pre-Removal Checklist
- [ ] Verify no code in codebase still calls `getOIRQuestions(testId)`
- [ ] Run full test suite to ensure coverage
- [ ] Update any integration tests that may use this method
- [ ] Document in release notes as breaking change

### References
- Documentation: `core/data/src/main/kotlin/com/ssbmax/core/data/repository/TestContentRepositoryImpl.kt` lines 42-68
- Migration guide in code comments

---

## Issue 2: Remove deprecated `User` data class

**Title:** [Phase 6] Remove deprecated `User` data class from domain model

**Labels:** `tech-debt`, `breaking-change`, `phase-6`

**Milestone:** Phase 6 (2025-Q2)

**Description:**

### Summary
Remove the legacy `User` data class from the domain model as part of Phase 6 tech debt cleanup. All code should use `SSBMaxUser` instead.

### Migration Path
**Deprecated in:** Phase 3 (2024-Q3)
**Removal target:** Phase 6 (2025-Q2)

Developers should migrate to the enhanced user model:

```kotlin
// OLD (deprecated)
val user = User(
    id = "123",
    email = "user@example.com",
    displayName = "John Doe",
    isPremium = true
)

// NEW (recommended)
val user = SSBMaxUser(
    id = "123",
    email = "user@example.com",
    displayName = "John Doe",
    subscriptionType = SubscriptionType.PRO,
    userRole = UserRole.CANDIDATE
)
```

### Breaking Changes
- `isPremium` replaced with `subscriptionType` (FREE/PRO/PREMIUM)
- Added `userRole` (CANDIDATE/COACH/ADMIN)
- Added profile fields (photoUrl, phone, registrationDate)

### Files to Update
- `core/domain/src/main/kotlin/com/ssbmax/core/domain/model/User.kt` (lines 3-45)
- Search entire codebase for `User(` constructor calls
- Update all type references from `User` to `SSBMaxUser`

### Pre-Removal Checklist
- [ ] Grep codebase for `import .*.User` (not UserRole, UserSubscription, SSBMaxUser)
- [ ] Verify no ViewModel/Repository uses `User` class
- [ ] Update serialization/deserialization if applicable
- [ ] Run full test suite including UI tests
- [ ] Check Firebase Firestore schema references
- [ ] Document in release notes as breaking change

### References
- Documentation: `core/domain/src/main/kotlin/com/ssbmax/core/domain/model/User.kt` lines 3-38
- See also: `SSBMaxUser` in `UserRole.kt`

---

## Issue 3: Remove deprecated `isPremium` property from SSBMaxUser

**Title:** [Phase 6] Remove deprecated `isPremium` property from SSBMaxUser

**Labels:** `tech-debt`, `breaking-change`, `phase-6`

**Milestone:** Phase 6 (2025-Q2)

**Description:**

### Summary
Remove the legacy `isPremium` property from `SSBMaxUser` as part of Phase 6 tech debt cleanup. Use `subscriptionTier` for granular tier checking.

### Migration Path
**Deprecated in:** Phase 3 (2024-Q3)
**Removal target:** Phase 6 (2025-Q2)

Developers should migrate to tier-specific checks:

```kotlin
// OLD (deprecated - boolean check)
if (user.isPremium) { /* ... */ }

// NEW (recommended - tier-specific checks)
when (user.subscriptionTier) {
    SubscriptionTier.FREE -> { /* Free tier */ }
    SubscriptionTier.PRO -> { /* Pro features */ }
    SubscriptionTier.PREMIUM -> { /* Premium features */ }
}

// Or for simple paid check:
if (user.subscriptionTier != SubscriptionTier.FREE) { /* Paid tier */ }
```

### Breaking Changes
- Binary check (free/premium) replaced with three-tier system (FREE/PRO/PREMIUM)
- Use `subscriptionTier` for more precise feature gating

### Files to Update
- `core/domain/src/main/kotlin/com/ssbmax/core/domain/model/UserRole.kt` (lines 34-68)
- Search codebase for `.isPremium` usage
- Update all conditional checks to use `subscriptionTier`

### Pre-Removal Checklist
- [ ] Grep codebase for `\.isPremium` to find all usage
- [ ] Update all ViewModels using this property
- [ ] Update all UI components checking premium status
- [ ] Verify feature gating logic maintains same behavior
- [ ] Run full test suite including subscription tests
- [ ] Document in release notes as breaking change

### References
- Documentation: `core/domain/src/main/kotlin/com/ssbmax/core/domain/model/UserRole.kt` lines 34-64
- Related: `SubscriptionTier` enum

---

## Issue 4: Remove deprecated `NotificationSettingsSection` legacy overload

**Title:** [Phase 5] Remove deprecated NotificationSettingsSection parameter-heavy overload

**Labels:** `tech-debt`, `breaking-change`, `phase-5`, `compose`

**Milestone:** Phase 5 (2025-Q1)

**Description:**

### Summary
Remove the legacy `NotificationSettingsSection` composable overload with 9 parameters. All usage should migrate to the new self-managed version.

### Migration Path
**Deprecated in:** Phase 3 (2024-Q3)
**Removal target:** Phase 5 (2025-Q1)

Developers should migrate to the simplified API:

```kotlin
// OLD (deprecated - manual state passing)
NotificationSettingsSection(
    preferences = viewModel.preferences,
    onTogglePushNotifications = { enabled -> viewModel.toggle(enabled) },
    onToggleGradingComplete = { ... },
    // ... 8 more parameters
)

// NEW (recommended - self-managed state)
NotificationSettingsSection(modifier = Modifier.fillMaxWidth())
// ViewModel injected automatically via Hilt
```

### Breaking Changes
- All state management parameters removed
- ViewModel now injected via `hiltViewModel()` internally
- Simpler API with single modifier parameter

### Rationale
- Follows Compose best practices (components manage own state)
- Reduces boilerplate (8 callback parameters eliminated)
- Better testability with dedicated ViewModel

### Files to Update
- `app/src/main/kotlin/com/ssbmax/ui/settings/components/NotificationSettingsSection.kt` (lines 46-111)
- Search codebase for `NotificationSettingsSection(` with parameters
- Update all call sites to use new parameterless version

### Pre-Removal Checklist
- [ ] Grep codebase for `NotificationSettingsSection\(.*preferences.*\)` to find usage
- [ ] Verify all settings screens use new API
- [ ] Run UI tests for settings screens
- [ ] Test notification toggle functionality
- [ ] Document in release notes as breaking change

### References
- Documentation: `app/src/main/kotlin/com/ssbmax/ui/settings/components/NotificationSettingsSection.kt` lines 46-81
- New implementation: lines 26-44 in same file

---

## Instructions for Creating Issues

1. Go to GitHub repository: https://github.com/{YOUR_ORG}/SSBMax/issues
2. Click "New Issue"
3. Copy each issue template above
4. Set appropriate labels, milestones, and assignees
5. Link issues to Phase 5 and Phase 6 project boards
6. Add to tech debt tracking spreadsheet if applicable

## Quick Commands

```bash
# Find usage of deprecated APIs
grep -r "getOIRQuestions(testId" . --include="*.kt"
grep -r "import.*\.User$" . --include="*.kt" | grep -v "SSBMaxUser\|UserRole\|UserSubscription"
grep -r "\.isPremium" . --include="*.kt"
grep -r "NotificationSettingsSection\(" . --include="*.kt" | grep "preferences"

# Verify no usage before removal
./gradle.sh build # Should compile successfully after removal
./gradle.sh test  # All tests should pass
```

## References
- Phase 5 Tech Debt Report: `docs/tech-debt/phase5-completion-report.md`
- ADR-003: `docs/architecture/ADR-003-GlobalScope-Usage.md`
- Deprecation Strategy: See CLAUDE.md Section on Code Quality

---

**Created:** 2024-11-20
**Last Updated:** 2024-11-20
**Maintainer:** Architecture Team
