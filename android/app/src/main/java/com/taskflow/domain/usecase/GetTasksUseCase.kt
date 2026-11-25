package com.taskflow.domain.usecase

import com.taskflow.domain.model.Task
import com.taskflow.domain.model.TaskFilter
import com.taskflow.domain.repository.TaskRepository
import javax.inject.Inject

class GetTasksUseCase @Inject constructor(
    private val repository: TaskRepository
) {
    suspend operator fun invoke(filter: TaskFilter? = null): Result<List<Task>> {
        return repository.getTasks(filter)
    }
}
