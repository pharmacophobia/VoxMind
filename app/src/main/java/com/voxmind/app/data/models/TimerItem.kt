package com.voxmind.app.data.models

import java.util.UUID

data class TimerItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "Timer",
    val totalSeconds: Long = 300L,
    val remainingSeconds: Long = 300L,
    val attachedReminder: String = "",
    val isRunning: Boolean = false,
    val isCompleted: Boolean = false,
    val targetEndTimeMillis: Long = 0L,
    val sendSmsOnCompletion: Boolean = false,
    val smsRecipient: String = "",
    val sendEmailOnCompletion: Boolean = false,
    val emailRecipient: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    fun getFormattedRemaining(): String {
        val hours = remainingSeconds / 3600
        val minutes = (remainingSeconds % 3600) / 60
        val seconds = remainingSeconds % 60
        return if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }

    fun getProgress(): Float {
        if (totalSeconds <= 0) return 0f
        val elapsed = (totalSeconds - remainingSeconds).coerceAtLeast(0)
        return (elapsed.toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f)
    }
}
