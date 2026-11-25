package com.taskflow.domain.usecase

import com.taskflow.domain.model.Task
import com.taskflow.domain.repository.TaskRepository
import javax.inject.Inject

class ToggleTaskUseCase @Inject constructor(
    private val repository: TaskRepository
) {
    suspend operator fun invoke(id: String): Result<Task> {
        return repository.toggleTask(id)
    }
}
