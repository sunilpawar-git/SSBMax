# FirestoreSubmissionRepository Domain Split - Phase-Wise Plan

## 🎯 Objective
Reduce `FirestoreSubmissionRepository.kt` from **1,902 lines** to **~200 lines** (facade pattern) while maintaining **100% test success** and **ZERO tech debt**.

## Quality Framework Compliance

Per [SSBMax_Quality_Framework.md](../architecture/SSBMax_Quality_Framework.md):
- ✅ File Size: All files ≤300 lines
- ✅ No commented code
- ✅ Single Responsibility Principle
- ✅ Complete implementation (no partial features)
- ✅ 100% test pass rate after each phase

---

## Current State (Post-Phase 3)

| Metric | Value |
|--------|-------|
| **File Size** | 1,902 lines |
| **Total Methods** | 59 |
| **Test Status** | ✅ All passing (`core:data:testDebugUnitTest`) |

### Method Distribution
- Submit methods (10): TAT, WAT, SRT, SDT, PPDT, OIR, PIQ, GPE, GD, Lecturette
- Generic CRUD (6): get, observe, delete, getUserSubmissions, etc.
- Psych OLQ methods (16): TAT/WAT/SRT/SDT get/update/observe (×4 each)
- PIQ-specific (3): parse, getLatest, etc.
- Archival (3): archiveOldSubmissions, etc.

---

## Phase 4: Extract parsePIQ to SubmissionMappers

**Duration:** 30 minutes  
**Goal:** Move 170-line `parsePIQSubmission()` to shared mappers file

