package com.taskflow.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskFilterTest {

    private val sampleTasks = listOf(
        Task(
            id = "1",
            title = "Task 1",
            description = null,
            dueDate = "2026-10-15",
            priority = Priority.HIGH,
            assignee = Assignee.AARAV,
            completed = false
        ),
        Task(
            id = "2",
            title = "Task 2",
            description = null,
            dueDate = "2026-10-12",
            priority = Priority.MEDIUM,
            assignee = Assignee.AARAV,
            completed = true
        ),
        Task(
            id = "3",
            title = "Task 3",
            description = null,
            dueDate = null,
            priority = Priority.LOW,
            assignee = Assignee.PRIYA,
            completed = false
        ),
        Task(
            id = "4",
            title = "Task 4",
            description = null,
            dueDate = "2026-10-20",
            priority = Priority.HIGH,
            assignee = Assignee.ROHAN,
            completed = true
        )
    )

    @Test
    fun `default filter matches all tasks`() {
        val filter = TaskFilter()
        assertFalse(filter.isFiltered)

        val filtered = sampleTasks.filter { filter.matches(it) }
        assertEquals(4, filtered.size)
    }

    @Test
    fun `filter by assignee returns only matching tasks`() {
        val filterAarav = TaskFilter(assignee = Assignee.AARAV)
        assertTrue(filterAarav.isFiltered)

        val filtered = sampleTasks.filter { filterAarav.matches(it) }
        assertEquals(2, filtered.size)
        assertTrue(filtered.all { it.assignee == Assignee.AARAV })
    }

    @Test
    fun `filter by status active returns only incomplete tasks`() {
        val filterActive = TaskFilter(status = TaskStatusFilter.ACTIVE)
        val filtered = sampleTasks.filter { filterActive.matches(it) }

        assertEquals(2, filtered.size)
        assertTrue(filtered.all { !it.completed })
    }

    @Test
    fun `filter by status completed returns only complete tasks`() {
        val filterCompleted = TaskFilter(status = TaskStatusFilter.COMPLETED)
        val filtered = sampleTasks.filter { filterCompleted.matches(it) }

        assertEquals(2, filtered.size)
        assertTrue(filtered.all { it.completed })
    }

    @Test
    fun `combined filters return tasks matching both criteria`() {
        // Assignee = Aarav and Status = Completed
        val combinedFilter = TaskFilter(
            assignee = Assignee.AARAV,
            status = TaskStatusFilter.COMPLETED
        )

        val filtered = sampleTasks.filter { combinedFilter.matches(it) }
        assertEquals(1, filtered.size)
        assertEquals("Task 2", filtered.first().title)
        assertEquals(Assignee.AARAV, filtered.first().assignee)
        assertTrue(filtered.first().completed)
    }

    @Test
    fun `combined filters yielding no results returns empty list (Edge Case 3)`() {
        // Assignee = Priya and Status = Completed (Priya has only 1 active task in sample)
        val filterNoMatch = TaskFilter(
            assignee = Assignee.PRIYA,
            status = TaskStatusFilter.COMPLETED
        )

        val filtered = sampleTasks.filter { filterNoMatch.matches(it) }
        assertTrue(filtered.isEmpty())
        assertTrue("Filter should report isFiltered = true", filterNoMatch.isFiltered)
    }

    @Test
    fun `TaskSummary calculates total, active, and completed counts accurately`() {
        val summary = TaskSummary.fromTaskList(sampleTasks)

        assertEquals(4, summary.total)
        assertEquals(2, summary.active)
        assertEquals(2, summary.completed)
    }
}
