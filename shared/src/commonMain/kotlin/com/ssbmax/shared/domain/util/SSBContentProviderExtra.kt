package com.ssbmax.shared.domain.util

import com.ssbmax.shared.domain.model.SSBInfoCard
import com.ssbmax.shared.domain.model.SSBInfoIcon

/**
 * The last two [SSBInfoCard]s of [SSBContentProvider]'s content, split into
 * their own file purely to keep both files under the repo's 300-line
 * Quality Limit -- no behavior change from having them inline.
 */
internal fun successStoriesCard() = SSBInfoCard(
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
)

internal fun importantPointsCard() = SSBInfoCard(
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
