# Group Planning Exercise (GPE) - Product Requirements Document

## 🎯 Overview

Group Planning Exercise (GPE) is a critical GTO task where candidates work in groups to solve hypothetical problems requiring strategic planning, resource management, and logical reasoning. This PRD outlines the implementation of a comprehensive GPE module for SSBMax, following the established patterns from Group Discussion and PPDT implementations.

## 📋 GPE Task Description

### What is GPE?
- Candidates are presented with a hypothetical scenario (e.g., rescue mission, disaster response, military operation)
- A map/model shows terrain, resources, objectives, and constraints
- Group must analyze the situation and create a detailed written plan
- Plan is submitted to GTO for evaluation
- Focus areas: Planning ability, logical reasoning, cooperation, practical thinking

### GPE Format
- **Duration**: 20-30 minutes
- **Group Size**: 8-10 candidates
- **Task Type**: Group planning and written submission
- **Assessment**: Plan quality, group coordination, individual contributions

## 🏗️ Technical Architecture

### Data Flow Architecture
```
Firestore (GPE Tasks) → GTOTaskCacheManager → GPE Cache → GPE ViewModel → UI Screens
```

### Integration Points
- **Existing GTO Framework**: Uses GTOTaskCacheManager for task loading
- **Firestore Structure**: `test_content/gto/task_batches` with `taskType: "GPE"`
- **UI Framework**: Jetpack Compose with Material Design 3
- **State Management**: StateFlow with proper lifecycle handling

## 📊 Data Models

### GPE Task Entity
```kotlin
@Entity(tableName = "cached_gto_tasks")
data class CachedGTOTaskEntity(
    @PrimaryKey val id: String,
    val taskType: String = "GPE",
    val title: String,
    val description: String,
    val instructions: String,
    val timeAllowedMinutes: Int = 25,
    val difficultyLevel: String?,
    val category: String?,
    val scenario: String?, // JSON: scenario details
    val resources: String?, // JSON: available resources
    val objectives: String?, // JSON: objectives to achieve
    val constraints: String?, // JSON: limitations/constraints
    val evaluationCriteria: String?, // JSON: scoring criteria
    // ... existing fields
)
```

### GPE Domain Models
```kotlin
data class GPETask(
    val id: String,
    val title: String,
    val description: String,
    val scenario: GPEScenario,
    val resources: List<GPEResource>,
    val objectives: List<GPEObjective>,
    val constraints: List<GPEConstraint>,
    val timeLimitMinutes: Int,
    val evaluationCriteria: List<String>
)

data class GPEScenario(
    val situation: String,
    val location: String,
    val background: String,
    val mapDescription: String,
    val keyElements: List<String>
)

data class GPEResource(
    val type: String, // personnel, equipment, vehicles, etc.
    val name: String,
    val quantity: Int,
    val capabilities: String
)

data class GPEObjective(
    val priority: Int,
    val description: String,
    val successCriteria: String
)

data class GPEConstraint(
    val type: String, // time, resources, terrain, weather
    val description: String,
    val impact: String
)
```

### GPE Submission Model
```kotlin
data class GPESubmission(
    val taskId: String,
    val groupPlan: GPEPlan,
    val individualContributions: Map<String, GPEIndividualContribution>,
    val submittedAt: Long,
    val timeSpentMinutes: Int
)

data class GPEPlan(
    val situationAnalysis: String,
    val objectives: List<String>,
    val strategy: String,
    val resourceAllocation: Map<String, List<String>>,
    val timeline: List<GPEPhase>,
    val contingencyPlans: List<String>,
    val risks: List<String>
)

data class GPEPhase(
    val phase: String,
    val duration: String,
    val actions: List<String>,
    val responsible: String
)

data class GPEIndividualContribution(
    val candidateId: String,
    val role: String,
    val contributions: List<String>,
    val qualityRating: Int // 1-5 scale
)
```

## 🎨 UI/UX Design

