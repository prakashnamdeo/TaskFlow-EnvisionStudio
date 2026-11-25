package com.taskflow.domain.model

enum class Priority(val value: String, val displayName: String) {
    LOW("low", "Low"),
    MEDIUM("medium", "Medium"),
    HIGH("high", "High");

    companion object {
        fun fromValue(value: String?): Priority {
            return entries.firstOrNull { it.value.equals(value, ignoreCase = true) } ?: MEDIUM
        }
    }
}
