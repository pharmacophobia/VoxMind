package com.voxmind.app.data.models

import java.util.UUID

enum class Priority {
    LOW,
    MEDIUM,
    HIGH,
    URGENT
}

data class Reminder(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val notes: String = "",
    val dueTimestamp: Long,
    val priority: Priority = Priority.MEDIUM,
    val category: String = "General",
    val isCompleted: Boolean = false,
    val sendSms: Boolean = false,
    val smsRecipientPhone: String = "",
    val sendEmail: Boolean = false,
    val emailRecipient: String = "",
    val repeatInterval: String = "NONE", // NONE, DAILY, WEEKLY
    val createdAt: Long = System.currentTimeMillis()
)
