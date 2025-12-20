# FirestoreSubmissionRepository Refactoring Plan

## Executive Summary

This document outlines a **phased, zero-regression refactoring strategy** for `FirestoreSubmissionRepository.kt` (2,140 lines) to solve:
1. **IMMEDIATE**: OLQ scoring regression bugs across TAT/WAT/SRT/SDT tests
2. **STRATEGIC**: Repository bloat and Single Responsibility Principle violations

**Guarantee**: 100% build success after EACH phase with comprehensive test coverage.

---

## Problem Analysis

### Current State
- **File Size**: 2,140 lines (7x the 300-line quality standard)
- **SRP Violations**: Single class handles 9+ test types (TAT, WAT, SRT, SDT, PPDT, PIQ, OIR, GD, GPE, Lecturette)
- **Bug Impact**: OLQ regression protection exists only in `observePPDTSubmission`, causing disappearing scores for other tests
- **Maintenance Cost**: Bug fixes require navigating massive file, increasing risk of regressions

### Root Cause
Firestore's offline cache synchronization can emit stale snapshots AFTER fresh server data when Activities are relaunched (e.g., via notification deep links). Only PPDT has protection against this.

### Dependencies Analysis
- **Workers**: TAT/WAT/SRT/SDT/PPDTAnalysisWorker all depend on `SubmissionRepository` interface
- **ViewModels**: Result screens depend on `observe*Submission()` methods
- **Tests**: 58 test files exist, including `FirestoreSubmissionRepositoryTest.kt`
- **Interface**: 42 methods in `SubmissionRepository.kt` must be maintained

---

## Quality Framework Compliance

