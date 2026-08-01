package com.ssbmax.workers

import com.ssbmax.shared.domain.model.TATQuestion
import com.ssbmax.shared.domain.model.TATStoryResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TATAnalysisWorkPlannerTest {

    private val planner = TATAnalysisWorkPlanner()

    @Test
    fun `creates bounded batches for 12 stories`() {
        val plan = planner.plan("sub-001", buildStories(12), buildQuestions(12))

        assertTrue(
            "Every batch must be at most ${TATAnalysisWorkPlanner.BATCH_SIZE} wide so Gemini is not hit with all calls at once",
            plan.storyBatches.all { it.size <= TATAnalysisWorkPlanner.BATCH_SIZE }
        )
        assertEquals(4, plan.storyBatches.size)
    }

    @Test
    fun `includes every story exactly once`() {
        val plan = planner.plan("sub-002", buildStories(12), buildQuestions(12))

        val totalRequests = plan.storyBatches.sumOf { it.size }
        assertEquals(12, totalRequests)
    }

    @Test
    fun `includes blank card in planned work`() {
        // Card 12 is the programmatic blank slide — it still produces a real TATStoryResponse.
        val stories = buildStories(12)
        val plan = planner.plan("sub-003", stories, buildQuestions(12))

        val blankCardStoryId = stories.last().questionId
        val ids = plan.storyBatches.flatten().map { it.workSpec.input.getString(TATStoryAnalysisWorker.KEY_QUESTION_ID) }
        assertTrue(ids.contains(blankCardStoryId))
    }

    @Test
    fun `creates synthesis dependency after all batches`() {
        val plan = planner.plan("sub-004", buildStories(12), buildQuestions(12))

        assertEquals(
            "sub-004",
            plan.synthesisRequest.workSpec.input.getString(TATSynthesisWorker.KEY_SUBMISSION_ID)
        )
    }

    @Test
    fun `preserves questionId and storyIndex correctly`() {
        val stories = buildStories(12)
        val plan = planner.plan("sub-005", stories, buildQuestions(12))

        plan.storyBatches.flatten().forEachIndexed { globalIndex, request ->
            assertEquals(stories[globalIndex].questionId, request.workSpec.input.getString(TATStoryAnalysisWorker.KEY_QUESTION_ID))
            assertEquals(globalIndex, request.workSpec.input.getInt(TATStoryAnalysisWorker.KEY_STORY_INDEX, -1))
        }
    }

    private fun buildStories(count: Int): List<TATStoryResponse> = (1..count).map { i ->
        TATStoryResponse(
            questionId = "tat_q_$i",
            story = "Story $i text here.",
            charactersCount = 200,
            viewingTimeTakenSeconds = 15,
            writingTimeTakenSeconds = 120,
            submittedAt = 1_717_171_000L + i
        )
    }

    private fun buildQuestions(count: Int): List<TATQuestion> = (1..count).map { i ->
        TATQuestion(
            id = "tat_q_$i",
            imageUrl = "https://example.com/tat_$i.jpg",
            cardPosition = i,
            viewingTimeSeconds = 30,
            writingTimeMinutes = 4,
            minCharacters = 150,
            maxCharacters = 1500
        )
    }
}
