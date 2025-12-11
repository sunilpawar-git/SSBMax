package com.ssbmax.core.domain.model.interview

import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

/**
 * Unit tests for InterviewQuestion model
 *
 * Tests:
 * - Question validation
 * - OLQ mapping
 * - Question sources
 * - Context information
 */
class InterviewQuestionTest {

    @Test
    fun `InterviewQuestion should have valid ID`() {
        val question = InterviewQuestion(
            id = UUID.randomUUID().toString(),
            questionText = "Tell me about yourself.",
            expectedOLQs = listOf(OLQ.SELF_CONFIDENCE, OLQ.POWER_OF_EXPRESSION),
            context = "Opening question",
            source = QuestionSource.GENERIC_POOL
        )

        assertNotNull(question.id)
        assertTrue(question.id.isNotEmpty())
    }

    @Test
    fun `InterviewQuestion should have non-empty question text`() {
        val question = InterviewQuestion(
            id = UUID.randomUUID().toString(),
            questionText = "Why do you want to join the armed forces?",
            expectedOLQs = listOf(OLQ.DETERMINATION),
            context = null,
            source = QuestionSource.PIQ_BASED
        )

        assertNotNull(question.questionText)
        assertTrue(question.questionText.isNotEmpty())
    }

    @Test
    fun `InterviewQuestion should have at least one expected OLQ`() {
        val question = InterviewQuestion(
            id = UUID.randomUUID().toString(),
            questionText = "Describe a challenging situation.",
            expectedOLQs = listOf(OLQ.REASONING_ABILITY, OLQ.SPEED_OF_DECISION),
            context = "Problem-solving assessment",
            source = QuestionSource.AI_GENERATED
        )

        assertTrue(question.expectedOLQs.isNotEmpty())
        assertEquals(2, question.expectedOLQs.size)
    }

    @Test
    fun `InterviewQuestion should support multiple OLQs`() {
        val question = InterviewQuestion(
            id = UUID.randomUUID().toString(),
            questionText = "How do you work in a team?",
            expectedOLQs = listOf(
                OLQ.COOPERATION,
                OLQ.SOCIAL_ADJUSTMENT,
                OLQ.INFLUENCE_GROUP
            ),
            context = "Teamwork assessment",
            source = QuestionSource.GENERIC_POOL
        )

        assertEquals(3, question.expectedOLQs.size)
        assertTrue(question.expectedOLQs.contains(OLQ.COOPERATION))
        assertTrue(question.expectedOLQs.contains(OLQ.SOCIAL_ADJUSTMENT))
        assertTrue(question.expectedOLQs.contains(OLQ.INFLUENCE_GROUP))
    }

    @Test
    fun `InterviewQuestion context can be null`() {
        val question = InterviewQuestion(
            id = UUID.randomUUID().toString(),
            questionText = "What are your hobbies?",
            expectedOLQs = listOf(OLQ.LIVELINESS),
            context = null,
            source = QuestionSource.GENERIC_POOL
        )

        assertNull(question.context)
    }

    @Test
    fun `InterviewQuestion should support different sources`() {
        val piqQuestion = InterviewQuestion(
            id = UUID.randomUUID().toString(),
            questionText = "Tell me about your NCC training.",
            expectedOLQs = listOf(OLQ.DETERMINATION),
            context = "Based on candidate's PIQ",
            source = QuestionSource.PIQ_BASED
        )

        val genericQuestion = InterviewQuestion(
            id = UUID.randomUUID().toString(),
            questionText = "What are your strengths?",
            expectedOLQs = listOf(OLQ.SELF_CONFIDENCE),
            context = "Standard question",
            source = QuestionSource.GENERIC_POOL
        )

        val aiQuestion = InterviewQuestion(
            id = UUID.randomUUID().toString(),
            questionText = "How would you handle disagreement in your team?",
            expectedOLQs = listOf(OLQ.COOPERATION),
            context = "AI-generated follow-up",
            source = QuestionSource.AI_GENERATED
        )

        assertEquals(QuestionSource.PIQ_BASED, piqQuestion.source)
        assertEquals(QuestionSource.GENERIC_POOL, genericQuestion.source)
        assertEquals(QuestionSource.AI_GENERATED, aiQuestion.source)
    }

    @Test
    fun `QuestionSource should have all three types`() {
        val sources = QuestionSource.entries
        assertEquals(3, sources.size)
        assertTrue(sources.contains(QuestionSource.PIQ_BASED))
        assertTrue(sources.contains(QuestionSource.GENERIC_POOL))
        assertTrue(sources.contains(QuestionSource.AI_GENERATED))
    }

