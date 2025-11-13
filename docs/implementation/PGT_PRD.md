# Progressive Group Task (PGT) - Product Requirements Document

## 🎯 Overview

Progressive Group Task (PGT) is a critical GTO activity where candidates work in groups to overcome physical obstacles requiring teamwork, problem-solving, and determination. This PRD outlines the implementation of a comprehensive PGT module for SSBMax, focusing on authentic obstacle visualization and interactive test simulation.

## 📋 PGT Task Description

### What is PGT?
- Candidates navigate through **4 outdoor obstacles** in sequence
- Each obstacle requires **physical effort and mental planning**
- Group uses **limited helping materials** provided by GTO
- Focus areas: Teamwork, practical intelligence, physical determination, leadership
- **Time limit**: 40 minutes for all 4 obstacles combined

### PGT Format
- **Duration**: 40 minutes
- **Group Size**: 8-10 candidates
- **Task Type**: Sequential obstacle navigation
- **Assessment**: Group coordination, problem-solving, determination

### Standard Obstacles (Progressive Difficulty)
1. **Obstacle 1**: 6ft Wall (Height challenge)
2. **Obstacle 2**: 9ft Ditch (Depth challenge)
3. **Obstacle 3**: Net Climb (Aerial challenge)
4. **Obstacle 4**: Rope Bridge (Balance challenge)

## 🏗️ Technical Architecture

### Data Flow Architecture
```
Firestore (PGT Obstacles) → GTOTaskCacheManager → PGT Cache → PGT ViewModel → UI Screens
```

### Integration Points
- **Existing GTO Framework**: Uses GTOTaskCacheManager for task loading
- **Firestore Structure**: `test_content/gto/task_batches` with `taskType: "PGT"`
- **UI Framework**: Jetpack Compose with Material Design 3
- **State Management**: StateFlow with proper lifecycle handling
- **Canvas Rendering**: Custom Compose Canvas for obstacle visualization

### Multi-Module Integration
```
core/data/ → PGT data models and repositories
core/domain/ → PGT business logic and use cases
app/ui/tests/gto/pgt/ → PGT-specific UI components
```

## 📊 Data Models

### PGT Task Entity
```kotlin
@Entity(tableName = "cached_gto_tasks")
data class CachedGTOTaskEntity(
    @PrimaryKey val id: String,
    val taskType: String = "PGT",
    val title: String,
    val description: String,
    val instructions: String,
    val timeAllowedMinutes: Int = 40,
    val difficultyLevel: String?,
    val category: String?,
    val obstaclesJson: String?, // JSON: List<PGTObstacle>
    val materialsJson: String?, // JSON: List<PGTMaterial>
    // ... existing fields
)
```

### PGT Domain Models
```kotlin
data class PGTTask(
    val id: String,
    val title: String,
    val description: String,
    val obstacles: List<PGTObstacle>,
    val availableMaterials: List<PGTMaterial>,
    val timeLimitMinutes: Int,
    val instructions: String,
    val assessmentCriteria: List<String>
)

data class PGTObstacle(
    val id: String,
    val number: Int, // 1-4
    val name: String,
    val type: ObstacleType,
    val dimensions: PGTDimensions,
    val position: PGTPosition, // Relative positioning
    val description: String,
    val difficulty: DifficultyLevel,
    val allowedMaterials: List<String>, // Material IDs
    val tips: List<String>
)

data class PGTMaterial(
    val id: String,
    val name: String,
    val type: MaterialType,
    val quantity: Int,
    val dimensions: PGTDimensions,
    val description: String,
    val usage: String
)

enum class ObstacleType {
    WALL, DITCH, NET_CLIMB, ROPE_BRIDGE, PLATFORM
}

enum class MaterialType {
    PLANK, ROPE, LADDER, BOX, POLE
}

data class PGTDimensions(
    val width: Float, // feet
    val height: Float, // feet
    val depth: Float? = null // feet, for ditches
)

data class PGTPosition(
    val x: Float, // 0-1 relative position
    val y: Float, // 0-1 relative position
    val distanceFromPrevious: Float // feet
)
```

