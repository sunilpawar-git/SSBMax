package com.example.ssbmax.data

// SSB Content Data Models and Sample Data

data class SSBStudyMaterial(
    val id: String,
    val title: String,
    val description: String,
    val category: SSBCategory,
    val content: String,
    val difficulty: DifficultyLevel = DifficultyLevel.BEGINNER
)

data class SSBTestQuestion(
    val id: String,
    val question: String,
    val type: TestType,
    val category: SSBCategory,
    val options: List<String>? = null,
    val correctAnswer: String? = null,
    val timeLimitSeconds: Int = 30
)

data class SSBTip(
    val id: String,
    val title: String,
    val content: String,
    val category: SSBCategory,
    val priority: TipPriority = TipPriority.MEDIUM
)

enum class SSBCategory {
    PSYCHOLOGY, GTO, INTERVIEW, CONFERENCE, GENERAL
}

enum class TestType {
    TAT, WAT, SRT, SDT, GD, INTERVIEW, GK
}

enum class DifficultyLevel {
    BEGINNER, INTERMEDIATE, ADVANCED
}

enum class TipPriority {
    LOW, MEDIUM, HIGH, CRITICAL
}

// Sample SSB Content Data
object SSBDataSource {

    val studyMaterials = listOf(
        SSBStudyMaterial(
            id = "tat_1",
            title = "Thematic Apperception Test (TAT)",
            description = "Learn how to interpret pictures and write meaningful stories",
            category = SSBCategory.PSYCHOLOGY,
            content = """
                The Thematic Apperception Test (TAT) is a psychological test where you will be shown pictures and asked to write stories about them.

                Key Points:
                • You will see 12 pictures (11 black and white, 1 blank)
                • Time limit: 4 minutes per picture for stories, 30 seconds for blank
                • Stories should be positive, logical, and show good character traits

                Tips for Success:
                • Focus on the hero's emotions, thoughts, and actions
                • Show problem-solving ability and positive outcomes
                • Keep stories realistic and believable
                • Demonstrate leadership, cooperation, and determination
            """.trimIndent(),
            difficulty = DifficultyLevel.BEGINNER
        ),

        SSBStudyMaterial(
            id = "wat_1",
            title = "Word Association Test (WAT)",
            description = "Practice giving quick, positive responses to words",
            category = SSBCategory.PSYCHOLOGY,
            content = """
                The Word Association Test measures your subconscious thoughts and personality.

                Format:
                • 60 words shown one by one
                • 15 seconds per word to write your response
                • First 30 words: Individual response
                • Last 30 words: Group response

                Guidelines:
                • Responses should be positive and natural
                • Avoid negative, violent, or controversial words
                • Show qualities like courage, honesty, leadership
                • Keep responses concise (2-5 words)
            """.trimIndent(),
            difficulty = DifficultyLevel.INTERMEDIATE
        ),

        SSBStudyMaterial(
            id = "gto_1",
            title = "Group Testing Officer (GTO) Tasks",
            description = "Master outdoor group activities and problem-solving",
            category = SSBCategory.GTO,
            content = """
                GTO tests assess your ability to work in teams and solve problems under pressure.

                Main Tasks:
                1. Group Discussion (GD)
                2. Group Planning Exercise (GPE)
                3. Progressive Group Task (PGT)
                4. Group Obstacle Race (GOR)
                5. Lecturette
                6. Command Task
                7. Individual Obstacles

                Important Qualities:
                • Leadership and initiative
                • Cooperation and teamwork
                • Problem-solving ability
                • Physical and mental endurance
                • Effective communication
            """.trimIndent(),
            difficulty = DifficultyLevel.ADVANCED
        )
    )

    val practiceQuestions = listOf(
        SSBTestQuestion(
            id = "wat_sample_1",
            question = "Army",
            type = TestType.WAT,
            category = SSBCategory.PSYCHOLOGY,
            timeLimitSeconds = 15
        ),

        SSBTestQuestion(
            id = "wat_sample_2",
            question = "Courage",
            type = TestType.WAT,
            category = SSBCategory.PSYCHOLOGY,
            timeLimitSeconds = 15
        ),

        SSBTestQuestion(
            id = "wat_sample_3",
            question = "Challenge",
            type = TestType.WAT,
            category = SSBCategory.PSYCHOLOGY,
            timeLimitSeconds = 15
        ),

        SSBTestQuestion(
            id = "srt_sample_1",
            question = "You are leading a team through a forest when one member gets injured. What would you do?",
            type = TestType.SRT,
            category = SSBCategory.PSYCHOLOGY,
            timeLimitSeconds = 30
        ),

        SSBTestQuestion(
            id = "srt_sample_2",
            question = "During a group task, two team members start arguing. How would you handle this?",
            type = TestType.SRT,
            category = SSBCategory.PSYCHOLOGY,
            timeLimitSeconds = 30
        )
    )

    val tips = listOf(
        SSBTip(
            id = "tip_general_1",
            title = "Be Yourself",
            content = "Authenticity is crucial in SSB. Don't try to be someone you're not. The board can easily spot fakeness.",
            category = SSBCategory.GENERAL,
            priority = TipPriority.HIGH
        ),

        SSBTip(
            id = "tip_general_2",
            title = "Stay Positive",
            content = "Maintain a positive attitude throughout all tests. Show enthusiasm and energy in everything you do.",
            category = SSBCategory.GENERAL,
            priority = TipPriority.HIGH
        ),

        SSBTip(
            id = "tip_psychology_1",
            title = "Practice Daily",
            content = "Practice TAT, WAT, and SRT daily. The more you practice, the more natural your responses become.",
            category = SSBCategory.PSYCHOLOGY,
            priority = TipPriority.CRITICAL
        ),

        SSBTip(
            id = "tip_gto_1",
            title = "Show Initiative",
            content = "In GTO tasks, don't wait for instructions. Take initiative and show leadership qualities naturally.",
            category = SSBCategory.GTO,
            priority = TipPriority.HIGH
        ),

        SSBTip(
            id = "tip_interview_1",
            title = "Prepare Thoroughly",
            content = "Research current affairs, defense topics, and personal questions. Be ready to discuss your strengths and weaknesses honestly.",
            category = SSBCategory.INTERVIEW,
            priority = TipPriority.CRITICAL
        )
    )

    // Get materials by category
    fun getMaterialsByCategory(category: SSBCategory): List<SSBStudyMaterial> {
        return studyMaterials.filter { it.category == category }
    }

    // Get tips by category
    fun getTipsByCategory(category: SSBCategory): List<SSBTip> {
        return tips.filter { it.category == category }
    }

    // Get questions by type
    fun getQuestionsByType(type: TestType): List<SSBTestQuestion> {
        return practiceQuestions.filter { it.type == type }
    }
}

