package com.taskflow.presentation.addtask

import app.cash.turbine.test
import com.taskflow.data.repository.ApiException
import com.taskflow.domain.model.Assignee
import com.taskflow.domain.model.Priority
import com.taskflow.domain.model.Task
import com.taskflow.domain.usecase.CreateTaskUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AddTaskViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val createTaskUseCase: CreateTaskUseCase = mockk()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `submitTask with empty title fails client validation without calling usecase`() = runTest(testDispatcher) {
        val viewModel = AddTaskViewModel(createTaskUseCase)

        viewModel.onTitleChanged("   ")
        viewModel.submitTask()

        val state = viewModel.formState.value
        assertFalse(state.isSubmitting)
        assertEquals("Title is required and cannot be empty", state.fieldErrors["title"])
    }

    @Test
    fun `submitTask with valid data calls usecase and emits taskCreatedEvent`() = runTest(testDispatcher) {
        val createdTask = Task(
            id = "new-task-1",
            title = "Test Create",
            description = "Test Desc",
            dueDate = "2026-11-20",
            priority = Priority.HIGH,
            assignee = Assignee.ROHAN,
            completed = false
        )

        coEvery {
            createTaskUseCase(
                title = "Test Create",
                description = "Test Desc",
                dueDate = "2026-11-20",
                priority = Priority.HIGH,
                assignee = Assignee.ROHAN
            )
        } returns Result.success(createdTask)

        val viewModel = AddTaskViewModel(createTaskUseCase)
        viewModel.onTitleChanged("Test Create")
        viewModel.onDescriptionChanged("Test Desc")
        viewModel.onDueDateSelected("2026-11-20")
        viewModel.onPrioritySelected(Priority.HIGH)
        viewModel.onAssigneeSelected(Assignee.ROHAN)

        viewModel.taskCreatedEvent.test {
            viewModel.submitTask()
            advanceUntilIdle()

            val emitted = awaitItem()
            assertEquals("new-task-1", emitted.id)
            assertEquals("Test Create", emitted.title)
            assertFalse(viewModel.formState.value.isSubmitting)
        }
    }

    @Test
    fun `submitTask handles server validation error and populates fieldErrors (Edge Case 4)`() = runTest(testDispatcher) {
        val apiException = ApiException(
            message = "Validation failed",
            fieldErrors = mapOf(
                "title" to "Title is required and cannot be empty",
                "dueDate" to "Due date must be in YYYY-MM-DD format"
            ),
            statusCode = 400
        )

        coEvery {
            createTaskUseCase(any(), any(), any(), any(), any())
        } returns Result.failure(apiException)

        val viewModel = AddTaskViewModel(createTaskUseCase)
        viewModel.onTitleChanged("Some Title")
        viewModel.onDescriptionChanged("Some Description")
        viewModel.onDueDateSelected("invalid-date")

        viewModel.submitTask()
        advanceUntilIdle()

        val state = viewModel.formState.value
        assertFalse(state.isSubmitting)
        // Form inputs must be retained
        assertEquals("Some Title", state.title)
        assertEquals("Some Description", state.description)
        assertEquals("invalid-date", state.dueDate)
        // Field errors must be mapped
        assertNotNull(state.fieldErrors["title"])
        assertNotNull(state.fieldErrors["dueDate"])
        assertEquals("Due date must be in YYYY-MM-DD format", state.fieldErrors["dueDate"])
    }
}
