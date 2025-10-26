package com.ssbmax.core.data.repository

import com.ssbmax.core.domain.model.*

/**
 * TEMPORARY: Provides mock test data for development and fallback when Firestore is unavailable.
 * 
 * This ensures seamless UX while Firestore integration is being set up.
 * When Firestore has real data, these mocks automatically stop being used.
 * 
 * TODO: Remove this file once all test questions are uploaded to Firestore in production.
 */
object MockTestDataProvider {
    
    /**
     * Mock OIR Questions - 10 sample questions across different types
     */
    fun getOIRQuestions(): List<OIRQuestion> {
        return listOf(
            // Verbal Reasoning Questions
            OIRQuestion(
                id = "oir_mock_1",
                questionNumber = 1,
                type = OIRQuestionType.VERBAL_REASONING,
                questionText = "Choose the word most similar in meaning to 'COURAGEOUS':",
                options = listOf(
                    OIROption("opt_1a", "Fearful"),
                    OIROption("opt_1b", "Brave"),
                    OIROption("opt_1c", "Timid"),
                    OIROption("opt_1d", "Weak")
                ),
                correctAnswerId = "opt_1b",
                explanation = "Courageous means brave or fearless in facing danger.",
                difficulty = QuestionDifficulty.EASY,
                timeSeconds = 60
            ),
            OIRQuestion(
                id = "oir_mock_2",
                questionNumber = 2,
                type = OIRQuestionType.VERBAL_REASONING,
                questionText = "If 'MOUNTAIN' is written as 'LPVMUZJO', how is 'VALLEY' written?",
                options = listOf(
                    OIROption("opt_2a", "UBMMFZ"),
                    OIROption("opt_2b", "WBMMFZ"),
                    OIROption("opt_2c", "UBKMDZ"),
                    OIROption("opt_2d", "VBMMFZ")
                ),
                correctAnswerId = "opt_2d",
                explanation = "Each letter is shifted back by one position in the alphabet.",
                difficulty = QuestionDifficulty.MEDIUM,
                timeSeconds = 90
            ),
            OIRQuestion(
                id = "oir_mock_3",
                questionNumber = 3,
                type = OIRQuestionType.VERBAL_REASONING,
                questionText = "Complete the series: AZ, BY, CX, DW, __",
                options = listOf(
                    OIROption("opt_3a", "EV"),
                    OIROption("opt_3b", "FU"),
                    OIROption("opt_3c", "EW"),
                    OIROption("opt_3d", "EU")
                ),
                correctAnswerId = "opt_3a",
                explanation = "First letter increases alphabetically, second letter decreases.",
                difficulty = QuestionDifficulty.EASY,
                timeSeconds = 60
            ),
            
            // Numerical Ability Questions
            OIRQuestion(
                id = "oir_mock_4",
                questionNumber = 4,
                type = OIRQuestionType.NUMERICAL_ABILITY,
                questionText = "If a train travels 120 km in 2 hours, what is its speed in km/h?",
                options = listOf(
                    OIROption("opt_4a", "50"),
                    OIROption("opt_4b", "60"),
                    OIROption("opt_4c", "70"),
                    OIROption("opt_4d", "80")
                ),
                correctAnswerId = "opt_4b",
                explanation = "Speed = Distance / Time = 120 / 2 = 60 km/h",
                difficulty = QuestionDifficulty.EASY,
                timeSeconds = 60
            ),
            OIRQuestion(
                id = "oir_mock_5",
                questionNumber = 5,
                type = OIRQuestionType.NUMERICAL_ABILITY,
                questionText = "What is 15% of 200?",
                options = listOf(
                    OIROption("opt_5a", "25"),
                    OIROption("opt_5b", "30"),
                    OIROption("opt_5c", "35"),
                    OIROption("opt_5d", "40")
                ),
                correctAnswerId = "opt_5b",
                explanation = "15% of 200 = (15/100) × 200 = 30",
                difficulty = QuestionDifficulty.EASY,
                timeSeconds = 60
            ),
            OIRQuestion(
                id = "oir_mock_6",
                questionNumber = 6,
                type = OIRQuestionType.NUMERICAL_ABILITY,
                questionText = "If x + 5 = 12, what is x?",
                options = listOf(
                    OIROption("opt_6a", "5"),
                    OIROption("opt_6b", "6"),
                    OIROption("opt_6c", "7"),
                    OIROption("opt_6d", "8")
                ),
                correctAnswerId = "opt_6c",
                explanation = "x = 12 - 5 = 7",
                difficulty = QuestionDifficulty.EASY,
                timeSeconds = 60
            ),
            
            // Spatial Reasoning Questions
            OIRQuestion(
                id = "oir_mock_7",
                questionNumber = 7,
                type = OIRQuestionType.SPATIAL_REASONING,
                questionText = "Which shape completes the pattern?",
                options = listOf(
                    OIROption("opt_7a", "Square"),
                    OIROption("opt_7b", "Circle"),
                    OIROption("opt_7c", "Triangle"),
                    OIROption("opt_7d", "Pentagon")
                ),
                correctAnswerId = "opt_7c",
                explanation = "The pattern alternates between polygons with increasing sides.",
                difficulty = QuestionDifficulty.MEDIUM,
                timeSeconds = 90
            ),
            OIRQuestion(
                id = "oir_mock_8",
                questionNumber = 8,
                type = OIRQuestionType.SPATIAL_REASONING,
                questionText = "How many faces does a cube have?",
                options = listOf(
                    OIROption("opt_8a", "4"),
                    OIROption("opt_8b", "6"),
                    OIROption("opt_8c", "8"),
                    OIROption("opt_8d", "12")
                ),
                correctAnswerId = "opt_8b",
                explanation = "A cube has 6 faces.",
                difficulty = QuestionDifficulty.EASY,
                timeSeconds = 60
            ),
            
            // Non-Verbal Reasoning Questions
            OIRQuestion(
                id = "oir_mock_9",
                questionNumber = 9,
                type = OIRQuestionType.NON_VERBAL_REASONING,
                questionText = "Identify the odd one out:",
                options = listOf(
                    OIROption("opt_9a", "Circle"),
                    OIROption("opt_9b", "Square"),
                    OIROption("opt_9c", "Rectangle"),
                    OIROption("opt_9d", "Line")
                ),
                correctAnswerId = "opt_9d",
                explanation = "Line is not a closed shape, while others are.",
                difficulty = QuestionDifficulty.EASY,
                timeSeconds = 60
            ),
            OIRQuestion(
                id = "oir_mock_10",
                questionNumber = 10,
                type = OIRQuestionType.NON_VERBAL_REASONING,
                questionText = "What comes next in the series: ○ □ △ ○ □ ?",
                options = listOf(
                    OIROption("opt_10a", "○"),
                    OIROption("opt_10b", "□"),
                    OIROption("opt_10c", "△"),
                    OIROption("opt_10d", "◇")
                ),
                correctAnswerId = "opt_10c",
                explanation = "The pattern repeats: circle, square, triangle.",
                difficulty = QuestionDifficulty.EASY,
                timeSeconds = 60
            )
        )
    }
    