### Screen Flow
```
GPE Selection → GPE Instructions → GPE Task Screen → GPE Planning → GPE Submission → GPE Results
```

### Key Screens

#### 1. GPE Instructions Screen
```
+-----------------------------------+
|         GPE Instructions          |
+-----------------------------------+
| ⏱️  Time: 25 minutes              |
| 👥 Group: 8-10 candidates        |
|                                   |
| 📋 Task Overview:                |
| • Analyze given scenario         |
| • Create comprehensive plan      |
| • Submit written solution        |
|                                   |
| 🎯 Assessment Criteria:          |
| • Logical reasoning              |
| • Practical planning             |
| • Resource management            |
| • Group coordination             |
|                                   |
| [Start GPE] [Cancel]             |
+-----------------------------------+
```

#### 2. GPE Task Screen (Main)
```
+-----------------------------------+
| 🗺️  Scenario Map/Model            |
| [Interactive Map Display]        |
+-----------------------------------+
| 📊 Scenario Details              |
| Situation: [Description]         |
| Location: [Details]              |
| Objectives: [List]               |
+-----------------------------------+
| 📋 Resources Available           |
| • Personnel: 20 soldiers         |
| • Vehicles: 3 trucks             |
| • Equipment: Medical supplies    |
+-----------------------------------+
| ⏰ Timer: 24:30 remaining        |
| [Start Planning]                 |
+-----------------------------------+
```

#### 3. GPE Planning Screen
```
+-----------------------------------+
| 📝 Group Planning                |
| ⏰ 22:15 remaining               |
+-----------------------------------+
| 1. Situation Analysis            |
| [Text Input - 200 chars]         |
+-----------------------------------+
| 2. Objectives & Strategy         |
| [Text Input - 300 chars]         |
+-----------------------------------+
| 3. Resource Allocation           |
| [Structured Input]               |
+-----------------------------------+
| 4. Action Plan & Timeline        |
| [Phase-based Input]              |
+-----------------------------------+
| 5. Risks & Contingencies         |
| [Text Input - 200 chars]         |
+-----------------------------------+
| [Submit Plan] [Save Draft]       |
+-----------------------------------+
```

### Interactive Elements

#### Scenario Visualization
```kotlin
@Composable
fun GPEScenarioDisplay(
    scenario: GPEScenario,
    modifier: Modifier = Modifier
) {
    // Interactive map with zoom/pan
    // Key locations marked
    // Resource positions shown
    // Terrain features highlighted
}
```

#### Resource Management
```kotlin
@Composable
fun GPEResourceAllocator(
    resources: List<GPEResource>,
    onAllocationChange: (Map<String, List<String>>) -> Unit
) {
    // Drag-and-drop resource assignment
    // Visual allocation interface
    // Real-time validation
}
```

#### Timeline Builder
```kotlin
@Composable
fun GPEPhasePlanner(
    phases: List<GPEPhase>,
    onPhaseUpdate: (List<GPEPhase>) -> Unit
) {
    // Visual timeline interface
    // Drag-and-drop phase ordering
    // Duration calculations
}
```

## 🔄 State Management

### GPE Session State
```kotlin
sealed class GPEState {
    object NotStarted : GPEState()
    object LoadingTask : GPEState()
    data class TaskLoaded(val task: GPETask) : GPEState()
    data class Planning(val remainingTime: Long, val currentPlan: GPEPlan) : GPEState()
    data class Submitting(val plan: GPEPlan) : GPEState()
    data class Completed(val submission: GPESubmission, val result: GPEResult) : GPEState()
    data class Error(val message: String) : GPEState()
}

data class GPEResult(
    val score: Int,
    val feedback: String,
    val strengths: List<String>,
    val areasForImprovement: List<String>
)
```