### PGT Session Model
```kotlin
data class PGTSession(
    val taskId: String,
    val startTime: Long,
    val endTime: Long?,
    val obstacleProgress: Map<String, ObstacleStatus>, // obstacleId -> status
    val materialUsage: Map<String, Int>, // materialId -> times used
    val observations: List<PGTObservation>,
    val finalScore: Int?
)

enum class ObstacleStatus {
    NOT_STARTED, IN_PROGRESS, COMPLETED, FAILED
}

data class PGTObservation(
    val obstacleId: String,
    val timestamp: Long,
    val observation: String,
    val candidateId: String? // For individual tracking
)
```

## 🎨 UI/UX Design

### Screen Flow
```
PGT Selection → PGT Instructions → PGT Visualization → PGT Test Session → PGT Results
```

### Key Screens

#### 1. PGT Instructions Screen
```
+-----------------------------------+
|         PGT Instructions          |
+-----------------------------------+
| ⏱️  Time: 40 minutes              |
| 👥 Group: 8-10 candidates        |
|                                   |
| 📋 Task Overview:                |
| • Navigate 4 outdoor obstacles   |
| • Use limited helping materials  |
| • Demonstrate teamwork & problem |
|   solving                        |
|                                   |
| 🎯 Assessment Criteria:          |
| • Teamwork & cooperation         |
| • Practical thinking             |
| • Physical determination         |
| • Leadership qualities           |
|                                   |
| 📐 Obstacle Layout:              |
| Click below to see how obstacles |
| are positioned                    |
|                                   |
| [View Obstacle Layout]           |
| [Start PGT Test] [Cancel]        |
+-----------------------------------+
```

#### 2. PGT Obstacle Layout Visualization (Main Screen)
```
+-----------------------------------+
|     PGT Obstacle Layout          |
+-----------------------------------+
| 🔍 [Zoom In] [Zoom Out] [Reset]   |
|                                   |
| +-------------------------------+ |
| |            START              | |
| |                               | |
| |       ┌─────────────┐         | |
| |       │   OBSTACLE  │         | |
| |       │      1      │         | |
| |       │  (6ft Wall) │         | |
| |       └─────────────┘         | |
| |                               | |
| |           50ft                | |
| |                               | |
| |       ┌─────────────┐         | |
| |       │   OBSTACLE  │         | |
| |       │      2      │         | |
| |       │ (9ft Ditch) │         | |
| |       └─────────────┘         | |
| |                               | |
| |           75ft                | |
| |                               | |
| |       ┌─────────────┐         | |
| |       │   OBSTACLE  │         | |
| |       │      3      │         | |
| |       │ (Net Climb) │         | |
| |       └─────────────┘         | |
| |                               | |
| |           60ft                | |
| |                               | |
| |       ┌─────────────┐         | |
| |       │   OBSTACLE  │         | |
| |       │      4      │         | |
| |       │(Rope Bridge)│         | |
| |       └─────────────┘         | |
| |                               | |
| |            FINISH             | |
| +-------------------------------+ |
|                                   |
| 📋 Available Materials:          |
| • 2 Planks (10ft each)           |
| • 1 Rope (20ft)                  |
| • 4 Wooden Boxes                 |
| • 2 Ladders                      |
|                                   |
| 💡 Tap obstacles for details     |
| [Back to Instructions]           |
+-----------------------------------+
```

#### 3. Obstacle Detail Modal
```
+-----------------------------------+
|       Obstacle 1 Details         |
+-----------------------------------+
| 🧱 6ft Wall                      |
|                                   |
| 📏 Dimensions: 6ft high x 8ft wide|
| 🎯 Objective: Cross the wall     |
|                                   |
| 🛠️  Helping Materials Allowed:   |
| • Planks for bridging            |
| • Boxes for stepping             |
| • Rope for pulling               |
|                                   |
| 💡 Tips:                         |
| • Coordinate team lifting        |
| • Use materials creatively       |
| • Maintain safety protocols      |
|                                   |
| [Close]                          |
+-----------------------------------+
```

