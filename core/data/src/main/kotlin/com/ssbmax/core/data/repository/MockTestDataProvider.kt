package com.ssbmax.core.data.repository

import com.ssbmax.shared.domain.model.*

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
     * Mock PPDT Questions - 1 sample image
     */
    fun getPPDTQuestions(): List<PPDTQuestion> {
        return listOf(
            PPDTQuestion(
                id = "ppdt_mock_1",
                imageUrl = "https://via.placeholder.com/800x600/4CAF50/FFFFFF?text=PPDT+Sample+Image",
                imageDescription = "A group of people in a meeting room discussing around a table",
                imageContext = PPDTImageContext(
                    sceneDescription = "A hazy image showing a formal meeting or discussion in progress"
                ),
                viewingTimeSeconds = 30,
                writingTimeMinutes = 4
            )
        )
    }
    
    /**
     * Mock TAT Questions - 3 sample images
     */
    fun getTATQuestions(): List<TATQuestion> {
        return (1..11).map { pos ->
            TATQuestion(
                id = "tat_mock_$pos",
                imageUrl = "https://via.placeholder.com/800x600/2196F3/FFFFFF?text=TAT+Image+$pos",
                cardPosition = pos,
                viewingTimeSeconds = 30,
                writingTimeMinutes = 4
            )
        } + TATQuestion(
            id = "blank_card",
            imageUrl = "",
            cardPosition = 12,
            viewingTimeSeconds = 30,
            writingTimeMinutes = 4
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
    /**
     * Mock GPE Questions - 1 sample scenario
     * Uses generated map from assets
     */
    fun getGPEQuestions(): List<GPEQuestion> {
        return listOf(
            GPEQuestion(
                id = "gpe_generated_1",
                imageUrl = "file:///android_asset/gpe_gen_map.png",
                scenario = """
                    You are a group of 8 students from Government College, returning from a nature camp in the forest. You are currently at the 'Rest House' (marked on likely map). It is 1400 hrs. The last bus to the city leaves from the 'Bus Stop' at 1700 hrs, which is 15 km away.
                    
                    While resting, a local villager rushes to you and informs:
                    1. A massive fire has broken out in the forest (North side) and is spreading towards the 'Tribal Settlement'. The villagers need immediate evacuation.
                    2. He saw a group of suspicious men planting explosives under the 'Railway Bridge' (as shown in map). A passenger train is due to pass in 45 minutes.
                    3. One of his friends has been bitten by a poisonous snake and is unconscious at the 'Old Temple' (East side).
                    4. Your own college van has a flat tire and the driver is missing.
                    
                    Resources available:
                    - 1 Jeep (can carry 4 people, speed 40 kmph on road, 20 kmph on track)
                    - 1 Motorboat at the river bank (capacity 3, speed 15 kmph)
                    - A bundle of rope, a first aid kit, and 2 flashlights.
                    - No mobile network coverage.
                    
                    Task:
                    Identify the problems, prioritize them, and produce a plan to handle all situations effectively and reach the Bus Stop by 1700 hrs to catch your bus.
                """.trimIndent(),
                solution = """
                    Priority 1: Stop the train and diffuse explosives at Railway Bridge (High Risk, Time Critical).
                    Priority 2: Evacuate villagers from forest fire (High Risk).
                    Priority 3: Rescue snake bite victim at Old Temple (Medical Emergency).
                    Priority 4: Fix college van and reach Bus Stop by 1700 hrs.
                    
                    Plan:
                    1. Divide group into 3 sub-groups using Jeep and Motorboat.
                    2. Group A (3 students + Jeep): Rush to Railway bridge to alert station master/driver.
                    3. Group B (2 students + Motorboat): Go to Old Temple for snake bite victim.
                    4. Group C (3 students): Stay back, fix van, and coordinate with villagers for fire evacuation.
                """.trimIndent(),
                imageDescription = "Tactical map showing a river with a railway bridge, a forest area to the north, a tribal settlement, a temple to the east, and connecting roads.",
                resources = listOf("Jeep", "Motorboat", "Rope", "First Aid Kit", "Flashlights"),
                viewingTimeSeconds = 60,
                planningTimeSeconds = 1740 // 29 mins
            )
        )
    }
}