### ViewModel Architecture
```kotlin
@HiltViewModel
class GPEViewModel @Inject constructor(
    private val gtoTaskRepository: GTOTaskRepository,
    private val submissionRepository: SubmissionRepository,
    private val timerManager: TimerManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow<GPEState>(GPEState.NotStarted)
    val uiState: StateFlow<GPEState> = _uiState.asStateFlow()

    private val taskId: String = savedStateHandle["taskId"] ?: ""

    fun loadTask() {
        viewModelScope.launch {
            _uiState.value = GPEState.LoadingTask
            try {
                val task = gtoTaskRepository.getTaskById(taskId)
                _uiState.value = GPEState.TaskLoaded(task)
            } catch (e: Exception) {
                _uiState.value = GPEState.Error(e.message ?: "Failed to load task")
            }
        }
    }

    fun startPlanning() {
        // Initialize plan and start timer
        val initialPlan = GPEPlan(
            situationAnalysis = "",
            objectives = emptyList(),
            strategy = "",
            resourceAllocation = emptyMap(),
            timeline = emptyList(),
            contingencyPlans = emptyList(),
            risks = emptyList()
        )

        timerManager.startTimer(25 * 60 * 1000L) // 25 minutes
        _uiState.value = GPEState.Planning(
            remainingTime = timerManager.remainingTime.value,
            currentPlan = initialPlan
        )
    }

    fun updatePlan(plan: GPEPlan) {
        val currentState = _uiState.value
        if (currentState is GPEState.Planning) {
            _uiState.value = currentState.copy(currentPlan = plan)
        }
    }

    fun submitPlan() {
        viewModelScope.launch {
            val currentState = _uiState.value
            if (currentState is GPEState.Planning) {
                _uiState.value = GPEState.Submitting(currentState.currentPlan)

                try {
                    val submission = GPESubmission(
                        taskId = taskId,
                        groupPlan = currentState.currentPlan,
                        individualContributions = emptyMap(), // Would be collected from group
                        submittedAt = System.currentTimeMillis(),
                        timeSpentMinutes = 25 - (timerManager.remainingTime.value / 60000).toInt()
                    )

                    val result = submissionRepository.submitGPE(submission)
                    _uiState.value = GPEState.Completed(submission, result)
                } catch (e: Exception) {
                    _uiState.value = GPEState.Error("Submission failed: ${e.message}")
                }
            }
        }
    }
}
```

## 💾 Data Storage & Persistence

### Local Cache Structure
- Uses existing `CachedGTOTaskEntity` with `taskType = "GPE"`
- Additional fields: `scenario`, `resources`, `objectives`, `constraints`

### Submission Storage
```kotlin
@Entity(tableName = "gpe_submissions")
data class GPESubmissionEntity(
    @PrimaryKey val id: String,
    val taskId: String,
    val userId: String,
    val planJson: String, // Serialized GPEPlan
    val submittedAt: Long,
    val timeSpentMinutes: Int,
    val score: Int?,
    val feedback: String?
)
```

### Offline Capability
- GPE tasks cached locally for offline access
- Draft plans auto-saved every 30 seconds
- Submissions queued when offline, sync when online
- Progress tracking with local timestamps

## ⏱️ Timer & Time Management

### Timer Requirements
- **Total Time**: 25 minutes (configurable per task)
- **Auto-save**: Every 30 seconds during planning
- **Warnings**: 5-minute and 1-minute remaining alerts
- **Auto-submit**: Optional auto-submission at time expiry
- **Pause/Resume**: For app backgrounding

### Timer Implementation
```kotlin
class GPETimerManager(
    private val totalTimeMillis: Long,
    private val onTimeExpired: () -> Unit,
    private val onWarning: (Int) -> Unit // minutes remaining
) {
    private var timerJob: Job? = null
    private val _remainingTime = MutableStateFlow(totalTimeMillis)
    val remainingTime: StateFlow<Long> = _remainingTime.asStateFlow()

    fun startTimer() {
        timerJob?.cancel()
        timerJob = CoroutineScope(Dispatchers.Main).launch {
            var remaining = totalTimeMillis

            while (remaining > 0) {
                delay(1000)
                remaining -= 1000
                _remainingTime.value = remaining

                // Warning alerts
                when (remaining) {
                    5 * 60 * 1000L -> onWarning(5)
                    1 * 60 * 1000L -> onWarning(1)
                    0L -> {
                        onTimeExpired()
                        break
                    }
                }
            }
        }
    }

    fun pauseTimer() {
        timerJob?.cancel()
    }

    fun resumeTimer() {
        startTimer()
    }

    fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }
}
```

