package com.voxmind.app.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.voxmind.app.MainActivity
import com.voxmind.app.R
import com.voxmind.app.data.models.AlarmItem
import com.voxmind.app.data.models.Reminder
import com.voxmind.app.data.models.TimerItem
import com.voxmind.app.receiver.AlarmReceiver
import com.voxmind.app.util.EmailHelper

class NotificationHelper(private val context: Context) {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        const val CHANNEL_REMINDERS = "voxmind_reminders_channel"
        const val CHANNEL_ALARMS = "voxmind_alarms_channel"
        const val CHANNEL_TIMERS = "voxmind_timers_channel"
    }

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val notifSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            val audioAttributesAlarm = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()

            val audioAttributesNotif = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build()

            // 1. Reminders Channel
            val reminderChannel = NotificationChannel(
                CHANNEL_REMINDERS,
                context.getString(R.string.channel_reminders_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.channel_reminders_desc)
                enableVibration(true)
                setSound(notifSound, audioAttributesNotif)
                setShowBadge(true)
            }

            // 2. Alarms Channel
            val alarmChannel = NotificationChannel(
                CHANNEL_ALARMS,
                context.getString(R.string.channel_alarms_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.channel_alarms_desc)
                enableVibration(true)
                setSound(alarmSound, audioAttributesAlarm)
                setShowBadge(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }

            // 3. Timers Channel
            val timerChannel = NotificationChannel(
                CHANNEL_TIMERS,
                context.getString(R.string.channel_timers_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.channel_timers_desc)
                enableVibration(true)
                setSound(alarmSound, audioAttributesAlarm)
                setShowBadge(true)
            }

            notificationManager.createNotificationChannel(reminderChannel)
            notificationManager.createNotificationChannel(alarmChannel)
            notificationManager.createNotificationChannel(timerChannel)
        }
    }

    fun showReminderNotification(reminder: Reminder) {
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_NAV_TARGET", "reminders")
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            reminder.id.hashCode(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val doneIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_MARK_DONE
            putExtra(AlarmReceiver.EXTRA_ITEM_ID, reminder.id)
        }
        val donePendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.id.hashCode() + 1,
            doneIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_SNOOZE
            putExtra(AlarmReceiver.EXTRA_ITEM_ID, reminder.id)
            putExtra(AlarmReceiver.EXTRA_ITEM_TITLE, reminder.title)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.id.hashCode() + 2,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val contentText = buildString {
            if (reminder.notes.isNotBlank()) append(reminder.notes)
            if (reminder.sendSms && reminder.smsRecipientPhone.isNotBlank()) {
                if (isNotEmpty()) append(" • ")
                append("SMS sent to ${reminder.smsRecipientPhone}")
            }
            if (reminder.sendEmail && reminder.emailRecipient.isNotBlank()) {
                if (isNotEmpty()) append(" • ")
                append("Email alert: ${reminder.emailRecipient}")
            }
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("⏰ Reminder: ${reminder.title}")
            .setContentText(contentText.ifBlank { "VoxMind scheduled reminder" })
            .setStyle(NotificationCompat.BigTextStyle().bigText(
                if (reminder.notes.isNotBlank()) "${reminder.title}\n\n${reminder.notes}" else reminder.title
            ))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(openPendingIntent)
            .addAction(android.R.drawable.checkbox_on_background, "Done", donePendingIntent)
            .addAction(android.R.drawable.ic_menu_recent_history, "Snooze 10m", snoozePendingIntent)

        if (reminder.sendEmail && reminder.emailRecipient.isNotBlank()) {
            val emailIntent = EmailHelper.createEmailIntent(
                reminder.emailRecipient,
                "⏰ [VoxMind Reminder] ${reminder.title}",
                if (reminder.notes.isNotBlank()) "${reminder.title}\n\n${reminder.notes}" else reminder.title
            )
            val emailPendingIntent = PendingIntent.getActivity(
                context,
                reminder.id.hashCode() + 3,
                emailIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(android.R.drawable.ic_dialog_email, "Send Email", emailPendingIntent)
        }

        notificationManager.notify(reminder.id.hashCode(), builder.build())
    }

    fun showAlarmNotification(alarm: AlarmItem) {
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_NAV_TARGET", "alarms")
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            alarm.id.hashCode(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_DISMISS
            putExtra(AlarmReceiver.EXTRA_ITEM_ID, alarm.id)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id.hashCode() + 1,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val body = if (alarm.attachedReminderNotes.isNotBlank()) {
            "Attached Task: ${alarm.attachedReminderNotes}"
        } else {
            "VoxMind Alarm Ringing"
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ALARMS)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("🚨 Alarm: ${alarm.label} (${alarm.getFormattedTime()})")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText("${alarm.label}\n\n$body"))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .setContentIntent(openPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Dismiss", dismissPendingIntent)

        notificationManager.notify(alarm.id.hashCode(), builder.build())
    }

    fun showTimerFinishedNotification(timer: TimerItem) {
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_NAV_TARGET", "timers")
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            timer.id.hashCode(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_DISMISS
            putExtra(AlarmReceiver.EXTRA_ITEM_ID, timer.id)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            timer.id.hashCode() + 1,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val content = if (timer.attachedReminder.isNotBlank()) {
            "Timer Done! Attached reminder: ${timer.attachedReminder}"
        } else {
            "Timer Done!"
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_TIMERS)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("⏳ Timer Finished: ${timer.title}")
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(openPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Dismiss", dismissPendingIntent)

        notificationManager.notify(timer.id.hashCode(), builder.build())
    }

    fun cancelNotification(id: Int) {
        notificationManager.cancel(id)
    }
}
