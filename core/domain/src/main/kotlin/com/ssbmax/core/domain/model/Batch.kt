package com.ssbmax.core.domain.model

/**
 * Batch/Class model for instructor-student grouping
 */
data class Batch(
    val id: String,
    val name: String,
    val description: String? = null,
    val instructorId: String,
    val inviteCode: String,
    val studentIds: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val startDate: Long? = null,
    val endDate: Long? = null,
    val isActive: Boolean = true,
    val maxStudents: Int = 50,
    val tags: List<String> = emptyList(), // e.g., ["NDA", "CDS", "AFCAT"]
    val syllabus: List<String> = emptyList() // Test types to cover
) {
    val currentStudentCount: Int
        get() = studentIds.size
    
    val isFull: Boolean
        get() = currentStudentCount >= maxStudents
    
    val canJoin: Boolean
        get() = isActive && !isFull
}

/**
 * Batch invitation details
 */
data class BatchInvitation(
    val batchId: String,
    val batchName: String,
    val instructorName: String,
    val inviteCode: String,
    val createdAt: Long,
    val expiresAt: Long? = null
) {
    val isExpired: Boolean
        get() = expiresAt?.let { it < System.currentTimeMillis() } ?: false
}

/**
 * Batch analytics for instructors
 */
data class BatchAnalytics(
    val batchId: String,
    val totalStudents: Int,
    val activeStudents: Int,
    val averageProgress: Float,
    val totalTestsSubmitted: Int,
    val averageScore: Float,
    val phase1Completion: Float,
    val phase2Completion: Float,
    val topPerformers: List<StudentPerformance>,
    val strugglingStudents: List<StudentPerformance>,
    val testTypeDistribution: Map<TestType, Int>
)

/**
 * Individual student performance summary
 */
data class StudentPerformance(
    val studentId: String,
    val studentName: String,
    val averageScore: Float,
    val testsCompleted: Int,
    val lastActiveAt: Long,
    val currentStreak: Int,
    val phase1Score: Float? = null,
    val phase2Score: Float? = null
)