## 📝 Assessment & Scoring

### GPE Evaluation Criteria
1. **Situation Analysis** (20%): Understanding of scenario, key elements identified
2. **Objective Setting** (15%): Clear, prioritized, achievable objectives
3. **Strategic Planning** (25%): Logical approach, practical solutions
4. **Resource Management** (15%): Effective allocation, utilization
5. **Timeline & Execution** (15%): Realistic planning, sequencing
6. **Risk Assessment** (10%): Contingency planning, risk identification

### Scoring Algorithm
```kotlin
data class GPEEvaluationCriteria(
    val situationAnalysisScore: Int, // 0-20
    val objectivesScore: Int, // 0-15
    val strategyScore: Int, // 0-25
    val resourceManagementScore: Int, // 0-15
    val timelineScore: Int, // 0-15
    val riskAssessmentScore: Int // 0-10
) {
    val totalScore: Int
        get() = situationAnalysisScore + objectivesScore + strategyScore +
                resourceManagementScore + timelineScore + riskAssessmentScore

    val percentage: Double
        get() = (totalScore / 100.0) * 100.0
}
```

### Feedback Generation
```kotlin
fun generateGPEFeedback(evaluation: GPEEvaluationCriteria): GPEFeedback {
    val strengths = mutableListOf<String>()
    val improvements = mutableListOf<String>()

    // Analyze each criterion and provide specific feedback
    when {
        evaluation.situationAnalysisScore >= 15 -> strengths.add("Excellent situation analysis")
        evaluation.situationAnalysisScore <= 10 -> improvements.add("Improve situation analysis depth")
    }

    // Similar logic for other criteria...

    return GPEFeedback(
        overallScore = evaluation.totalScore,
        percentage = evaluation.percentage,
        grade = calculateGrade(evaluation.percentage),
        strengths = strengths,
        areasForImprovement = improvements,
        detailedFeedback = generateDetailedFeedback(evaluation)
    )
}
```

## 🔄 Integration with Existing Systems

### GTO Task Management Integration
- Extends existing `GTOTaskCacheManager` with `taskType = "GPE"`
- Uses same batch loading and caching mechanism
- Follows existing GTO UI navigation patterns

### Submission System Integration
- Uses existing `SubmissionRepository` pattern
- Integrates with progress tracking system
- Supports both individual and group submissions

### Analytics Integration
```kotlin
class GPEAnalyticsTracker {
    fun trackGPESession(taskId: String, duration: Long) {
        analytics.logEvent("gpe_session_started") {
            param("task_id", taskId)
            param("duration_minutes", duration / 60000)
        }
    }

    fun trackGPESubmission(submission: GPESubmission) {
        analytics.logEvent("gpe_submitted") {
            param("task_id", submission.taskId)
            param("time_spent", submission.timeSpentMinutes)
            param("plan_complexity", calculateComplexity(submission.groupPlan))
        }
    }
}
```

## 🧪 Testing Strategy

### Unit Tests
```kotlin
class GPEViewModelTest {
    @Test
    fun `should load GPE task successfully`() = runTest {
        // Given
        val mockTask = createMockGPETask()
        coEvery { repository.getTaskById(any()) } returns mockTask

        // When
        viewModel.loadTask()

        // Then
        assertTrue(viewModel.uiState.value is GPEState.TaskLoaded)
    }

    @Test
    fun `should submit plan within time limit`() = runTest {
        // Test submission logic with timer constraints
    }
}
```

