package com.voxmind.app.data.models

import java.util.UUID

data class AlarmItem(
    val id: String = UUID.randomUUID().toString(),
    val hour: Int,
    val minute: Int,
    val label: String = "Alarm",
    val attachedReminderNotes: String = "",
    val repeatDays: Set<Int> = emptySet(), // 1=Mon, 2=Tue, ..., 7=Sun (Calendar format or ISO)
    val isEnabled: Boolean = true,
    val vibrate: Boolean = true,
    val soundTitle: String = "Standard Alarm Chime",
    val createdAt: Long = System.currentTimeMillis()
) {
    fun getFormattedTime(): String {
        val h = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
        val amPm = if (hour < 12) "AM" else "PM"
        return String.format("%d:%02d %s", h, minute, amPm)
    }

    fun getRepeatDaysFormatted(): String {
        if (repeatDays.isEmpty()) return "Once"
        if (repeatDays.size == 7) return "Every day"
        if (repeatDays == setOf(1, 2, 3, 4, 5)) return "Weekdays"
        if (repeatDays == setOf(6, 7)) return "Weekends"
        val dayNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        return repeatDays.sorted().mapNotNull { dayIndex ->
            if (dayIndex in 1..7) dayNames[dayIndex - 1] else null
        }.joinToString(", ")
    }
}