This plan follows [SSBMax_Quality_Framework.md](file:///Users/sunil/Downloads/SSBMax/docs/architecture/SSBMax_Quality_Framework.md):

### Pre-Implementation Checklist for Each Phase
- ✅ **Architecture Compliance**: Domain layer isolation maintained (interface unchanged)
- ✅ **File Size**: Phase 3 splits files to stay under 300 lines
- ✅ **Single Responsibility**: Phase 3 creates focused repositories per test domain
- ✅ **Error Handling**: Existing `Result<T>` pattern preserved
- ✅ **Test Coverage**: Every phase verified with existing + new tests
- ✅ **StateFlow Pattern**: Not applicable (repository layer)
- ✅ **Zero Tech Debt**: No commented code, no incomplete features

### Build & CI Verification (MANDATORY AFTER EACH PHASE)
```bash
# Phase completion criteria
./gradlew lint              # Zero new lint errors
./gradlew test              # All unit tests pass
./gradlew build             # Clean compilation
./gradlew :core:data:test   # Repository-specific tests
```

---

## Phase-Wise Implementation Plan

---

## 📋 PHASE 0: Pre-Implementation Validation

**Objective**: Establish baseline and verify test infrastructure

**Duration**: 30 minutes

### Steps

#### 0.1 Verify Current Build State
```bash
cd /Users/sunil/Downloads/SSBMax
./gradlew clean build --stacktrace
# EXPECTED: Clean build with ZERO errors
# Document any warnings for comparison
```

#### 0.2 Run Existing Tests
```bash
./gradlew test --stacktrace
# EXPECTED: All tests pass
# Capture test count for regression detection
```

#### 0.3 Create Test Baseline Report
```bash
./gradlew testDebugUnitTest --tests "FirestoreSubmissionRepositoryTest"
# EXPECTED: All 6 submission tests pass
# Save output to docs/troubleshooting/phase0_baseline.txt
```

### Quality Checklist
- [ ] Build succeeds with zero errors
- [ ] All 58+ unit tests pass
- [ ] `FirestoreSubmissionRepositoryTest` passes (6 tests)
- [ ] Architecture tests pass (`ArchitectureTest.kt`)
- [ ] Baseline test report saved

### Success Criteria
✅ Clean build + all tests passing = ready for Phase 1

---

## 🔴 PHASE 1: Critical Bug Fix - OLQ Regression Protection

**Objective**: Apply stale cache regression fix to TAT/WAT/SRT/SDT observers (immediate user impact)

**Duration**: 2-3 hours

**CRITICAL**: This phase MUST complete successfully before refactoring

### Rationale
Users are currently experiencing disappearing OLQ scores. This is a **critical production bug** that must be fixed before architectural improvements.

### Implementation Steps

#### 1.1 Update `observeTATSubmission` (Lines 1095-1117)

**Current Implementation**:
```kotlin
override fun observeTATSubmission(submissionId: String): Flow<TATSubmission?> = callbackFlow {
    val listener = submissionsCollection.document(submissionId)
        .addSnapshotListener { snapshot, error ->
            if (error != null) { close(error); return@addSnapshotListener }
            if (snapshot == null || !snapshot.exists()) { trySend(null); return@addSnapshotListener }
            try {
                val data = snapshot.get(FIELD_DATA) as? Map<*, *>
                trySend(if (data != null) parseTATSubmission(data) else null)
            } catch (e: Exception) { }
        }
    awaitClose { listener.remove() }
}
```

**Add Regression Protection**:
```kotlin
override fun observeTATSubmission(submissionId: String): Flow<TATSubmission?> = callbackFlow {
    var hasSeenCompleteAnalysis = false
    
    val listener = submissionsCollection.document(submissionId)
        .addSnapshotListener { snapshot, error ->
            if (error != null) { close(error); return@addSnapshotListener }
            if (snapshot == null || !snapshot.exists()) { trySend(null); return@addSnapshotListener }
            
            try {
                val data = snapshot.get(FIELD_DATA) as? Map<*, *>
                if (data == null) { trySend(null); return@addSnapshotListener }
                
                val metadata = snapshot.metadata
                
                // Check analysis status
                val analysisStatus = data["analysisStatus"] as? String
                val hasOlqResult = data["olqResult"] != null
                val isComplete = analysisStatus == "COMPLETED" && hasOlqResult
                
                if (isComplete) {
                    hasSeenCompleteAnalysis = true
                }
                
                // Skip stale cache that would regress from complete state
                val isFromCacheOnly = metadata.isFromCache && !metadata.hasPendingWrites()
                val wouldRegress = hasSeenCompleteAnalysis && !isComplete && isFromCacheOnly
                
                if (wouldRegress) {
                    Log.d(TAG, "⚠️ Ignoring stale cache for TAT $submissionId (would regress)")
                    return@addSnapshotListener
                }
                
                trySend(parseTATSubmission(data))
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing TAT submission", e)
            }
        }
    awaitClose { listener.remove() }
}
```

#### 1.2 Update `observeWATSubmission` (Lines 1144-1153)
Apply identical pattern with "WAT" in log messages.

#### 1.3 Update `observeSRTSubmission` (Lines 1179-1188)
Apply identical pattern with "SRT" in log messages.

#### 1.4 Update `observeSDTSubmission` (Lines 1214-1223)
Apply identical pattern with "SDT" in log messages.

### Testing Strategy

#### Unit Tests (NEW)
Create `FirestoreSubmissionRepositoryRegressionTest.kt`:

```kotlin
@Test
fun `observeTATSubmission filters stale cache after seeing complete analysis`() = runTest {
    // GIVEN: Submission that completes analysis
    // WHEN: Stale cache snapshot arrives AFTER complete snapshot
    // THEN: Stale snapshot is ignored, complete data retained
}

// Repeat for WAT, SRT, SDT
```

#### Manual Verification
1. **Setup**: Submit a TAT test via app
2. **Trigger Worker**: Wait for `TATAnalysisWorker` to complete (~30s)
3. **Verify OLQ Displayed**: Result screen shows OLQ scores
4. **Test Regression**: Tap notification to relaunch result screen
5. **Expected**: OLQ scores PERSIST (do not disappear)
6. **Repeat**: For WAT, SRT, SDT tests

#### Architecture Test Update
```kotlin
@Test
fun `all observe methods have regression protection`() {
    // Verify observeTAT/WAT/SRT/SDT/PPDT all implement hasSeenCompleteAnalysis
}
```

### Files Modified
- [MODIFY] [FirestoreSubmissionRepository.kt](file:///Users/sunil/Downloads/SSBMax/core/data/src/main/kotlin/com/ssbmax/core/data/remote/FirestoreSubmissionRepository.kt)
  - Update 4 observe methods (TAT/WAT/SRT/SDT)
  - Add regression protection logic identical to PPDT
  - Lines affected: ~100 lines total

### Quality Gates
- [ ] `./gradlew lint` passes (0 new errors)
- [ ] `./gradlew test` passes (all existing + 4 new tests)
- [ ] `./gradlew build` succeeds
- [ ] Manual test: TAT OLQ scores persist after notification tap
- [ ] Manual test: WAT OLQ scores persist after notification tap
- [ ] Manual test: SRT OLQ scores persist after notification tap
- [ ] Manual test: SDT OLQ scores persist after notification tap
- [ ] Architecture test verifies all 5 observe methods have protection

### Success Criteria
✅ **100% build pass** + OLQ regression **ELIMINATED** = ready for Phase 2

---

## 🟡 PHASE 2: Extract Common Logic & Helper Methods

**Objective**: Prepare for refactoring by extracting reusable logic without changing external interface

**Duration**: 3-4 hours

**Risk Level**: LOW (internal refactoring only, no interface changes)

### Rationale
Before splitting the repository, we must eliminate code duplication. This makes Phase 3 cleaner and prevents copy-paste errors.

### Implementation Steps

#### 2.1 Extract OLQ Regression Protection Logic

**Create Shared Helper**:
```kotlin
/**
 * Helper class for OLQ regression protection in Firestore observers
 */
private class OLQRegressionFilter {
    private var hasSeenCompleteAnalysis = false
    
    /**
     * Check if snapshot should be filtered due to stale cache regression
     * 
     * @param data Firestore document data
     * @param metadata Firestore snapshot metadata
     * @param testType Test type for logging
     * @return true if snapshot should be IGNORED, false if should be emitted
     */
    fun shouldFilterSnapshot(
        data: Map<*, *>,
        metadata: com.google.firebase.firestore.SnapshotMetadata,
        testType: String
    ): Boolean {
        val analysisStatus = data["analysisStatus"] as? String
        val hasOlqResult = data["olqResult"] != null
        val isComplete = analysisStatus == "COMPLETED" && hasOlqResult
        
        if (isComplete) {
            hasSeenCompleteAnalysis = true
        }
        
        val isFromCacheOnly = metadata.isFromCache && !metadata.hasPendingWrites()
        val wouldRegress = hasSeenCompleteAnalysis && !isComplete && isFromCacheOnly
        
        if (wouldRegress) {
            Log.d(TAG, "⚠️ Ignoring stale cache for $testType (would regress)")
        }
        
        return wouldRegress
    }
}
```

**Refactor observe methods to use helper**:
```kotlin
override fun observeTATSubmission(submissionId: String): Flow<TATSubmission?> = callbackFlow {
    val regressionFilter = OLQRegressionFilter()
    
    val listener = submissionsCollection.document(submissionId)
        .addSnapshotListener { snapshot, error ->
            if (error != null) { close(error); return@addSnapshotListener }
            if (snapshot == null || !snapshot.exists()) { trySend(null); return@addSnapshotListener }
            
            try {
                val data = snapshot.get(FIELD_DATA) as? Map<*, *>
                if (data == null) { trySend(null); return@addSnapshotListener }
                
                // Use shared regression filter
                if (regressionFilter.shouldFilterSnapshot(data, snapshot.metadata, "TAT $submissionId")) {
                    return@addSnapshotListener
                }
                
                trySend(parseTATSubmission(data))
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing TAT submission", e)
            }
        }
    awaitClose { listener.remove() }
}
```

#### 2.2 Extract Common Submission Utilities

**Create Constants Object**:
```kotlin
private object SubmissionConstants {
    const val ANALYSIS_STATUS_COMPLETED = "COMPLETED"
    const val ANALYSIS_STATUS_PENDING = "PENDING_ANALYSIS"
    const val ANALYSIS_STATUS_ANALYZING = "ANALYZING"
}
```

#### 2.3 Extract Common OLQ Mapping Logic

**Create OLQ Mapper**:
```kotlin
private object OLQMapper {
    fun toFirestoreMap(olqResult: OLQAnalysisResult): Map<String, Any?> {
        return mapOf(
            "submissionId" to olqResult.submissionId,
            "testType" to olqResult.testType.name,
            "olqScores" to olqResult.olqScores.mapKeys { it.key.name }.mapValues { (_, score) ->
                mapOf("score" to score.score, "confidence" to score.confidence, "reasoning" to score.reasoning)
            },
            "overallScore" to olqResult.overallScore,
            "overallRating" to olqResult.overallRating,
            "strengths" to olqResult.strengths,
            "weaknesses" to olqResult.weaknesses,
            "recommendations" to olqResult.recommendations,
            "analyzedAt" to olqResult.analyzedAt,
            "aiConfidence" to olqResult.aiConfidence
        )
    }
}
```

### Testing Strategy

#### Regression Tests
```bash
# CRITICAL: All existing tests must still pass
./gradlew :core:data:test --tests "FirestoreSubmissionRepositoryTest"
# Expected: 6/6 tests pass

./gradlew :app:test --tests "*AnalysisWorkerTest"
# Expected: All worker tests pass (TAT, WAT, SRT, SDT, PPDT)
```

#### New Unit Tests
```kotlin
@Test
fun `OLQRegressionFilter blocks stale cache correctly`() {
    // Test the extracted helper in isolation
}

@Test
fun `OLQMapper produces correct Firestore structure`() {
    // Verify OLQ mapping matches existing format
}
```

### Files Modified
- [MODIFY] [FirestoreSubmissionRepository.kt](file:///Users/sunil/Downloads/SSBMax/core/data/src/main/kotlin/com/ssbmax/core/data/remote/FirestoreSubmissionRepository.kt)
  - Add private helper classes (OLQRegressionFilter, OLQMapper, SubmissionConstants)
  - Refactor 5 observe methods to use OLQRegressionFilter
  - Refactor updateOLQResult to use OLQMapper
  - Net change: +150 lines, -50 lines (code becomes more readable)

### Quality Gates
- [ ] `./gradlew build` succeeds with 0 errors
- [ ] All existing tests still pass (regression check)
- [ ] New helper unit tests pass
- [ ] Architecture tests pass
- [ ] Code review: No duplicated logic remains in observe methods

### Success Criteria
✅ **100% build pass** + **code duplication eliminated** = ready for Phase 3

---

## 🟢 PHASE 3: Domain-Based Repository Split

**Objective**: Refactor monolithic repository into focused, maintainable repositories

**Duration**: 6-8 hours

**Risk Level**: MEDIUM (requires interface design + dependency updates)

### Rationale
A 2,140-line repository violates SRP. We'll split by test domain while maintaining the single `SubmissionRepository` interface.

### Design: Facade Pattern

**Strategy**: Use composition, not inheritance. Keep the interface, delegate to specialized repositories.

#### Architecture Diagram
```
┌─────────────────────────────────────────────────────┐
│           SubmissionRepository (Interface)          │
│                   42 methods                         │
└─────────────────────────────────────────────────────┘
                         ▲
                         │
                         │implements
                         │
┌────────────────────────────────────────────────────┐
│   FirestoreSubmissionRepository (Facade)           │
│   - Delegates to specialized repositories          │
│   - 200 lines (composition only)                   │
└────────────────────────────────────────────────────┘
         │         │           │            │
         │         │           │            │
         ▼         ▼           ▼            ▼
┌──────────┐ ┌──────────┐ ┌──────────┐ ┌───────────┐
│PsychTest │ │   GTO    │ │ Personal │ │  Common   │
│Repository│ │Repository│ │Repository│ │Repository │
│          │ │          │ │          │ │           │
│TAT/WAT/  │ │GD/GPE/   │ │PIQ/OIR   │ │Generic    │
│SRT/SDT/  │ │Lecturette│ │          │ │operations │
│PPDT      │ │          │ │          │ │           │
│~400lines │ │~300lines │ │~250lines │ │~200 lines │
└──────────┘ └──────────┘ └──────────┘ └───────────┘
```

### Implementation Steps

#### 3.1 Create PsychTestSubmissionRepository

**File**: `core/data/src/main/kotlin/com/ssbmax/core/data/remote/PsychTestSubmissionRepository.kt`

**Responsibilities**:
- TAT submission/retrieval/observation
- WAT submission/retrieval/observation
- SRT submission/retrieval/observation
- SDT submission/retrieval/observation
- PPDT submission/retrieval/observation
- Shared OLQ analysis methods
- OLQ regression protection (via OLQRegressionFilter)

**Methods** (extracted from FirestoreSubmissionRepository):
- `submitTAT`, `getTATSubmission`, `updateTATAnalysisStatus`, `updateTATOLQResult`, `observeTATSubmission`
- `submitWAT`, `getWATSubmission`, `updateWATAnalysisStatus`, `updateWATOLQResult`, `observeWATSubmission`
- `submitSRT`, `getSRTSubmission`, `updateSRTAnalysisStatus`, `updateSRTOLQResult`, `observeSRTSubmission`
- `submitSDT`, `getSDTSubmission`, `updateSDTAnalysisStatus`, `updateSDTOLQResult`, `observeSDTSubmission`
- `submitPPDT`, `getPPDTSubmission`, `updatePPDTAnalysisStatus`, `updatePPDTOLQResult`, `observePPDTSubmission`
- Private: `parseTATSubmission`, `parseWATSubmission`, etc.
- Private: `updateOLQResult` (shared)

**Lines**: ~400

#### 3.2 Create GTOSubmissionRepository

**File**: `core/data/src/main/kotlin/com/ssbmax/core/data/remote/GTOSubmissionRepository.kt`

**Responsibilities**:
- GD (Group Discussion) submission
- GPE (Group Planning Exercise) submission
- Lecturette submission

**Methods**:
- `submitGD`
- `submitGPE`
- `submitLecturette`

**Lines**: ~300

#### 3.3 Create PersonalTestSubmissionRepository

**File**: `core/data/src/main/kotlin/com/ssbmax/core/data/remote/PersonalTestSubmissionRepository.kt`

**Responsibilities**:
- PIQ (Personal Information Questionnaire) submission/retrieval
- OIR (Officer Intelligence Rating) submission/retrieval

**Methods**:
- `submitPIQ`, `getLatestPIQSubmission`
- `submitOIR`, `getLatestOIRSubmission`
- Private: `parsePIQSubmission`, `parseOIRSubmission`

**Lines**: ~250

#### 3.4 Create CommonSubmissionRepository

**File**: `core/data/src/main/kotlin/com/ssbmax/core/data/remote/CommonSubmissionRepository.kt`

**Responsibilities**:
- Generic submission operations (not test-specific)
- Archival operations
- Status updates

**Methods**:
- `getSubmission`
- `getUserSubmissions`
- `getUserSubmissionsByTestType`
- `observeSubmission`
- `observeUserSubmissions`
- `updateSubmissionStatus`
- `archiveOldSubmissions`

**Lines**: ~200

#### 3.5 Refactor FirestoreSubmissionRepository to Facade

**File**: `FirestoreSubmissionRepository.kt` (MODIFIED)

**New Implementation**:
```kotlin
@Singleton
class FirestoreSubmissionRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) : SubmissionRepository {
    
    // Compose specialized repositories
    private val psychTestRepo = PsychTestSubmissionRepository(firestore)
    private val gtoRepo = GTOSubmissionRepository(firestore)
    private val personalTestRepo = PersonalTestSubmissionRepository(firestore)
    private val commonRepo = CommonSubmissionRepository(firestore)
    
    // Delegate to specialized repositories
    override suspend fun submitTAT(submission: TATSubmission, batchId: String?) = 
        psychTestRepo.submitTAT(submission, batchId)
    
    override suspend fun submitWAT(submission: WATSubmission, batchId: String?) = 
        psychTestRepo.submitWAT(submission, batchId)
    
    override suspend fun submitSRT(submission: SRTSubmission, batchId: String?) = 
        psychTestRepo.submitSRT(submission, batchId)
        
    override suspend fun submitSDT(submission: SDTSubmission, batchId: String?) = 
        psychTestRepo.submitSDT(submission, batchId)
        
    override suspend fun submitPPDT(submission: PPDTSubmission, batchId: String?) = 
        psychTestRepo.submitPPDT(submission, batchId)
    
    override suspend fun submitGD(submission: GTOSubmission.GDSubmission, batchId: String?) = 
        gtoRepo.submitGD(submission, batchId)
        
    override suspend fun submitGPE(submission: GTOSubmission.GPESubmission, batchId: String?) = 
        gtoRepo.submitGPE(submission, batchId)
        
    override suspend fun submitLecturette(submission: GTOSubmission.LecturetteSubmission, batchId: String?) = 
        gtoRepo.submitLecturette(submission, batchId)
    
    override suspend fun submitPIQ(submission: PIQSubmission, batchId: String?) = 
        personalTestRepo.submitPIQ(submission, batchId)
        
    override suspend fun submitOIR(submission: OIRSubmission, batchId: String?) = 
        personalTestRepo.submitOIR(submission, batchId)
    
    override suspend fun getSubmission(submissionId: String) = 
        commonRepo.getSubmission(submissionId)
        
    override suspend fun getUserSubmissions(userId: String, limit: Int) = 
        commonRepo.getUserSubmissions(userId, limit)
    
    override suspend fun getUserSubmissionsByTestType(userId: String, testType: TestType, limit: Int) = 
        commonRepo.getUserSubmissionsByTestType(userId, testType, limit)
    
    override fun observeSubmission(submissionId: String) = 
        commonRepo.observeSubmission(submissionId)
        
    override fun observeUserSubmissions(userId: String, limit: Int) = 
        commonRepo.observeUserSubmissions(userId, limit)
    
    override suspend fun updateSubmissionStatus(submissionId: String, status: SubmissionStatus) = 
        commonRepo.updateSubmissionStatus(submissionId, status)
    
    // TAT methods
    override suspend fun getTATSubmission(submissionId: String) = 
        psychTestRepo.getTATSubmission(submissionId)
        
    override suspend fun updateTATAnalysisStatus(submissionId: String, status: AnalysisStatus) = 
        psychTestRepo.updateTATAnalysisStatus(submissionId, status)
        
    override suspend fun updateTATOLQResult(submissionId: String, olqResult: OLQAnalysisResult) = 
        psychTestRepo.updateTATOLQResult(submissionId, olqResult)
        
    override fun observeTATSubmission(submissionId: String) = 
        psychTestRepo.observeTATSubmission(submissionId)
    
    // WAT methods
    override suspend fun getWATSubmission(submissionId: String) = 
        psychTestRepo.getWATSubmission(submissionId)
        
    override suspend fun updateWATAnalysisStatus(submissionId: String, status: AnalysisStatus) = 
        psychTestRepo.updateWATAnalysisStatus(submissionId, status)
        
    override suspend fun updateWATOLQResult(submissionId: String, olqResult: OLQAnalysisResult) = 
        psychTestRepo.updateWATOLQResult(submissionId, olqResult)
        
    override fun observeWATSubmission(submissionId: String) = 
        psychTestRepo.observeWATSubmission(submissionId)
    
    // SRT methods
    override suspend fun getSRTSubmission(submissionId: String) = 
        psychTestRepo.getSRTSubmission(submissionId)
        
    override suspend fun updateSRTAnalysisStatus(submissionId: String, status: AnalysisStatus) = 
        psychTestRepo.updateSRTAnalysisStatus(submissionId, status)
        
    override suspend fun updateSRTOLQResult(submissionId: String, olqResult: OLQAnalysisResult) = 
        psychTestRepo.updateSRTOLQResult(submissionId, olqResult)
        
    override fun observeSRTSubmission(submissionId: String) = 
        psychTestRepo.observeSRTSubmission(submissionId)
    
    // SDT methods
    override suspend fun getSDTSubmission(submissionId: String) = 
        psychTestRepo.getSDTSubmission(submissionId)
        
    override suspend fun updateSDTAnalysisStatus(submissionId: String, status: AnalysisStatus) = 
        psychTestRepo.updateSDTAnalysisStatus(submissionId, status)
        
    override suspend fun updateSDTOLQResult(submissionId: String, olqResult: OLQAnalysisResult) = 
        psychTestRepo.updateSDTOLQResult(submissionId, olqResult)
        
    override fun observeSDTSubmission(submissionId: String) = 
        psychTestRepo.observeSDTSubmission(submissionId)
    
    // PPDT methods
    override suspend fun getPPDTSubmission(submissionId: String) = 
        psychTestRepo.getPPDTSubmission(submissionId)
        
    override suspend fun updatePPDTAnalysisStatus(submissionId: String, status: AnalysisStatus) = 
        psychTestRepo.updatePPDTAnalysisStatus(submissionId, status)
        
    override suspend fun updatePPDTOLQResult(submissionId: String, olqResult: OLQAnalysisResult) = 
        psychTestRepo.updatePPDTOLQResult(submissionId, olqResult)
        
    override fun observePPDTSubmission(submissionId: String) = 
        psychTestRepo.observePPDTSubmission(submissionId)
    
    // Personal test methods
    override suspend fun getLatestPIQSubmission(userId: String) = 
        personalTestRepo.getLatestPIQSubmission(userId)
        
    override suspend fun getLatestOIRSubmission(userId: String) = 
        personalTestRepo.getLatestOIRSubmission(userId)
    
    override suspend fun getLatestPPDTSubmission(userId: String) = 
        psychTestRepo.getLatestPPDTSubmission(userId)
    
    // Archival
    override suspend fun archiveOldSubmissions(beforeTimestamp: Long) = 
        commonRepo.archiveOldSubmissions(beforeTimestamp)
}
```

**Lines**: ~200 (pure delegation)

### Testing Strategy

#### Compilation Verification
```bash
# CRITICAL: Build must succeed with zero errors
./gradlew :core:data:build --stacktrace
# Expected: SUCCESS
```

#### Unit Tests (Existing - MUST PASS)
```bash
./gradlew :core:data:test --tests "FirestoreSubmissionRepositoryTest"
# Expected: 6/6 tests pass (TAT, WAT, SRT, PPDT, OIR, cross-validation)
```

#### Worker Integration Tests
```bash
./gradlew :app:test --tests "*AnalysisWorkerTest"
# Expected: All 5 worker tests pass (TAT, WAT, SRT, SDT, PPDT)
# These workers depend on SubmissionRepository interface, not implementation
```

#### ViewModel Tests
```bash
./gradlew :app:test --tests "*SubmissionResultViewModelTest"
# Expected: All result ViewModel tests pass
# Verifies observe methods still work correctly
```

#### Architecture Tests
Add new architecture test:
```kotlin
@Test
fun `repository files under 300 lines`() {
    val repositoryFiles = listOf(
        "PsychTestSubmissionRepository.kt",
        "GTOSubmissionRepository.kt",
        "PersonalTestSubmissionRepository.kt",
        "CommonSubmissionRepository.kt",
        "FirestoreSubmissionRepository.kt"
    )
    
    repositoryFiles.forEach { file ->
        val lineCount = File("core/data/src/main/kotlin/com/ssbmax/core/data/remote/$file")
            .readLines().size
        assertTrue("$file exceeds 300 lines: $lineCount", lineCount <= 300)
    }
}
```

### Files Created
- [NEW] `PsychTestSubmissionRepository.kt` (~400 lines)
- [NEW] `GTOSubmissionRepository.kt` (~300 lines)
- [NEW] `PersonalTestSubmissionRepository.kt` (~250 lines)
- [NEW] `CommonSubmissionRepository.kt` (~200 lines)

### Files Modified
- [MODIFY] `FirestoreSubmissionRepository.kt` (2,140 → 200 lines)

### Files Deleted
- None (old implementation becomes facade)

### Quality Gates
- [ ] `./gradlew build` succeeds
- [ ] All 58+ unit tests pass
- [ ] All worker integration tests pass
- [ ] All ViewModel tests pass
- [ ] Architecture test: All repository files ≤ 300 lines
- [ ] Lint: Zero new errors
- [ ] Code review: Each repository has single, clear responsibility

### Success Criteria
✅ **100% build pass** + **all files ≤ 300 lines** + **SRP satisfied** = refactoring complete

---

## 🔵 PHASE 4: Final Verification & Documentation

**Objective**: Comprehensive end-to-end testing and documentation updates

**Duration**: 2-3 hours

**Risk Level**: MINIMAL (validation only)

### Implementation Steps

#### 4.1 Full Test Suite Execution
```bash
# Run ALL tests
./gradlew test --stacktrace

# Run specific critical tests
./gradlew :core:data:test
./gradlew :app:test --tests "*Worker*"
./gradlew :app:test --tests "*ViewModel*"
./gradlew :app:testDebugUnitTest --tests "ArchitectureTest"
```

#### 4.2 Manual End-to-End Testing

**Test Plan**:
1. **TAT Test Flow**
   - Submit TAT test
   - Verify worker analyzes submission
   - Check OLQ scores display
   - Tap notification → verify scores persist

2. **WAT Test Flow**
   - Submit WAT test
   - Verify worker analyzes submission
   - Check OLQ scores display
   - Tap notification → verify scores persist

3. **SRT Test Flow**
   - Submit SRT test
   - Verify worker analyzes submission
   - Check OLQ scores display
   - Tap notification → verify scores persist

4. **SDT Test Flow**
   - Submit SDT test
   - Verify worker analyzes submission
   - Check OLQ scores display
   - Tap notification → verify scores persist

5. **PPDT Test Flow**
   - Submit PPDT test
   - Verify worker analyzes submission
   - Check OLQ scores display
   - Tap notification → verify scores persist

6. **Dashboard**
   - Verify all 5 test types show OLQ scores on dashboard
   - Verify no "analyzing..." stuck states

#### 4.3 Update Documentation

**Update Architecture Diagram** (`docs/architecture/repository_structure.md`):
```markdown
## Submission Repository Architecture

The submission repository follows the Facade pattern:

- **FirestoreSubmissionRepository**: Facade that delegates to specialized repos
- **PsychTestSubmissionRepository**: TAT, WAT, SRT, SDT, PPDT (psychology tests with OLQ)
- **GTOSubmissionRepository**: GD, GPE, Lecturette (GTO tests)
- **PersonalTestSubmissionRepository**: PIQ, OIR (personal information tests)
- **CommonSubmissionRepository**: Generic submission operations

### Benefits
- Single Responsibility Principle: Each repo handles one test domain
- Maintainability: Bug fixes isolated to specific domain
- Testability: Smaller, focused unit tests
- Scalability: New test types added without modifying existing repos
```

**Update Troubleshooting Guide** (`docs/troubleshooting/olq_scoring_issues.md`):
```markdown
## OLQ Scoring Disappearing (RESOLVED)

### Problem
OLQ scores would appear briefly then disappear after tapping notifications.

### Root Cause
Firestore offline cache emitted stale snapshots AFTER fresh server data when Activities relaunched via deep links.

### Solution (Implemented in Phase 1)
All psychology test observers now implement regression protection:
- Track `hasSeenCompleteAnalysis` state
- Filter stale cache snapshots that would regress OLQ data
- Implemented in: observeTAT/WAT/SRT/SDT/PPDTSubmission

### Verification
Test by submitting a test, waiting for OLQ analysis, then tapping notification.
Scores should persist.
```

**Create Maintenance Guide** (`docs/architecture/submission_repository_maintenance.md`):
```markdown
## Submission Repository Maintenance Guide

### Adding a New Test Type

1. **Identify Domain**: Psychology test? GTO test? Personal info?
2. **Add to Appropriate Repo**:
   - Psychology tests with OLQ → `PsychTestSubmissionRepository`
   - GTO tests → `GTOSubmissionRepository`
   - Personal info → `PersonalTestSubmissionRepository`
3. **Add Interface Method**: Update `SubmissionRepository.kt`
4. **Add Facade Delegation**: Update `FirestoreSubmissionRepository.kt`
5. **Add Tests**: Create unit tests in appropriate test file
6. **Verify Build**: `./gradlew build` must pass

### File Size Policy
- Each repository file MUST stay under 300 lines
- If approaching limit, extract helper classes
- Never merge repositories to "save files"

### OLQ Regression Protection
- All psychology test observers MUST use `OLQRegressionFilter`
- See `observePPDTSubmission` as reference implementation
```

#### 4.4 Commit & Tag Strategy

**Commit Messages**:
```bash
# Phase 1
git commit -m "fix(data): Add OLQ regression protection to TAT/WAT/SRT/SDT observers

- Prevent stale Firestore cache from overwriting completed OLQ analysis
- Implement regression filter identical to PPDT
- Fixes disappearing OLQ scores bug

Tested: Manual verification + unit tests for all 4 test types
Refs: #BUG-123"

# Phase 2
git commit -m "refactor(data): Extract common OLQ logic to shared helpers

- Create OLQRegressionFilter helper class
- Create OLQMapper for Firestore serialization
- Extract SubmissionConstants
- Eliminate code duplication in observe methods

No functional changes. All tests pass."

# Phase 3
git commit -m "refactor(data): Split FirestoreSubmissionRepository into domain-based repos

- Create PsychTestSubmissionRepository (TAT/WAT/SRT/SDT/PPDT)
- Create GTOSubmissionRepository (GD/GPE/Lecturette)
- Create PersonalTestSubmissionRepository (PIQ/OIR)
- Create CommonSubmissionRepository (generic operations)
- Refactor FirestoreSubmissionRepository to facade pattern

Benefits: SRP compliance, all files < 300 lines, improved maintainability
Breaking: None (interface unchanged)
Tested: All 58+ unit tests pass, worker tests pass"

# Phase 4
git commit -m "docs(architecture): Update repository architecture documentation

- Add repository structure diagram
- Add OLQ scoring troubleshooting guide
- Add submission repository maintenance guide

Closes: #REFACTOR-456"
```

**Git Tags**:
```bash
git tag -a v1.2.0-repository-refactor -m "Repository refactoring complete

Changes:
- OLQ regression bug fixed for all psychology tests
- FirestoreSubmissionRepository split into focused, maintainable repos
- All files comply with 300-line limit
- Zero tech debt, SRP compliance achieved

Verified: 100% build success, all tests passing"
```

### Quality Gates
- [ ] All 58+ tests pass
- [ ] Manual E2E tests complete (all 5 test types)
- [ ] Dashboard shows OLQ scores for all tests
- [ ] Documentation updated
- [ ] Architecture diagram updated
- [ ] Commits follow conventional commit format
- [ ] Git tags created

### Success Criteria
✅ **All tests pass** + **docs updated** + **E2E verified** = COMPLETE

---

## Summary & Rollout

### Phased Rollout Timeline

| Phase | Duration | Risk | Build Guarantee |
|-------|----------|------|-----------------|
| Phase 0: Validation | 30 min | NONE | ✅ |
| Phase 1: Bug Fix | 2-3 hrs | LOW | ✅ |
| Phase 2: Extract Logic | 3-4 hrs | LOW | ✅ |
| Phase 3: Repository Split | 6-8 hrs | MEDIUM | ✅ |
| Phase 4: Verification | 2-3 hrs | MINIMAL | ✅ |
| **TOTAL** | **14-18.5 hrs** | **LOW** | **✅ 100%** |

### Regression Prevention

**After Each Phase**:
1. Run full test suite
2. Verify build succeeds
3. Check architecture tests
4. Manual verification (if user-facing changes)

**Test Coverage**:
- ✅ 58+ existing unit tests (must all pass)
- ✅ Worker integration tests (TAT, WAT, SRT, SDT, PPDT)
- ✅ ViewModel tests (result screens)
- ✅ Architecture tests (SRP, file size, lint)
- ✅ Manual E2E tests (critical user flows)

### Benefits Summary

**Immediate (Phase 1)**:
- ✅ OLQ scoring bug FIXED for all psychology tests
- ✅ User-facing issue resolved

**Strategic (Phases 2-3)**:
- ✅ SRP compliance: Single responsibility per repository
- ✅ Maintainability: Bug fixes isolated to specific domains
- ✅ File size: All files ≤ 300 lines (7x improvement)
- ✅ Testability: Focused unit tests per domain
- ✅ Scalability: New test types easy to add

**Quality**:
- ✅ Zero tech debt
- ✅ SSBMax Quality Framework compliance
- ✅ 100% build success after each phase
- ✅ Comprehensive test coverage
- ✅ Clean architecture

### Risks & Mitigations

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| Build breaks during refactoring | LOW | HIGH | Phased approach + comprehensive tests |
| Tests fail after split | MEDIUM | HIGH | Interface unchanged, delegation only |
| Worker integration breaks | LOW | HIGH | Workers depend on interface, not impl |
| Performance regression | MINIMAL | MEDIUM | No logic changes, pure refactoring |
| Merge conflicts | LOW | MEDIUM | Complete all phases in single branch |

### Acceptance Criteria

**Phase 1 Complete**:
- [ ] OLQ scores persist for TAT/WAT/SRT/SDT after notification taps
- [ ] No "analyzing..." stuck states
- [ ] All tests pass

**Phase 3 Complete**:
- [ ] FirestoreSubmissionRepository ≤ 200 lines
- [ ] All specialized repositories ≤ 300 lines
- [ ] Each repository has single, clear responsibility
- [ ] All 42 interface methods still implemented

**Final Acceptance**:
- [ ] Build: `./gradlew build` succeeds with 0 errors
- [ ] Tests: All 58+ unit tests pass
- [ ] Lint: Zero new lint errors
- [ ] E2E: Manual verification complete for all test types
- [ ] Docs: Architecture diagrams + troubleshooting guides updated
- [ ] Quality: SSBMax Quality Framework checklist ✅

---

## Post-Refactoring Maintenance

### Adding New Test Types

When adding a new test type (e.g., "Command Task"):

1. **Choose Repository**: Based on test domain
   - Psychology test with OLQ? → `PsychTestSubmissionRepository`
   - GTO test? → `GTOSubmissionRepository`
   - New domain? → Create new specialized repository

2. **Add Interface Method**: `SubmissionRepository.kt`
3. **Implement in Specialized Repo**: Add methods + logic
4. **Add Facade Delegation**: `FirestoreSubmissionRepository.kt`
5. **Add Tests**: Unit tests + integration tests
6. **Verify**: `./gradlew build && ./gradlew test`

### Code Review Checklist

When reviewing submission repository changes:
- [ ] File size still ≤ 300 lines?
- [ ] Single responsibility maintained?
- [ ] OLQ regression protection present (if psychology test)?
- [ ] Tests added for new functionality?
- [ ] No commented-out code?
- [ ] Follows SSBMax Quality Framework?

### Performance Monitoring

No performance impact expected (pure refactoring), but monitor:
- Submission write latency (should be unchanged)
- Observer emission frequency (should be unchanged)
- Worker completion times (should be unchanged)

---

## Conclusion

This phased plan delivers:
1. **Immediate**: OLQ scoring bug fix (user-facing critical)
2. **Strategic**: Clean, maintainable architecture (developer experience)
3. **Quality**: Zero tech debt, 100% build success (SSBMax standards)

**Estimated Total Time**: 14-18.5 hours over 2-3 days
**Risk Level**: LOW (phased approach with comprehensive testing)
**Confidence**: HIGH (existing test coverage + manual verification)

**Next Steps**: Execute Phase 0 to establish baseline.
