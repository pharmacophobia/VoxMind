package com.voxmind.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.voxmind.app.data.repository.VoxMindRepository
import com.voxmind.app.util.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            val repository = VoxMindRepository(context)
            val scheduler = AlarmScheduler(context)

            CoroutineScope(Dispatchers.IO).launch {
                val reminders = repository.reminders.value
                val now = System.currentTimeMillis()
                for (reminder in reminders) {
                    if (!reminder.isCompleted && reminder.dueTimestamp > now) {
                        scheduler.scheduleReminder(reminder)
                    }
                }

                val alarms = repository.alarms.value
                for (alarm in alarms) {
                    if (alarm.isEnabled) {
                        scheduler.scheduleAlarm(alarm)
                    }
                }
            }
        }
    }
}
