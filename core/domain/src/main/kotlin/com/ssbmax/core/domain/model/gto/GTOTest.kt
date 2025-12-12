package com.ssbmax.core.domain.model.gto

/**
 * Base sealed class for all 8 GTO tests
 * Each test has specific properties based on its format
 */
sealed class GTOTest {
    abstract val id: String
    abstract val type: GTOTestType
    abstract val timeLimit: Int // seconds
    abstract val order: Int
    
    /**
     * Group Discussion Test
     * Topic-based discussion with keyboard STT + white noise
     */
    data class GDTest(
        override val id: String,
        val topic: String,
        val category: String = "General",
        val difficulty: String = "Medium",
        val minWords: Int = 300,
        val maxWords: Int = 1500,
        val hasWhiteNoise: Boolean = true,
        override val timeLimit: Int = 1200, // 20 minutes
        override val type: GTOTestType = GTOTestType.GROUP_DISCUSSION,
        override val order: Int = 1
    ) : GTOTest()
    
    /**
     * Group Planning Exercise Test
     * Image-based tactical scenario planning (similar to PPDT)
     */
    data class GPETest(
        override val id: String,
        val imageUrl: String,
        val scenario: String,
        val resources: List<String> = emptyList(),
        val difficulty: String = "Medium",
        val viewingTimeSeconds: Int = 60,
        val planningTimeSeconds: Int = 1740, // 29 minutes
        val minCharacters: Int = 500,
        override val timeLimit: Int = 1800, // 30 minutes total
        override val type: GTOTestType = GTOTestType.GROUP_PLANNING_EXERCISE,
        override val order: Int = 2
    ) : GTOTest()
    
    /**
     * Lecturette Test
     * 3-minute speech on chosen topic (4 choices provided)
     */
    data class LecturetteTest(
        override val id: String,
        val topicChoices: List<String>, // 4 topics to choose from
        val speechTimeSeconds: Int = 180, // 3 minutes
        val preparationTimeSeconds: Int = 0, // No preparation time
        val hasWhiteNoise: Boolean = true,
        override val timeLimit: Int = 180,
        override val type: GTOTestType = GTOTestType.LECTURETTE,
        override val order: Int = 3
    ) : GTOTest()
    
    /**
     * Progressive Group Task Test
     * 4 progressively difficult obstacles with video game animation
     */
    data class PGTTest(
        override val id: String,
        val obstacles: List<ObstacleConfig>, // 4 progressive obstacles
        val animationType: AnimationType = AnimationType.VIDEO_GAME_2D,
        override val timeLimit: Int = 1800, // 30 minutes
        override val type: GTOTestType = GTOTestType.PROGRESSIVE_GROUP_TASK,
        override val order: Int = 4
    ) : GTOTest()
    
    /**
     * Half Group Task Test
     * Single major obstacle with leadership focus
     */
    data class HGTTest(
        override val id: String,
        val obstacle: ObstacleConfig,
        val animationType: AnimationType = AnimationType.VIDEO_GAME_2D,
        override val timeLimit: Int = 900, // 15 minutes
        override val type: GTOTestType = GTOTestType.HALF_GROUP_TASK,
        override val order: Int = 5
    ) : GTOTest()
    
    /**
     * Group Obstacle Race Test
     * 10-obstacle race course simulation
     */
    data class GORTest(
        override val id: String,
        val obstacles: List<ObstacleConfig>, // 10 obstacles
        val animationType: AnimationType = AnimationType.VIDEO_GAME_2D,
        override val timeLimit: Int = 1200, // 20 minutes
        override val type: GTOTestType = GTOTestType.GROUP_OBSTACLE_RACE,
        override val order: Int = 6
    ) : GTOTest()
    
    /**
     * Individual Obstacles Test
     * 10 numbered obstacles, individual navigation
     */
    data class IOTest(
        override val id: String,
        val obstacles: List<ObstacleConfig>, // 10 individual obstacles
        val animationType: AnimationType = AnimationType.VIDEO_GAME_2D,
        override val timeLimit: Int = 1800, // 30 minutes
        override val type: GTOTestType = GTOTestType.INDIVIDUAL_OBSTACLES,
        override val order: Int = 7
    ) : GTOTest()
    
    /**
     * Command Task Test
     * Scenario-based obstacle with resource allocation
     */
    data class CTTest(
        override val id: String,
        val scenario: String,
        val obstacle: ObstacleConfig,
        val availableResources: List<String> = emptyList(),
        val animationType: AnimationType = AnimationType.VIDEO_GAME_2D,
        override val timeLimit: Int = 900, // 15 minutes
        override val type: GTOTestType = GTOTestType.COMMAND_TASK,
        override val order: Int = 8
    ) : GTOTest()
}

/**
 * Obstacle configuration for animation-based tests
 */
data class ObstacleConfig(
    val id: String,
    val name: String,
    val description: String,
    val difficulty: Int, // 1-4 for progressive difficulty
    val animationAsset: String, // Lottie JSON URL or video URL
    val resources: List<String> = emptyList(), // Available resources for solving
    val height: Float? = null, // For walls (in feet)
    val width: Float? = null, // For ditches (in feet)
    val depth: Float? = null // For ditches/rivers (in feet)
)

/**
 * Animation type for obstacle visualization
 */
enum class AnimationType {
    VIDEO_GAME_2D,      // Compose Canvas rendering
    LOTTIE_ANIMATION,   // Lottie JSON animations
    VIDEO_PLAYBACK,     // Pre-rendered video
    STATIC_IMAGE        // Fallback for low-end devices
}