#### 4. PGT Test Session Screen
```
+-----------------------------------+
|         PGT Test Session         |
+-----------------------------------+
| ⏰ Time Remaining: 35:42          |
| 👥 Group Progress: 3/4 obstacles |
|                                   |
| 🏃‍♂️ Current Obstacle: 4 (Rope Bridge)|
| 📍 Status: In Progress            |
|                                   |
| [Obstacle Layout Visualization]   |
| (With progress indicators)        |
|                                   |
| 📝 Notes:                        |
| [Text area for observations]      |
|                                   |
| [Submit Observation] [End Test]   |
+-----------------------------------+
```

### Interactive Elements

#### Obstacle Visualization Canvas
```kotlin
@Composable
fun PGTObstacleCanvas(
    obstacles: List<PGTObstacle>,
    materials: List<PGTMaterial>,
    progress: PGTProgress? = null,
    onObstacleClick: (PGTObstacle) -> Unit,
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var selectedObstacle by remember { mutableStateOf<PGTObstacle?>(null) }

    Canvas(modifier = modifier
        .aspectRatio(16f/9f)
        .pointerInput(Unit) {
            detectTransformGestures { _, pan, zoom, _ ->
                scale = (scale * zoom).coerceIn(0.5f, 3f)
                offset += pan
            }
        }
        .pointerInput(Unit) {
            detectTapGestures { tapOffset ->
                // Convert tap to canvas coordinates and check obstacle hits
                val canvasPoint = (tapOffset - offset) / scale
                val hitObstacle = obstacles.find { obstacle ->
                    obstacle.bounds.contains(canvasPoint)
                }
                hitObstacle?.let { onObstacleClick(it) }
            }
        }
    ) {
        // Draw ground plane
        drawRect(color = MaterialTheme.colorScheme.surfaceVariant)

        // Draw start/finish lines
        drawStartFinishLines(scale, offset)

        // Draw obstacles
        obstacles.forEach { obstacle ->
            drawObstacle(obstacle, scale, offset, progress)
        }

        // Draw measurements
        drawMeasurements(obstacles, scale, offset)

        // Draw material indicators
        drawMaterials(materials, scale, offset)
    }
}
```

#### Material Management UI
```kotlin
@Composable
fun PGTMaterialInventory(
    materials: List<PGTMaterial>,
    usage: Map<String, Int> = emptyMap(),
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(materials) { material ->
            MaterialCard(
                material = material,
                usageCount = usage[material.id] ?: 0
            )
        }
    }
}
```

#### Progress Tracking
```kotlin
@Composable
fun PGTProgressIndicator(
    obstacles: List<PGTObstacle>,
    progress: Map<String, ObstacleStatus>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        obstacles.forEach { obstacle ->
            ObstacleProgressChip(
                obstacle = obstacle,
                status = progress[obstacle.id] ?: ObstacleStatus.NOT_STARTED
            )
        }
    }
}
```

## 🔄 State Management

### PGT Session State
```kotlin
sealed class PGTState {
    object NotStarted : PGTState()
    object LoadingTask : PGTState()
    data class TaskLoaded(val task: PGTTask) : PGTState()
    data class Visualizing(val task: PGTTask) : PGTState()
    data class InSession(val session: PGTSession) : PGTState()
    data class Completed(val result: PGTResult) : PGTState()
    data class Error(val message: String) : PGTState()
}

data class PGTProgress(
    val currentObstacleId: String?,
    val completedObstacles: Set<String>,
    val timeRemaining: Long,
    val materialUsage: Map<String, Int>
)

data class PGTResult(
    val session: PGTSession,
    val score: Int,
    val feedback: String,
    val strengths: List<String>,
    val areasForImprovement: List<String>
)
```