    @Test
    fun `InterviewQuestion should map to correct OLQ categories`() {
        val question = InterviewQuestion(
            id = UUID.randomUUID().toString(),
            questionText = "Describe a time you demonstrated leadership.",
            expectedOLQs = listOf(
                OLQ.ORGANIZING_ABILITY,      // Intellectual
                OLQ.INITIATIVE,               // Dynamic
                OLQ.EFFECTIVE_INTELLIGENCE    // Intellectual
            ),
            context = "Leadership assessment",
            source = QuestionSource.GENERIC_POOL
        )

        val categories = question.expectedOLQs.map { it.category }.distinct()
        assertTrue(categories.contains(OLQCategory.INTELLECTUAL))
        assertTrue(categories.contains(OLQCategory.DYNAMIC))
    }

    @Test
    fun `InterviewQuestion should support single OLQ`() {
        val question = InterviewQuestion(
            id = UUID.randomUUID().toString(),
            questionText = "What motivates you?",
            expectedOLQs = listOf(OLQ.DETERMINATION),
            context = "Motivation assessment",
            source = QuestionSource.PIQ_BASED
        )

        assertEquals(1, question.expectedOLQs.size)
        assertEquals(OLQ.DETERMINATION, question.expectedOLQs.first())
    }

    @Test
    fun `InterviewQuestion should have unique IDs`() {
        val question1 = InterviewQuestion(
            id = UUID.randomUUID().toString(),
            questionText = "Question 1",
            expectedOLQs = listOf(OLQ.SELF_CONFIDENCE),
            context = null,
            source = QuestionSource.GENERIC_POOL
        )

        val question2 = InterviewQuestion(
            id = UUID.randomUUID().toString(),
            questionText = "Question 2",
            expectedOLQs = listOf(OLQ.COURAGE),
            context = null,
            source = QuestionSource.GENERIC_POOL
        )

        assertNotEquals(question1.id, question2.id)
    }

    @Test
    fun `InterviewQuestion should be data class with copy`() {
        val original = InterviewQuestion(
            id = UUID.randomUUID().toString(),
            questionText = "Original question",
            expectedOLQs = listOf(OLQ.SELF_CONFIDENCE),
            context = "Original context",
            source = QuestionSource.GENERIC_POOL
        )

        val modified = original.copy(questionText = "Modified question")

        assertEquals(original.id, modified.id)
        assertEquals("Modified question", modified.questionText)
        assertEquals(original.expectedOLQs, modified.expectedOLQs)
        assertEquals(original.context, modified.context)
        assertEquals(original.source, modified.source)
    }

    @Test
    fun `InterviewQuestion should support all OLQ categories`() {
        // Intellectual question
        val intellectualQ = InterviewQuestion(
            id = UUID.randomUUID().toString(),
            questionText = "How do you organize complex tasks?",
            expectedOLQs = listOf(OLQ.ORGANIZING_ABILITY, OLQ.REASONING_ABILITY),
            context = null,
            source = QuestionSource.GENERIC_POOL
        )

        // Social question
        val socialQ = InterviewQuestion(
            id = UUID.randomUUID().toString(),
            questionText = "How do you build relationships?",
            expectedOLQs = listOf(OLQ.SOCIAL_ADJUSTMENT, OLQ.COOPERATION),
            context = null,
            source = QuestionSource.GENERIC_POOL
        )

        // Dynamic question
        val dynamicQ = InterviewQuestion(
            id = UUID.randomUUID().toString(),
            questionText = "Describe a time you took initiative.",
            expectedOLQs = listOf(OLQ.INITIATIVE, OLQ.SELF_CONFIDENCE),
            context = null,
            source = QuestionSource.GENERIC_POOL
        )

        // Character & Physical question
        val characterQ = InterviewQuestion(
            id = UUID.randomUUID().toString(),
            questionText = "How do you handle physical challenges?",
            expectedOLQs = listOf(OLQ.STAMINA, OLQ.COURAGE),
            context = null,
            source = QuestionSource.GENERIC_POOL
        )

        // Verify each maps to correct categories
        assertTrue(intellectualQ.expectedOLQs.all { it.category == OLQCategory.INTELLECTUAL })
        assertTrue(socialQ.expectedOLQs.all { it.category == OLQCategory.SOCIAL })
        assertTrue(dynamicQ.expectedOLQs.all { it.category == OLQCategory.DYNAMIC })
        assertTrue(characterQ.expectedOLQs.all { it.category == OLQCategory.CHARACTER })
    }
}
