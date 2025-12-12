package com.ssbmax.core.domain.model.gto

/**
 * GTO Test Types - 8 individual Group Testing Officer tests
 * 
 * These tests must be completed in sequential order:
 * GD → GPE → Lecturette → PGT → HGT → GOR → IO → CT
 */
enum class GTOTestType(
    val displayName: String,
    val fullName: String,
    val order: Int,
    val description: String
) {
    GROUP_DISCUSSION(
        displayName = "Group Discussion",
        fullName = "Group Discussion",
        order = 1,
        description = "Discuss topics with your group and demonstrate leadership"
    ),
    
    GROUP_PLANNING_EXERCISE(
        displayName = "Group Planning Exercise",
        fullName = "Group Planning Exercise",
        order = 2,
        description = "Plan tactical solutions to group challenges"
    ),
    
    LECTURETTE(
        displayName = "Lecturette",
        fullName = "Lecturette",
        order = 3,
        description = "Deliver a 3-minute speech on a chosen topic"
    ),
    
    PROGRESSIVE_GROUP_TASK(
        displayName = "Progressive Group Task",
        fullName = "Progressive Group Task",
        order = 4,
        description = "Solve progressively difficult group obstacles"
    ),
    
    HALF_GROUP_TASK(
        displayName = "Half Group Task",
        fullName = "Half Group Task",
        order = 5,
        description = "Lead half your group through problem-solving tasks"
    ),
    
    GROUP_OBSTACLE_RACE(
        displayName = "Group Obstacle Race",
        fullName = "Group Obstacle Race",
        order = 6,
        description = "Navigate physical obstacles as a coordinated team"
    ),
    
    INDIVIDUAL_OBSTACLES(
        displayName = "Individual Obstacles",
        fullName = "Individual Obstacles",
        order = 7,
        description = "Complete individual physical challenges"
    ),
    
    COMMAND_TASK(
        displayName = "Command Task",
        fullName = "Command Task",
        order = 8,
        description = "Command and lead your group through tactical exercises"
    );
    
    companion object {
        /**
         * Get test type by order (1-8)
         */
        fun fromOrder(order: Int): GTOTestType? {
            return entries.find { it.order == order }
        }
        
        /**
         * Get all tests that must be completed before this test
         */
        fun getPrerequisites(testType: GTOTestType): List<GTOTestType> {
            return entries.filter { it.order < testType.order }
        }
        
        /**
         * Get the next test in sequence
         */
        fun getNextTest(testType: GTOTestType): GTOTestType? {
            return fromOrder(testType.order + 1)
        }
    }
}