    /**
     * Mock PPDT Questions - 1 sample image
     */
    fun getPPDTQuestions(): List<PPDTQuestion> {
        return listOf(
            PPDTQuestion(
                id = "ppdt_mock_1",
                imageUrl = "https://via.placeholder.com/800x600/4CAF50/FFFFFF?text=PPDT+Sample+Image",
                imageDescription = "A group of people in a meeting room discussing around a table",
                viewingTimeSeconds = 30,
                writingTimeMinutes = 4
            )
        )
    }
    
    /**
     * Mock TAT Questions - 3 sample images
     */
    fun getTATQuestions(): List<TATQuestion> {
        return listOf(
            TATQuestion(
                id = "tat_mock_1",
                imageUrl = "https://via.placeholder.com/800x600/2196F3/FFFFFF?text=TAT+Image+1",
                sequenceNumber = 1,
                prompt = "Write a story about what you see in this picture",
                viewingTimeSeconds = 30,
                writingTimeMinutes = 4
            ),
            TATQuestion(
                id = "tat_mock_2",
                imageUrl = "https://via.placeholder.com/800x600/FF9800/FFFFFF?text=TAT+Image+2",
                sequenceNumber = 2,
                prompt = "Write a story about what you see in this picture",
                viewingTimeSeconds = 30,
                writingTimeMinutes = 4
            ),
            TATQuestion(
                id = "tat_mock_3",
                imageUrl = "https://via.placeholder.com/800x600/9C27B0/FFFFFF?text=TAT+Image+3",
                sequenceNumber = 3,
                prompt = "Write a story about what you see in this picture",
                viewingTimeSeconds = 30,
                writingTimeMinutes = 4
            )
        )
    }
    
