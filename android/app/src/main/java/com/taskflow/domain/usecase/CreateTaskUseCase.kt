package com.taskflow.domain.usecase

import com.taskflow.domain.model.Assignee
import com.taskflow.domain.model.Priority
import com.taskflow.domain.model.Task
import com.taskflow.domain.repository.TaskRepository
import javax.inject.Inject

class CreateTaskUseCase @Inject constructor(
    private val repository: TaskRepository
) {
    suspend operator fun invoke(
        title: String,
        description: String?,
        dueDate: String?,
        priority: Priority,
        assignee: Assignee
    ): Result<Task> {
        val trimmedTitle = title.trim()
        if (trimmedTitle.isEmpty()) {
            return Result.failure(IllegalArgumentException("Title is required and cannot be empty"))
        }

        val trimmedDesc = description?.trim()?.ifEmpty { null }
        val cleanDueDate = dueDate?.trim()?.ifEmpty { null }

        return repository.createTask(
            title = trimmedTitle,
            description = trimmedDesc,
            dueDate = cleanDueDate,
            priority = priority,
            assignee = assignee
        )
    }
}
