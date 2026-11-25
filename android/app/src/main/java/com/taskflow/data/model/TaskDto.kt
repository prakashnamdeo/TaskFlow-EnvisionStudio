package com.taskflow.data.model

import com.google.gson.annotations.SerializedName
import com.taskflow.domain.model.Assignee
import com.taskflow.domain.model.Priority
import com.taskflow.domain.model.Task

data class TaskDto(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String?,
    @SerializedName("dueDate") val dueDate: String?,
    @SerializedName("priority") val priority: String,
    @SerializedName("assigneeId") val assigneeId: String,
    @SerializedName("completed") val completed: Boolean,
    @SerializedName("createdAt") val createdAt: String?
) {
    fun toDomain(): Task {
        return Task(
            id = id,
            title = title,
            description = description,
            dueDate = dueDate,
            priority = Priority.fromValue(priority),
            assignee = Assignee.fromId(assigneeId),
            completed = completed,
            createdAt = createdAt
        )
    }
}

data class CreateTaskRequest(
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String?,
    @SerializedName("dueDate") val dueDate: String?,
    @SerializedName("priority") val priority: String,
    @SerializedName("assigneeId") val assigneeId: String
)

data class ApiErrorResponse(
    @SerializedName("error") val error: String?,
    @SerializedName("errors") val errors: Map<String, String>?
)