### UI Tests
```kotlin
@RunWith(AndroidJUnit4::class)
class GPEPlanningScreenTest {
    @Test
    fun testTimerDisplaysCorrectly() {
        composeTestRule.setContent {
            GPEPlanningScreen(viewModel = mockViewModel)
        }

        composeTestRule.onNodeWithText("25:00").assertIsDisplayed()
    }

    @Test
    fun testPlanSubmission() {
        // Test plan input and submission flow
    }
}
```

### Integration Tests
- Test complete GPE workflow from task loading to submission
- Test offline functionality and sync
- Test timer management across app lifecycle events

## 📈 Performance Requirements

### Loading Performance
- **Task Loading**: < 2 seconds from cache
- **UI Rendering**: < 1 second initial render
- **Map Display**: < 3 seconds for complex scenarios

### Memory Management
- **Cache Size**: Limit to 50 GPE tasks (configurable)
- **Image Assets**: Optimize scenario maps for mobile
- **Auto-cleanup**: Remove old submissions after 30 days

### Battery Optimization
- **Timer Efficiency**: Use efficient countdown mechanism
- **Background Handling**: Pause timer when app backgrounded
- **Auto-save Frequency**: Balance between data safety and battery usage

## 🌐 Accessibility Features

### Screen Reader Support
- Descriptive labels for all interactive elements
- Alt text for scenario maps and visual elements
- Audio feedback for timer warnings

### Visual Accessibility
- High contrast mode support
- Scalable text for plan input fields
- Color-blind friendly resource allocation interface

### Motor Accessibility
- Large touch targets for planning interface
- Keyboard navigation support
- Voice input for plan text entry

## 🚀 Implementation Phases

### Phase 1: Core Infrastructure (Week 1-2)
- [ ] Define GPE data models and entities
- [ ] Extend GTOTaskCacheManager for GPE tasks
- [ ] Create basic GPE ViewModel structure
- [ ] Implement timer management system

### Phase 2: UI Development (Week 3-4)
- [ ] Create GPE instructions screen
- [ ] Build scenario display component
- [ ] Implement planning interface
- [ ] Add submission flow

### Phase 3: Advanced Features (Week 5-6)
- [ ] Interactive map component
- [ ] Resource allocation interface
- [ ] Timeline builder
- [ ] Offline support and auto-save

### Phase 4: Testing & Polish (Week 7-8)
- [ ] Unit and UI tests
- [ ] Performance optimization
- [ ] Accessibility implementation
- [ ] Analytics integration

## ✅ Success Metrics

### Technical Metrics
- **Load Time**: < 2 seconds for task loading
- **Crash Rate**: < 0.1% during GPE sessions
- **Memory Usage**: < 100MB during active sessions
- **Offline Success Rate**: > 95% functionality preservation

### User Experience Metrics
- **Task Completion Rate**: > 85% of started sessions completed
- **Average Session Time**: 20-25 minutes (as designed)
- **User Satisfaction**: > 4.0/5.0 rating
- **Feature Usage**: > 70% of GPE features utilized

### Educational Metrics
- **Learning Effectiveness**: Measurable improvement in planning skills
- **Assessment Accuracy**: Correlation with real SSB performance
- **Content Quality**: > 90% positive feedback on scenarios

## 🔧 Maintenance & Updates

### Content Updates
- **Scenario Library**: Monthly additions of new GPE scenarios
- **Difficulty Balancing**: Regular review and adjustment
- **Cultural Adaptation**: Localization for regional contexts

### Performance Monitoring
- **Usage Analytics**: Track popular scenarios and completion rates
- **Performance Metrics**: Monitor load times and crash reports
- **User Feedback**: Regular collection and analysis

### Feature Enhancements
- **AI Assistance**: Optional hints and guidance
- **Peer Review**: Group member feedback system
- **Advanced Analytics**: Detailed performance breakdowns

---

**This PRD provides a comprehensive blueprint for implementing Group Planning Exercise functionality in SSBMax, ensuring it integrates seamlessly with existing GTO task management while providing an authentic and effective training experience for SSB candidates.**


