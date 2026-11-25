package com.taskflow.presentation.addtask

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskflow.data.repository.ApiException
import com.taskflow.domain.model.Assignee
import com.taskflow.domain.model.Priority
import com.taskflow.domain.model.Task
import com.taskflow.domain.usecase.CreateTaskUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AddTaskFormState(
    val title: String = "",
    val description: String = "",
    val dueDate: String? = null,
    val priority: Priority = Priority.MEDIUM,
    val assignee: Assignee = Assignee.AARAV,
    val isSubmitting: Boolean = false,
    val fieldErrors: Map<String, String> = emptyMap(),
    val generalError: String? = null
)

@HiltViewModel
class AddTaskViewModel @Inject constructor(
    private val createTaskUseCase: CreateTaskUseCase
) : ViewModel() {

    private val _formState = MutableStateFlow(AddTaskFormState())
    val formState: StateFlow<AddTaskFormState> = _formState.asStateFlow()

    private val _taskCreatedEvent = MutableSharedFlow<Task>()
    val taskCreatedEvent: SharedFlow<Task> = _taskCreatedEvent.asSharedFlow()

    fun onTitleChanged(title: String) {
        val currentFieldErrors = _formState.value.fieldErrors.toMutableMap()
        currentFieldErrors.remove("title")
        _formState.value = _formState.value.copy(
            title = title,
            fieldErrors = currentFieldErrors,
            generalError = null
        )
    }

    fun onDescriptionChanged(description: String) {
        _formState.value = _formState.value.copy(
            description = description,
            generalError = null
        )
    }

    fun onDueDateSelected(dateIso: String?) {
        val currentFieldErrors = _formState.value.fieldErrors.toMutableMap()
        currentFieldErrors.remove("dueDate")
        _formState.value = _formState.value.copy(
            dueDate = dateIso,
            fieldErrors = currentFieldErrors,
            generalError = null
        )
    }

    fun onPrioritySelected(priority: Priority) {
        _formState.value = _formState.value.copy(priority = priority)
    }

    fun onAssigneeSelected(assignee: Assignee) {
        _formState.value = _formState.value.copy(assignee = assignee)
    }

    fun submitTask() {
        val current = _formState.value
        if (current.isSubmitting) return

        // Client-side pre-validation
        val trimmedTitle = current.title.trim()
        if (trimmedTitle.isEmpty()) {
            _formState.value = current.copy(
                fieldErrors = mapOf("title" to "Title is required and cannot be empty"),
                generalError = null
            )
            return
        }

        // Begin submission
        _formState.value = current.copy(
            isSubmitting = true,
            fieldErrors = emptyMap(),
            generalError = null
        )

        viewModelScope.launch {
            val result = createTaskUseCase(
                title = trimmedTitle,
                description = current.description.ifBlank { null },
                dueDate = current.dueDate,
                priority = current.priority,
                assignee = current.assignee
            )

            result.onSuccess { createdTask ->
                _formState.value = _formState.value.copy(isSubmitting = false)
                _taskCreatedEvent.emit(createdTask)
            }.onFailure { error ->
                // Handle backend validation error (Edge Case 4)
                if (error is ApiException && error.fieldErrors.isNotEmpty()) {
                    _formState.value = _formState.value.copy(
                        isSubmitting = false,
                        fieldErrors = error.fieldErrors,
                        generalError = error.message
                    )
                } else {
                    _formState.value = _formState.value.copy(
                        isSubmitting = false,
                        generalError = error.message ?: "Failed to create task"
                    )
                }
            }
        }
    }
}
