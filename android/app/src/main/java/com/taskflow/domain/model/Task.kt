package com.taskflow.domain.model

import com.taskflow.utils.DateUtils
import java.time.Clock

data class Task(
    val id: String,
    val title: String,
    val description: String?,
    val dueDate: String?,
    val priority: Priority,
    val assignee: Assignee,
    val completed: Boolean,
    val createdAt: String? = null
) {
    /**
     * Computed property indicating whether this task is currently overdue.
     * Respects Edge Case 1 (missing due date), Edge Case 2 (completed tasks not overdue),
     * and safe local date comparison.
     */
    fun isOverdue(clock: Clock = Clock.systemDefaultZone()): Boolean {
        return DateUtils.isOverdue(dueDate, completed, clock)
    }

    val displayDueDate: String
        get() = DateUtils.formatDisplayDate(dueDate)
}
