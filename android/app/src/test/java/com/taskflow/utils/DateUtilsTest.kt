package com.taskflow.utils

import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DateUtilsTest {

    // Fixed clock set to 2026-10-15T12:00:00Z for deterministic tests
    private val fixedZone = ZoneId.of("UTC")
    private val fixedInstant = Instant.parse("2026-10-15T12:00:00Z")
    private lateinit var fixedClock: Clock

    @Before
    fun setUp() {
        fixedClock = Clock.fixed(fixedInstant, fixedZone)
    }

    @Test
    fun `isOverdue returns true for active task with due date in the past`() {
        // Given today is 2026-10-15, a due date of 2026-10-14 is in the past
        val result = DateUtils.isOverdue(
            dueDateStr = "2026-10-14",
            isCompleted = false,
            clock = fixedClock
        )
        assertTrue("Active task with yesterday's due date should be overdue", result)
    }

    @Test
    fun `isOverdue returns false for active task due today`() {
        // Today is 2026-10-15
        val result = DateUtils.isOverdue(
            dueDateStr = "2026-10-15",
            isCompleted = false,
            clock = fixedClock
        )
        assertFalse("Task due today should not be overdue", result)
    }

    @Test
    fun `isOverdue returns false for active task due in the future`() {
        // Due date tomorrow (2026-10-16)
        val result = DateUtils.isOverdue(
            dueDateStr = "2026-10-16",
            isCompleted = false,
            clock = fixedClock
        )
        assertFalse("Task due in the future should not be overdue", result)
    }

    @Test
    fun `isOverdue returns false when dueDate is null (Edge Case 1)`() {
        val result = DateUtils.isOverdue(
            dueDateStr = null,
            isCompleted = false,
            clock = fixedClock
        )
        assertFalse("Task with null due date must never be overdue", result)
    }

    @Test
    fun `isOverdue returns false when dueDate is blank (Edge Case 1)`() {
        val result = DateUtils.isOverdue(
            dueDateStr = "   ",
            isCompleted = false,
            clock = fixedClock
        )
        assertFalse("Task with blank due date must never be overdue", result)
    }

    @Test
    fun `isOverdue returns false when task is completed even if due date is past (Edge Case 2)`() {
        // Due date was 2026-10-10 (5 days ago), but completed is true
        val result = DateUtils.isOverdue(
            dueDateStr = "2026-10-10",
            isCompleted = true,
            clock = fixedClock
        )
        assertFalse("Completed task should never display an overdue warning", result)
    }

    @Test
    fun `isOverdue handles malformed or invalid date strings safely without crashing`() {
        val invalidDates = listOf(
            "invalid-date",
            "2026-13-45",
            "15-10-2026",
            "2026/10/15",
            "abc"
        )
        for (date in invalidDates) {
            val result = DateUtils.isOverdue(
                dueDateStr = date,
                isCompleted = false,
                clock = fixedClock
            )
            assertFalse("Malformed date '$date' must not crash and should return false", result)
        }
    }

    @Test
    fun `parseDate returns null for invalid formats`() {
        assertNull(DateUtils.parseDate("not-a-date"))
        assertNull(DateUtils.parseDate(null))
        assertNull(DateUtils.parseDate(""))
    }

    @Test
    fun `formatDisplayDate returns No due date for null or empty`() {
        assertEquals("No due date", DateUtils.formatDisplayDate(null))
        assertEquals("No due date", DateUtils.formatDisplayDate(""))
        assertEquals("No due date", DateUtils.formatDisplayDate("   "))
    }

    @Test
    fun `formatDisplayDate formats valid ISO string correctly`() {
        val formatted = DateUtils.formatDisplayDate("2026-10-15")
        assertEquals("Oct 15, 2026", formatted)
    }
}
