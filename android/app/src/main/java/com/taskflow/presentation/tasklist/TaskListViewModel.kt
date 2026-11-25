package com.taskflow.presentation.tasklist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskflow.domain.model.Assignee
import com.taskflow.domain.model.Task
import com.taskflow.domain.model.TaskFilter
import com.taskflow.domain.model.TaskStatusFilter
import com.taskflow.domain.model.TaskSummary
import com.taskflow.domain.usecase.GetTasksUseCase
import com.taskflow.domain.usecase.ToggleTaskUseCase
import com.taskflow.presentation.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class TaskListViewModel @Inject constructor(
    private val getTasksUseCase: GetTasksUseCase,
    private val toggleTaskUseCase: ToggleTaskUseCase
) : ViewModel() {

    // Filter state is maintained across navigation to preserve user context (Decision 2)
    private val _filter = MutableStateFlow(TaskFilter())
    val filter: StateFlow<TaskFilter> = _filter.asStateFlow()

    private val _uiState = MutableStateFlow<UiState<List<Task>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<Task>>> = _uiState.asStateFlow()

    private val _summary = MutableStateFlow(TaskSummary())
    val summary: StateFlow<TaskSummary> = _summary.asStateFlow()

    private val _userMessage = MutableSharedFlow<String>()
    val userMessage: SharedFlow<String> = _userMessage.asSharedFlow()

    // Full unfiltered list of tasks from server to keep summary statistics accurate
    private var allTasks: List<Task> = emptyList()

    init {
        loadTasks()
    }

    fun loadTasks(isSwipeRefresh: Boolean = false) {
        if (!isSwipeRefresh && _uiState.value !is UiState.Success) {
            _uiState.value = UiState.Loading
        }

        viewModelScope.launch {
            // Fetch all tasks so statistics can always reflect the full dataset
            val result = getTasksUseCase()
            result.onSuccess { tasks ->
                allTasks = tasks
                _summary.value = TaskSummary.fromTaskList(tasks)
                applyCurrentFilter()
            }.onFailure { error ->
                _uiState.value = UiState.Error(
                    error.message ?: "Failed to load tasks. Please try again."
                )
            }
        }
    }

    fun setAssigneeFilter(assignee: Assignee?) {
        if (_filter.value.assignee == assignee) return
        _filter.value = _filter.value.copy(assignee = assignee)
        applyCurrentFilter()
    }

    fun setStatusFilter(status: TaskStatusFilter) {
        if (_filter.value.status == status) return
        _filter.value = _filter.value.copy(status = status)
        applyCurrentFilter()
    }

    fun clearFilters() {
        _filter.value = TaskFilter()
        applyCurrentFilter()
    }

    private fun applyCurrentFilter() {
        if (allTasks.isEmpty() && _uiState.value !is UiState.Error) {
            _uiState.value = UiState.Empty
            return
        }

        val currentFilter = _filter.value
        val filtered = allTasks.filter { currentFilter.matches(it) }

        _uiState.value = when {
            filtered.isEmpty() && currentFilter.isFiltered -> UiState.EmptyFiltered
            filtered.isEmpty() -> UiState.Empty
            else -> UiState.Success(filtered)
        }
    }

    /**
     * Toggles task completion using an Optimistic Update (Decision 1):
     * 1. Updates local list and summary immediately for instant responsiveness.
     * 2. Asynchronously calls the server PATCH endpoint.
     * 3. If server fails, rolls back to previous state and notifies the user.
     */
    fun toggleTaskCompletion(task: Task) {
        val previousAllTasks = allTasks
        val previousSummary = _summary.value

        // 1. Optimistic local update
        val updatedTask = task.copy(completed = !task.completed)
        allTasks = allTasks.map { if (it.id == task.id) updatedTask else it }
        _summary.value = TaskSummary.fromTaskList(allTasks)
        applyCurrentFilter()

        // 2. Network call with rollback
        viewModelScope.launch {
            val result = toggleTaskUseCase(task.id)
            result.onSuccess { serverTask ->
                // Confirm server state
                allTasks = allTasks.map { if (it.id == serverTask.id) serverTask else it }
                _summary.value = TaskSummary.fromTaskList(allTasks)
                applyCurrentFilter()
            }.onFailure { error ->
                // 3. Rollback on failure
                allTasks = previousAllTasks
                _summary.value = previousSummary
                applyCurrentFilter()
                _userMessage.emit(
                    "Failed to update task on server: ${error.message}. Changes rolled back."
                )
            }
        }
    }
}
