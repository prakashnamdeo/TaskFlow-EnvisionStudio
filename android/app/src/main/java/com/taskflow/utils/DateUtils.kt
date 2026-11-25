package com.taskflow.utils

import java.time.Clock
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

object DateUtils {

    private val ISO_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US)
    private val DISPLAY_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.US)

    /**
     * Safely determines whether a task is overdue.
     *
     * Edge case handling:
     * 1. Returns false if task is already completed (Edge Case 2: completed tasks do not display overdue warning).
     * 2. Returns false if dueDate is null or blank (Edge Case 1: null due dates are not overdue).
     * 3. Returns false if dueDate cannot be parsed (malformed date strings do not crash the app).
     * 4. Uses device local date (via Clock.systemDefaultZone() or supplied clock) rather than UTC to prevent timezone skew.
     */
    fun isOverdue(
        dueDateStr: String?,
        isCompleted: Boolean,
        clock: Clock = Clock.systemDefaultZone()
    ): Boolean {
        if (isCompleted) {
            return false
        }
        if (dueDateStr.isNullOrBlank()) {
            return false
        }

        val dueDate = parseDate(dueDateStr) ?: return false
        val today = LocalDate.now(clock)

        return dueDate.isBefore(today)
    }

    /**
     * Parses a date string in YYYY-MM-DD format safely. Returns null if invalid or malformed.
     */
    fun parseDate(dateStr: String?): LocalDate? {
        if (dateStr.isNullOrBlank()) return null
        return try {
            LocalDate.parse(dateStr.trim(), ISO_DATE_FORMATTER)
        } catch (e: DateTimeParseException) {
            null
        }
    }

    /**
     * Formats a YYYY-MM-DD date string into a user-friendly display string (e.g. "Oct 15, 2026").
     * Returns "No due date" if null or empty, or the raw string if unparseable.
     */
    fun formatDisplayDate(dateStr: String?): String {
        if (dateStr.isNullOrBlank()) {
            return "No due date"
        }
        val parsed = parseDate(dateStr) ?: return dateStr
        return parsed.format(DISPLAY_DATE_FORMATTER)
    }

    /**
     * Converts a LocalDate to standard API format "YYYY-MM-DD".
     */
    fun toIsoDateString(date: LocalDate): String {
        return date.format(ISO_DATE_FORMATTER)
    }
}