### ViewModel Architecture
```kotlin
@HiltViewModel
class PGTViewModel @Inject constructor(
    private val gtoTaskRepository: GTOTaskRepository,
    private val sessionRepository: SessionRepository,
    private val timerManager: TimerManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow<PGTState>(PGTState.NotStarted)
    val uiState: StateFlow<PGTState> = _uiState.asStateFlow()

    private val taskId: String = savedStateHandle["taskId"] ?: ""

    fun loadTask() {
        viewModelScope.launch {
            _uiState.value = PGTState.LoadingTask
            try {
                val task = gtoTaskRepository.getTaskById(taskId)
                _uiState.value = PGTState.TaskLoaded(task)
            } catch (e: Exception) {
                _uiState.value = PGTState.Error(e.message ?: "Failed to load task")
            }
        }
    }

    fun startVisualization() {
        val currentState = _uiState.value
        if (currentState is PGTState.TaskLoaded) {
            _uiState.value = PGTState.Visualizing(currentState.task)
        }
    }

    fun startTest() {
        val currentState = _uiState.value
        if (currentState is PGTState.TaskLoaded) {
            val initialSession = PGTSession(
                taskId = taskId,
                startTime = System.currentTimeMillis(),
                endTime = null,
                obstacleProgress = emptyMap(),
                materialUsage = emptyMap(),
                observations = emptyList(),
                finalScore = null
            )

            timerManager.startTimer(currentState.task.timeLimitMinutes * 60 * 1000L)
            _uiState.value = PGTState.InSession(initialSession)
        }
    }

    fun updateProgress(obstacleId: String, status: ObstacleStatus) {
        val currentState = _uiState.value
        if (currentState is PGTState.InSession) {
            val updatedProgress = currentState.session.obstacleProgress.toMutableMap()
            updatedProgress[obstacleId] = status

            val updatedSession = currentState.session.copy(
                obstacleProgress = updatedProgress
            )

            _uiState.value = currentState.copy(session = updatedSession)
        }
    }

    fun addObservation(obstacleId: String, observation: String) {
        val currentState = _uiState.value
        if (currentState is PGTState.InSession) {
            val newObservation = PGTObservation(
                obstacleId = obstacleId,
                timestamp = System.currentTimeMillis(),
                observation = observation,
                candidateId = null // TODO: Add candidate tracking
            )

            val updatedSession = currentState.session.copy(
                observations = currentState.session.observations + newObservation
            )

            _uiState.value = currentState.copy(session = updatedSession)
        }
    }

    fun submitTest() {
        viewModelScope.launch {
            val currentState = _uiState.value
            if (currentState is PGTState.InSession) {
                val completedSession = currentState.session.copy(
                    endTime = System.currentTimeMillis()
                )

                try {
                    val result = sessionRepository.submitPGTSession(completedSession)
                    _uiState.value = PGTState.Completed(result)
                } catch (e: Exception) {
                    _uiState.value = PGTState.Error("Submission failed: ${e.message}")
                }
            }
        }
    }
}
```

## 💾 Data Storage & Persistence

### Local Cache Structure
- Uses existing `CachedGTOTaskEntity` with `taskType = "PGT"`
- Additional fields: `obstaclesJson`, `materialsJson`

### Session Storage
```kotlin
@Entity(tableName = "pgt_sessions")
data class PGTSessionEntity(
    @PrimaryKey val id: String,
    val taskId: String,
    val userId: String,
    val startTime: Long,
    val endTime: Long?,
    val obstacleProgressJson: String, // Map<String, ObstacleStatus>
    val materialUsageJson: String, // Map<String, Int>
    val observationsJson: String, // List<PGTObservation>
    val score: Int?,
    val feedback: String?
)
```

