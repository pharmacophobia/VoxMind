package com.voxmind.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import com.voxmind.app.data.models.AlarmItem
import com.voxmind.app.data.models.Priority
import com.voxmind.app.data.models.Reminder
import com.voxmind.app.data.models.TimerItem
import com.voxmind.app.data.repository.VoxMindRepository
import com.voxmind.app.notifications.NotificationHelper
import com.voxmind.app.util.AlarmScheduler
import com.voxmind.app.util.SmsHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_TRIGGER_REMINDER = "com.voxmind.app.ACTION_TRIGGER_REMINDER"
        const val ACTION_TRIGGER_ALARM = "com.voxmind.app.ACTION_TRIGGER_ALARM"
        const val ACTION_TRIGGER_TIMER = "com.voxmind.app.ACTION_TRIGGER_TIMER"
        const val ACTION_SNOOZE = "com.voxmind.app.ACTION_SNOOZE"
        const val ACTION_DISMISS = "com.voxmind.app.ACTION_DISMISS"
        const val ACTION_MARK_DONE = "com.voxmind.app.ACTION_MARK_DONE"

        const val EXTRA_ITEM_ID = "extra_item_id"
        const val EXTRA_ITEM_TITLE = "extra_item_title"
        const val EXTRA_ITEM_NOTES = "extra_item_notes"
        const val EXTRA_SEND_SMS = "extra_send_sms"
        const val EXTRA_PHONE = "extra_phone"
        const val EXTRA_SEND_EMAIL = "extra_send_email"
        const val EXTRA_EMAIL = "extra_email"
        const val EXTRA_ATTACHED_REMINDER = "extra_attached_reminder"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "VoxMind:AlarmReceiverWakeLock")
        wakeLock.acquire(10000L) // 10 seconds max

        val notifHelper = NotificationHelper(context)
        val repository = VoxMindRepository(context)

        try {
            when (action) {
                ACTION_TRIGGER_REMINDER -> {
                    val id = intent.getStringExtra(EXTRA_ITEM_ID) ?: ""
                    val title = intent.getStringExtra(EXTRA_ITEM_TITLE) ?: "Reminder"
                    val notes = intent.getStringExtra(EXTRA_ITEM_NOTES) ?: ""
                    val sendSms = intent.getBooleanExtra(EXTRA_SEND_SMS, false)
                    val phone = intent.getStringExtra(EXTRA_PHONE) ?: ""
                    val sendEmail = intent.getBooleanExtra(EXTRA_SEND_EMAIL, false)
                    val email = intent.getStringExtra(EXTRA_EMAIL) ?: ""

                    val reminder = Reminder(
                        id = id,
                        title = title,
                        notes = notes,
                        dueTimestamp = System.currentTimeMillis(),
                        sendSms = sendSms,
                        smsRecipientPhone = phone,
                        sendEmail = sendEmail,
                        emailRecipient = email
                    )

                    notifHelper.showReminderNotification(reminder)

                    // Dispatch SMS if requested
                    if (sendSms && phone.isNotBlank()) {
                        val smsMsg = if (notes.isNotBlank()) "$title: $notes" else title
                        SmsHelper.sendSmsReminder(context, phone, smsMsg)
                    }
                }

                ACTION_TRIGGER_ALARM -> {
                    val id = intent.getStringExtra(EXTRA_ITEM_ID) ?: ""
                    val title = intent.getStringExtra(EXTRA_ITEM_TITLE) ?: "Alarm"
                    val attachedNotes = intent.getStringExtra(EXTRA_ATTACHED_REMINDER) ?: ""

                    val alarm = AlarmItem(
                        id = id,
                        hour = 0,
                        minute = 0,
                        label = title,
                        attachedReminderNotes = attachedNotes
                    )
                    notifHelper.showAlarmNotification(alarm)
                }

                ACTION_TRIGGER_TIMER -> {
                    val id = intent.getStringExtra(EXTRA_ITEM_ID) ?: ""
                    val title = intent.getStringExtra(EXTRA_ITEM_TITLE) ?: "Timer"
                    val attachedReminder = intent.getStringExtra(EXTRA_ATTACHED_REMINDER) ?: ""
                    val sendSms = intent.getBooleanExtra(EXTRA_SEND_SMS, false)
                    val phone = intent.getStringExtra(EXTRA_PHONE) ?: ""

                    val timer = TimerItem(
                        id = id,
                        title = title,
                        attachedReminder = attachedReminder,
                        isCompleted = true,
                        isRunning = false,
                        remainingSeconds = 0L
                    )
                    notifHelper.showTimerFinishedNotification(timer)
                    repository.updateTimerRemaining(id, 0L, isRunning = false, isCompleted = true)

                    if (sendSms && phone.isNotBlank()) {
                        val msg = if (attachedReminder.isNotBlank()) {
                            "Timer '$title' completed! Attached reminder: $attachedReminder"
                        } else {
                            "Timer '$title' completed!"
                        }
                        SmsHelper.sendSmsReminder(context, phone, msg)
                    }
                }

                ACTION_MARK_DONE -> {
                    val id = intent.getStringExtra(EXTRA_ITEM_ID) ?: ""
                    if (id.isNotBlank()) {
                        repository.toggleReminderCompletion(id)
                        notifHelper.cancelNotification(id.hashCode())
                    }
                }

                ACTION_SNOOZE -> {
                    val id = intent.getStringExtra(EXTRA_ITEM_ID) ?: ""
                    val title = intent.getStringExtra(EXTRA_ITEM_TITLE) ?: "Reminder"
                    notifHelper.cancelNotification(id.hashCode())

                    // Snooze for 10 minutes
                    val snoozeTime = System.currentTimeMillis() + (10 * 60 * 1000L)
                    val snoozedReminder = Reminder(
                        id = id,
                        title = title,
                        dueTimestamp = snoozeTime
                    )
                    AlarmScheduler(context).scheduleReminder(snoozedReminder)
                }

                ACTION_DISMISS -> {
                    val id = intent.getStringExtra(EXTRA_ITEM_ID) ?: ""
                    if (id.isNotBlank()) {
                        notifHelper.cancelNotification(id.hashCode())
                    }
                }
            }
        } finally {
            if (wakeLock.isHeld) {
                wakeLock.release()
            }
        }
    }
}
