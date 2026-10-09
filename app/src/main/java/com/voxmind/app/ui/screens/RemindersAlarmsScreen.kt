package com.voxmind.app.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import com.voxmind.app.data.deepseek.DeepSeekClient
import com.voxmind.app.data.models.AlarmItem
import com.voxmind.app.data.models.Priority
import com.voxmind.app.data.models.Reminder
import com.voxmind.app.data.models.TimerItem
import com.voxmind.app.data.repository.SettingsRepository
import com.voxmind.app.data.repository.VoxMindRepository
import com.voxmind.app.ui.components.AlarmCard
import com.voxmind.app.ui.components.ReminderCard
import com.voxmind.app.ui.components.TimerCard
import com.voxmind.app.ui.theme.AmberWarning
import com.voxmind.app.ui.theme.CyanAccent
import com.voxmind.app.ui.theme.EmeraldSuccess
import com.voxmind.app.ui.theme.IndigoPrimary
import com.voxmind.app.ui.theme.Slate800
import com.voxmind.app.ui.theme.Slate900
import com.voxmind.app.ui.theme.Slate950
import com.voxmind.app.util.AlarmScheduler
import com.voxmind.app.util.EmailHelper
import com.voxmind.app.util.SmsHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersAlarmsScreen(
    repository: VoxMindRepository,
    deepSeekClient: DeepSeekClient,
    settingsRepo: SettingsRepository,
    alarmScheduler: AlarmScheduler,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val reminders by repository.reminders.collectAsState()
    val alarms by repository.alarms.collectAsState()
    val timers by repository.timers.collectAsState()

    val defaultPhone by settingsRepo.defaultPhone.collectAsState()
    val defaultEmail by settingsRepo.defaultEmail.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Reminders, 1: Alarms, 2: Timers

    var showCreateReminderDialog by remember { mutableStateOf(false) }
    var showCreateAlarmDialog by remember { mutableStateOf(false) }
    var showCreateTimerDialog by remember { mutableStateOf(false) }

    var aiReminderPrompt by remember { mutableStateOf("") }
    var isSchedulingAiReminder by remember { mutableStateOf(false) }

    // Live timer ticker for active running timers
    LaunchedEffect(timers) {
        while (true) {
            val runningTimers = timers.filter { it.isRunning && !it.isCompleted }
            if (runningTimers.isNotEmpty()) {
                runningTimers.forEach { t ->
                    if (t.remainingSeconds > 1) {
                        repository.updateTimerRemaining(
                            id = t.id,
                            remaining = t.remainingSeconds - 1,
                            isRunning = true,
                            isCompleted = false
                        )
                    } else {
                        // Timer completed!
                        repository.updateTimerRemaining(
                            id = t.id,
                            remaining = 0,
                            isRunning = false,
                            isCompleted = true
                        )
                        // Trigger completion alert via scheduler
                        alarmScheduler.scheduleTimer(t.copy(remainingSeconds = 0))
                    }
                }
            }
            delay(1000L)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Slate950,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    when (selectedTab) {
                        0 -> showCreateReminderDialog = true
                        1 -> showCreateAlarmDialog = true
                        2 -> showCreateTimerDialog = true
                    }
                },
                containerColor = when (selectedTab) {
                    0 -> IndigoPrimary
                    1 -> AmberWarning
                    else -> CyanAccent
                },
                contentColor = Color.Black
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Item")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Header tabs
            val tabs = listOf("⏰ Reminders", "🚨 Alarms", "⏳ Timers")
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Slate900,
                contentColor = IndigoPrimary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = when (selectedTab) {
                            0 -> IndigoPrimary
                            1 -> AmberWarning
                            else -> CyanAccent
                        }
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontSize = 13.sp,
                                color = if (selectedTab == index) Color.White else Color.Gray
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            when (selectedTab) {
                0 -> { // Reminders
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Slate900),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = AmberWarning,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "⚡ DeepSeek Auto-Scheduler",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = AmberWarning
                                    )
                                }
                                Text(
                                    text = "Auto-detect dates, times, SMS phone numbers and emails to schedule exact alarms.",
                                    fontSize = 11.sp,
                                    color = Color.LightGray
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = aiReminderPrompt,
                                        onValueChange = { aiReminderPrompt = it },
                                        placeholder = { Text("e.g. Text mom tomorrow at 10 AM...", fontSize = 12.sp, color = Color.Gray) },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedBorderColor = AmberWarning,
                                            unfocusedBorderColor = Color.DarkGray
                                        )
                                    )
                                    Button(
                                        onClick = {
                                            if (aiReminderPrompt.isNotBlank()) {
                                                scope.launch {
                                                    isSchedulingAiReminder = true
                                                    repository.autoExtractAndScheduleReminders(
                                                        deepSeekClient = deepSeekClient,
                                                        alarmScheduler = alarmScheduler,
                                                        defaultPhone = defaultPhone,
                                                        defaultEmail = defaultEmail,
                                                        defaultSmsEnabled = settingsRepo.defaultSmsEnabled.value,
                                                        defaultEmailEnabled = settingsRepo.defaultEmailEnabled.value,
                                                        rawText = aiReminderPrompt
                                                    ).fold(
                                                        onSuccess = { summary ->
                                                            if (summary.remindersCreated > 0) {
                                                                val smsInfo = if (summary.smsEnabledCount > 0) " (${summary.smsEnabledCount} SMS)" else ""
                                                                val emailInfo = if (summary.emailEnabledCount > 0) " (${summary.emailEnabledCount} Email)" else ""
                                                                Toast.makeText(context, "⚡ Auto-scheduled ${summary.remindersCreated} reminder(s)$smsInfo$emailInfo!", Toast.LENGTH_LONG).show()
                                                                aiReminderPrompt = ""
                                                            } else {
                                                                Toast.makeText(context, "No reminder detected in prompt", Toast.LENGTH_SHORT).show()
                                                            }
                                                        },
                                                        onFailure = { Toast.makeText(context, "Error: ${it.localizedMessage}", Toast.LENGTH_SHORT).show() }
                                                    )
                                                    isSchedulingAiReminder = false
                                                }
                                            }
                                        },
                                        enabled = aiReminderPrompt.isNotBlank() && !isSchedulingAiReminder,
                                        colors = ButtonDefaults.buttonColors(containerColor = AmberWarning)
                                    ) {
                                        if (isSchedulingAiReminder) {
                                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black, strokeWidth = 2.dp)
                                        } else {
                                            Text("Auto-Set", color = Color.Black, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }

                        if (reminders.isEmpty()) {
                            EmptyStateView(
                                icon = Icons.Default.Notifications,
                                title = "No Scheduled Reminders",
                                subtitle = "Type above or dictate in Transcribe to auto-set voice reminders"
                            )
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                            items(reminders, key = { it.id }) { reminder ->
                                ReminderCard(
                                    reminder = reminder,
                                    onToggleDone = { repository.toggleReminderCompletion(reminder.id) },
                                    onDelete = {
                                        alarmScheduler.cancelReminder(reminder.id)
                                        repository.deleteReminder(reminder.id)
                                    },
                                    onTriggerSms = {
                                        if (reminder.smsRecipientPhone.isNotBlank()) {
                                            SmsHelper.sendSmsReminder(
                                                context,
                                                reminder.smsRecipientPhone,
                                                "${reminder.title}: ${reminder.notes}"
                                            )
                                            Toast.makeText(context, "SMS Sent!", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    onTriggerEmail = {
                                        if (reminder.emailRecipient.isNotBlank()) {
                                            EmailHelper.openEmailClient(
                                                context,
                                                reminder.emailRecipient,
                                                "VoxMind Reminder: ${reminder.title}",
                                                reminder.notes
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            1 -> { // Alarms
                    if (alarms.isEmpty()) {
                        EmptyStateView(
                            icon = Icons.Default.Alarm,
                            title = "No Clock Alarms Set",
                            subtitle = "Create alarms with attached reminder notes"
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(alarms, key = { it.id }) { alarm ->
                                AlarmCard(
                                    alarm = alarm,
                                    onToggleEnabled = { enabled ->
                                        repository.toggleAlarm(alarm.id, enabled)
                                        if (enabled) {
                                            alarmScheduler.scheduleAlarm(alarm.copy(isEnabled = true))
                                        } else {
                                            alarmScheduler.cancelAlarm(alarm.id)
                                        }
                                    },
                                    onDelete = {
                                        alarmScheduler.cancelAlarm(alarm.id)
                                        repository.deleteAlarm(alarm.id)
                                    }
                                )
                            }
                        }
                    }
                }

                2 -> { // Timers
                    if (timers.isEmpty()) {
                        EmptyStateView(
                            icon = Icons.Default.Timer,
                            title = "No Timers Active",
                            subtitle = "Create countdown timers with attached task reminders"
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(timers, key = { it.id }) { timer ->
                                TimerCard(
                                    timer = timer,
                                    onTogglePlayPause = {
                                        val newState = !timer.isRunning
                                        repository.saveTimer(timer.copy(isRunning = newState))
                                        if (newState) {
                                            alarmScheduler.scheduleTimer(timer)
                                        } else {
                                            alarmScheduler.cancelTimer(timer.id)
                                        }
                                    },
                                    onReset = {
                                        alarmScheduler.cancelTimer(timer.id)
                                        repository.saveTimer(
                                            timer.copy(
                                                remainingSeconds = timer.totalSeconds,
                                                isRunning = false,
                                                isCompleted = false
                                            )
                                        )
                                    },
                                    onDelete = {
                                        alarmScheduler.cancelTimer(timer.id)
                                        repository.deleteTimer(timer.id)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // --- CREATE REMINDER DIALOG ---
    if (showCreateReminderDialog) {
        CreateReminderDialog(
            defaultPhone = defaultPhone,
            defaultEmail = defaultEmail,
            onDismiss = { showCreateReminderDialog = false },
            onSave = { reminder ->
                repository.saveReminder(reminder)
                alarmScheduler.scheduleReminder(reminder)
                showCreateReminderDialog = false
                Toast.makeText(context, "Reminder Scheduled!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // --- CREATE ALARM DIALOG ---
    if (showCreateAlarmDialog) {
        CreateAlarmDialog(
            onDismiss = { showCreateAlarmDialog = false },
            onSave = { alarm ->
                repository.saveAlarm(alarm)
                alarmScheduler.scheduleAlarm(alarm)
                showCreateAlarmDialog = false
                Toast.makeText(context, "Alarm set for ${alarm.getFormattedTime()}!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // --- CREATE TIMER DIALOG ---
    if (showCreateTimerDialog) {
        CreateTimerDialog(
            defaultPhone = defaultPhone,
            defaultEmail = defaultEmail,
            onDismiss = { showCreateTimerDialog = false },
            onSave = { timer ->
                repository.saveTimer(timer)
                alarmScheduler.scheduleTimer(timer)
                showCreateTimerDialog = false
                Toast.makeText(context, "Timer started!", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
private fun EmptyStateView(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.DarkGray,
                modifier = Modifier.size(54.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = title, color = Color.White, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = subtitle, color = Color.Gray, fontSize = 12.sp)
        }
    }
}

@Composable
fun CreateReminderDialog(
    defaultPhone: String,
    defaultEmail: String,
    onDismiss: () -> Unit,
    onSave: (Reminder) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf(Priority.MEDIUM) }

    val calendar = remember { Calendar.getInstance().apply { add(Calendar.HOUR_OF_DAY, 1) } }
    var dueTimestamp by remember { mutableLongStateOf(calendar.timeInMillis) }

    var sendSms by remember { mutableStateOf(defaultPhone.isNotBlank()) }
    var smsPhone by remember { mutableStateOf(defaultPhone) }

    var sendEmail by remember { mutableStateOf(defaultEmail.isNotBlank()) }
    var emailRecipient by remember { mutableStateOf(defaultEmail) }

    val dateFormat = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault())

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Slate900,
        title = { Text("⏰ New Reminder", color = Color.White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Reminder Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                // Date & Time Picker trigger
                OutlinedButton(
                    onClick = {
                        val currentCal = Calendar.getInstance().apply { timeInMillis = dueTimestamp }
                        DatePickerDialog(
                            context,
                            { _, year, month, dayOfMonth ->
                                currentCal.set(Calendar.YEAR, year)
                                currentCal.set(Calendar.MONTH, month)
                                currentCal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                TimePickerDialog(
                                    context,
                                    { _, hourOfDay, minute ->
                                        currentCal.set(Calendar.HOUR_OF_DAY, hourOfDay)
                                        currentCal.set(Calendar.MINUTE, minute)
                                        dueTimestamp = currentCal.timeInMillis
                                    },
                                    currentCal.get(Calendar.HOUR_OF_DAY),
                                    currentCal.get(Calendar.MINUTE),
                                    false
                                ).show()
                            },
                            currentCal.get(Calendar.YEAR),
                            currentCal.get(Calendar.MONTH),
                            currentCal.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Due: ${dateFormat.format(Date(dueTimestamp))}", color = CyanAccent)
                }

                // SMS Alert option
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = sendSms,
                        onCheckedChange = { sendSms = it },
                        colors = CheckboxDefaults.colors(checkedColor = IndigoPrimary)
                    )
                    Text("Send SMS Reminder", color = Color.White, fontSize = 13.sp)
                }

                if (sendSms) {
                    OutlinedTextField(
                        value = smsPhone,
                        onValueChange = { smsPhone = it },
                        label = { Text("Recipient Phone Number") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }

                // Email Alert option
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = sendEmail,
                        onCheckedChange = { sendEmail = it },
                        colors = CheckboxDefaults.colors(checkedColor = IndigoPrimary)
                    )
                    Text("Send Email Reminder", color = Color.White, fontSize = 13.sp)
                }

                if (sendEmail) {
                    OutlinedTextField(
                        value = emailRecipient,
                        onValueChange = { emailRecipient = it },
                        label = { Text("Recipient Email Address") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onSave(
                            Reminder(
                                title = title.trim(),
                                notes = notes.trim(),
                                dueTimestamp = dueTimestamp,
                                priority = priority,
                                sendSms = sendSms,
                                smsRecipientPhone = smsPhone.trim(),
                                sendEmail = sendEmail,
                                emailRecipient = emailRecipient.trim()
                            )
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary)
            ) {
                Text("Schedule", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.Gray)
            }
        }
    )
}

@Composable
fun CreateAlarmDialog(
    onDismiss: () -> Unit,
    onSave: (AlarmItem) -> Unit
) {
    val context = LocalContext.current
    var label by remember { mutableStateOf("Wake Up") }
    var attachedNotes by remember { mutableStateOf("") }
    var hour by remember { mutableIntStateOf(7) }
    var minute by remember { mutableIntStateOf(0) }
    var vibrate by remember { mutableStateOf(true) }

    val formattedTime = remember(hour, minute) {
        val h = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
        val amPm = if (hour < 12) "AM" else "PM"
        String.format("%d:%02d %s", h, minute, amPm)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Slate900,
        title = { Text("🚨 New Alarm", color = Color.White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Alarm Label") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                OutlinedTextField(
                    value = attachedNotes,
                    onValueChange = { attachedNotes = it },
                    label = { Text("Attached Reminder Note (shown when ringing)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                OutlinedButton(
                    onClick = {
                        TimePickerDialog(
                            context,
                            { _, h, m ->
                                hour = h
                                minute = m
                            },
                            hour,
                            minute,
                            false
                        ).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Alarm Time: $formattedTime", color = AmberWarning, fontSize = 16.sp)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = vibrate,
                        onCheckedChange = { vibrate = it },
                        colors = CheckboxDefaults.colors(checkedColor = AmberWarning)
                    )
                    Text("Vibrate on alarm", color = Color.White, fontSize = 13.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        AlarmItem(
                            hour = hour,
                            minute = minute,
                            label = label.trim(),
                            attachedReminderNotes = attachedNotes.trim(),
                            vibrate = vibrate
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = AmberWarning)
            ) {
                Text("Set Alarm", color = Color.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.Gray)
            }
        }
    )
}

@Composable
fun CreateTimerDialog(
    defaultPhone: String,
    defaultEmail: String,
    onDismiss: () -> Unit,
    onSave: (TimerItem) -> Unit
) {
    var title by remember { mutableStateOf("Quick Timer") }
    var attachedReminder by remember { mutableStateOf("") }
    var minutes by remember { mutableIntStateOf(10) }
    var seconds by remember { mutableIntStateOf(0) }

    var sendSms by remember { mutableStateOf(false) }
    var smsPhone by remember { mutableStateOf(defaultPhone) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Slate900,
        title = { Text("⏳ New Timer", color = Color.White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Timer Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                OutlinedTextField(
                    value = attachedReminder,
                    onValueChange = { attachedReminder = it },
                    label = { Text("Attached Reminder (fired on completion)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                // Quick presets
                Text("Duration Presets:", color = Color.Gray, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(1, 5, 10, 15, 25, 30).forEach { mins ->
                        OutlinedButton(
                            onClick = {
                                minutes = mins
                                seconds = 0
                            },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = if (minutes == mins) CyanAccent else Color.LightGray
                            )
                        ) {
                            Text("${mins}m", fontSize = 11.sp)
                        }
                    }
                }

                // SMS alert toggle
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = sendSms,
                        onCheckedChange = { sendSms = it },
                        colors = CheckboxDefaults.colors(checkedColor = CyanAccent)
                    )
                    Text("Send SMS alert when finished", color = Color.White, fontSize = 12.sp)
                }

                if (sendSms) {
                    OutlinedTextField(
                        value = smsPhone,
                        onValueChange = { smsPhone = it },
                        label = { Text("Recipient Phone") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val totalSec = (minutes * 60L) + seconds
                    if (totalSec > 0) {
                        onSave(
                            TimerItem(
                                title = title.trim(),
                                totalSeconds = totalSec,
                                remainingSeconds = totalSec,
                                attachedReminder = attachedReminder.trim(),
                                isRunning = true,
                                sendSmsOnCompletion = sendSms,
                                smsRecipient = smsPhone.trim()
                            )
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent)
            ) {
                Text("Start Timer", color = Color.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.Gray)
            }
        }
    )
}