### Offline Capability
- PGT tasks and obstacle data cached locally for offline access
- Session progress auto-saved every 30 seconds
- Observations and progress queued when offline, sync when online
- Canvas visualization works entirely offline

## ⏱️ Timer & Time Management

### Timer Requirements
- **Total Time**: 40 minutes (configurable per task)
- **Auto-save**: Every 30 seconds during session
- **Warnings**: 10-minute, 5-minute, and 1-minute remaining alerts
- **Obstacle Time Tracking**: Individual obstacle completion times
- **Pause/Resume**: For app backgrounding

### Timer Implementation
```kotlin
class PGTTimerManager(
    private val totalTimeMillis: Long,
    private val onTimeExpired: () -> Unit,
    private val onWarning: (Int) -> Unit,
    private val onObstacleTimeout: (String) -> Unit
) {
    private var timerJob: Job? = null
    private val _remainingTime = MutableStateFlow(totalTimeMillis)
    val remainingTime: StateFlow<Long> = _remainingTime.asStateFlow()

    private val _obstacleTimers = mutableMapOf<String, Long>()

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
                    10 * 60 * 1000L -> onWarning(10)
                    5 * 60 * 1000L -> onWarning(5)
                    1 * 60 * 1000L -> {
                        onWarning(1)
                        onTimeExpired()
                        break
                    }
                }

                // Check obstacle timeouts (5 minutes per obstacle)
                checkObstacleTimeouts()
            }
        }
    }

    private fun checkObstacleTimeouts() {
        _obstacleTimers.forEach { (obstacleId, startTime) ->
            val elapsed = System.currentTimeMillis() - startTime
            if (elapsed > 5 * 60 * 1000) { // 5 minutes
                onObstacleTimeout(obstacleId)
            }
        }
    }

    fun startObstacleTimer(obstacleId: String) {
        _obstacleTimers[obstacleId] = System.currentTimeMillis()
    }

    fun pauseTimer() { timerJob?.cancel() }
    fun resumeTimer() { startTimer() }
    fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
        _obstacleTimers.clear()
    }
}
```

## 📝 Assessment & Scoring

### PGT Evaluation Criteria
1. **Teamwork & Cooperation** (25%): Group coordination, communication, helping others
2. **Practical Intelligence** (25%): Creative problem-solving, material utilization
3. **Physical Determination** (20%): Perseverance, overcoming physical challenges
4. **Leadership** (15%): Taking initiative, organizing group efforts
5. **Safety Awareness** (10%): Following safety protocols, risk assessment
6. **Time Management** (5%): Efficient use of available time

### Scoring Algorithm
```kotlin
data class PGTEvaluationCriteria(
    val teamworkScore: Int, // 0-25
    val practicalIntelligenceScore: Int, // 0-25
    val determinationScore: Int, // 0-20
    val leadershipScore: Int, // 0-15
    val safetyScore: Int, // 0-10
    val timeManagementScore: Int // 0-5
) {
    val totalScore: Int
        get() = teamworkScore + practicalIntelligenceScore + determinationScore +
                leadershipScore + safetyScore + timeManagementScore

    val percentage: Double
        get() = (totalScore / 100.0) * 100.0
}
```

### Feedback Generation
```kotlin
fun generatePGTFeedback(evaluation: PGTEvaluationCriteria): PGTFeedback {
    val strengths = mutableListOf<String>()
    val improvements = mutableListOf<String>()

    // Analyze each criterion and provide specific feedback
    when {
        evaluation.teamworkScore >= 20 -> strengths.add("Excellent teamwork and group coordination")
        evaluation.teamworkScore <= 15 -> improvements.add("Improve group communication and cooperation")
    }

    when {
        evaluation.practicalIntelligenceScore >= 20 -> strengths.add("Outstanding practical problem-solving")
        evaluation.practicalIntelligenceScore <= 15 -> improvements.add("Develop more creative approaches to challenges")
    }

    // Similar logic for other criteria...

    return PGTFeedback(
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
- Extends existing `GTOTaskCacheManager` with `taskType = "PGT"`
- Uses same batch loading and caching mechanism
- Follows existing GTO UI navigation patterns

### Session Management Integration
- Uses existing `SessionRepository` pattern
- Integrates with progress tracking system
- Supports both individual and group submissions

### Analytics Integration
```kotlin
class PGTAnalyticsTracker {
    fun trackPGTSession(taskId: String, duration: Long) {
        analytics.logEvent("pgt_session_started") {
            param("task_id", taskId)
            param("duration_minutes", duration / 60000)
        }
    }

