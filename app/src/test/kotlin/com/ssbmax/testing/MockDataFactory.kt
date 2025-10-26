package com.ssbmax.testing

import com.ssbmax.core.domain.model.*
import java.time.Instant

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
        subscriptionTier: SubscriptionTier = SubscriptionTier.BASIC
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
        phoneNumber: String = "+91 9876543210",
        dateOfBirth: String = "2000-01-01",
        gender: Gender = Gender.MALE,
        entryType: EntryType = EntryType.NDA,
        educationLevel: String = "12th Grade",
        preferredServiceBranch: String = "Army"
    ): UserProfile {
        return UserProfile(
            userId = userId,
            fullName = fullName,
            phoneNumber = phoneNumber,
            dateOfBirth = dateOfBirth,
            gender = gender,
            entryType = entryType,
            educationLevel = educationLevel,
            preferredServiceBranch = preferredServiceBranch,
            profilePhotoUrl = null,
            bio = "Aspiring to serve the nation",
            targetSSBDate = null,
            subscriptionType = SubscriptionType.FREE,
            subscriptionExpiryDate = null
        )
    }
    
    // Test-related mocks
    fun createMockTATQuestion(
        id: String = "tat-question-1",
        imageUrl: String = "https://example.com/tat-image-1.jpg",
        position: Int = 1,
        timeLimit: Int = 240
    ): TATQuestion {
        return TATQuestion(
            id = id,
            imageUrl = imageUrl,
            position = position,
            timeLimit = timeLimit
        )
    }
    
    fun createMockWATWord(
        id: String = "wat-word-1",
        word: String = "VICTORY",
        position: Int = 1,
        timeLimit: Int = 15
    ): WATWord {
        return WATWord(
            id = id,
            word = word,
            position = position,
            timeLimit = timeLimit
        )
    }
    
    fun createMockSRTSituation(
        id: String = "srt-situation-1",
        situation: String = "You are leading a team project with a tight deadline.",
        position: Int = 1,
        timeLimit: Int = 30
    ): SRTSituation {
        return SRTSituation(
            id = id,
            situation = situation,
            position = position,
            timeLimit = timeLimit
        )
    }
    
    fun createMockOIRQuestion(
        id: String = "oir-question-1",
        question: String = "What is the capital of India?",
        options: List<String> = listOf("Mumbai", "Delhi", "Kolkata", "Chennai"),
        correctAnswer: Int = 1,
        position: Int = 1
    ): OIRQuestion {
        return OIRQuestion(
            id = id,
            question = question,
            options = options,
            correctAnswer = correctAnswer,
            position = position
        )
    }
    
    fun createMockPPDTQuestion(
        id: String = "ppdt-question-1",
        imageUrl: String = "https://example.com/ppdt-image-1.jpg",
        position: Int = 1,
        observationTime: Int = 30,
        writingTime: Int = 240
    ): PPDTQuestion {
        return PPDTQuestion(
            id = id,
            imageUrl = imageUrl,
            position = position,
            observationTime = observationTime,
            writingTime = writingTime
        )
    }
    
    // Submission-related mocks
    fun createMockTATSubmission(
        userId: String = "test-user-123",
        testId: String = "tat-test-1",
        responses: List<TATResponse> = listOf(
            TATResponse(
                questionId = "tat-question-1",
                story = "A test story about leadership and courage",
                timeSpent = 230
            )
        )
    ): TATSubmission {
        return TATSubmission(
            userId = userId,
            testId = testId,
            testType = TestType.TAT,
            responses = responses,
            startTime = Instant.now().minusSeconds(300),
            endTime = Instant.now(),
            totalTimeSpent = 300
        )
    }
    
    fun createMockWATSubmission(
        userId: String = "test-user-123",
        testId: String = "wat-test-1",
        responses: List<WATResponse> = listOf(
            WATResponse(
                questionId = "wat-word-1",
                word = "VICTORY",
                response = "Achievement through determination",
                timeSpent = 14
            )
        )
    ): WATSubmission {
        return WATSubmission(
            userId = userId,
            testId = testId,
            testType = TestType.WAT,
            responses = responses,
            startTime = Instant.now().minusSeconds(900),
            endTime = Instant.now(),
            totalTimeSpent = 900
        )
    }
    
    fun createMockSRTSubmission(
        userId: String = "test-user-123",
        testId: String = "srt-test-1",
        responses: List<SRTResponse> = listOf(
            SRTResponse(
                questionId = "srt-situation-1",
                situation = "You are leading a team project with a tight deadline.",
                response = "I would organize the team, delegate tasks based on strengths, and maintain clear communication",
                timeSpent = 28
            )
        )
    ): SRTSubmission {
        return SRTSubmission(
            userId = userId,
            testId = testId,
            testType = TestType.SRT,
            responses = responses,
            startTime = Instant.now().minusSeconds(1800),
            endTime = Instant.now(),
            totalTimeSpent = 1800
        )
    }
    
    // Test Progress mocks
    fun createMockPhase1Progress(
        userId: String = "test-user-123",
        oirCompleted: Boolean = true,
        ppdtCompleted: Boolean = false,
        tatCompleted: Boolean = false,
        watCompleted: Boolean = false,
        srtCompleted: Boolean = false
    ): Phase1Progress {
        return Phase1Progress(
            userId = userId,
            oirCompleted = oirCompleted,
            ppdtCompleted = ppdtCompleted,
            tatCompleted = tatCompleted,
            watCompleted = watCompleted,
            srtCompleted = srtCompleted,
            totalTests = 5,
            completedTests = listOf(oirCompleted, ppdtCompleted, tatCompleted, watCompleted, srtCompleted)
                .count { it },
            lastUpdated = Instant.now()
        )
    }
    
    fun createMockPhase2Progress(
        userId: String = "test-user-123",
        gtoCompleted: Boolean = false,
        interviewCompleted: Boolean = false,
        conferenceCompleted: Boolean = false
    ): Phase2Progress {
        return Phase2Progress(
            userId = userId,
            gtoCompleted = gtoCompleted,
            interviewCompleted = interviewCompleted,
            conferenceCompleted = conferenceCompleted,
            totalTests = 3,
            completedTests = listOf(gtoCompleted, interviewCompleted, conferenceCompleted)
                .count { it },
            lastUpdated = Instant.now()
        )
    }
    
    // Test lists
    fun createMockTATQuestions(count: Int = 12): List<TATQuestion> {
        return (1..count).map { i ->
            createMockTATQuestion(
                id = "tat-question-$i",
                imageUrl = "https://example.com/tat-image-$i.jpg",
                position = i,
                timeLimit = 240
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
                position = i,
                timeLimit = 15
            )
        }
    }
    
    fun createMockSRTSituations(count: Int = 60): List<SRTSituation> {
        return (1..count).map { i ->
            createMockSRTSituation(
                id = "srt-situation-$i",
                situation = "Test situation $i requiring problem-solving and decision-making.",
                position = i,
                timeLimit = 30
            )
        }
    }
    
    fun createMockOIRQuestions(count: Int = 50): List<OIRQuestion> {
        return (1..count).map { i ->
            createMockOIRQuestion(
                id = "oir-question-$i",
                question = "Test question $i about general knowledge or reasoning?",
                options = listOf("Option A", "Option B", "Option C", "Option D"),
                correctAnswer = i % 4,
                position = i
            )
        }
    }
    
    fun createMockPPDTQuestions(count: Int = 2): List<PPDTQuestion> {
        return (1..count).map { i ->
            createMockPPDTQuestion(
                id = "ppdt-question-$i",
                imageUrl = "https://example.com/ppdt-image-$i.jpg",
                position = i,
                observationTime = 30,
                writingTime = 240
            )
        }
    }
}

