package com.taskflow.domain.model

enum class Assignee(val id: String, val displayName: String) {
    AARAV("aarav", "Aarav"),
    PRIYA("priya", "Priya"),
    ROHAN("rohan", "Rohan");

    companion object {
        fun fromId(id: String?): Assignee {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: AARAV
        }
    }
}
