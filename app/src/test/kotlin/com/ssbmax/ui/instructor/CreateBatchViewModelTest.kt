package com.ssbmax.ui.instructor

import com.ssbmax.core.domain.model.SSBMaxUser
import com.ssbmax.core.domain.model.UserRole
import com.ssbmax.core.domain.repository.BatchRepository
import com.ssbmax.core.domain.usecase.auth.ObserveCurrentUserUseCase
import com.ssbmax.testing.BaseViewModelTest
import io.mockk.coEvery
import io.mockk.coVerify
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
class CreateBatchViewModelTest : BaseViewModelTest() {

    private lateinit var batchRepository: BatchRepository
    private lateinit var observeCurrentUser: ObserveCurrentUserUseCase
    private lateinit var viewModel: CreateBatchViewModel

    @Before
    fun setup() {
        batchRepository = mockk()
        observeCurrentUser = mockk()
        viewModel = CreateBatchViewModel(batchRepository, observeCurrentUser)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `inputs update UI state correctly`() {
        viewModel.onNameChanged("NDA Batch")
        viewModel.onDescriptionChanged("Detailed training")
        viewModel.onMaxStudentsChanged("30")

        val state = viewModel.uiState.value
        assertEquals("NDA Batch", state.name)
        assertEquals("Detailed training", state.description)
        assertEquals("30", state.maxStudents)
    }

    @Test
    fun `createBatch sets error when name is empty`() = runTest {
        viewModel.createBatch()

        val state = viewModel.uiState.value
        assertEquals("Batch name cannot be empty", state.error)
    }

    @Test
    fun `createBatch sets error when maxStudents is invalid`() = runTest {
        viewModel.onNameChanged("NDA Batch")
        viewModel.onMaxStudentsChanged("0")
        viewModel.createBatch()

        val state = viewModel.uiState.value
        assertEquals("Max students must be greater than 0", state.error)
    }

    @Test
    fun `createBatch creates batch successfully when logged in`() = runTest {
        val testUser = SSBMaxUser("inst_1", "inst@test.com", "Instructor", role = UserRole.INSTRUCTOR)
        coEvery { observeCurrentUser() } returns flowOf(testUser)
        coEvery { batchRepository.createBatch(any()) } returns Result.success(Unit)

        viewModel.onNameChanged("NDA Super Batch")
        viewModel.onDescriptionChanged("Prepare for NDA")
        viewModel.onMaxStudentsChanged("45")
        viewModel.createBatch()

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.isSuccess)
        assertNotNull(state.createdBatchId)
        coVerify(exactly = 1) { batchRepository.createBatch(any()) }
    }

    @Test
    fun `createBatch sets error when user not logged in`() = runTest {
        coEvery { observeCurrentUser() } returns flowOf(null)

        viewModel.onNameChanged("NDA Super Batch")
        viewModel.createBatch()

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNotNull(state.error)
        assertTrue(state.error?.contains("logged in") == true)
    }
}
