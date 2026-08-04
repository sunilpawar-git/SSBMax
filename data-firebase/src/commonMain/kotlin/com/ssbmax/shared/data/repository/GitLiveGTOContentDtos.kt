package com.ssbmax.shared.data.repository

import com.ssbmax.shared.domain.model.gto.*
import kotlinx.serialization.Serializable

/**
 * Obstacle/GPE-scenario DTOs and animation-test-construction helpers used by
 * [GitLiveGTORepository]'s test-content/cache cluster (`getRandomTest`/`getRandomGPEScenario`/
 * `getObstaclesForTest`). Split into its own file purely to keep
 * [GitLiveGTORepository]'s file under the repo's 300-line-per-file limit — no behavior change
 * from the original merged class.
 */

/** Maps an animation [GTOTestType] to its Firestore path segment; null if it has no obstacles. */
internal fun obstacleTestTypePath(testType: GTOTestType): String? = when (testType) {
    GTOTestType.PROGRESSIVE_GROUP_TASK -> "pgt"
    GTOTestType.HALF_GROUP_TASK -> "hgt"
    GTOTestType.GROUP_OBSTACLE_RACE -> "gor"
    GTOTestType.INDIVIDUAL_OBSTACLES -> "io"
    GTOTestType.COMMAND_TASK -> "ct"
    else -> null
}

internal fun createGtoAnimationTest(testType: GTOTestType, obstacles: List<ObstacleConfig>): GTOTest = when (testType) {
    GTOTestType.PROGRESSIVE_GROUP_TASK -> GTOTest.PGTTest(id = randomId(), obstacles = obstacles)
    GTOTestType.HALF_GROUP_TASK -> GTOTest.HGTTest(
        id = randomId(),
        obstacle = obstacles.firstOrNull() ?: defaultObstacleConfig()
    )
    GTOTestType.GROUP_OBSTACLE_RACE -> GTOTest.GORTest(id = randomId(), obstacles = obstacles)
    GTOTestType.INDIVIDUAL_OBSTACLES -> GTOTest.IOTest(id = randomId(), obstacles = obstacles)
    GTOTestType.COMMAND_TASK -> GTOTest.CTTest(
        id = randomId(),
        scenario = "Default command scenario",
        obstacle = obstacles.firstOrNull() ?: defaultObstacleConfig()
    )
    else -> throw IllegalArgumentException("Invalid animation test type: $testType")
}

private fun defaultObstacleConfig(): ObstacleConfig = ObstacleConfig(
    id = "default",
    name = "Default Obstacle",
    description = "Default obstacle",
    difficulty = 1,
    animationAsset = ""
)

internal fun ObstacleConfigDto.toDomain(): ObstacleConfig = ObstacleConfig(
    id = id ?: randomId(),
    name = name ?: "",
    description = description ?: "",
    difficulty = difficulty ?: 1,
    animationAsset = animationAsset ?: "",
    resources = resources ?: emptyList(),
    height = height,
    width = width,
    depth = depth
)

@Serializable
internal data class ObstacleConfigDto(
    val id: String? = null,
    val name: String? = null,
    val description: String? = null,
    val difficulty: Int? = null,
    val animationAsset: String? = null,
    val resources: List<String>? = null,
    val height: Float? = null,
    val width: Float? = null,
    val depth: Float? = null
)

@Serializable
internal data class ObstacleSetDto(val obstacles: List<ObstacleConfigDto> = emptyList())

@Serializable
internal data class GPEScenarioDto(
    val id: String? = null,
    val imageUrl: String? = null,
    val scenario: String? = null,
    val solution: String? = null,
    val resources: List<String>? = null,
    val difficulty: String? = null
)

@Serializable
internal data class GPEScenarioBatchDto(val scenarios: List<GPEScenarioDto> = emptyList())
