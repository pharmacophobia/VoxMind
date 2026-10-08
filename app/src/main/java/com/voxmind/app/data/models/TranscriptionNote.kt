package com.voxmind.app.data.models

import java.util.UUID

data class ExtractedReminderItem(
    val id: String = UUID.randomUUID().toString(),
    val taskTitle: String,
    val detectedDateOrTime: String,
    val notes: String = "",
    val isScheduled: Boolean = false
)

data class TranscriptionNote(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "Voice Note",
    val rawTranscript: String,
    val organizedThoughts: String = "",
    val bulletPoints: List<String> = emptyList(),
    val summary: String = "",
    val extractedReminders: List<ExtractedReminderItem> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)
