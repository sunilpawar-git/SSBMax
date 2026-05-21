package com.ssbmax.core.data.repository

import com.ssbmax.core.domain.model.TestResponse
import io.mockk.mockk
import org.junit.Assert.*
import org.junit.Test

class TestResponseMapperTest {

    @Test
    fun testMultipleChoiceRoundtrip() {
        val original = TestResponse.MultipleChoice(
            questionId = "q_mc_1",
            timestamp = 123456789L,
            selectedOption = 3,
            isCorrect = true
        )
        val repository = TestSubmissionRepositoryImpl(mockk(relaxed = true))
        with(repository) {
            val map = original.toMap()
            assertEquals("MultipleChoice", map["type"])
            assertEquals("q_mc_1", map["questionId"])
            assertEquals(123456789L, map["timestamp"])
            assertEquals(3, map["selectedOption"])
            assertEquals(true, map["isCorrect"])

            val restored = map.toTestResponse()
            assertNotNull(restored)
            assertTrue(restored is TestResponse.MultipleChoice)
            val mcRestored = restored as TestResponse.MultipleChoice
            assertEquals(original.questionId, mcRestored.questionId)
            assertEquals(original.timestamp, mcRestored.timestamp)
            assertEquals(original.selectedOption, mcRestored.selectedOption)
            assertEquals(original.isCorrect, mcRestored.isCorrect)
        }
    }

    @Test
    fun testTextResponseRoundtrip() {
        val original = TestResponse.TextResponse(
            questionId = "q_txt_1",
            timestamp = 123456789L,
            answer = "This is an authentic psychological test response for TAT."
        )
        val repository = TestSubmissionRepositoryImpl(mockk(relaxed = true))
        with(repository) {
            val map = original.toMap()
            assertEquals("TextResponse", map["type"])
            assertEquals("q_txt_1", map["questionId"])
            assertEquals(123456789L, map["timestamp"])
            assertEquals("This is an authentic psychological test response for TAT.", map["answer"])
            assertEquals(9, map["wordCount"])

            val restored = map.toTestResponse()
            assertNotNull(restored)
            assertTrue(restored is TestResponse.TextResponse)
            val textRestored = restored as TestResponse.TextResponse
            assertEquals(original.questionId, textRestored.questionId)
            assertEquals(original.timestamp, textRestored.timestamp)
            assertEquals(original.answer, textRestored.answer)
            assertEquals(original.wordCount, textRestored.wordCount)
        }
    }

    @Test
    fun testImageBasedResponseRoundtrip() {
        val original = TestResponse.ImageBasedResponse(
            questionId = "q_img_1",
            timestamp = 123456789L,
            imageUrl = "https://ssbmax.com/images/tat1.jpg",
            description = "A candidate looking at the starry sky with determination."
        )
        val repository = TestSubmissionRepositoryImpl(mockk(relaxed = true))
        with(repository) {
            val map = original.toMap()
            assertEquals("ImageBasedResponse", map["type"])
            assertEquals("q_img_1", map["questionId"])
            assertEquals(123456789L, map["timestamp"])
            assertEquals("https://ssbmax.com/images/tat1.jpg", map["imageUrl"])
            assertEquals("A candidate looking at the starry sky with determination.", map["description"])

            val restored = map.toTestResponse()
            assertNotNull(restored)
            assertTrue(restored is TestResponse.ImageBasedResponse)
            val imgRestored = restored as TestResponse.ImageBasedResponse
            assertEquals(original.questionId, imgRestored.questionId)
            assertEquals(original.timestamp, imgRestored.timestamp)
            assertEquals(original.imageUrl, imgRestored.imageUrl)
            assertEquals(original.description, imgRestored.description)
        }
    }

    @Test
    fun testRatingResponseRoundtrip() {
        val original = TestResponse.RatingResponse(
            questionId = "q_rate_1",
            timestamp = 123456789L,
            rating = 5,
            comment = "Excellent experience."
        )
        val repository = TestSubmissionRepositoryImpl(mockk(relaxed = true))
        with(repository) {
            val map = original.toMap()
            assertEquals("RatingResponse", map["type"])
            assertEquals("q_rate_1", map["questionId"])
            assertEquals(123456789L, map["timestamp"])
            assertEquals(5, map["rating"])
            assertEquals("Excellent experience.", map["comment"])

            val restored = map.toTestResponse()
            assertNotNull(restored)
            assertTrue(restored is TestResponse.RatingResponse)
            val rateRestored = restored as TestResponse.RatingResponse
            assertEquals(original.questionId, rateRestored.questionId)
            assertEquals(original.timestamp, rateRestored.timestamp)
            assertEquals(original.rating, rateRestored.rating)
            assertEquals(original.comment, rateRestored.comment)
        }
    }
}
