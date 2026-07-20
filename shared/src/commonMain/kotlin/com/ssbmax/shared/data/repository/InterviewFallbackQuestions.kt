package com.ssbmax.shared.data.repository

import com.ssbmax.shared.domain.model.interview.InterviewQuestion
import com.ssbmax.shared.domain.model.interview.OLQ
import com.ssbmax.shared.domain.model.interview.QuestionSource

/**
 * Emergency-fallback interview questions, used by [InterviewQuestionGenerator] only when both the
 * question cache *and* AI generation fail.
 *
 * **Documented deviation from the Android original:** the Android `InterviewQuestionGenerator`
 * reads these from a bundled JSON asset (`app/src/main/assets/fallback_interview_questions.json`,
 * read via `context.assets.open(...)` + Gson). No `GitLive*`/data-layer port in `shared` has
 * established a bundled-resource-reading convention yet (compose-resources is on the classpath
 * but unused elsewhere in this module), and introducing one for 27 static dev-fallback questions
 * would be more machinery than the data warrants. This inlines the same 27 questions as a Kotlin
 * list literal instead — identical content and behavior to the JSON asset, just a different
 * storage format. If a real KMP resource-bundling convention is established later, this can be
 * swapped to read from it without any caller-visible change.
 */
internal val FALLBACK_INTERVIEW_QUESTIONS: List<InterviewQuestion> = listOf(
    fallbackQuestion("Tell me about yourself and your background.", OLQ.SELF_CONFIDENCE, OLQ.POWER_OF_EXPRESSION),
    fallbackQuestion("Why do you want to join the armed forces?", OLQ.DETERMINATION, OLQ.SENSE_OF_RESPONSIBILITY),
    fallbackQuestion(
        "Describe a challenging situation you faced and how you handled it.",
        OLQ.REASONING_ABILITY, OLQ.SPEED_OF_DECISION, OLQ.COURAGE
    ),
    fallbackQuestion("What are your strengths and weaknesses?", OLQ.SELF_CONFIDENCE, OLQ.POWER_OF_EXPRESSION),
    fallbackQuestion(
        "How do you handle working in a team?",
        OLQ.COOPERATION, OLQ.SOCIAL_ADJUSTMENT, OLQ.INFLUENCE_GROUP
    ),
    fallbackQuestion(
        "Describe a time when you demonstrated leadership.",
        OLQ.ORGANIZING_ABILITY, OLQ.INITIATIVE, OLQ.EFFECTIVE_INTELLIGENCE
    ),
    fallbackQuestion("What are your hobbies and interests?", OLQ.LIVELINESS, OLQ.SOCIAL_ADJUSTMENT),
    fallbackQuestion("How do you handle stress and pressure?", OLQ.STAMINA, OLQ.COURAGE, OLQ.SELF_CONFIDENCE),
    fallbackQuestion(
        "What do you know about the role you are applying for?",
        OLQ.EFFECTIVE_INTELLIGENCE, OLQ.SENSE_OF_RESPONSIBILITY
    ),
    fallbackQuestion("Where do you see yourself in 5 years?", OLQ.DETERMINATION, OLQ.ORGANIZING_ABILITY),
    fallbackQuestion(
        "Describe a situation where you had to take initiative without being told.",
        OLQ.INITIATIVE, OLQ.SELF_CONFIDENCE, OLQ.DETERMINATION
    ),
    fallbackQuestion(
        "How would you convince a group of people who disagree with your plan?",
        OLQ.INFLUENCE_GROUP, OLQ.POWER_OF_EXPRESSION, OLQ.REASONING_ABILITY
    ),
    fallbackQuestion(
        "Tell me about a time when you had to make a quick decision with limited information.",
        OLQ.SPEED_OF_DECISION, OLQ.COURAGE, OLQ.EFFECTIVE_INTELLIGENCE
    ),
    fallbackQuestion(
        "How do you organize your day and prioritize tasks?",
        OLQ.ORGANIZING_ABILITY, OLQ.EFFECTIVE_INTELLIGENCE
    ),
    fallbackQuestion(
        "Describe a situation where you had to stand up for what you believed was right.",
        OLQ.COURAGE, OLQ.DETERMINATION, OLQ.SENSE_OF_RESPONSIBILITY
    ),
    fallbackQuestion(
        "How do you maintain your physical fitness?",
        OLQ.STAMINA, OLQ.DETERMINATION, OLQ.LIVELINESS
    ),
    fallbackQuestion(
        "Tell me about a time when you helped someone in need.",
        OLQ.COOPERATION, OLQ.SENSE_OF_RESPONSIBILITY, OLQ.SOCIAL_ADJUSTMENT
    ),
    fallbackQuestion(
        "How do you handle criticism or negative feedback?",
        OLQ.SELF_CONFIDENCE, OLQ.SOCIAL_ADJUSTMENT, OLQ.REASONING_ABILITY
    ),
    fallbackQuestion(
        "Describe a goal you achieved that required sustained effort over a long period.",
        OLQ.DETERMINATION, OLQ.STAMINA, OLQ.ORGANIZING_ABILITY
    ),
    fallbackQuestion(
        "How would you handle a conflict between two team members?",
        OLQ.COOPERATION, OLQ.INFLUENCE_GROUP, OLQ.REASONING_ABILITY
    ),
    fallbackQuestion(
        "What current affairs topic interests you the most and why?",
        OLQ.EFFECTIVE_INTELLIGENCE, OLQ.POWER_OF_EXPRESSION
    ),
    fallbackQuestion(
        "Describe a time when you failed at something. What did you learn?",
        OLQ.SELF_CONFIDENCE, OLQ.REASONING_ABILITY, OLQ.DETERMINATION
    ),
    fallbackQuestion(
        "How do you stay motivated when things get difficult?",
        OLQ.DETERMINATION, OLQ.STAMINA, OLQ.SELF_CONFIDENCE
    ),
    fallbackQuestion(
        "Tell me about your family and their influence on you.",
        OLQ.SOCIAL_ADJUSTMENT, OLQ.SENSE_OF_RESPONSIBILITY
    ),
    fallbackQuestion(
        "How would you handle a situation where you had to lead people older than you?",
        OLQ.INITIATIVE, OLQ.INFLUENCE_GROUP, OLQ.SELF_CONFIDENCE
    ),
    fallbackQuestion(
        "Describe your approach to learning new skills.",
        OLQ.EFFECTIVE_INTELLIGENCE, OLQ.INITIATIVE, OLQ.DETERMINATION
    ),
    fallbackQuestion(
        "What would you do if you witnessed a colleague doing something unethical?",
        OLQ.COURAGE, OLQ.SENSE_OF_RESPONSIBILITY, OLQ.REASONING_ABILITY
    )
)

private fun fallbackQuestion(text: String, vararg olqs: OLQ): InterviewQuestion = InterviewQuestion(
    id = randomId(),
    questionText = text,
    expectedOLQs = olqs.toList(),
    context = null,
    source = QuestionSource.GENERIC_POOL
)
