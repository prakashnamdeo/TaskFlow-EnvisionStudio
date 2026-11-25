package com.taskflow.domain.repository

import com.taskflow.domain.model.Assignee
import com.taskflow.domain.model.Priority
import com.taskflow.domain.model.Task
import com.taskflow.domain.model.TaskFilter

interface TaskRepository {
    suspend fun getTasks(filter: TaskFilter? = null): Result<List<Task>>
    suspend fun getTaskById(id: String): Result<Task>
    suspend fun createTask(
        title: String,
        description: String?,
        dueDate: String?,
        priority: Priority,
        assignee: Assignee
    ): Result<Task>
    suspend fun toggleTask(id: String): Result<Task>
}
