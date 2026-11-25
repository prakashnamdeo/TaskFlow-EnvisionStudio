package com.taskflow.presentation.taskdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskflow.domain.model.Task
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
class TaskDetailViewModel @Inject constructor(
    private val toggleTaskUseCase: ToggleTaskUseCase,
    private val getTasksUseCase: GetTasksUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val taskId: String? = savedStateHandle["taskId"]

    private val _uiState = MutableStateFlow<UiState<Task>>(UiState.Loading)
    val uiState: StateFlow<UiState<Task>> = _uiState.asStateFlow()

    private val _userMessage = MutableSharedFlow<String>()
    val userMessage: SharedFlow<String> = _userMessage.asSharedFlow()

    init {
        taskId?.let { loadTask(it) }
    }

    fun loadTask(id: String) {
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            // Find task by ID
            val result = getTasksUseCase()
            result.onSuccess { tasks ->
                val task = tasks.find { it.id == id }
                if (task != null) {
                    _uiState.value = UiState.Success(task)
                } else {
                    _uiState.value = UiState.Error("Task not found.")
                }
            }.onFailure { error ->
                _uiState.value = UiState.Error(error.message ?: "Failed to load task details.")
            }
        }
    }

    fun setInitialTask(task: Task) {
        _uiState.value = UiState.Success(task)
    }

    /**
     * Toggles task completion using an Optimistic Update (Decision 1):
     * 1. Updates local detail UI immediately for seamless responsiveness.
     * 2. Asynchronously calls the server PATCH endpoint.
     * 3. If server fails, rolls back to previous state and sends error message.
     */
    fun toggleCompletion() {
        val currentState = _uiState.value
        if (currentState !is UiState.Success) return

        val originalTask = currentState.data
        val updatedTask = originalTask.copy(completed = !originalTask.completed)

        // 1. Optimistic update
        _uiState.value = UiState.Success(updatedTask)

        // 2. Call server
        viewModelScope.launch {
            val result = toggleTaskUseCase(originalTask.id)
            result.onSuccess { serverTask ->
                _uiState.value = UiState.Success(serverTask)
            }.onFailure { error ->
                // 3. Rollback
                _uiState.value = UiState.Success(originalTask)
                _userMessage.emit("Failed to update status on server: ${error.message}. Changes rolled back.")
            }
        }
    }
}
