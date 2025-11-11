package com.ssbmax.testing

import com.ssbmax.core.domain.model.*

/**
 * Factory for creating mock test data consistently across tests
 * Provides realistic test data with configurable properties
 */
object MockDataFactory {
    
    // User-related mocks
    fun createMockUser(
        id: String = "test-user-123",
        email: String = "test@ssbmax.com",
        displayName: String = "Test User",
        role: UserRole = UserRole.STUDENT,
        subscriptionTier: SubscriptionTier = SubscriptionTier.FREE
    ): SSBMaxUser {
        return SSBMaxUser(
            id = id,
            email = email,
            displayName = displayName,
            role = role,
            subscriptionTier = subscriptionTier,
            subscription = null
        )
    }
    
    fun createMockUserProfile(
        userId: String = "test-user-123",
        fullName: String = "Test Aspirant",
        age: Int = 22,
        gender: Gender = Gender.MALE,
        entryType: EntryType = EntryType.GRADUATE,
        subscriptionType: SubscriptionType = SubscriptionType.FREE
    ): UserProfile {
        return UserProfile(
            userId = userId,
            fullName = fullName,
            age = age,
            gender = gender,
            entryType = entryType,
            profilePictureUrl = null,
            subscriptionType = subscriptionType
        )
    }
    
    // Test question mocks
    fun createMockTATQuestion(
        id: String = "tat-question-1",
        imageUrl: String = "https://example.com/tat-image-1.jpg",
        sequenceNumber: Int = 1,
        prompt: String = "Write a story about what you see in the picture",
        viewingTimeSeconds: Int = 30,
        writingTimeMinutes: Int = 4,
        minCharacters: Int = 150,
        maxCharacters: Int = 800
    ): TATQuestion {
        return TATQuestion(
            id = id,
            imageUrl = imageUrl,
            sequenceNumber = sequenceNumber,
            prompt = prompt,
            viewingTimeSeconds = viewingTimeSeconds,
            writingTimeMinutes = writingTimeMinutes,
            minCharacters = minCharacters,
            maxCharacters = maxCharacters
        )
    }
    
    fun createMockWATWord(
        id: String = "wat-word-1",
        word: String = "VICTORY",
        sequenceNumber: Int = 1
    ): WATWord {
        return WATWord(
            id = id,
            word = word,
            sequenceNumber = sequenceNumber
        )
    }
    
    fun createMockSRTSituation(
        id: String = "srt-situation-1",
        situation: String = "You are leading a team project with a tight deadline.",
        sequenceNumber: Int = 1,
        category: SRTCategory = SRTCategory.LEADERSHIP
    ): SRTSituation {
        return SRTSituation(
            id = id,
            situation = situation,
            sequenceNumber = sequenceNumber,
            category = category
        )
    }
    
    fun createMockOIRQuestion(
        id: String = "oir-question-1",
        questionNumber: Int = 1,
        questionText: String = "What is the capital of India?",
        options: List<OIROption> = listOf(
            OIROption(id = "opt1", text = "Mumbai"),
            OIROption(id = "opt2", text = "Delhi"),
            OIROption(id = "opt3", text = "Kolkata"),
            OIROption(id = "opt4", text = "Chennai")
        ),
        correctAnswerId: String = "opt2",
        type: OIRQuestionType = OIRQuestionType.VERBAL_REASONING,
        difficulty: QuestionDifficulty = QuestionDifficulty.MEDIUM
    ): OIRQuestion {
        return OIRQuestion(
            id = id,
            questionNumber = questionNumber,
            type = type,
            questionText = questionText,
            options = options,
            correctAnswerId = correctAnswerId,
            explanation = "Delhi is the capital of India",
            difficulty = difficulty
        )
    }
    
    fun createMockPPDTQuestion(
        id: String = "ppdt-question-1",
        imageUrl: String = "https://example.com/ppdt-image-1.jpg",
        imageDescription: String = "A group scene with people in various activities"
    ): PPDTQuestion {
        return PPDTQuestion(
            id = id,
            imageUrl = imageUrl,
            imageDescription = imageDescription
        )
    }
    
    // Test Progress mocks
    fun createMockPhase1Progress(
        oirStatus: TestStatus = TestStatus.NOT_ATTEMPTED,
        ppdtStatus: TestStatus = TestStatus.NOT_ATTEMPTED
    ): Phase1Progress {
        return Phase1Progress(
            oirProgress = TestProgress(TestType.OIR, oirStatus),
            ppdtProgress = TestProgress(TestType.PPDT, ppdtStatus)
        )
    }
    
    fun createMockPhase2Progress(
        tatStatus: TestStatus = TestStatus.NOT_ATTEMPTED,
        watStatus: TestStatus = TestStatus.NOT_ATTEMPTED,
        srtStatus: TestStatus = TestStatus.NOT_ATTEMPTED
    ): Phase2Progress {
        return Phase2Progress(
            psychologyProgress = TestProgress(TestType.TAT, tatStatus),
            gtoProgress = TestProgress(TestType.GTO_GD, TestStatus.NOT_ATTEMPTED),
            interviewProgress = TestProgress(TestType.IO, TestStatus.NOT_ATTEMPTED)
        )
    }
    
    // Test lists
    fun createMockTATQuestions(count: Int = 12): List<TATQuestion> {
        return (1..count).map { i ->
            createMockTATQuestion(
                id = "tat-question-$i",
                imageUrl = "https://example.com/tat-image-$i.jpg",
                sequenceNumber = i
            )
        }
    }
    
    fun createMockWATWords(count: Int = 60): List<WATWord> {
        val words = listOf(
            "VICTORY", "COURAGE", "LEADER", "TEAM", "DUTY",
            "HONOR", "BRAVE", "DISCIPLINE", "STRENGTH", "WISDOM"
        )
        return (1..count).map { i ->
            createMockWATWord(
                id = "wat-word-$i",
                word = words[i % words.size],
                sequenceNumber = i
            )
        }
    }
    
    fun createMockSRTSituations(count: Int = 60): List<SRTSituation> {
        val categories = SRTCategory.values()
        return (1..count).map { i ->
            createMockSRTSituation(
                id = "srt-situation-$i",
                situation = "Test situation $i requiring problem-solving and decision-making.",
                sequenceNumber = i,
                category = categories[i % categories.size]
            )
        }
    }
    
    fun createMockOIRQuestions(count: Int = 50): List<OIRQuestion> {
        return (1..count).map { i ->
            createMockOIRQuestion(
                id = "oir-question-$i",
                questionNumber = i,
                questionText = "Test question $i about general knowledge or reasoning?"
            )
        }
    }
    
    fun createMockPPDTQuestions(count: Int = 2): List<PPDTQuestion> {
        return (1..count).map { i ->
            createMockPPDTQuestion(
                id = "ppdt-question-$i",
                imageUrl = "https://example.com/ppdt-image-$i.jpg",
                imageDescription = "Test image $i with multiple characters"
            )
        }
    }
}

