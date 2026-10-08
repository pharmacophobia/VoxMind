package com.voxmind.app.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.voxmind.app.MainActivity
import com.voxmind.app.data.models.AlarmItem
import com.voxmind.app.data.models.Reminder
import com.voxmind.app.data.models.TimerItem
import com.voxmind.app.receiver.AlarmReceiver
import java.util.Calendar

class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    companion object {
        private const val TAG = "VoxMind_AlarmScheduler"
    }

    fun canScheduleExact(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    // --- REMINDER SCHEDULING ---
    fun scheduleReminder(reminder: Reminder) {
        if (reminder.isCompleted || reminder.dueTimestamp <= System.currentTimeMillis()) {
            return
        }

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_TRIGGER_REMINDER
            putExtra(AlarmReceiver.EXTRA_ITEM_ID, reminder.id)
            putExtra(AlarmReceiver.EXTRA_ITEM_TITLE, reminder.title)
            putExtra(AlarmReceiver.EXTRA_ITEM_NOTES, reminder.notes)
            putExtra(AlarmReceiver.EXTRA_SEND_SMS, reminder.sendSms)
            putExtra(AlarmReceiver.EXTRA_PHONE, reminder.smsRecipientPhone)
            putExtra(AlarmReceiver.EXTRA_SEND_EMAIL, reminder.sendEmail)
            putExtra(AlarmReceiver.EXTRA_EMAIL, reminder.emailRecipient)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    reminder.dueTimestamp,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    reminder.dueTimestamp,
                    pendingIntent
                )
            }
            Log.i(TAG, "Scheduled reminder '${reminder.title}' for timestamp ${reminder.dueTimestamp}")
        } catch (e: SecurityException) {
            Log.w(TAG, "Exact alarm permission missing, falling back to inexact alarm: ${e.message}")
            alarmManager.set(AlarmManager.RTC_WAKEUP, reminder.dueTimestamp, pendingIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling reminder: ${e.message}", e)
        }
    }

    fun cancelReminder(reminderId: String) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_TRIGGER_REMINDER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.hashCode(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    // --- ALARM CLOCK SCHEDULING ---
    fun scheduleAlarm(alarm: AlarmItem) {
        if (!alarm.isEnabled) return

        val triggerTime = computeNextAlarmTimeMillis(alarm.hour, alarm.minute, alarm.repeatDays)

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_TRIGGER_ALARM
            putExtra(AlarmReceiver.EXTRA_ITEM_ID, alarm.id)
            putExtra(AlarmReceiver.EXTRA_ITEM_TITLE, alarm.label)
            putExtra(AlarmReceiver.EXTRA_ATTACHED_REMINDER, alarm.attachedReminderNotes)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Show in status bar as native alarm clock
        val showIntent = Intent(context, MainActivity::class.java)
        val showPendingIntent = PendingIntent.getActivity(
            context,
            alarm.id.hashCode() + 10,
            showIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerTime, showPendingIntent)
        try {
            alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
            Log.i(TAG, "Scheduled alarm clock '${alarm.label}' for $triggerTime")
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling alarm clock: ${e.message}", e)
        }
    }

    fun cancelAlarm(alarmId: String) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_TRIGGER_ALARM
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId.hashCode(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    // --- TIMER SCHEDULING ---
    fun scheduleTimer(timer: TimerItem) {
        if (timer.remainingSeconds <= 0) return

        val triggerTime = System.currentTimeMillis() + (timer.remainingSeconds * 1000L)

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_TRIGGER_TIMER
            putExtra(AlarmReceiver.EXTRA_ITEM_ID, timer.id)
            putExtra(AlarmReceiver.EXTRA_ITEM_TITLE, timer.title)
            putExtra(AlarmReceiver.EXTRA_ATTACHED_REMINDER, timer.attachedReminder)
            putExtra(AlarmReceiver.EXTRA_SEND_SMS, timer.sendSmsOnCompletion)
            putExtra(AlarmReceiver.EXTRA_PHONE, timer.smsRecipient)
            putExtra(AlarmReceiver.EXTRA_SEND_EMAIL, timer.sendEmailOnCompletion)
            putExtra(AlarmReceiver.EXTRA_EMAIL, timer.emailRecipient)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            timer.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }
            Log.i(TAG, "Scheduled timer '${timer.title}' for $triggerTime")
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling timer: ${e.message}", e)
        }
    }

    fun cancelTimer(timerId: String) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_TRIGGER_TIMER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            timerId.hashCode(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    private fun computeNextAlarmTimeMillis(hour: Int, minute: Int, repeatDays: Set<Int>): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (repeatDays.isEmpty()) {
            if (target.before(now)) {
                target.add(Calendar.DAY_OF_YEAR, 1)
            }
            return target.timeInMillis
        }

        // Handle specific repeat days (1=Mon ... 7=Sun in ISO, or Calendar.DAY_OF_WEEK: 1=Sun, 2=Mon... 7=Sat)
        // Let's normalize repeatDays: 1=Mon, 2=Tue, 3=Wed, 4=Thu, 5=Fri, 6=Sat, 7=Sun
        for (i in 0..7) {
            val candidate = (target.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, i)
            }
            val calDay = candidate.get(Calendar.DAY_OF_WEEK)
            val isoDay = when (calDay) {
                Calendar.MONDAY -> 1
                Calendar.TUESDAY -> 2
                Calendar.WEDNESDAY -> 3
                Calendar.THURSDAY -> 4
                Calendar.FRIDAY -> 5
                Calendar.SATURDAY -> 6
                Calendar.SUNDAY -> 7
                else -> 1
            }
            if (repeatDays.contains(isoDay)) {
                if (candidate.after(now)) {
                    return candidate.timeInMillis
                }
            }
        }

        // Fallback: 1 day ahead
        target.add(Calendar.DAY_OF_YEAR, 1)
        return target.timeInMillis
    }
}