    fun trackPGTObstacleCompletion(obstacleId: String, timeSpent: Long) {
        analytics.logEvent("pgt_obstacle_completed") {
            param("obstacle_id", obstacleId)
            param("time_spent_seconds", timeSpent / 1000)
        }
    }

    fun trackPGTMaterialUsage(materialId: String, timesUsed: Int) {
        analytics.logEvent("pgt_material_used") {
            param("material_id", materialId)
            param("usage_count", timesUsed)
        }
    }
}
```

## 🧪 Testing Strategy

### Unit Tests
```kotlin
class PGTViewModelTest {
    @Test
    fun `should load PGT task successfully`() = runTest {
        // Given
        val mockTask = createMockPGTTask()
        coEvery { repository.getTaskById(any()) } returns mockTask

        // When
        viewModel.loadTask()

        // Then
        assertTrue(viewModel.uiState.value is PGTState.TaskLoaded)
    }

    @Test
    fun `should track obstacle progress correctly`() = runTest {
        // Given
        val initialSession = createMockPGTSession()

        // When
        viewModel.updateProgress("obstacle_1", ObstacleStatus.COMPLETED)

        // Then
        val state = viewModel.uiState.value as PGTState.InSession
        assertEquals(ObstacleStatus.COMPLETED, state.session.obstacleProgress["obstacle_1"])
    }
}
```

### UI Tests
```kotlin
@RunWith(AndroidJUnit4::class)
class PGTVisualizationScreenTest {
    @Test
    fun testObstacleCanvasDisplaysCorrectly() {
        composeTestRule.setContent {
            PGTObstacleCanvas(
                obstacles = mockObstacles,
                materials = mockMaterials,
                onObstacleClick = {}
            )
        }

        composeTestRule.onNodeWithText("Obstacle 1").assertIsDisplayed()
    }

