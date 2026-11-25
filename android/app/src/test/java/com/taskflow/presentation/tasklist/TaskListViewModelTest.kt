package com.taskflow.presentation.tasklist

import app.cash.turbine.test
import com.taskflow.domain.model.Assignee
import com.taskflow.domain.model.Priority
import com.taskflow.domain.model.Task
import com.taskflow.domain.model.TaskStatusFilter
import com.taskflow.domain.usecase.GetTasksUseCase
import com.taskflow.domain.usecase.ToggleTaskUseCase
import com.taskflow.presentation.common.UiState
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TaskListViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val getTasksUseCase: GetTasksUseCase = mockk()
    private val toggleTaskUseCase: ToggleTaskUseCase = mockk()

    private val testTasks = listOf(
        Task(
            id = "task-1",
            title = "Task 1",
            description = "Desc 1",
            dueDate = "2026-10-15",
            priority = Priority.HIGH,
            assignee = Assignee.AARAV,
            completed = false
        ),
        Task(
            id = "task-2",
            title = "Task 2",
            description = "Desc 2",
            dueDate = "2026-10-12",
            priority = Priority.MEDIUM,
            assignee = Assignee.AARAV,
            completed = true
        ),
        Task(
            id = "task-3",
            title = "Task 3",
            description = "Desc 3",
            dueDate = null,
            priority = Priority.LOW,
            assignee = Assignee.PRIYA,
            completed = false
        )
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadTasks transitions to Success and computes summary correctly`() = runTest(testDispatcher) {
        coEvery { getTasksUseCase(any()) } returns Result.success(testTasks)

        val viewModel = TaskListViewModel(getTasksUseCase, toggleTaskUseCase)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("State should be Success", state is UiState.Success)
        assertEquals(3, (state as UiState.Success).data.size)

        val summary = viewModel.summary.value
        assertEquals(3, summary.total)
        assertEquals(2, summary.active)
        assertEquals(1, summary.completed)
    }

    @Test
    fun `loadTasks transitions to Error when use case fails`() = runTest(testDispatcher) {
        coEvery { getTasksUseCase(any()) } returns Result.failure(RuntimeException("Connection timeout"))

        val viewModel = TaskListViewModel(getTasksUseCase, toggleTaskUseCase)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("State should be Error", state is UiState.Error)
        assertEquals("Connection timeout", (state as UiState.Error).message)
    }

    @Test
    fun `filter changes filter tasks and maintains selection (Decision 2)`() = runTest(testDispatcher) {
        coEvery { getTasksUseCase(any()) } returns Result.success(testTasks)

        val viewModel = TaskListViewModel(getTasksUseCase, toggleTaskUseCase)
        advanceUntilIdle()

        // Apply Aarav filter
        viewModel.setAssigneeFilter(Assignee.AARAV)
        advanceUntilIdle()

        var state = viewModel.uiState.value as UiState.Success
        assertEquals(2, state.data.size)
        assertEquals(Assignee.AARAV, viewModel.filter.value.assignee)

        // Combine with Completed status
        viewModel.setStatusFilter(TaskStatusFilter.COMPLETED)
        advanceUntilIdle()

        state = viewModel.uiState.value as UiState.Success
        assertEquals(1, state.data.size)
        assertEquals("task-2", state.data.first().id)
        assertEquals(Assignee.AARAV, viewModel.filter.value.assignee)
        assertEquals(TaskStatusFilter.COMPLETED, viewModel.filter.value.status)

        // Reload tasks: filter must be preserved!
        viewModel.loadTasks()
        advanceUntilIdle()

        assertEquals(Assignee.AARAV, viewModel.filter.value.assignee)
        assertEquals(TaskStatusFilter.COMPLETED, viewModel.filter.value.status)
        val reloadedState = viewModel.uiState.value as UiState.Success
        assertEquals(1, reloadedState.data.size)
    }

    @Test
    fun `combined filters with no matching tasks transitions to EmptyFiltered state (Edge Case 3)`() = runTest(testDispatcher) {
        coEvery { getTasksUseCase(any()) } returns Result.success(testTasks)

        val viewModel = TaskListViewModel(getTasksUseCase, toggleTaskUseCase)
        advanceUntilIdle()

        // Priya has no completed tasks in testTasks
        viewModel.setAssigneeFilter(Assignee.PRIYA)
        viewModel.setStatusFilter(TaskStatusFilter.COMPLETED)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("Should transition to EmptyFiltered", state is UiState.EmptyFiltered)

        // Clearing filters restores normal list
        viewModel.clearFilters()
        advanceUntilIdle()

        val restoredState = viewModel.uiState.value
        assertTrue("Should restore to Success", restoredState is UiState.Success)
        assertEquals(3, (restoredState as UiState.Success).data.size)
    }

    @Test
    fun `toggleTaskCompletion performs optimistic update and rolls back on failure (Decision 1)`() = runTest(testDispatcher) {
        coEvery { getTasksUseCase(any()) } returns Result.success(testTasks)
        // Failure simulation on server
        coEvery { toggleTaskUseCase("task-1") } returns Result.failure(RuntimeException("Internal Server Error"))

        val viewModel = TaskListViewModel(getTasksUseCase, toggleTaskUseCase)
        advanceUntilIdle()

        val initialTask = (viewModel.uiState.value as UiState.Success).data.first { it.id == "task-1" }
        assertFalse("Initially task-1 is not completed", initialTask.completed)

        viewModel.userMessage.test {
            // Trigger toggle
            viewModel.toggleTaskCompletion(initialTask)

            // Verify local optimistic state changed immediately before advanceUntilIdle
            val optimisticState = viewModel.uiState.value as UiState.Success
            val optimisticTask = optimisticState.data.first { it.id == "task-1" }
            assertTrue("Task should be optimistically marked completed", optimisticTask.completed)
            assertEquals("Summary active count should decrease", 1, viewModel.summary.value.active)
            assertEquals("Summary completed count should increase", 2, viewModel.summary.value.completed)

            // Let coroutine run and fail
            advanceUntilIdle()

            // Verify user message emitted
            val message = awaitItem()
            assertTrue("Message should mention rollback", message.contains("rolled back", ignoreCase = true))

            // Verify state rolled back to original
            val rolledBackState = viewModel.uiState.value as UiState.Success
            val rolledBackTask = rolledBackState.data.first { it.id == "task-1" }
            assertFalse("Task should be rolled back to incomplete", rolledBackTask.completed)
            assertEquals("Summary active count restored", 2, viewModel.summary.value.active)
            assertEquals("Summary completed count restored", 1, viewModel.summary.value.completed)
        }
    }
}
