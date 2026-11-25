package com.taskflow.domain.model

enum class TaskStatusFilter(val value: String?, val displayName: String) {
    ALL(null, "All"),
    ACTIVE("active", "Active"),
    COMPLETED("completed", "Completed")
}

data class TaskFilter(
    val assignee: Assignee? = null,
    val status: TaskStatusFilter = TaskStatusFilter.ALL
) {
    val isFiltered: Boolean
        get() = assignee != null || status != TaskStatusFilter.ALL

    fun matches(task: Task): Boolean {
        val assigneeMatches = assignee == null || task.assignee == assignee
        val statusMatches = when (status) {
            TaskStatusFilter.ALL -> true
            TaskStatusFilter.ACTIVE -> !task.completed
            TaskStatusFilter.COMPLETED -> task.completed
        }
        return assigneeMatches && statusMatches
    }
}

data class TaskSummary(
    val total: Int = 0,
    val active: Int = 0,
    val completed: Int = 0
) {
    companion object {
        fun fromTaskList(tasks: List<Task>): TaskSummary {
            val total = tasks.size
            val completed = tasks.count { it.completed }
            val active = total - completed
            return TaskSummary(total, active, completed)
        }
    }
}
