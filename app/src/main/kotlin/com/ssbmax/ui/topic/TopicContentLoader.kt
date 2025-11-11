package com.ssbmax.ui.topic

import com.ssbmax.core.domain.model.TestType

/**
 * Helper class to load topic-specific content.
 * Separated from ViewModel to keep files under 300 lines.
 */
object TopicContentLoader {
    
    fun getTopicInfo(testType: String): TopicInfo {
        return when (testType.uppercase()) {
            // Phase 1 Topics
            "OIR" -> TopicInfo(
                title = "Officer Intelligence Rating",
                introduction = getIntroduction(testType),
                studyMaterials = getStudyMaterials(testType),
                tests = listOf(TestType.OIR)
            )
            "PPDT" -> TopicInfo(
                title = "Picture Perception & Description Test",
                introduction = getIntroduction(testType),
                studyMaterials = getStudyMaterials(testType),
                tests = listOf(TestType.PPDT)
            )
            // Phase 2 Topics
            "PIQ_FORM", "PIQ" -> TopicInfo(
                title = "Personal Information Questionnaire",
                introduction = getIntroduction("PIQ_FORM"),
                studyMaterials = getStudyMaterials("PIQ_FORM"),
                tests = listOf(TestType.PIQ) // Show PIQ form filling option
            )
            "PSYCHOLOGY" -> TopicInfo(
                title = "Psychology Tests",
                introduction = getIntroduction(testType),
                studyMaterials = getStudyMaterials(testType),
                tests = listOf(TestType.TAT, TestType.WAT, TestType.SRT, TestType.SD)
            )
            "GTO" -> TopicInfo(
                title = "Group Testing Officer Tasks",
                introduction = getIntroduction(testType),
                studyMaterials = getStudyMaterials(testType),
                tests = listOf(
                    TestType.GTO_GD,
                    TestType.GTO_GPE,
                    TestType.GTO_PGT,
                    TestType.GTO_GOR,
                    TestType.GTO_HGT,
                    TestType.GTO_LECTURETTE,
                    TestType.GTO_IO,
                    TestType.GTO_CT
                )
            )
            "INTERVIEW" -> TopicInfo(
                title = "Interview Preparation",
                introduction = getIntroduction(testType),
                studyMaterials = getStudyMaterials(testType),
                tests = listOf(TestType.IO)
            )
            "CONFERENCE" -> TopicInfo(
                title = "Conference",
                introduction = getIntroduction("CONFERENCE"),
                studyMaterials = getStudyMaterials("CONFERENCE"),
                tests = emptyList() // Conference is final stage, no tests
            )
            // Other Topics
            "MEDICALS" -> TopicInfo(
                title = "Medical Examination",
                introduction = getIntroduction("MEDICALS"),
                studyMaterials = getStudyMaterials("MEDICALS"),
                tests = emptyList() // Medicals is examination, not a test
            )
            "SSB_OVERVIEW" -> TopicInfo(
                title = "Overview of SSB",
                introduction = getIntroduction("SSB_OVERVIEW"),
                studyMaterials = getStudyMaterials("SSB_OVERVIEW"),
                tests = emptyList() // Overview is informational
            )
            else -> TopicInfo(
                title = "SSB Topic",
                introduction = "Learn about SSB selection process.",
                studyMaterials = emptyList(),
                tests = emptyList()
            )
        }
    }
    
