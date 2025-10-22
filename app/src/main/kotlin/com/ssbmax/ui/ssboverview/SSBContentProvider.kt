package com.ssbmax.ui.ssboverview

import com.ssbmax.core.domain.model.SSBInfoCard
import com.ssbmax.core.domain.model.SSBInfoIcon

/**
 * Mock data provider for SSB overview content
 * This will be replaced with real repository data in production
 */
object SSBContentProvider {

    fun getInfoCards(): List<SSBInfoCard> {
        return listOf(
            SSBInfoCard(
                id = "what_is_ssb",
                title = "What is SSB?",
                content = """
                    The Services Selection Board (SSB) is a 5-day comprehensive assessment process conducted by the Indian Armed Forces to select suitable candidates for commissioned service in the Army, Navy, and Air Force.
                    
                    SSB evaluates your personality, intelligence, and suitability for becoming an officer through various tests and tasks designed by expert psychologists and military personnel.
                    
                    Key Points:
                    - Duration: 5 days
                    - Conducted at multiple SSB centers across India
                    - Assesses 15 Officer Like Qualities (OLQs)
                    - Success rate: Approximately 3-5%
                    - Final recommendation requires consensus from all assessors
                    
                    The board consists of:
                    - Interviewing Officer (IO)
                    - Group Testing Officer (GTO)
                    - Psychologist
                    - President of the board
                """.trimIndent(),
                icon = SSBInfoIcon.INFORMATION,
                order = 1
            ),
            SSBInfoCard(
                id = "selection_process",
                title = "5-Day Selection Process",
                content = """
                    **Day 1: Screening Tests**
                    - Officer Intelligence Rating (OIR) Test - 40-50 questions in 30 minutes
                    - Picture Perception & Description Test (PPDT)
                    - Group Discussion on stories
                    - Only screened-in candidates proceed to Day 2
                    
                    **Day 2: Psychology Tests**
                    - Thematic Apperception Test (TAT) - 12 pictures, 30 seconds each
                    - Word Association Test (WAT) - 60 words, 15 seconds each
                    - Situation Reaction Test (SRT) - 60 situations
                    - Self Description Test (SD) - Write about yourself
                    
                    **Day 3-5: GTO Tasks & Interview**
                    - Group Discussion (GD)
                    - Group Planning Exercise (GPE)
                    - Progressive Group Task (PGT)
                    - Half Group Task (HGT)
                    - Individual Obstacles
                    - Command Task
                    - Final Group Task (FGT)
                    - Lecturette
                    - Personal Interview with IO (30-45 minutes)
                    
                    **Day 5: Conference**
                    - All assessors discuss your performance
                    - Final recommendation decided
                    - Results declared same day
                """.trimIndent(),
                icon = SSBInfoIcon.PROCESS,
                order = 2
            ),
            SSBInfoCard(
                id = "officer_qualities",
                title = "15 Officer Like Qualities (OLQs)",
                content = """
                    SSB assesses candidates on 15 fundamental qualities required in an officer:
                    
                    **1. Effective Intelligence**
                    Ability to make quick, practical decisions and solve problems efficiently.
                    
                    **2. Reasoning Ability**
                    Logical thinking and ability to draw correct conclusions.
                    
                    **3. Organizing Ability**
                    Planning and managing resources effectively.
                    
                    **4. Power of Expression**
                    Clear communication, both verbal and written.
                    
                    **5. Social Adjustment**
                    Ability to work well in a team and adapt to different situations.
                    
                    **6. Cooperation**
                    Working harmoniously with others towards common goals.
                    
                    **7. Sense of Responsibility**
                    Taking ownership of tasks and being accountable.
                    
                    **8. Initiative**
                    Taking action without being prompted.
                    
                    **9. Self Confidence**
                    Belief in one's abilities without arrogance.
                    
                    **10. Speed of Decision**
                    Making timely decisions under pressure.
                    
                    **11. Ability to Influence the Group**
                    Natural leadership and persuasion skills.
                    
                    **12. Liveliness**
                    Enthusiasm and positive energy.
                    
                    **13. Determination**
                    Perseverance in the face of obstacles.
                    
                    **14. Courage**
                    Physical and moral bravery.
                    
                    **15. Stamina**
                    Physical and mental endurance.
                    
                    These qualities are assessed throughout all SSB tests and tasks.
                """.trimIndent(),
                icon = SSBInfoIcon.QUALITIES,
                order = 3
            ),
            SSBInfoCard(
                id = "preparation_tips",
                title = "Preparation Tips",
                content = """
                    **Know Yourself**
                    - Fill your Personal Information Questionnaire (PIQ) honestly
                    - Be prepared to explain everything you've written
                    - Practice self-awareness exercises
                    
                    **Current Affairs & General Knowledge**
                    - Read newspapers daily (The Hindu, Indian Express)
                    - Focus on national and international events
                    - Study Indian Armed Forces history and recent developments
                    - Know about your state, city, and educational background
                    
                    **Physical Fitness**
                    - Start running daily (build stamina)
                    - Practice pull-ups, push-ups, sit-ups
                    - Work on flexibility and agility
                    - Outdoor tasks require good fitness
                    
                    **Communication Skills**
                    - Practice speaking English fluently
                    - Improve vocabulary for WAT
                    - Work on body language and confidence
                    - Practice group discussions
                    
                    **Practice Tests**
                    - Solve OIR practice sets regularly
                    - Practice TAT story writing (30 seconds thinking)
                    - Do WAT with timer (15 seconds per word)
                    - Write responses to SRT situations
                    
                    **Mental Preparation**
                    - Stay calm and composed
                    - Be yourself, don't fake personality
                    - Think positively and stay motivated
                    - Don't get discouraged by Day 1 screening
                    
                    **Common Mistakes to Avoid**
                    - Memorizing answers or stories
                    - Lying or exaggerating in PIQ or interview
                    - Being overconfident or under-confident
                    - Trying to act like a "model officer"
                    - Ignoring physical fitness
                """.trimIndent(),
                icon = SSBInfoIcon.PREPARATION,
                order = 4
            ),
            SSBInfoCard(
                id = "day_wise_schedule",
                title = "Typical Day-wise Schedule",
                content = """
                    **Reporting Day (Day 0)**
                    - Report to SSB center by afternoon
                    - Document verification
                    - Chest number allotted
                    - Briefing about the process
                    
                    **Day 1: Screening**
                    - 0800 hrs: Reporting for OIR Test
                    - 0830-0930 hrs: OIR Test (50 questions, 30 min)
                    - 1000 hrs: PPDT (Picture shown for 30 sec)
                    - 1005-1009 hrs: Story writing (4 min)
                    - 1030 hrs onwards: Group discussions
                    - 1400 hrs: Results declared (screened in/out)
                    
                    **Day 2: Psychology Tests**
                    - 0900-1000 hrs: TAT (12 pictures)
                    - 1015-1030 hrs: WAT (60 words)
                    - 1045-1145 hrs: SRT (60 situations)
                    - 1200-1230 hrs: Self Description
                    
                    **Day 3: GTO Tasks - Part 1**
                    - 0900 hrs: Group Discussion
                    - 1000 hrs: Group Planning Exercise
                    - 1130 hrs: Progressive Group Task (PGT)
                    - 1500 hrs: Lecturette preparation
                    
                    **Day 4: GTO Tasks - Part 2**
                    - 0900 hrs: Half Group Task (HGT)
                    - 1100 hrs: Individual Obstacles
                    - 1400 hrs: Command Tasks
                    - 1600 hrs: Final Group Task (FGT)
                    
                    **Day 5: Interview & Conference**
                    - 0900-1400 hrs: Personal interviews (scheduled)
                    - 1500 hrs: Conference (candidates not present)
                    - 1700 hrs: Results declared
                    
                    Note: Timings may vary slightly by SSB center
                """.trimIndent(),
                icon = SSBInfoIcon.CALENDAR,
                order = 5
            ),
            SSBInfoCard(
                id = "success_stories",
                title = "Success Stories & Tips",
                content = """
                    **From Recommended Candidates:**
                    
                    "Be yourself. The assessors are trained to see through fake behavior. I was rejected twice when I tried to act, but recommended when I was just myself." - Lt. Rajesh Kumar, NDA Entry
                    
                    "Physical fitness saved me in GTO tasks. Even though I wasn't the strongest, my stamina helped me complete all obstacles." - Capt. Priya Sharma, CDS Entry
                    
                    "I read newspapers daily for 6 months before SSB. In my interview, 70% questions were from current affairs I had studied." - Lt. Amit Singh, AFCAT Entry
                    
                    "Don't be afraid to fail. I was screened out twice before being recommended. Each attempt taught me something new." - Capt. Neha Gupta, TGC Entry
                    
                    **Key Success Factors:**
                    
                    - Honesty and authenticity throughout the process
                    - Good command over current affairs and general knowledge
                    - Physical fitness and mental stamina
                    - Clear communication skills
                    - Positive attitude and team spirit
                    - Quick decision-making ability
                    - Thorough knowledge of your PIQ
                    
                    **Average Selection Statistics:**
                    - Candidates appearing: 100%
                    - Screened in: 40-50%
                    - Recommended: 3-5% of total
                    - Female candidates: Similar success rate
                    - Multiple attempts: Common (don't give up!)
                    
                    **Remember:**
                    SSB is not about perfection, it's about showing your genuine personality and potential to be an officer. Many successful officers were recommended after 2-3 attempts.
                """.trimIndent(),
                icon = SSBInfoIcon.SUCCESS,
                order = 6
            ),
            SSBInfoCard(
                id = "important_points",
                title = "Important Points to Remember",
                content = """
                    **Documents Required:**
                    - Valid ID proof (Aadhaar, PAN, or Passport)
                    - Educational certificates and mark sheets
                    - Recent passport-size photographs
                    - Call letter (printed copy)
                    - Medical fitness certificate (if required)
                    
                    **What to Carry:**
                    - Formal clothes for interview (shirt, trousers, tie)
                    - Sports clothes and shoes for GTO tasks
                    - Toiletries and personal items
                    - Watch (for time management)
                    - Stationery (pen, pencil, eraser)
                    - Any prescribed medicines
                    
                    **SSB Centers in India:**
                    - Allahabad (UP) - ASB
                    - Bangalore (Karnataka) - ASB, NSB, AFSB
                    - Bhopal (MP) - ASB
                    - Dehradun (Uttarakhand) - NSB
                    - Kapurthala (Punjab) - ASB
                    - Varanasi (UP) - AFSB
                    
                    **After SSB:**
                    - Medical examination at designated centers
                    - Merit list preparation
                    - Final selection and training allocation
                    - Joining instructions sent to recommended candidates
                    
                    **Contact Information:**
                    - Helpline: 1800-XXX-XXXX
                    - Email: support@ssbmax.com
                    - Website: www.joinindianarmy.nic.in / www.indiannavy.nic.in / www.careerindianairforce.cdac.in
                    
                    **Stay Updated:**
                    Follow official portals and social media handles of the Indian Armed Forces for latest updates on recruitment, SSB dates, and policy changes.
                """.trimIndent(),
                icon = SSBInfoIcon.BOOK,
                order = 7
            )
        )
    }
}

