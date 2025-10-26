package com.ssbmax.core.data.repository

import com.ssbmax.core.domain.model.*

/**
 * Provides mock test data for testing purposes only.
 * Used in unit tests and integration tests.
 */
object TestDataFactory {
    
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
                questionText = "If LEADER is to COMMAND, then TEACHER is to:",
                options = listOf(
                    OIROption("opt_2a", "Student"),
                    OIROption("opt_2b", "School"),
                    OIROption("opt_2c", "Educate"),
                    OIROption("opt_2d", "Learn")
                ),
                correctAnswerId = "opt_2c",
                explanation = "A leader commands, similarly a teacher educates.",
                difficulty = QuestionDifficulty.MEDIUM,
                timeSeconds = 60
            ),
            OIRQuestion(
                id = "oir_mock_3",
                questionNumber = 3,
                type = OIRQuestionType.VERBAL_REASONING,
                questionText = "Complete the series: Optimistic, Hopeful, _____, Confident",
                options = listOf(
                    OIROption("opt_3a", "Pessimistic"),
                    OIROption("opt_3b", "Positive"),
                    OIROption("opt_3c", "Negative"),
                    OIROption("opt_3d", "Doubtful")
                ),
                correctAnswerId = "opt_3b",
                explanation = "The series shows increasing levels of positive outlook.",
                difficulty = QuestionDifficulty.MEDIUM,
                timeSeconds = 60
            ),
            
            // Numerical Ability Questions
            OIRQuestion(
                id = "oir_mock_4",
                questionNumber = 4,
                type = OIRQuestionType.NUMERICAL_ABILITY,
                questionText = "What is 15% of 200?",
                options = listOf(
                    OIROption("opt_4a", "20"),
                    OIROption("opt_4b", "25"),
                    OIROption("opt_4c", "30"),
                    OIROption("opt_4d", "35")
                ),
                correctAnswerId = "opt_4c",
                explanation = "15% of 200 = (15/100) × 200 = 30",
                difficulty = QuestionDifficulty.EASY,
                timeSeconds = 60
            ),
            OIRQuestion(
                id = "oir_mock_5",
                questionNumber = 5,
                type = OIRQuestionType.NUMERICAL_ABILITY,
                questionText = "Find the next number in the series: 2, 6, 12, 20, ?",
                options = listOf(
                    OIROption("opt_5a", "28"),
                    OIROption("opt_5b", "30"),
                    OIROption("opt_5c", "32"),
                    OIROption("opt_5d", "34")
                ),
                correctAnswerId = "opt_5b",
                explanation = "Pattern: +4, +6, +8, +10. Next is 20 + 10 = 30",
                difficulty = QuestionDifficulty.MEDIUM,
                timeSeconds = 60
            ),
            OIRQuestion(
                id = "oir_mock_6",
                questionNumber = 6,
                type = OIRQuestionType.NUMERICAL_ABILITY,
                questionText = "If a train travels 120 km in 2 hours, what is its speed in km/hr?",
                options = listOf(
                    OIROption("opt_6a", "50"),
                    OIROption("opt_6b", "55"),
                    OIROption("opt_6c", "60"),
                    OIROption("opt_6d", "65")
                ),
                correctAnswerId = "opt_6c",
                explanation = "Speed = Distance / Time = 120 / 2 = 60 km/hr",
                difficulty = QuestionDifficulty.EASY,
                timeSeconds = 60
            ),
            
            // Spatial Reasoning Questions
            OIRQuestion(
                id = "oir_mock_7",
                questionNumber = 7,
                type = OIRQuestionType.SPATIAL_REASONING,
                questionText = "Which direction will you face if you start facing North and turn 90° clockwise?",
                options = listOf(
                    OIROption("opt_7a", "South"),
                    OIROption("opt_7b", "East"),
                    OIROption("opt_7c", "West"),
                    OIROption("opt_7d", "North")
                ),
                correctAnswerId = "opt_7b",
                explanation = "From North, 90° clockwise turn leads to East.",
                difficulty = QuestionDifficulty.EASY,
                timeSeconds = 60
            ),
            OIRQuestion(
                id = "oir_mock_8",
                questionNumber = 8,
                type = OIRQuestionType.SPATIAL_REASONING,
                questionText = "How many faces does a cube have?",
                options = listOf(
                    OIROption("opt_8a", "4"),
                    OIROption("opt_8b", "5"),
                    OIROption("opt_8c", "6"),
                    OIROption("opt_8d", "8")
                ),
                correctAnswerId = "opt_8c",
                explanation = "A cube has 6 faces: top, bottom, front, back, left, right.",
                difficulty = QuestionDifficulty.EASY,
                timeSeconds = 60
            ),
            
            // Non-Verbal Reasoning Questions
            OIRQuestion(
                id = "oir_mock_9",
                questionNumber = 9,
                type = OIRQuestionType.NON_VERBAL_REASONING,
                questionText = "Find the odd one out: Square, Rectangle, Triangle, Circle",
                options = listOf(
                    OIROption("opt_9a", "Square"),
                    OIROption("opt_9b", "Rectangle"),
                    OIROption("opt_9c", "Triangle"),
                    OIROption("opt_9d", "Circle")
                ),
                correctAnswerId = "opt_9d",
                explanation = "Circle is the only shape without straight edges.",
                difficulty = QuestionDifficulty.MEDIUM,
                timeSeconds = 60
            ),
            OIRQuestion(
                id = "oir_mock_10",
                questionNumber = 10,
                type = OIRQuestionType.NON_VERBAL_REASONING,
                questionText = "If a mirror is placed vertically, which letter looks the same: A, B, C, D?",
                options = listOf(
                    OIROption("opt_10a", "A"),
                    OIROption("opt_10b", "B"),
                    OIROption("opt_10c", "C"),
                    OIROption("opt_10d", "D")
                ),
                correctAnswerId = "opt_10a",
                explanation = "Letter 'A' is vertically symmetrical and looks the same in a vertical mirror.",
                difficulty = QuestionDifficulty.MEDIUM,
                timeSeconds = 60
            )
        )
    }
    
    /**
     * Mock PPDT Question - 1 sample question with image
     */
    fun getPPDTQuestions(): List<PPDTQuestion> {
        return listOf(
            PPDTQuestion(
                id = "ppdt_mock_1",
                imageUrl = "https://via.placeholder.com/800x600/4A90E2/FFFFFF?text=PPDT+Sample+Image",
                imageDescription = "A group of people in a challenging situation",
                viewingTimeSeconds = 30,
                writingTimeMinutes = 4
            )
        )
    }
    
    /**
     * Mock TAT Questions - 3 sample images with prompts
     */
    fun getTATQuestions(): List<TATQuestion> {
        return listOf(
            TATQuestion(
                id = "tat_mock_1",
                imageUrl = "https://via.placeholder.com/800x600/E74C3C/FFFFFF?text=TAT+Image+1",
                sequenceNumber = 1,
                prompt = "Write a story about what is happening in this picture. Include what led to this situation and what will happen next.",
                viewingTimeSeconds = 30,
                writingTimeMinutes = 4
            ),
            TATQuestion(
                id = "tat_mock_2",
                imageUrl = "https://via.placeholder.com/800x600/27AE60/FFFFFF?text=TAT+Image+2",
                sequenceNumber = 2,
                prompt = "Write a story about what is happening in this picture. Include what led to this situation and what will happen next.",
                viewingTimeSeconds = 30,
                writingTimeMinutes = 4
            ),
            TATQuestion(
                id = "tat_mock_3",
                imageUrl = "https://via.placeholder.com/800x600/F39C12/FFFFFF?text=TAT+Image+3",
                sequenceNumber = 3,
                prompt = "Write a story about what is happening in this picture. Include what led to this situation and what will happen next.",
                viewingTimeSeconds = 30,
                writingTimeMinutes = 4
            )
        )
    }
    
    /**
     * Mock WAT Words - 20 sample words for rapid response
     */
    fun getWATWords(): List<WATWord> {
        return listOf(
            WATWord(id = "wat_mock_1", word = "COURAGE", sequenceNumber = 1),
            WATWord(id = "wat_mock_2", word = "LEADERSHIP", sequenceNumber = 2),
            WATWord(id = "wat_mock_3", word = "CHALLENGE", sequenceNumber = 3),
            WATWord(id = "wat_mock_4", word = "VICTORY", sequenceNumber = 4),
            WATWord(id = "wat_mock_5", word = "DISCIPLINE", sequenceNumber = 5),
            WATWord(id = "wat_mock_6", word = "TEAMWORK", sequenceNumber = 6),
            WATWord(id = "wat_mock_7", word = "AMBITION", sequenceNumber = 7),
            WATWord(id = "wat_mock_8", word = "RESPONSIBILITY", sequenceNumber = 8),
            WATWord(id = "wat_mock_9", word = "INTEGRITY", sequenceNumber = 9),
            WATWord(id = "wat_mock_10", word = "DEDICATION", sequenceNumber = 10),
            WATWord(id = "wat_mock_11", word = "SACRIFICE", sequenceNumber = 11),
            WATWord(id = "wat_mock_12", word = "PATRIOTISM", sequenceNumber = 12),
            WATWord(id = "wat_mock_13", word = "ADVENTURE", sequenceNumber = 13),
            WATWord(id = "wat_mock_14", word = "HONOR", sequenceNumber = 14),
            WATWord(id = "wat_mock_15", word = "RESILIENCE", sequenceNumber = 15),
            WATWord(id = "wat_mock_16", word = "CONFIDENCE", sequenceNumber = 16),
            WATWord(id = "wat_mock_17", word = "DETERMINATION", sequenceNumber = 17),
            WATWord(id = "wat_mock_18", word = "SERVICE", sequenceNumber = 18),
            WATWord(id = "wat_mock_19", word = "EXCELLENCE", sequenceNumber = 19),
            WATWord(id = "wat_mock_20", word = "COMMITMENT", sequenceNumber = 20)
        )
    }
    
    /**
     * Mock SRT Situations - 10 sample situations for quick responses
     */
    fun getSRTSituations(): List<SRTSituation> {
        return listOf(
            SRTSituation(
                id = "srt_mock_1",
                situation = "You are leading a team on a trek and suddenly bad weather sets in. What will you do?",
                sequenceNumber = 1,
                category = SRTCategory.LEADERSHIP,
                timeAllowedSeconds = 30
            ),
            SRTSituation(
                id = "srt_mock_2",
                situation = "You find out that your friend has been cheating in exams. How will you handle this?",
                sequenceNumber = 2,
                category = SRTCategory.ETHICAL_DILEMMA,
                timeAllowedSeconds = 30
            ),
            SRTSituation(
                id = "srt_mock_3",
                situation = "During a group discussion, two members start arguing. What will you do?",
                sequenceNumber = 3,
                category = SRTCategory.INTERPERSONAL,
                timeAllowedSeconds = 30
            ),
            SRTSituation(
                id = "srt_mock_4",
                situation = "You are assigned a project with a very tight deadline. How will you approach it?",
                sequenceNumber = 4,
                category = SRTCategory.DECISION_MAKING,
                timeAllowedSeconds = 30
            ),
            SRTSituation(
                id = "srt_mock_5",
                situation = "You witness an accident on the road. What will be your immediate action?",
                sequenceNumber = 5,
                category = SRTCategory.CRISIS_MANAGEMENT,
                timeAllowedSeconds = 30
            ),
            SRTSituation(
                id = "srt_mock_6",
                situation = "Your senior asks you to do something unethical. How will you respond?",
                sequenceNumber = 6,
                category = SRTCategory.ETHICAL_DILEMMA,
                timeAllowedSeconds = 30
            ),
            SRTSituation(
                id = "srt_mock_7",
                situation = "You are lost in an unfamiliar city without your phone. What will you do?",
                sequenceNumber = 7,
                category = SRTCategory.CRISIS_MANAGEMENT,
                timeAllowedSeconds = 30
            ),
            SRTSituation(
                id = "srt_mock_8",
                situation = "Your team member is not contributing to the project. How will you handle this?",
                sequenceNumber = 8,
                category = SRTCategory.TEAMWORK,
                timeAllowedSeconds = 30
            ),
            SRTSituation(
                id = "srt_mock_9",
                situation = "You receive criticism from your superior in front of others. What will you do?",
                sequenceNumber = 9,
                category = SRTCategory.INTERPERSONAL,
                timeAllowedSeconds = 30
            ),
            SRTSituation(
                id = "srt_mock_10",
                situation = "You have two important commitments at the same time. How will you manage?",
                sequenceNumber = 10,
                category = SRTCategory.RESPONSIBILITY,
                timeAllowedSeconds = 30
            )
        )
    }
}

