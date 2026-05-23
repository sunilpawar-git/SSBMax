package com.ssbmax.ui.instructor

import androidx.lifecycle.SavedStateHandle
import com.ssbmax.core.domain.model.Batch
import com.ssbmax.core.domain.model.StudentPerformance
import com.ssbmax.core.domain.repository.BatchRepository
import com.ssbmax.testing.BaseViewModelTest
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BatchDetailViewModelTest : BaseViewModelTest() {

    private lateinit var batchRepository: BatchRepository
    private lateinit var savedStateHandle: SavedStateHandle

    @Before
    fun setup() {
        batchRepository = mockk()
        savedStateHandle = SavedStateHandle(mapOf("batchId" to "batch_123"))
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `init loads batch details and enrolled students successfully`() = runTest {
        val testBatch = Batch(
            id = "batch_123",
            name = "NDA Special Batch",
            description = "Special coaching",
            instructorId = "inst_999",
            inviteCode = "NDA999"
        )
        val students = listOf(
            StudentPerformance(
                studentId = "stud_1",
                studentName = "Rahul Kumar",
                averageScore = 85f,
                testsCompleted = 12,
                lastActiveAt = System.currentTimeMillis(),
                currentStreak = 5
            )
        )

        coEvery { batchRepository.getBatch("batch_123") } returns flowOf(Result.success(testBatch))
        coEvery { batchRepository.getStudentsInBatch("batch_123") } returns flowOf(Result.success(students))

        val viewModel = BatchDetailViewModel(batchRepository, savedStateHandle)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(testBatch, state.batch)
        assertEquals(students, state.students)
        assertNull(state.error)
    }

    @Test
    fun `init sets error state when batch retrieval fails`() = runTest {
        coEvery { batchRepository.getBatch("batch_123") } returns flowOf(Result.failure(Exception("Batch retrieval error")))
        coEvery { batchRepository.getStudentsInBatch("batch_123") } returns flowOf(Result.success(emptyList()))

        val viewModel = BatchDetailViewModel(batchRepository, savedStateHandle)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.batch)
        assertEquals("Batch retrieval error", state.error)
    }
}
