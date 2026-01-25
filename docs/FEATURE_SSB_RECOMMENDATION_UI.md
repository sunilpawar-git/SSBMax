# SSB Recommendation Banner UI - Phased Implementation Plan

## 🎯 Objective
Display SSB validation recommendation ("Recommended" / "Borderline" / "Not Recommended") prominently on all 15 test result screens, positioned **above the Overall Performance card**.

---

## 📊 Test Result Screens Inventory (15 Total)

### Psychology Tests (5)
1. `PPDTSubmissionResultScreen.kt` - Picture Perception & Description Test
2. `TATSubmissionResultScreen.kt` - Thematic Apperception Test
3. `WATSubmissionResultScreen.kt` - Word Association Test
4. `SRTSubmissionResultScreen.kt` - Situation Reaction Test
5. `SDTSubmissionResultScreen.kt` - Self Description Test

### GTO Tests (8)
6. `GDResultScreen.kt` - Group Discussion
7. `GPESubmissionResultScreen.kt` - Group Planning Exercise
8. `LecturetteResultScreen.kt` - Lecturette
9. PGT Result Screen - Progressive Group Task
10. HGT Result Screen - Half Group Task
11. GOR Result Screen - Group Obstacle Race
12. IO Result Screen - Individual Obstacles
13. CT Result Screen - Command Task

### Other Tests (2)
14. `OIRTestResultScreen.kt` - Officer Intelligence Rating
15. `InterviewResultScreen.kt` - Personal Interview

---

## 🏗️ Phased Implementation

### Phase 1: Domain & UI Component Foundation ✅
**Goal:** Create reusable `SSBRecommendationBanner` composable + UI data model

**Files to Create:**
- `app/.../ui/components/SSBRecommendationBanner.kt` - Reusable banner component
- `app/.../ui/components/SSBRecommendationBannerTest.kt` - UI tests
- `core/domain/.../validation/SSBRecommendationUIModel.kt` - UI state model

**Acceptance Criteria:**
- [ ] Banner displays RECOMMENDED (green), BORDERLINE (yellow), NOT_RECOMMENDED (red)
- [ ] Shows limitation count, critical warnings
- [ ] Unit tests for all 3 states
- [ ] Build successful

### Phase 2: PPDT Integration (Reference Implementation)
**Goal:** Integrate banner into PPDT result screen as reference

**Files to Modify:**
- `PPDTSubmissionResultViewModel.kt` - Add validation call
- `PPDTSubmissionResultScreen.kt` - Add banner above OverallScoreCard
- `PPDTSubmissionResultViewModelTest.kt` - Add tests

**Acceptance Criteria:**
- [ ] Banner shows above Overall Performance card
- [ ] Validation runs on result load
- [ ] ViewModel tests pass
- [ ] Build successful

### Phase 3: Psychology Tests (TAT, WAT, SRT, SDT)
**Goal:** Apply same pattern to remaining psychology tests

**Files to Modify:**
- `TATSubmissionResultViewModel.kt` + Screen
- `WATSubmissionResultViewModel.kt` + Screen
- `SRTSubmissionResultViewModel.kt` + Screen
- `SDTSubmissionResultViewModel.kt` + Screen

**Acceptance Criteria:**
- [ ] All 4 screens show recommendation banner
- [ ] All ViewModel tests pass
- [ ] Build successful

### Phase 4: GTO Tests (GD, GPE, Lecturette)
**Goal:** Apply pattern to GTO result screens

**Files to Modify:**
- `GDResultViewModel.kt` + Screen
- `GPESubmissionResultViewModel.kt` + Screen
- `LecturetteResultViewModel.kt` + Screen

**Acceptance Criteria:**
- [ ] All 3 screens show recommendation banner
- [ ] All ViewModel tests pass
- [ ] Build successful

### Phase 5: Remaining GTO Tests (PGT, HGT, GOR, IO, CT)
**Goal:** Apply pattern to remaining GTO tests

**Note:** These may share a common result screen or have individual ones

**Acceptance Criteria:**
- [ ] All 5 screens show recommendation banner
- [ ] Build successful

### Phase 6: OIR & Interview
**Goal:** Complete remaining test types

**Files to Modify:**
- `OIRTestResultViewModel.kt` + Screen (if OLQ-based)
- `InterviewResultViewModel.kt` + Screen

**Acceptance Criteria:**
- [ ] Both screens show recommendation banner (where applicable)
- [ ] All tests pass
- [ ] Build successful

### Phase 7: Polish & Integration Testing
**Goal:** Final QA and consistency check

**Tasks:**
- [ ] Visual consistency across all 15 screens
- [ ] Edge case handling (no scores, partial scores)
- [ ] Performance validation
- [ ] End-to-end test with real submission

---

## 🧪 TDD Strategy

Each phase follows:
1. **RED:** Write failing tests for expected behavior
2. **GREEN:** Implement minimum code to pass tests
3. **REFACTOR:** Clean up, remove duplication

### Test Categories:
- **Unit Tests:** Component rendering, state handling
- **ViewModel Tests:** Validation integration, state updates
- **Integration Tests:** Full flow from submission to banner display

---

## 🏛️ Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                         UI Layer                                 │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │              SSBRecommendationBanner                       │  │
│  │  ┌─────────────────────────────────────────────────────┐  │  │
│  │  │  🎖️ RECOMMENDED / ⚠️ BORDERLINE / ❌ NOT RECOMMENDED │  │  │
│  │  │  Limitations: 2/4 | Critical: None | Factor II: OK   │  │  │
│  │  └─────────────────────────────────────────────────────┘  │  │
│  └───────────────────────────────────────────────────────────┘  │
│                              ↑                                   │
│                     SSBRecommendationUIModel                     │
│                              ↑                                   │
├─────────────────────────────────────────────────────────────────┤
│                       ViewModel Layer                            │
│           ValidationIntegration.validateScores()                 │
│                              ↑                                   │
├─────────────────────────────────────────────────────────────────┤
│                        Domain Layer                              │
│           OLQScoreValidationResult → SSBRecommendationUIModel    │
└─────────────────────────────────────────────────────────────────┘
```

---

## 📏 Zero Tech Debt Principles

1. **SSOT:** Single source for recommendation logic (ValidationIntegration)
2. **DRY:** One banner component, one mapper function
3. **Testable:** Pure functions, injectable dependencies
4. **Consistent:** Same UX pattern across all 15 screens

---

## 📅 Progress Tracking

| Phase | Status | Tests | Tech Debt |
|-------|--------|-------|-----------|
| 1     | ⬜ Not Started | 0 | None |
| 2     | ⬜ Not Started | 0 | None |
| 3     | ⬜ Not Started | 0 | None |
| 4     | ⬜ Not Started | 0 | None |
| 5     | ⬜ Not Started | 0 | None |
| 6     | ⬜ Not Started | 0 | None |
| 7     | ⬜ Not Started | 0 | None |

---

## 🚀 Ready to Begin Phase 1
