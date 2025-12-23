# SSBMax Test Implementation Reference
**Knowledge Base for Bug Fixing & System Understanding**

Last Updated: December 23, 2025

---

## Table of Contents
1. [System Overview](#system-overview)
2. [Test-by-Test Implementation](#test-by-test-implementation)
3. [Unified OLQ Scoring System](#unified-olq-scoring-system)
4. [Dashboard Integration](#dashboard-integration)
5. [Data Flow Architecture](#data-flow-architecture)
6. [Troubleshooting Guide](#troubleshooting-guide)

---

## System Overview

### Architecture Pattern
- **MVVM**: ViewModel → Repository → Firestore
- **Navigation**: ID-based (process-death safe)
- **State Management**: StateFlow with `.update {}` pattern
- **Background Processing**: WorkManager with retry logic
- **AI Service**: Gemini Flash 1.5 via Cloud Functions

### Test Categories
- **Phase 1**: OIR, PPDT (instant + OLQ scoring)
- **Phase 2 Psychology**: TAT, WAT, SRT, SDT (OLQ scoring)
- **Phase 2 GTO**: GD, GPE, Lecturette (OLQ scoring)
- **Phase 2 Interview**: AI-powered OLQ assessment

---

## Test-by-Test Implementation

### 1. OIR (Officer Intelligence Rating)

**Type**: Multiple choice (50 questions, 30 minutes)

**User Response Flow**:
```
User selects option → Answer saved in session → Next question
Timer countdown (delta-based, 200ms updates)
Submit → Calculate instant score
```

**Timer Implementation**:
- File: `OIRTestViewModel.kt` lines 171-215
- Pattern: Delta-based countdown using `System.currentTimeMillis()`
- Auto-submit when time expires

**Response Storage**:
```
OIRTestSession.answers: Map<String, OIRAnswer>
  ├── questionId → selectedOption
  └── skipped flag
```

**Scoring**: Client-side instant calculation
```kotlin
correctAnswers / totalQuestions × 100 = percentageScore
Category breakdown by OIRQuestionType
Difficulty breakdown by QuestionDifficulty
```

**Firestore Storage**:
```
submissions/{submissionId}
  ├── userId
  ├── testType: "OIR"
  ├── status: "SUBMITTED_PENDING_REVIEW"
  └── data:
      └── testResult:
          ├── percentageScore
          ├── correctAnswers
          ├── categoryScores
          └── difficultyBreakdown
```

**Result Display**: Instant (no background worker)
- Screen: `OIRTestResultScreen.kt`
- Shows: Percentage, grade, category breakdown, difficulty analysis

**Dashboard Display**: Percentage score (NOT OLQ-based)

---

### 2. PPDT (Picture Perception & Description Test)

**Type**: Story writing (30s viewing + 4min writing)

**User Response Flow**:
```
View image (30s) → Write story (4min) → Submit
Two-phase timer: viewing then writing
```

**Timer Implementation**:
- File: `PPDTTestViewModel.kt` lines 208-291
- Pattern: Two separate timers (viewing, writing)
- Auto-transition from viewing to writing

**Response Storage**:
```
PPDTSubmission
  ├── story: String (user's text)
  ├── charactersCount: Int
  ├── viewingTimeTakenSeconds
  └── writingTimeTakenMinutes
```

**Firestore Storage**:
```
submissions/{submissionId}
  ├── userId, testType: "PPDT"
  ├── status: "SUBMITTED_PENDING_REVIEW"
  └── data:
      ├── story
      ├── analysisStatus: "PENDING_ANALYSIS"
      └── olqResult: null (filled by worker)

ppdt_results/{submissionId}  ← Written by worker
  ├── userId
  ├── olqScores: Map<OLQ, OLQScore>
  ├── overallScore
  ├── strengths, weaknesses, recommendations
  └── analyzedAt
```

**Background Worker**: `PPDTAnalysisWorker.kt`
```
1. Fetch submission from Firestore
2. Generate prompt: PsychologyTestPrompts.generatePPDTAnalysisPrompt()
3. Call Gemini AI: aiService.analyzePPDTResponse()
4. Parse JSON response → OLQAnalysisResult
5. Write to ppdt_results/{submissionId}
6. Update submission status → COMPLETED
7. Send push notification
```

**Gemini Prompt Structure**:
- Input: Story text, character count, completion time
- Instructions: Analyze for all 15 OLQs (1-10 scale, lower = better)
- Output: JSON with olqScores, overallScore, strengths, weaknesses

**Result Display**:
- Screen: `PPDTSubmissionResultScreen.kt`
- Fetches from: `ppdt_results/{submissionId}` via ViewModel
- Shows: OLQ breakdown, overall rating, strengths/weaknesses

---

### 3. TAT (Thematic Apperception Test)

**Type**: Story writing (12 images, 4min each)

**User Response Flow**:
```
View image → Write story (4min) → Next image
Repeat 12 times → Submit all
```

**Timer Implementation**:
- File: `TATTestViewModel.kt` lines 305-359
- Pattern: Per-image countdown (240 seconds each)
- Auto-advance when time expires

**Response Storage**:
```
TATSubmission
  └── stories: List<TATStory>
      ├── imageUrl
      ├── storyText
      ├── charCount
      └── timeTakenSeconds
```

**Firestore Storage**:
```
submissions/{submissionId}
  ├── testType: "TAT"
  └── data:
      ├── stories: [12 stories]
      └── analysisStatus: "PENDING_ANALYSIS"

psych_results/{submissionId}  ← Unified collection
  ├── userId
  ├── testType: "TAT"
  ├── olqScores: Map<OLQ, OLQScore>
  └── overallScore
```

**Background Worker**: `TATAnalysisWorker.kt`
```
1. Fetch submission with 12 stories
2. Generate prompt with all stories
3. Gemini analyzes patterns across stories
4. Write to psych_results/{submissionId}
5. Update submission → COMPLETED
6. Send notification
```

**Gemini Analysis Focus**:
- Hero characteristics (positive, proactive)
- Problem-solving approaches
- Outcome positivity
- Leadership indicators
- OLQ pattern detection across 12 stories

**Result Display**: `TATSubmissionResultScreen.kt`

---

### 4. WAT (Word Association Test)

**Type**: 60 words, 15 seconds each

**User Response Flow**:
```
Word displayed → Type response (15s) → Auto-advance
Repeat 60 times → Auto-submit
```

**Timer Implementation**:
- File: `WATTestViewModel.kt` lines 223-265
- Pattern: Per-word countdown (15 seconds)
- Auto-submit response when time expires
- No manual "Next" button

**Response Storage**:
```
WATSubmission
  └── responses: List<WATWordResponse>
      ├── word: "LEADER"
      ├── response: "Takes charge"
      ├── timeTakenSeconds: 8
      └── isSkipped: false
```

**Firestore Storage**:
```
submissions/{submissionId}
  ├── testType: "WAT"
  └── data:
      └── responses: [60 responses]

psych_results/{submissionId}
  ├── testType: "WAT"
  └── olqScores: Map<OLQ, OLQScore>
```

**Background Worker**: `WATAnalysisWorker.kt`

**Gemini Analysis Focus**:
- Response speed (faster = better)
- Positive vs negative associations
- Leadership/action-oriented words
- Avoid: Personal statements, facts, idioms
- Prefer: Observational statements

**Result Display**: `WATSubmissionResultScreen.kt`

---

### 5. SRT (Situation Reaction Test)

**Type**: 60 situations, 30 seconds each

**User Response Flow**:
```
Situation displayed → Write reaction (30s) → Next
Repeat 60 times → Submit
```

**Timer Implementation**:
- File: `SRTTestViewModel.kt` lines 258-301
- Pattern: Per-situation countdown (30 seconds)
- Manual "Next" button (no auto-advance)

**Response Storage**:
```
SRTSubmission
  └── responses: List<SRTSituationResponse>
      ├── situation: "Fire in building"
      ├── reaction: "Alert others, call 911"
      ├── timeTakenSeconds: 20
      └── charCount: 25
```

**Firestore Storage**:
```
submissions/{submissionId}
  ├── testType: "SRT"
  └── data: responses

psych_results/{submissionId}
  ├── testType: "SRT"
  └── olqScores
```

**Background Worker**: `SRTAnalysisWorker.kt`

**Gemini Analysis Focus**:
- Proactive vs reactive responses
- Helping others mentioned
- Leadership actions
- Practical solutions
- Speed of decision-making

**Result Display**: `SRTSubmissionResultScreen.kt`

---

### 6. SDT (Self Description Test)

**Type**: 4 questions, 15 minutes total

**User Response Flow**:
```
Question 1: Parents' opinion → Write
Question 2: Teachers' opinion → Write
Question 3: Friends' opinion → Write
Question 4: Own opinion → Write
Shared 15-minute timer
```

**Timer Implementation**:
- File: `SDTTestViewModel.kt` lines 300-343
- Pattern: Shared timer (900 seconds total)
- User can switch between questions freely

**Response Storage**:
```
SDTSubmission
  └── responses: List<SDTQuestionResponse>
      ├── question: "How parents see you"
      ├── answer: (user text)
      ├── charCount
      └── timeTakenSeconds
```

**Firestore Storage**:
```
submissions/{submissionId}
  ├── testType: "SD"
  └── data: responses

psych_results/{submissionId}
  ├── testType: "SD"
  └── olqScores
```

**Background Worker**: `SDTAnalysisWorker.kt`
- Enqueued: Line 264 in `SDTTestViewModel.kt`
- Method: `enqueueSDTAnalysisWorker()` (lines 350-365)

**Gemini Analysis Focus**:
- Self-awareness consistency
- Honesty in self-perception
- Goal orientation
- Maturity indicators
- Social relationships

**Result Display**: `SDTSubmissionResultScreen.kt`

---

### 7. GD (Group Discussion)

**Type**: Topic-based discussion, 20 minutes

**User Response Flow**:
```
Topic displayed → Type response (20min) → Submit
White noise audio during test (optional)
```

**Timer Implementation**:
- File: `GDTestViewModel.kt`
- Pattern: Single 20-minute countdown

**Response Storage**:
```
GTOSubmission.GDSubmission
  ├── topic: "Leadership in military"
  ├── response: (user text)
  ├── charCount
  └── timeSpent
```

**Firestore Storage**:
```
submissions/{submissionId}
  ├── testType: "GTO_GD"
  └── data: { topic, response, timeSpent }

gto_results/{submissionId}  ← Separate collection
  ├── userId
  ├── testType: "GROUP_DISCUSSION"
  ├── olqScores: Map<OLQ, OLQScore>
  └── overallScore
```

**Background Worker**: `GTOAnalysisWorker.kt`
```
1. Fetch submission
2. Generate prompt: GTOTestPrompts (not PsychologyTestPrompts)
3. Gemini analysis
4. Batch write:
   - gto_results/{submissionId}
   - Update submission status → COMPLETED
5. Send notification
```

**Gemini Analysis Focus**:
- Communication clarity
- Logical argumentation
- Team orientation
- Leadership indicators

**Result Display**: `GDSubmissionResultScreen.kt`

---

### 8. GPE (Group Planning Exercise)

**Type**: Tactical scenario planning, 30 minutes

**User Response Flow**:
```
View image + scenario (60s) → Write plan (29min) → Submit
```

**Timer Implementation**:
- Two-phase: 60s viewing + 1740s planning

**Response Storage**:
```
GTOSubmission.GPESubmission
  ├── imageUrl
  ├── scenario
  ├── solution (optional)
  ├── plan: (user text)
  └── characterCount
```

**Firestore Storage**:
```
submissions/{submissionId}
  ├── testType: "GTO_GPE"
  └── data: scenario + plan

gto_results/{submissionId}
  ├── testType: "GROUP_PLANNING_EXERCISE"
  └── olqScores
```

**Background Worker**: `GTOAnalysisWorker.kt` (same as GD)

**Gemini Analysis Focus**:
- Tactical planning
- Resource utilization
- Risk assessment
- Organizing ability
- Decision-making

**Result Display**: `GPESubmissionResultScreen.kt`

---

### 9. Lecturette

**Type**: 3-minute speech, 4 topic choices

**User Response Flow**:
```
4 topics shown → Select 1 → Prepare (3min) → Record/Type speech → Submit
```

**Timer Implementation**:
- 3-minute countdown for speech delivery

**Response Storage**:
```
GTOSubmission.LecturetteSubmission
  ├── topicChoices: [4 topics]
  ├── selectedTopic
  ├── speechTranscript: (text)
  └── charCount
```

**Firestore Storage**:
```
submissions/{submissionId}
  ├── testType: "GTO_LECTURETTE"
  └── data: topic + transcript

gto_results/{submissionId}
  ├── testType: "LECTURETTE"
  └── olqScores
```

**Background Worker**: `GTOAnalysisWorker.kt`

**Gemini Analysis Focus**:
- Communication skills
- Knowledge depth
- Confidence indicators
- Power of expression
- Organizing thoughts

**Result Display**: `LecturetteResultScreen.kt`

---

### 10. Interview

**Type**: Multi-question AI interview, ~10-15 minutes

**User Response Flow**:
```
Question displayed → Type/Speak response → Submit
AI generates follow-up → Repeat
All responses saved → Submit interview
```

**Timer Implementation**:
- Per-question thinking time tracking
- No hard time limit (user-paced)

**Response Storage**:
```
InterviewResponse (per question)
  ├── questionId
  ├── questionText
  ├── responseText
  ├── responseMode: TEXT | VOICE
  ├── thinkingTimeSec
  └── olqScores: {} (empty until analyzed)

InterviewSession
  ├── sessionId
  ├── questionIds: [5-15 questions]
  └── status: PENDING_ANALYSIS
```

**Firestore Storage**:
```
interview_sessions/{sessionId}
  ├── userId
  ├── mode: MOCK | PRACTICE
  ├── questionIds
  └── status: "PENDING_ANALYSIS"

interview_responses/{responseId}  ← One per question
  ├── sessionId
  ├── questionId
  ├── responseText
  └── olqScores: {} (filled by worker)

interview_results/{sessionId}  ← Final aggregation
  ├── userId
  ├── overallOLQScores: Map<OLQ, OLQScore>
  ├── responses: [all responses with scores]
  └── completedAt
```

**Background Worker**: `InterviewAnalysisWorker.kt`
```
1. Fetch all responses for session
2. For each response:
   - Generate prompt with question context
   - Gemini analyzes for 3-5 target OLQs
   - Parse JSON → update response.olqScores
3. Aggregate all OLQ scores
4. Create InterviewResult
5. Update session → COMPLETED
6. Send notification
```

**Worker Triggering**:
- File: `InterviewSessionViewModel.kt` lines 476-521
- Method: `enqueueAnalysisWorker()` (lines 502-521)

**Gemini Analysis**:
- Per-response analysis (not batch)
- Focuses on 3-5 OLQs per question
- Looks for: Leadership, decision-making, reasoning, responsibility
- Uses: `SSBInterviewPrompts.kt` (not PsychologyTestPrompts)

**Result Display**: `InterviewResultScreen.kt`
- Shows: Overall OLQ averages + per-question breakdown

---

## Unified OLQ Scoring System

### Core Components

**OLQ Enum** (`core/domain/model/interview/OLQ.kt`):
```kotlin
15 OLQs grouped in 4 categories:
├── INTELLECTUAL (4): Effective Intelligence, Reasoning, Organizing, Expression
├── SOCIAL (3): Social Adjustment, Cooperation, Responsibility
├── DYNAMIC (5): Initiative, Confidence, Speed of Decision, Influence, Liveliness
└── CHARACTER (3): Determination, Courage, Stamina
```

**OLQ Score** (1-10 scale, **LOWER = BETTER**):
```
1-3: Exceptional (rare, outstanding)
4:   Excellent (top tier)
5:   Very Good (best common score)
6:   Good (above average)
7:   Average (typical)
8:   Below Average (needs improvement)
9-10: Poor (major deficiency)
```

**OLQAnalysisResult** (`core/domain/model/scoring/UnifiedOLQResult.kt`):
```kotlin
data class OLQAnalysisResult(
    submissionId: String,
    testType: TestType,
    olqScores: Map<OLQ, OLQScore>,  // All 15 OLQs
    overallScore: Float,             // Average of all scores
    overallRating: String,           // "Exceptional", "Good", etc.
    strengths: List<String>,         // Top 3 OLQs (lowest scores)
    weaknesses: List<String>,        // Bottom 3 OLQs (highest scores)
    recommendations: List<String>,
    analyzedAt: Long,
    aiConfidence: Int                // 0-100
)
```

### Analysis Flow

**1. Submission → Worker Enqueue**
```kotlin
// Pattern used by all tests:
viewModelScope.launch {
    val submissionId = submitTest().getOrThrow()
    enqueueAnalysisWorker(submissionId)
    getOLQDashboard.invalidateCache(userId)
}
```

**2. Worker Execution**
```kotlin
// Pattern: PPDTAnalysisWorker.kt (reference implementation)
override suspend fun doWork(): Result {
    val submissionId = inputData.getString(KEY_SUBMISSION_ID)
    
    // 1. Fetch submission
    val submission = submissionRepository.getSubmission(submissionId)
    
    // 2. Verify PENDING_ANALYSIS
    if (submission.analysisStatus != PENDING_ANALYSIS) return
    
    // 3. Update to ANALYZING
    submissionRepository.updateStatus(submissionId, ANALYZING)
    
    // 4. Generate prompt
    val prompt = PsychologyTestPrompts.generate...(submission)
    
    // 5. Call Gemini with retry logic
    val olqScores = analyzeWithRetry(prompt, maxRetries = 3)
    
    // 6. Create OLQAnalysisResult
    val result = OLQAnalysisResult(
        submissionId = submissionId,
        testType = TestType.XXX,
        olqScores = olqScores,  // All 15 OLQs
        overallScore = olqScores.values.map { it.score }.average(),
        // ... compute strengths/weaknesses
    )
    
    // 7. Write to results collection
    submissionRepository.updateOLQResult(submissionId, result)
    
    // 8. Update submission → COMPLETED
    submissionRepository.updateStatus(submissionId, COMPLETED)
    
    // 9. Send notification
    notificationHelper.showResultsReady(submissionId)
    
    return Result.success()
}
```

**3. Retry Logic** (all workers):
```kotlin
private suspend fun analyzeWithRetry(prompt: String): Map<OLQ, OLQScore>? {
    repeat(3) { attempt ->
        try {
            val result = aiService.analyze(prompt)
            if (result.olqScores.size >= 14) {  // Accept 14-15 OLQs
                return fillMissingOLQs(result.olqScores)
            }
        } catch (e: Exception) {
            delay(2000L * (attempt + 1))  // Exponential backoff
        }
    }
    return null  // Failed after 3 retries
}

private fun fillMissingOLQs(scores: Map<OLQ, OLQScore>): Map<OLQ, OLQScore> {
    return OLQ.entries.associateWith { olq ->
        scores[olq] ?: OLQScore(
            score = 6,  // Neutral default
            confidence = 30,
            reasoning = "AI did not assess - neutral assigned"
        )
    }
}
```

### Gemini Prompt Structure

**Common Pattern** (all psychology tests):
```
You are an SSB PSYCHOLOGIST analyzing [TEST_NAME].

CANDIDATE RESPONSES:
[User's test responses]

OLQ SCORING REFERENCE:
[15 OLQ definitions with examples]

SCORING SCALE (SSB Convention - LOWER IS BETTER):
1-2 = Exceptional
3   = Excellent
4   = Very Good
5   = Good
6   = Average
7   = Below Average
8-10 = Poor

DISTRIBUTION GUIDELINE:
- Most candidates: 5-7 (70%)
- Exceptional (1-3) and Poor (8-10): rare (15% each)

YOUR TASK:
Analyze responses and provide OLQ assessment.

OUTPUT FORMAT (Return ONLY valid JSON):
{
  "olqScores": {
    "EFFECTIVE_INTELLIGENCE": {"score": 5, "confidence": 80, "reasoning": "..."},
    "REASONING_ABILITY": {"score": 6, "confidence": 75, "reasoning": "..."},
    ... (all 15 OLQs)
  },
  "overallScore": 5.5,
  "overallRating": "Good",
  "strengths": ["Leadership shown", "Proactive responses"],
  "weaknesses": ["Limited self-awareness", "Reactive patterns"],
  "recommendations": ["Focus on X", "Improve Y"],
  "aiConfidence": 82
}

CRITICAL: Return ONLY JSON. No markdown, no explanations.
```

**Test-Specific Focus**:
- **TAT**: Story heroes, positive outcomes, problem-solving patterns
- **WAT**: Response speed, positive associations, action-orientation
- **SRT**: Proactive responses, helping others, leadership actions
- **SDT**: Self-awareness, goal orientation, maturity
- **GTO**: Team orientation, planning ability, communication
- **Interview**: Specific OLQs per question (3-5 OLQs each)

### Result Storage Patterns

**Psychology Tests** (TAT, WAT, SRT, SDT):
```
psych_results/{submissionId}
  ├── userId (CRITICAL for security rules)
  ├── submissionId
  ├── testType: "TAT" | "WAT" | "SRT" | "SD"
  ├── olqScores: {
  │     "EFFECTIVE_INTELLIGENCE": {score: 5, confidence: 80, reasoning: "..."},
  │     ... (all 15)
  │   }
  ├── overallScore: 5.5
  ├── overallRating: "Good"
  ├── strengths: ["..."]
  ├── weaknesses: ["..."]
  ├── recommendations: ["..."]
  └── analyzedAt: timestamp
```

**GTO Tests** (GD, GPE, Lecturette):
```
gto_results/{submissionId}
  ├── userId
  ├── submissionId
  ├── testType: "GROUP_DISCUSSION" | "GROUP_PLANNING_EXERCISE" | "LECTURETTE"
  ├── olqScores: Map<OLQ, OLQScore>
  └── ... (same as psych_results)
```

**PPDT**:
```
ppdt_results/{submissionId}
  ├── userId
  ├── submissionId
  ├── testType: "PPDT"
  └── ... (same structure)
```

**Interview**:
```
interview_results/{sessionId}
  ├── userId
  ├── sessionId (not submissionId)
  ├── overallOLQScores: Map<OLQ, OLQScore>  ← Aggregated
  ├── responses: [
  │     {questionId, responseText, olqScores: {...}},
  │     ... (per-question OLQ scores)
  │   ]
  └── completedAt
```

---

## Dashboard Integration

### Data Fetching Flow

**Use Case**: `GetOLQDashboardUseCase.kt`

**Execution**:
```kotlin
suspend operator fun invoke(userId: String, forceRefresh: Boolean = false) {
    // 1. Check cache (5-minute TTL)
    val cached = cache[userId]
    if (!forceRefresh && cached != null && !isExpired(cached)) {
        return cached.data
    }
    
    // 2. Fetch all test results in parallel
    val oirResult = getLatestOIRSubmission(userId).getOrNull()?.testResult
    val ppdtResult = getLatestPPDTSubmission(userId).getOrNull()
    val ppdtOLQ = getPPDTResult(ppdtResult?.id).getOrNull()
    
    val tatResult = getTATResult(getLatestTATSubmission(userId).id).getOrNull()
    val watResult = getWATResult(getLatestWATSubmission(userId).id).getOrNull()
    val srtResult = getSRTResult(getLatestSRTSubmission(userId).id).getOrNull()
    val sdResult = getSDTResult(getLatestSDTSubmission(userId).id).getOrNull()
    
    val gtoResults = GTOTestType.entries.associateWith { type ->
        gtoRepository.getUserResults(userId, type).first().firstOrNull()
    }
    
    val interviewResult = interviewRepository.getUserResults(userId).first().firstOrNull()
    
    // 3. Build dashboard data
    val dashboard = OLQDashboardData(
        userId = userId,
        phase1Results = Phase1Results(oirResult, ppdtResult, ppdtOLQ),
        phase2Results = Phase2Results(
            tatResult, watResult, srtResult, sdResult,
            gtoResults, interviewResult
        )
    )
    
    // 4. Compute aggregations ONCE (not in UI)
    val averageOLQScores = computeAverageOLQScores(dashboard)
    val topOLQs = averageOLQScores.sortedBy { it.value }.take(3)
    val improvementOLQs = averageOLQScores.sortedByDescending { it.value }.take(3)
    val overallAverage = computeOverallAverage(dashboard)
    
    // 5. Return processed data
    return ProcessedDashboardData(
        dashboard, averageOLQScores, topOLQs, improvementOLQs, overallAverage
    )
}
```

**Cache Invalidation**:
```kotlin
// Called after every test submission
getOLQDashboard.invalidateCache(userId)

// Locations:
- OIRTestViewModel.kt line 366
- PPDTTestViewModel.kt (similar)
- TATTestViewModel.kt (similar)
- WATTestViewModel.kt line 366
- SRTTestViewModel.kt line 410
- SDTTestViewModel.kt line 274
- GTOTestViewModels (all)
- InterviewViewModel (similar)
```

### OLQ Aggregation Logic

**Average OLQ Scores** (across all tests):
```kotlin
private fun computeAverageOLQScores(dashboard: OLQDashboardData): Map<OLQ, Float> {
    val olqScoresMap = mutableMapOf<OLQ, Float>()
    
    OLQ.entries.forEach { olq ->
        val scores = mutableListOf<Float>()
        
        // Phase 1: PPDT only (OIR not OLQ-based)
        dashboard.phase1Results.ppdtOLQResult?.olqScores?.get(olq)?.score
            ?.let { scores.add(it.toFloat()) }
        
        // Phase 2: All psychology tests
        dashboard.phase2Results.tatResult?.olqScores?.get(olq)?.score
            ?.let { scores.add(it.toFloat()) }
        dashboard.phase2Results.watResult?.olqScores?.get(olq)?.score
            ?.let { scores.add(it.toFloat()) }
        dashboard.phase2Results.srtResult?.olqScores?.get(olq)?.score
            ?.let { scores.add(it.toFloat()) }
        dashboard.phase2Results.sdResult?.olqScores?.get(olq)?.score
            ?.let { scores.add(it.toFloat()) }
        
        // GTO Tests (all 8 types)
        dashboard.phase2Results.gtoResults.values.forEach { gtoResult ->
            gtoResult.olqScores[olq]?.score?.let { scores.add(it.toFloat()) }
        }
        
        // Interview
        dashboard.phase2Results.interviewResult?.overallOLQScores?.get(olq)?.score
            ?.let { scores.add(it.toFloat()) }
        
        if (scores.isNotEmpty()) {
            olqScoresMap[olq] = scores.average().toFloat()
        }
    }
    
    return olqScoresMap
}
```

**Overall Average** (single score across all tests):
```kotlin
private fun computeOverallAverage(dashboard: OLQDashboardData): Float? {
    val allScores = mutableListOf<Float>()
    
    dashboard.phase1Results.ppdtOLQResult?.overallScore?.let { allScores.add(it) }
    dashboard.phase2Results.tatResult?.overallScore?.let { allScores.add(it) }
    dashboard.phase2Results.watResult?.overallScore?.let { allScores.add(it) }
    dashboard.phase2Results.srtResult?.overallScore?.let { allScores.add(it) }
    dashboard.phase2Results.sdResult?.overallScore?.let { allScores.add(it) }
    
    dashboard.phase2Results.gtoResults.values.forEach { 
        allScores.add(it.overallScore) 
    }
    
    dashboard.phase2Results.interviewResult?.getAverageOLQScore()
        ?.let { allScores.add(it) }
    
    return if (allScores.isNotEmpty()) allScores.average().toFloat() else null
}
```

### UI Display

**Dashboard Card**: `OLQDashboardCard.kt`

**Layout**:
```
┌─────────────────────────────────────┐
│ Your SSB Progress    [8/10 Tests]  │
├─────────────┬───────────────────────┤
│  PHASE 1    │     PHASE 2           │
│             │                       │
│  OIR: 85.0  │  Psychology           │
│  PPDT: 5.5  │   TAT: 5.2            │
│             │   WAT: 5.8            │
│             │   SRT: 5.4            │
│             │   Self Desc: 6.1      │
│             │                       │
│             │  GTO                  │
│             │   GD: 5.5             │
│             │   GPE: 5.7            │
│             │   Lecturette: 6.0     │
│             │                       │
│             │  Interview            │
│             │   Interview: 5.6      │
├─────────────┴───────────────────────┤
│ Overall Average: 5.6                │
├─────────────────────────────────────┤
│ 🌟 Your Strengths                   │
│  • Effective Intelligence (4.8)     │
│  • Initiative (5.0)                 │
│  • Reasoning Ability (5.2)          │
├─────────────────────────────────────┤
│ 📈 Focus Areas                      │
│  • Courage (7.2)                    │
│  • Stamina (6.8)                    │
│  • Determination (6.5)              │
└─────────────────────────────────────┘
```

**Color Coding**:
```
Score ≤ 5.0: Green (Good)
Score 5.1-7.0: Amber (Average)
Score > 7.0: Red (Needs Improvement)
```

**Clickable Navigation**:
```kotlin
TestScoreChip(
    testName = "TAT",
    score = dashboard.tatResult?.overallScore,
    onClick = { onNavigateToResult(TestType.TAT, submissionId) }
)
```

---

## Data Flow Architecture

### Complete Flow Diagram

```
┌──────────────────┐
│  User Completes  │
│      Test        │
└────────┬─────────┘
         │
         ▼
┌──────────────────────────────────┐
│ ViewModel.submitTest()           │
│ ├─ Create submission             │
│ ├─ Save to Firestore             │
│ ├─ Enqueue WorkManager job       │
│ ├─ Invalidate dashboard cache    │
│ └─ Navigate to result/pending    │
└────────┬─────────────────────────┘
         │
         ▼
┌──────────────────────────────────┐
│ WorkManager Background Worker    │
│ ├─ Fetch submission from Firestore│
│ ├─ Generate AI prompt           │
│ ├─ Call Gemini AI (with retry)  │
│ ├─ Parse JSON → OLQAnalysisResult│
│ ├─ Write to results collection   │
│ ├─ Update submission → COMPLETED│
│ └─ Send push notification        │
└────────┬─────────────────────────┘
         │
         ▼
┌──────────────────────────────────┐
│ User Taps Notification           │
│ OR Navigates to Home Screen      │
└────────┬─────────────────────────┘
         │
         ▼
┌──────────────────────────────────┐
│ GetOLQDashboardUseCase           │
│ ├─ Check cache (5min TTL)        │
│ ├─ Fetch all test results        │
│ ├─ Compute aggregations           │
│ └─ Return ProcessedDashboardData │
└────────┬─────────────────────────┘
         │
         ▼
┌──────────────────────────────────┐
│ OLQDashboardCard                 │
│ ├─ Display all scores            │
│ ├─ Show strengths/weaknesses     │
│ └─ Navigate to individual results│
└──────────────────────────────────┘
```

### Firestore Collections

```
submissions/{submissionId}
  ├── Used by: All tests
  ├── Contains: Raw test responses + metadata
  ├── Status field: "PENDING_ANALYSIS" → "ANALYZING" → "COMPLETED"
  └── Security: User can read/write own submissions

psych_results/{submissionId}
  ├── Used by: TAT, WAT, SRT, SDT
  ├── Contains: OLQAnalysisResult
  ├── Written by: Workers (not client)
  └── Security: User can read/write own results (userId field required)

gto_results/{submissionId}
  ├── Used by: GD, GPE, Lecturette, PGT, HGT, GOR, IO, CT
  ├── Contains: OLQAnalysisResult
  └── Security: Same as psych_results

ppdt_results/{submissionId}
  ├── Used by: PPDT only
  ├── Contains: OLQAnalysisResult
  └── Security: Same as psych_results

interview_sessions/{sessionId}
  ├── Used by: Interview
  ├── Contains: Session metadata + question IDs
  └── Status: "IN_PROGRESS" → "PENDING_ANALYSIS" → "COMPLETED"

interview_responses/{responseId}
  ├── Used by: Interview (per question)
  ├── Contains: Single question response + olqScores
  └── Updated by: InterviewAnalysisWorker

interview_results/{sessionId}
  ├── Used by: Interview
  ├── Contains: Aggregated OLQ scores + all responses
  └── Created by: InterviewAnalysisWorker
```

### Security Rules Pattern

```javascript
// Firestore rules (firestore.rules)

// Submissions - users can read/write own
match /submissions/{submissionId} {
  allow read: if resource.data.userId == request.auth.uid;
  allow create: if request.resource.data.userId == request.auth.uid;
  allow update: if resource.data.userId == request.auth.uid;
}

// Results - users can read/write own (userId required)
match /psych_results/{resultId} {
  allow read, write: if (resource == null || 
                          resource.data.userId == request.auth.uid || 
                          request.resource.data.userId == request.auth.uid);
}

match /gto_results/{resultId} {
  allow read, write: if (resource == null || 
                          resource.data.userId == request.auth.uid || 
                          request.resource.data.userId == request.auth.uid);
}

match /ppdt_results/{resultId} {
  allow read, write: if (resource == null || 
                          resource.data.userId == request.auth.uid || 
                          request.resource.data.userId == request.auth.uid);
}
```

---

## Troubleshooting Guide

### Common Issues & Solutions

**Issue**: OLQ scores not showing on dashboard
```
Check:
1. Worker completed? → Check Firestore: results collection exists
2. Cache stale? → Pull-to-refresh or wait 5 minutes
3. UserId in results? → Firestore rules require userId field
4. Correct collection? → TAT/WAT/SRT/SDT use psych_results, GTO uses gto_results
```

**Issue**: Worker not triggering
```
Check:
1. WorkManager enqueued? → Look for enqueue...Worker() call in ViewModel
2. Network constraint? → Ensure device has internet
3. Submission saved? → Check Firestore submissions collection
4. Status correct? → Should be PENDING_ANALYSIS after submission
```

**Issue**: AI analysis stuck in "ANALYZING"
```
Check:
1. Gemini API quota → Firebase Functions logs
2. JSON parse error → Worker logs (adb logcat | grep "Worker")
3. Retry exhausted → Check worker retry count (max 3)
4. Network timeout → Increase timeout in AIService
```

**Issue**: Dashboard shows "—" for test
```
Check:
1. Test completed? → Check submissions collection
2. Analysis complete? → Check results collection
3. OLQ result fetched? → GetOLQDashboardUseCase logs
4. Dashboard cache? → Invalidated after submission?
```

**Issue**: Timer not working correctly
```
Check:
1. Delta-based? → Should use System.currentTimeMillis() delta
2. ViewModelScope? → Timer job in viewModelScope (auto-cancels)
3. State updates? → Use .update {} pattern
4. Config change? → Timer should survive rotation (StateFlow)
```

### Debug Log Patterns

**Test Submission**:
```
adb logcat | grep "ViewModel"
✅ Submission successful! ID: abc123
📍 Enqueueing ...AnalysisWorker...
✅ ...AnalysisWorker enqueued successfully
📍 Recording test usage for subscription...
✅ Test usage recorded successfully!
📍 Invalidating OLQ dashboard cache...
✅ Dashboard cache invalidated!
```

**Worker Execution**:
```
adb logcat | grep "AnalysisWorker"
🔄 Starting TAT analysis for submission: abc123
   Step 1: TAT submission found with 12 stories
   Step 2: Status updated to ANALYZING
   Step 3: Generated TAT analysis prompt
   Step 4: AI analysis complete - received 15/15 OLQ scores
   Step 5: Submission updated with OLQ result
✅ Push notification sent successfully!
🎉 TAT analysis completed successfully in 8432ms
```

**Dashboard Loading**:
```
adb logcat | grep "GetOLQDashboard"
📍 Fetching dashboard for user: userId123
✅ Cache hit - returning cached data (load time: 45ms)
OR
❌ Cache miss - fetching from Firestore (load time: 1203ms)
```

### File Locations Quick Reference

**ViewModels**:
- OIR: `app/ui/tests/oir/OIRTestViewModel.kt`
- PPDT: `app/ui/tests/ppdt/PPDTTestViewModel.kt`
- TAT: `app/ui/tests/tat/TATTestViewModel.kt`
- WAT: `app/ui/tests/wat/WATTestViewModel.kt`
- SRT: `app/ui/tests/srt/SRTTestViewModel.kt`
- SDT: `app/ui/tests/sdt/SDTTestViewModel.kt`
- GD: `app/ui/tests/gto/gd/GDTestViewModel.kt`
- GPE: `app/ui/tests/gpe/GPETestViewModel.kt`
- Lecturette: `app/ui/tests/gto/lecturette/LecturetteTestViewModel.kt`
- Interview: `app/ui/interview/session/InterviewSessionViewModel.kt`

**Workers**:
- PPDT: `app/workers/PPDTAnalysisWorker.kt`
- TAT: `app/workers/TATAnalysisWorker.kt`
- WAT: `app/workers/WATAnalysisWorker.kt`
- SRT: `app/workers/SRTAnalysisWorker.kt`
- SDT: `app/workers/SDTAnalysisWorker.kt`
- GTO: `app/workers/GTOAnalysisWorker.kt`
- Interview: `app/workers/InterviewAnalysisWorker.kt`

**Repositories**:
- Psych Tests: `core/data/remote/PsychTestSubmissionRepository.kt`
- GTO: `core/data/repository/FirestoreGTORepository.kt`
- Interview: `core/data/repository/FirestoreInterviewRepository.kt`
- OIR: `core/data/repository/FirestoreSubmissionRepository.kt`

**Dashboard**:
- Use Case: `core/domain/usecase/dashboard/GetOLQDashboardUseCase.kt`
- UI Card: `app/ui/home/student/components/OLQDashboardCard.kt`

**Prompts**:
- Psychology: `core/data/ai/prompts/PsychologyTestPrompts.kt`
- GTO: `core/data/ai/prompts/GTOTestPrompts.kt`
- Interview: `core/data/ai/prompts/SSBInterviewPrompts.kt`

---

## Key Architectural Patterns

### 1. ID-Based Navigation
All tests pass only `submissionId` (String) between screens, never complex objects.
Result screens fetch data via ViewModel using the ID.

### 2. StateFlow Updates
Always use `.update { it.copy(...) }` pattern for thread safety.
Never use `.value = _state.value.copy(...)` (race condition).

### 3. Worker Pattern
All background AI analysis uses WorkManager with:
- Network constraint
- Retry logic (3 attempts)
- Exponential backoff
- Fill missing OLQs with neutral score (6)

### 4. Cache Management
Dashboard uses 5-minute in-memory cache.
Invalidated after every test submission.
Force refresh via pull-to-refresh.

### 5. Error Handling
- Domain layer: `Result<T>` only
- Data/Presentation: `ErrorLogger.log()`
- User messages: String resources (`R.string.*`)

### 6. Firestore Split Architecture
- Submissions: Raw responses
- Results: OLQ analysis (separate collection)
- Security: Both require userId field

---

**Document Version**: 1.0  
**Last Verified**: December 23, 2025  
**Status**: All 10 tests operational with unified OLQ scoring