    /**
     * Mock WAT Words - 20 sample words
     */
    fun getWATWords(): List<WATWord> {
        return listOf(
            WATWord("wat_mock_1", "COURAGE", 1, 15),
            WATWord("wat_mock_2", "LEADERSHIP", 2, 15),
            WATWord("wat_mock_3", "HONESTY", 3, 15),
            WATWord("wat_mock_4", "DISCIPLINE", 4, 15),
            WATWord("wat_mock_5", "DETERMINATION", 5, 15),
            WATWord("wat_mock_6", "RESPONSIBILITY", 6, 15),
            WATWord("wat_mock_7", "INTEGRITY", 7, 15),
            WATWord("wat_mock_8", "CONFIDENCE", 8, 15),
            WATWord("wat_mock_9", "ADAPTABILITY", 9, 15),
            WATWord("wat_mock_10", "TEAMWORK", 10, 15),
            WATWord("wat_mock_11", "PERSEVERANCE", 11, 15),
            WATWord("wat_mock_12", "DEDICATION", 12, 15),
            WATWord("wat_mock_13", "LOYALTY", 13, 15),
            WATWord("wat_mock_14", "SACRIFICE", 14, 15),
            WATWord("wat_mock_15", "PATRIOTISM", 15, 15),
            WATWord("wat_mock_16", "INNOVATION", 16, 15),
            WATWord("wat_mock_17", "COMPASSION", 17, 15),
            WATWord("wat_mock_18", "RESILIENCE", 18, 15),
            WATWord("wat_mock_19", "EXCELLENCE", 19, 15),
            WATWord("wat_mock_20", "ACHIEVEMENT", 20, 15)
        )
    }
    
    /**
     * Mock SRT Situations - 10 sample scenarios
     */
    fun getSRTSituations(): List<SRTSituation> {
        return listOf(
            SRTSituation(
                id = "srt_mock_1",
                situation = "You are leading a team on an important mission, and one of your team members falls sick. What will you do?",
                sequenceNumber = 1,
                category = SRTCategory.LEADERSHIP,
                timeAllowedSeconds = 30
            ),
            SRTSituation(
                id = "srt_mock_2",
                situation = "You witness a senior colleague taking credit for your work. How do you handle this?",
                sequenceNumber = 2,
                category = SRTCategory.ETHICAL_DILEMMA,
                timeAllowedSeconds = 30
            ),
            SRTSituation(
                id = "srt_mock_3",
                situation = "During a training exercise, you notice a fire breaking out in a nearby building. What will you do?",
                sequenceNumber = 3,
                category = SRTCategory.CRISIS_MANAGEMENT,
                timeAllowedSeconds = 30
            ),
            SRTSituation(
                id = "srt_mock_4",
                situation = "You are given two important tasks with the same deadline. How will you manage?",
                sequenceNumber = 4,
                category = SRTCategory.DECISION_MAKING,
                timeAllowedSeconds = 30
            ),
            SRTSituation(
                id = "srt_mock_5",
                situation = "Your friend asks you to help them cheat in an exam. What will you do?",
                sequenceNumber = 5,
                category = SRTCategory.ETHICAL_DILEMMA,
                timeAllowedSeconds = 30
            ),
            SRTSituation(
                id = "srt_mock_6",
                situation = "You are camping in the mountains and get separated from your group during a storm. What will you do?",
                sequenceNumber = 6,
                category = SRTCategory.CRISIS_MANAGEMENT,
                timeAllowedSeconds = 30
            ),
            SRTSituation(
                id = "srt_mock_7",
                situation = "Your commanding officer gives you an order that you believe is wrong. How do you respond?",
                sequenceNumber = 7,
                category = SRTCategory.LEADERSHIP,
                timeAllowedSeconds = 30
            ),
            SRTSituation(
                id = "srt_mock_8",
                situation = "You discover that a teammate has been leaking confidential information. What will you do?",
                sequenceNumber = 8,
                category = SRTCategory.RESPONSIBILITY,
                timeAllowedSeconds = 30
            ),
            SRTSituation(
                id = "srt_mock_9",
                situation = "During physical training, you injure your leg but the instructor is watching. What will you do?",
                sequenceNumber = 9,
                category = SRTCategory.DECISION_MAKING,
                timeAllowedSeconds = 30
            ),
            SRTSituation(
                id = "srt_mock_10",
                situation = "You are selected for a prestigious assignment, but your best friend who is more deserving is not. How do you handle this?",
                sequenceNumber = 10,
                category = SRTCategory.ETHICAL_DILEMMA,
                timeAllowedSeconds = 30
            )
        )
    }
}

