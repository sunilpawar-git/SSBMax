package com.ssbmax.testing

import com.ssbmax.core.domain.model.*

/**
 * Factory for creating test data objects for UI tests
 */
object TestDataFactory {

    fun createTestUserProfile(
        userId: String = "test-user-123",
        fullName: String = "Test User",
        age: Int = 25,
        gender: Gender = Gender.MALE,
        entryType: EntryType = EntryType.ENTRY_10_PLUS_2,
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

    /**
     * Convenience method to create a FREE tier user
     */
    fun createFreeUser(
        userId: String = "free-user-123",
        fullName: String = "Free User",
        age: Int = 25
    ): UserProfile = createTestUserProfile(
        userId = userId,
        fullName = fullName,
        age = age,
        subscriptionType = SubscriptionType.FREE
    )

    /**
     * Convenience method to create a Pro tier user (Premium Assessor)
     */
    fun createProUser(
        userId: String = "pro-user-123",
        fullName: String = "Pro User",
        age: Int = 25
    ): UserProfile = createTestUserProfile(
        userId = userId,
        fullName = fullName,
        age = age,
        subscriptionType = SubscriptionType.PRO
    )

    /**
     * Convenience method to create an AI Premium tier user
     */
    fun createAIPremiumUser(
        userId: String = "ai-user-123",
        fullName: String = "AI Premium User",
        age: Int = 25
    ): UserProfile = createTestUserProfile(
        userId = userId,
        fullName = fullName,
        age = age,
        subscriptionType = SubscriptionType.PREMIUM
    )

    fun createTestTATQuestion(
        id: String = "tat-1",
        imageUrl: String = "https://example.com/tat1.jpg",
        sequenceNumber: Int = 1
    ): TATQuestion {
        return TATQuestion(
            id = id,
            imageUrl = imageUrl,
            sequenceNumber = sequenceNumber,
            viewingTimeSeconds = 30,
            writingTimeMinutes = 4
        )
    }

    fun createTestWATWord(
        id: String = "wat-1",
        word: String = "BRAVE",
        sequenceNumber: Int = 1
    ): WATWord {
        return WATWord(
            id = id,
            word = word,
            sequenceNumber = sequenceNumber,
            timeAllowedSeconds = 15
        )
    }

    fun createTestSRTSituation(
        id: String = "srt-1",
        situation: String = "You are lost in a forest...",
        sequenceNumber: Int = 1
    ): SRTSituation {
        return SRTSituation(
            id = id,
            situation = situation,
            sequenceNumber = sequenceNumber,
            category = SRTCategory.CRISIS_MANAGEMENT,
            timeAllowedSeconds = 30
        )
    }

    fun createTestOIRQuestion(
        id: String = "oir-1",
        questionNumber: Int = 1,
        questionText: String = "What is 2+2?",
        options: List<OIROption> = listOf(
            OIROption("opt1", "3"),
            OIROption("opt2", "4"),
            OIROption("opt3", "5"),
            OIROption("opt4", "6")
        ),
        correctAnswerId: String = "opt2"
    ): OIRQuestion {
        return OIRQuestion(
            id = id,
            questionNumber = questionNumber,
            type = OIRQuestionType.VERBAL_REASONING,
            questionText = questionText,
            options = options,
            correctAnswerId = correctAnswerId,
            explanation = "2+2=4",
            difficulty = QuestionDifficulty.EASY
        )
    }

    fun createTestPPDTQuestion(
        id: String = "ppdt-1",
        imageUrl: String = "https://example.com/ppdt1.jpg",
        imageDescription: String = "A group scenario"
    ): PPDTQuestion {
        return PPDTQuestion(
            id = id,
            imageUrl = imageUrl,
            imageDescription = imageDescription,
            viewingTimeSeconds = 30,
            writingTimeMinutes = 4
        )
    }

    fun createTestPhase1Progress(
        oirStatus: TestStatus = TestStatus.NOT_ATTEMPTED,
        ppdtStatus: TestStatus = TestStatus.NOT_ATTEMPTED
    ): Phase1Progress {
        return Phase1Progress(
            oirProgress = TestProgress(TestType.OIR, oirStatus),
            ppdtProgress = TestProgress(TestType.PPDT, ppdtStatus)
        )
    }

    fun createTestPhase2Progress(
        psychologyStatus: TestStatus = TestStatus.NOT_ATTEMPTED,
        gtoStatus: TestStatus = TestStatus.NOT_ATTEMPTED,
        interviewStatus: TestStatus = TestStatus.NOT_ATTEMPTED
    ): Phase2Progress {
        return Phase2Progress(
            psychologyProgress = TestProgress(TestType.TAT, psychologyStatus),
            gtoProgress = TestProgress(TestType.GTO_GD, gtoStatus),
            interviewProgress = TestProgress(TestType.IO, interviewStatus)
        )
    }
}