### Pre-Implementation Checklist
- [ ] Verify `SubmissionMappers.kt` exists and compiles
- [ ] Confirm parsePIQ is `private` in current file
- [ ] No tests directly reference parsePIQ (it's private)

### Implementation Steps

1. **Copy `parsePIQSubmission()` to SubmissionMappers.kt**
   - Change visibility: `private` → `internal`
   - Verify all helper functions are included
   
2. **Update FirestoreSubmissionRepository.kt**
   - Remove `private fun parsePIQSubmission()`
   - No other changes needed (method was private)

3. **Verify Build**
   ```bash
   ./gradlew :core:data:compileDebugKotlin
   ./gradlew :core:data:testDebugUnitTest
   ```

### Success Criteria
- [ ] `SubmissionMappers.kt` ≤600 lines (490 + 170 = 660, refactor if needed)
- [ ] `FirestoreSubmissionRepository.kt` reduced to ~1,732 lines
- [ ] ✅ All tests pass
- [ ] ✅ Build successful

---

## Phase 5: Create CommonSubmissionRepository

**Duration:** 1 hour  
**Goal:** Extract generic CRUD operations (lowest risk)

### Pre-Implementation Checklist
- [ ] Identify all common methods: `getSubmission`, `observeSubmission`, `getUserSubmissions`, `getUserSubmissionsByTestType`, `observeUserSubmissions`, `updateSubmissionStatus`, `deleteSubmission`
- [ ] Verify no domain-specific logic in these methods
- [ ] Check if any tests mock these methods

### Implementation Steps

1. **Create `CommonSubmissionRepository.kt` (~150 lines)**
   ```kotlin
   @Singleton
   class CommonSubmissionRepository @Inject constructor() {
       private val firestore = FirebaseFirestore.getInstance()
       private val submissionsCollection = firestore.collection("submissions")
       
       fun getSubmission(submissionId: String): Result<Map<String, Any>?> { ... }
       fun observeSubmission(submissionId: String): Flow<Map<String, Any>?> { ... }
       fun getUserSubmissions(userId: String, limit: Int): Result<List<Map<String, Any>>> { ... }
       fun getUserSubmissionsByTestType(...): Result<List<Map<String, Any>>> { ... }
       fun observeUserSubmissions(userId: String, limit: Int): Flow<List<Map<String, Any>>> { ... }
       fun updateSubmissionStatus(submissionId: String, status: SubmissionStatus): Result<Unit> { ... }
       fun deleteSubmission(submissionId: String): Result<Unit> { ... }
   }
   ```

2. **Update FirestoreSubmissionRepository.kt**
   - Inject `CommonSubmissionRepository`
   - Delegate: `override fun getSubmission(...) = commonRepo.getSubmission(...)`
   - Remove original implementations (~225 lines)

3. **Update DataModule.kt**
   ```kotlin
   @Provides
   @Singleton
   fun provideCommonSubmissionRepository(): CommonSubmissionRepository = 
       CommonSubmissionRepository()
   ```

4. **Verify Build & Tests**
   ```bash
   ./gradlew :core:data:compileDebugKotlin
   ./gradlew :core:data:testDebugUnitTest
   ```

### Success Criteria
- [ ] `CommonSubmissionRepository.kt` = ~150 lines
- [ ] `FirestoreSubmissionRepository.kt` reduced to ~1,507 lines
- [ ] ✅ All tests pass (no regression)
- [ ] ✅ Build successful

---

## Phase 6: Create SubmissionArchiveRepository

**Duration:** 30 minutes  
**Goal:** Extract archival/cleanup operations

### Pre-Implementation Checklist
- [ ] Identify methods: `archiveOldSubmissions`, `deleteOldArchivedSubmissions`
- [ ] Verify no dependencies on other repositories

### Implementation Steps

1. **Create `SubmissionArchiveRepository.kt` (~100 lines)**
   ```kotlin
   @Singleton
   class SubmissionArchiveRepository @Inject constructor() {
       private val firestore = FirebaseFirestore.getInstance()
       
       suspend fun archiveOldSubmissions(daysOld: Int, batchSize: Int): Result<Int> { ... }
       suspend fun deleteOldArchivedSubmissions(daysOld: Int, batchSize: Int): Result<Int> { ... }
   }
   ```

2. **Update FirestoreSubmissionRepository.kt**
   - Inject `SubmissionArchiveRepository`
   - Delegate archival methods
   - Remove implementations (~100 lines)

3. **Update DataModule.kt**

4. **Verify Build & Tests**

### Success Criteria
- [ ] `SubmissionArchiveRepository.kt` = ~100 lines
- [ ] `FirestoreSubmissionRepository.kt` reduced to ~1,407 lines
- [ ] ✅ All tests pass
- [ ] ✅ Build successful

---

## Phase 7: Create GTOSubmissionRepository

**Duration:** 1 hour  
**Goal:** Extract GTO test submissions (GPE, GD, Lecturette)

### Pre-Implementation Checklist
- [ ] Identify methods: `submitGPE`, `submitGD`, `submitLecturette`
- [ ] Verify `SubmissionMappers.kt` has GTO mappers
- [ ] Check if `GTOTestViewModelTest` or similar tests exist

### Implementation Steps

1. **Create `GTOSubmissionRepository.kt` (~200 lines)**
   ```kotlin
   @Singleton
   class GTOSubmissionRepository @Inject constructor() {
       private val firestore = FirebaseFirestore.getInstance()
       private val submissionsCollection = firestore.collection("submissions")
       
       suspend fun submitGPE(submission: GPESubmission, batchId: String?): Result<String> { ... }
       suspend fun submitGD(submission: GDSubmission, batchId: String?): Result<String> { ... }
       suspend fun submitLecturette(submission: LecturetteSubmission, batchId: String?): Result<String> { ... }
   }
   ```

2. **Update FirestoreSubmissionRepository.kt**
   - Inject `GTOSubmissionRepository`
   - Delegate: `override suspend fun submitGPE(...) = gtoRepo.submitGPE(...)`
   - Remove implementations

3. **Update DataModule.kt**

4. **Check for Test Impact**
   - Search for tests mocking `submitGPE`, `submitGD`, `submitLecturette`
   - Update if necessary

5. **Verify Build & Tests**
   ```bash
   ./gradlew :core:data:testDebugUnitTest
   ./gradlew :app:testDebugUnitTest --tests "*GTO*"
   ```

### Success Criteria
- [ ] `GTOSubmissionRepository.kt` = ~200 lines
- [ ] `FirestoreSubmissionRepository.kt` reduced to ~1,207 lines
- [ ] ✅ All tests pass
- [ ] ✅ No test regressions in GTO tests

---

## Phase 8: Create PersonalTestSubmissionRepository

**Duration:** 1.5 hours  
**Goal:** Extract PIQ, OIR, PPDT submissions and parsing

### Pre-Implementation Checklist
- [ ] Identify methods: `submitPIQ`, `submitOIR`, `submitPPDT`, `getPPDTSubmission`, `getLatestPIQ/OIR/PPDT`, `parseOIR`, `parsePPDT`
- [ ] Verify `parsePIQ` already in SubmissionMappers (Phase 4)
- [ ] Check `PPDTTestViewModelTest`, `OIRTestViewModelTest` for mocking

### Implementation Steps

1. **Create `PersonalTestSubmissionRepository.kt` (~280 lines)**
   ```kotlin
   @Singleton  
   class PersonalTestSubmissionRepository @Inject constructor() {
       private val firestore = FirebaseFirestore.getInstance()
       private val submissionsCollection = firestore.collection("submissions")
       
       // Submit methods
       suspend fun submitPIQ(submission: PIQSubmission, batchId: String?): Result<String> { ... }
       suspend fun submitOIR(submission: OIRSubmission, batchId: String?): Result<String> { ... }
       suspend fun submitPPDT(submission: PPDTSubmission, batchId: String?): Result<String> { ... }
       
       // Get latest methods
       suspend fun getLatestPIQ(userId: String): Result<PIQSubmission?> { ... }
       suspend fun getLatestOIR(userId: String): Result<OIRSubmission?> { ... }
       suspend fun getLatestPPDT(userId: String): Result<PPDTSubmission?> { ... }
       
       // Parse methods
       fun parsePIQ(data: Map<*, *>): PIQSubmission? { ... }
       fun parseOIR(data: Map<*, *>): OIRSubmission? { ... }
       fun parsePPDT(data: Map<*, *>): PPDTSubmission? { ... }
       
       // PPDT-specific (OLQ)
       suspend fun getPPDTSubmission(submissionId: String): Result<PPDTSubmission?> { ... }
       suspend fun updatePPDTAnalysisStatus(...): Result<Unit> { ... }
       suspend fun updatePPDTOLQResult(...): Result<Unit> { ... }
   }
   ```

2. **Update FirestoreSubmissionRepository.kt**
   - Inject `PersonalTestSubmissionRepository`
   - Delegate all PIQ/OIR/PPDT methods
   - Remove implementations

3. **Update DataModule.kt**

4. **Update Tests**
   - **CRITICAL**: Update `PPDTTestViewModelTest` mock:
     ```kotlin
     coEvery { mockSubmissionRepo.submitPPDT(any(), any()) } returns Result.success("id")
     ```
   - Check `OIRTestViewModelTest`, `PIQTestViewModelTest` if they exist

5. **Verify Build & Tests**
   ```bash
   ./gradlew :core:data:testDebugUnitTest
   ./gradlew :app:testDebugUnitTest --tests "*PPDT*"
   ./gradlew :app:testDebugUnitTest --tests "*OIR*"
   ./gradlew :app:testDebugUnitTest --tests "*PIQ*"
   ```

### Success Criteria
- [ ] `PersonalTestSubmissionRepository.kt` = ~280 lines
- [ ] `FirestoreSubmissionRepository.kt` reduced to ~927 lines
- [ ] ✅ All unit tests pass (especially PPDT tests)
- [ ] ✅ No test regressions

---

## Phase 9: Create PsychTestSubmissionRepository

**Duration:** 2 hours  
**Goal:** Extract psychology test operations (TAT, WAT, SRT, SDT) - most complex

### Pre-Implementation Checklist
- [ ] Identify 16 methods: submit×4, get×4, observe×4, updateAnalysisStatus×4, updateOLQResult×4
- [ ] Verify OLQ regression filter in SubmissionMappers
- [ ] Check TAT/WAT/SRT/SDT test files

### Implementation Steps

1. **Create `PsychTestSubmissionRepository.kt` (~250 lines)**
   ```kotlin
   @Singleton
   class PsychTestSubmissionRepository @Inject constructor() {
       private val firestore = FirebaseFirestore.getInstance()
       private val submissionsCollection = firestore.collection("submissions")
       
       // TAT methods
       suspend fun submitTAT(submission: TATSubmission, batchId: String?): Result<String> { ... }
       suspend fun getTATSubmission(submissionId: String): Result<TATSubmission?> { ... }
       suspend fun updateTATAnalysisStatus(...): Result<Unit> { ... }
       suspend fun updateTATOLQResult(...): Result<Unit> { ... }
       fun observeTATSubmission(submissionId: String): Flow<TATSubmission?> { ... }
       
       // WAT, SRT, SDT methods (same pattern)
       ...
   }
   ```

2. **Update FirestoreSubmissionRepository.kt**
   - Inject `PsychTestSubmissionRepository`
   - Delegate all TAT/WAT/SRT/SDT methods
   - Remove implementations (~240 lines)

3. **Update DataModule.kt**

4. **Update Tests**
   - Check `TATTestViewModelTest`, `WATTestViewModelTest`, etc.
   - Update any mocks if needed

5. **Verify Build & Tests**
   ```bash
   ./gradlew :core:data:testDebugUnitTest
   ./gradlew :app:testDebugUnitTest --tests "*TAT*"  
   ./gradlew :app:testDebugUnitTest --tests "*WAT*"
   ./gradlew :app:testDebugUnitTest --tests "*SRT*"
   ./gradlew :app:testDebugUnitTest --tests "*SDT*"
   ```

### Success Criteria
- [ ] `PsychTestSubmissionRepository.kt` = ~250 lines
- [ ] `FirestoreSubmissionRepository.kt` reduced to ~687 lines
- [ ] ✅ All tests pass
- [ ] ✅ No regressions in psychology test suite

---

## Phase 10: Convert to Facade Pattern

**Duration:** 1 hour  
**Goal:** Convert main repository to pure delegation facade (~200 lines)

### Pre-Implementation Checklist
- [ ] Verify all 5 sub-repositories created and tested
- [ ] Confirm `SubmissionRepository` interface unchanged
- [ ] Check all methods are delegated

### Implementation Steps

1. **Update `FirestoreSubmissionRepository.kt`**
   ```kotlin
   @Singleton
   class FirestoreSubmissionRepository @Inject constructor(
       private val psychTest: PsychTestSubmissionRepository,
       private val gto: GTOSubmissionRepository,
       private val personal: PersonalTestSubmissionRepository,
       private val common: CommonSubmissionRepository,
       private val archive: SubmissionArchiveRepository
   ) : SubmissionRepository {
       
       private val firestore = FirebaseFirestore.getInstance()
       private val submissionsCollection = firestore.collection("submissions")
       
       companion object {
           private const val TAG = "FirestoreSubmissionRepository"
           // Keep constants
       }
       
       // Delegate ALL methods
       override suspend fun submitTAT(...) = psychTest.submitTAT(...)
       override suspend fun submitWAT(...) = psychTest.submitWAT(...)
       // ... all other methods
       
       override fun getSubmission(...) = common.getSubmission(...)
       override fun observeSubmission(...) = common.observeSubmission(...)
       // ... etc.
   }
   ```

2. **Remove All Implementation Code**
   - Keep only delegation calls
   - Preserve companion object with constants

3. **Final DataModule.kt Update**
   ```kotlin
   @Module
   @InstallIn(SingletonComponent::class)
   object DataModule {
       
       @Provides
       @Singleton
       fun provideCommonSubmissionRepository(): CommonSubmissionRepository = 
           CommonSubmissionRepository()
       
       @Provides
       @Singleton
       fun provideArchiveRepository(): SubmissionArchiveRepository = 
           SubmissionArchiveRepository()
       
       @Provides
       @Singleton
       fun provideGTORepository(): GTOSubmissionRepository = 
           GTOSubmissionRepository()
       
       @Provides
       @Singleton
       fun providePersonalTestRepository(): PersonalTestSubmissionRepository = 
           PersonalTestSubmissionRepository()
       
       @Provides
       @Singleton
       fun providePsychTestRepository(): PsychTestSubmissionRepository = 
           PsychTestSubmissionRepository()
       
       @Provides
       @Singleton
       fun provideSubmissionRepository(
           psychTest: PsychTestSubmissionRepository,
           gto: GTOSubmissionRepository,
           personal: PersonalTestSubmissionRepository,
           common: CommonSubmissionRepository,
           archive: SubmissionArchiveRepository
       ): SubmissionRepository = FirestoreSubmissionRepository(
           psychTest, gto, personal, common, archive
       )
   }
   ```

4. **Verify Complete Build**
   ```bash
   ./gradlew :core:data:compileDebugKotlin
   ./gradlew :core:data:testDebugUnitTest
   ./gradlew :app:compileDebugKotlin
   ./gradlew :app:testDebugUnitTest
   ./gradlew build
   ```

### Success Criteria
- [ ] `FirestoreSubmissionRepository.kt` = ~200 lines ✅
- [ ] All 5 sub-repositories exist
- [ ] ✅ Full build successful
- [ ] ✅ **All 576 app tests pass**
- [ ] ✅ **All core:data tests pass**

---

## Phase 11: Final Verification & Documentation

**Duration:** 30 minutes  
**Goal:** Ensure zero tech debt, update docs

### Verification Checklist

#### File Size Compliance
- [ ] `FirestoreSubmissionRepository.kt` ≤ 300 lines (target: ~200)
- [ ] `CommonSubmissionRepository.kt` ≤ 300 lines (target: ~150)
- [ ] `SubmissionArchiveRepository.kt` ≤ 300 lines (target: ~100)
- [ ] `GTOSubmissionRepository.kt` ≤ 300 lines (target: ~200)
- [ ] `PersonalTestSubmissionRepository.kt` ≤ 300 lines (target: ~280)
- [ ] `PsychTestSubmissionRepository.kt` ≤ 300 lines (target: ~250)
- [ ] `SubmissionMappers.kt` ≤ 700 lines (currently ~660 after Phase 4)

#### Quality Framework Compliance
- [ ] No commented-out code in any file
- [ ] No magic numbers (all extracted as constants)
- [ ] All files follow Single Responsibility Principle
- [ ] No incomplete features
- [ ] No Android dependencies in domain layer

#### Test Coverage
- [ ] ✅ `./gradlew :core:data:testDebugUnitTest` - 100% pass
- [ ] ✅ `./gradlew :app:testDebugUnitTest` - 100% pass (576 tests)
- [ ] ✅ `./gradlew build` - SUCCESS

#### Documentation Updates
- [ ] Update `FirestoreSubmissionRepository_Refactoring_Plan.md` with results
- [ ] Create architecture diagram showing facade pattern
- [ ] Document sub-repository responsibilities

### Implementation Steps

1. **Run Full Verification Suite**
   ```bash
   ./gradlew clean
   ./gradlew build
   ./gradlew test
   wc -l core/data/src/main/kotlin/com/ssbmax/core/data/remote/*.kt
   ```

2. **Check for Tech Debt**
   ```bash
   # Search for commented code
   grep -r "//.*TODO\|//.*FIXME\|//.*HACK" core/data/src/
   
   # Search for magic numbers
   grep -rE "\b[0-9]{2,}\b" core/data/src/ | grep -v "\.kt:"
   ```

3. **Update Documentation**
   - Update this plan with actual line counts
   - Create final walkthrough showing before/after

### Success Criteria
- [ ] ✅ All files ≤300 lines
- [ ] ✅ Zero commented code
- [ ] ✅ Zero magic numbers
- [ ] ✅ 100% test pass rate
- [ ] ✅ Documentation updated

---

## Summary

| Phase | Duration | Result | Line Reduction |
|-------|----------|--------|----------------|
| 4: Extract parsePIQ | 30 min | 1,732 lines | -170 |
| 5: CommonRepository | 1 hour | 1,507 lines | -225 |
| 6: ArchiveRepository | 30 min | 1,407 lines | -100 |
| 7: GTORepository | 1 hour | 1,207 lines | -200 |
| 8: PersonalTestRepository | 1.5 hours | 927 lines | -280 |
| 9: PsychTestRepository | 2 hours | 687 lines | -240 |
| 10: Facade Conversion | 1 hour | **~200 lines** | -487 |
| 11: Verification | 30 min | ✅ Verified | - |
| **Total** | **8.5 hours** | **200 lines** | **-1,702 lines** |

## Pre-Requisites (Completed ✅)
1. Phase 0: Pre-implementation validation
2. Phase 1: OLQ regression protection  
3. Phase 2: Common helpers extracted
4. Phase 3: SubmissionMappers.kt created

## New Files Created
1. `CommonSubmissionRepository.kt` (~150 lines)
2. `SubmissionArchiveRepository.kt` (~100 lines)
3. `GTOSubmissionRepository.kt` (~200 lines)
4. `PersonalTestSubmissionRepository.kt` (~280 lines)
5. `PsychTestSubmissionRepository.kt` (~250 lines)
6. Updated: `SubmissionMappers.kt` (~660 lines)

## Quality Guarantees
- ✅ **Zero tech debt** - Quality Framework fully followed
- ✅ **100% test success** - Verified after each phase
- ✅ **File size compliance** - All files ≤300 lines
- ✅ **Single Responsibility** - Each repository has one domain
- ✅ **Backward compatible** - Interface unchanged, pure delegation