    @Test
    fun testZoomControlsWork() {
        // Test zoom in/out functionality
        composeTestRule.onNodeWithContentDescription("Zoom In").performClick()
        // Verify canvas scale increased
    }
}
```

### Canvas Rendering Tests
```kotlin
class PGTObstacleCanvasTest {
    @Test
    fun testObstacleDrawing() {
        val obstacle = PGTObstacle(
            id = "test",
            number = 1,
            name = "Test Wall",
            type = ObstacleType.WALL,
            dimensions = PGTDimensions(width = 8f, height = 6f),
            position = PGTPosition(x = 0.2f, y = 0.5f, distanceFromPrevious = 0f),
            description = "Test obstacle",
            difficulty = DifficultyLevel.MEDIUM,
            allowedMaterials = listOf("plank", "box"),
            tips = emptyList()
        )

        // Test obstacle bounds calculation
        assertTrue(obstacle.bounds.width > 0)
        assertTrue(obstacle.bounds.height > 0)
    }
}
```

### Integration Tests
- Test complete PGT workflow from visualization to submission
- Test offline functionality and sync
- Test timer management across app lifecycle events
- Test canvas interaction with different screen sizes

## 📈 Performance Requirements

### Loading Performance
- **Task Loading**: < 2 seconds from cache
- **Canvas Rendering**: < 1 second initial render
- **Visualization Load**: < 3 seconds for complex layouts

### Memory Management
- **Canvas Bitmaps**: Efficient bitmap recycling for obstacle graphics
- **State Management**: Minimal recomposition with stable keys
- **Cache Size**: Limit to 10 PGT tasks (configurable)
- **Auto-cleanup**: Remove old sessions after 60 days

### Battery Optimization
- **Canvas Updates**: Only redraw when necessary
- **Timer Efficiency**: Use efficient countdown mechanism
- **Background Handling**: Pause canvas updates when not visible

## 🌐 Accessibility Features

### Screen Reader Support
- Descriptive labels for all interactive elements
- Alt text for obstacle visualizations ("6 foot wall obstacle")
- Audio feedback for timer warnings and obstacle completions
- Keyboard navigation for zoom controls and material inventory

### Visual Accessibility
- High contrast mode support for obstacle outlines
- Scalable text for obstacle details and instructions
- Color-blind friendly progress indicators
- Minimum touch target sizes (48dp) for interactive elements

### Motor Accessibility
- Large touch targets for canvas interactions
- Alternative input methods for zoom/pan (buttons vs gestures)
- Reduced motion options for animations
- Voice input for observation notes

## 🚀 Implementation Phases

### Phase 1: Core Infrastructure (Week 1-2)
- [ ] Define PGT data models and entities
- [ ] Extend GTOTaskCacheManager for PGT tasks
- [ ] Create basic PGT ViewModel structure
- [ ] Implement timer management system

### Phase 2: Canvas Visualization (Week 3-4)
- [ ] Create PGTObstacleCanvas component
- [ ] Implement obstacle rendering logic
- [ ] Add zoom and pan controls
- [ ] Build obstacle detail modals

### Phase 3: Interactive Features (Week 5-6)
- [ ] Add tap-to-inspect functionality
- [ ] Implement material inventory UI
- [ ] Create progress tracking system
- [ ] Add session management

### Phase 4: Testing & Polish (Week 7-8)
- [ ] Unit and UI tests
- [ ] Performance optimization
- [ ] Accessibility implementation
- [ ] Analytics integration

## ✅ Success Metrics

### Technical Metrics
- **Load Time**: < 2 seconds for task loading and visualization
- **Crash Rate**: < 0.1% during PGT sessions
- **Memory Usage**: < 150MB during active sessions with canvas
- **Offline Success Rate**: > 95% functionality preservation

### User Experience Metrics
- **Visualization Clarity**: > 90% users can understand obstacle layouts
- **Task Completion Rate**: > 80% of started sessions completed
- **Average Session Time**: 35-40 minutes (as designed)
- **User Satisfaction**: > 4.2/5.0 rating for visualization helpfulness

### Educational Metrics
- **Preparation Effectiveness**: Measurable improvement in PGT performance
- **Assessment Accuracy**: Correlation with real SSB PGT performance
- **Content Quality**: > 95% positive feedback on obstacle authenticity
- **Learning Outcomes**: > 85% users report better understanding of PGT requirements

## 🔧 Maintenance & Updates

### Content Updates
- **Obstacle Variations**: Monthly additions of new obstacle configurations
- **Difficulty Balancing**: Regular review and adjustment based on user performance
- **Cultural Adaptation**: Localization for regional PGT variations

### Performance Monitoring
- **Usage Analytics**: Track popular visualization features and completion rates
- **Performance Metrics**: Monitor canvas rendering times and memory usage
- **User Feedback**: Regular collection and analysis of visualization effectiveness

### Feature Enhancements
- **3D Visualization**: Optional 3D view for complex obstacles
- **Video Demonstrations**: Success example videos for each obstacle
- **Peer Comparisons**: Anonymous comparison with other candidates' approaches
- **AI Assistance**: Optional hints and strategy suggestions

---

**This PRD provides a comprehensive blueprint for implementing Progressive Group Task functionality in SSBMax, ensuring candidates receive clear, interactive visualizations of obstacle layouts while maintaining authentic SSB testing conditions and providing an exceptional user experience.**