    private fun getIntroduction(testType: String): String {
        return when (testType.uppercase()) {
            "OIR" -> """
                The Officer Intelligence Rating (OIR) test evaluates your cognitive abilities, 
                logical reasoning, and problem-solving skills. It consists of verbal and 
                non-verbal reasoning questions designed to assess your mental alertness and 
                decision-making capabilities under time pressure.
                
                The test typically includes:
                - Verbal reasoning questions
                - Numerical ability problems
                - Abstract reasoning puzzles
                - Spatial visualization tasks
                
                Duration: 30-40 minutes
                Questions: 40-50 questions
                Difficulty: Moderate to High
            """.trimIndent()
            "PPDT" -> """
                Picture Perception and Description Test (PPDT) assesses your perception, 
                imagination, and ability to construct a meaningful story from an ambiguous picture.
                
                The test evaluates:
                - Power of perception
                - Ability to interpret situations
                - Narration skills
                - Group discussion capabilities
                
                Process:
                1. Picture shown for 30 seconds
                2. Write a story in 4 minutes
                3. Group discussion on stories
                4. Final narration
            """.trimIndent()
            "PIQ_FORM" -> """
                Personal Information Questionnaire (PIQ) is a comprehensive form that captures 
                your personal details, educational background, family information, and interests.
                
                Key sections:
                - Personal details and contact information
                - Educational qualifications
                - Family background
                - Hobbies and interests
                - Sports and extra-curricular activities
                
                Tips:
                - Fill honestly and accurately
                - Be consistent with your responses
                - Prepare to explain any gaps
                - Know your PIQ thoroughly for interview
            """.trimIndent()
            "PSYCHOLOGY" -> """
                Psychology Tests assess your personality traits, mental makeup, and suitability 
                for a career in the Armed Forces. These tests reveal your true self through 
                projective techniques.
                
                Tests included:
                - TAT (Thematic Apperception Test)
                - WAT (Word Association Test)
                - SRT (Situation Reaction Test)
                - SD (Self Description)
                
                What they assess:
                - Officer Like Qualities (15 OLQs)
                - Personality traits
                - Response patterns under stress
                - Leadership potential
            """.trimIndent()
            "GTO" -> """
                Group Testing Officer (GTO) tasks evaluate your performance in group settings 
                and assess practical implementation of leadership qualities.
                
                Tasks include:
                - Group Discussion
                - Group Planning Exercise
                - Progressive Group Task (PGT)
                - Half Group Task (HGT)
                - Command Task
                - Final Group Task (FGT)
                - Lecturette
                
                Duration: 3 days (Day 3-5)
                Focus: Teamwork, leadership, problem-solving
            """.trimIndent()
            "INTERVIEW" -> """
                Personal Interview is conducted by the Interviewing Officer (IO) to assess 
                your personality, motivation, and suitability for commissioned service.
                
                Interview covers:
                - Personal background
                - Educational details
                - Current affairs and general knowledge
                - Motivation for joining Armed Forces
                - Career goals and aspirations
                
                Tips:
                - Be honest and confident
                - Know your PIQ thoroughly
                - Stay updated with current affairs
                - Express genuine interest
                - Maintain eye contact
            """.trimIndent()
            "CONFERENCE" -> """
                **The Conference: Final Stage of SSB Selection**
                
                The Conference is the culminating event of the 5-day SSB interview process, held on Day 5. This is where all assessors come together to make the final decision about your recommendation for commissioning into the Indian Armed Forces.
                
                **Who Conducts the Conference?**
                
                - President of the Board (presiding officer)
                - Interviewing Officer (IO)
                - Group Testing Officer (GTO)
                - Psychologist
                - Deputy President (if present)
                
                **Conference Procedure:**
                
                **1. Individual Appearance**
                
                Candidates are called one by one into the conference room, arranged in a U-shaped seating with all board members present.
                
                **2. Questions You May Face**
                
                - "How was your overall SSB experience?"
                - "What did you learn during these 5 days?"
                - "Any suggestions to improve the selection process?"
                - "Clarifications on inconsistencies in your responses"
                - "Why do you want to join the Armed Forces?" (reconfirmation)
                - Questions about your PIQ or interview responses
                
                **3. Assessment Discussion**
                
                After you leave, the board discusses:
                
                - Your Officer Like Qualities (all 15 OLQs)
                - Consistency across Psychology, GTO, and Interview
                - Overall personality assessment
                - Suitability for armed forces career
                - Any red flags or outstanding qualities
                
                **4. Final Decision**
                
                The board reaches a consensus through:
                
                - Collective evaluation by all assessors
                - Cross-verification of observations
                - Holistic view of your 5-day performance
                - Voting or unanimous decision (varies by center)
                
                **5. Result Declaration**
                
                After all candidates have appeared:
                
                - Results are compiled and finalized
                - All candidates assemble in the conference room
                - President announces chest numbers of recommended candidates
                - Detailed discussion may follow for recommended candidates
                
                **What They Assess:**
                
                - **Leadership Qualities**: Demonstrated through GTO tasks
                - **Communication**: Clarity in interview and group discussions
                - **Consistency**: Between written tests and practical tasks
                - **Motivation**: Genuine interest in armed forces career
                - **Personality Traits**: Stability, maturity, composure
                - **Physical Fitness**: Performance in outdoor tasks
                - **Social Skills**: Teamwork and cooperation
                
                **Key Points to Remember:**
                
                - **Be Honest**: Provide genuine feedback about the process
                - **Stay Composed**: Maintain confidence and professionalism
                - **Be Brief**: Answer questions concisely and clearly
                - **Show Gratitude**: Thank the board for the opportunity
                - **Dress Formally**: Wear proper formal attire (shirt, trousers, tie)
                - **Maintain Eye Contact**: Look at all board members when speaking
                - **Be Yourself**: Don't try to impress; be natural
                
                **Common Questions to Prepare:**
                
                - Highlights and low points of your SSB experience
                - What you learned about yourself during these 5 days
                - Constructive suggestions for improvement
                - Reconfirmation of your motivations and goals
                - How you handled challenging tasks
                
                **Timeline:**
                
                - Morning: Individual conferences begin (30-45 minutes per candidate)
                - Afternoon: Board deliberation (candidates wait outside)
                - Late Afternoon/Evening: Results declaration (typically 4-5 PM)
                
                **Important Notes:**
                
                - The conference is NOT a test; it's a final assessment review
                - Your fate is mostly decided before the conference
                - It's an opportunity to clarify any ambiguities
                - Stay positive regardless of your self-assessment
                - Some centers may have different procedures
                
                **After Results:**
                
                - **If Recommended**: Congratulations! You proceed to medical examination
                - **If Not Recommended**: Request feedback (some centers provide it)
                - You can reappear multiple times; many officers succeeded after 2-3 attempts
                - Learn from the experience and improve for next attempt
                
                **Success Rate:**
                
                Approximately 3-5% of screened-in candidates get recommended. The conference ensures only the most suitable candidates proceed to commissioning.
            """.trimIndent()
            "MEDICALS" -> """
                Medical Examination ensures you meet the physical and medical standards 
                required for commissioned service in the Armed Forces.
                
                Examinations include:
                - Physical fitness tests
                - Eye test (6/6 vision without glasses for some entries)
                - Blood and urine tests
                - X-ray and ECG
                - Dental examination
                - ENT examination
                
                Common grounds for rejection:
                - Vision problems
                - Hearing defects
                - Orthopedic issues
                - Chronic diseases
                
                Tip: Maintain good health throughout preparation
            """.trimIndent()
            "SSB_OVERVIEW" -> """
                The Services Selection Board (SSB) is a 5-day comprehensive assessment 
                process to select suitable candidates for commissioned service in the 
                Indian Armed Forces.
                
                5-Day breakdown:
                - Day 1: Screening (OIR & PPDT)
                - Day 2: Psychology Tests
                - Day 3-5: GTO Tasks & Interview
                - Day 5: Conference & Results
                
                What SSB assesses:
                - 15 Officer Like Qualities (OLQs)
                - Leadership potential
                - Personality traits
                - Physical fitness
                - Mental alertness
                
                Success rate: Approximately 3-5% of screened candidates
            """.trimIndent()
            else -> "Detailed information about this topic will be available soon."
        }
    }
    
    private fun getStudyMaterials(testType: String): List<StudyMaterialItem> {
        return StudyMaterialsProvider.getStudyMaterials(testType)
    }
}

/**
 * Topic information model
 */
data class TopicInfo(
    val title: String,
    val introduction: String,
    val studyMaterials: List<StudyMaterialItem>,
    val tests: List<TestType>
)

