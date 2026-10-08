package com.voxmind.app.data.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.voxmind.app.data.models.AlarmItem
import com.voxmind.app.data.models.Checklist
import com.voxmind.app.data.models.Reminder
import com.voxmind.app.data.models.TimerItem
import com.voxmind.app.data.models.TodoItem
import com.voxmind.app.data.models.TranscriptionNote
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.util.UUID

class VoxMindRepository(private val context: Context) {

    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    private val scope = CoroutineScope(Dispatchers.IO)
    private val mutex = Mutex()

    private val remindersFile = File(context.filesDir, "reminders.json")
    private val alarmsFile = File(context.filesDir, "alarms.json")
    private val timersFile = File(context.filesDir, "timers.json")
    private val checklistsFile = File(context.filesDir, "checklists.json")
    private val notesFile = File(context.filesDir, "transcription_notes.json")

    private val _reminders = MutableStateFlow<List<Reminder>>(emptyList())
    val reminders: StateFlow<List<Reminder>> = _reminders.asStateFlow()

    private val _alarms = MutableStateFlow<List<AlarmItem>>(emptyList())
    val alarms: StateFlow<List<AlarmItem>> = _alarms.asStateFlow()

    private val _timers = MutableStateFlow<List<TimerItem>>(emptyList())
    val timers: StateFlow<List<TimerItem>> = _timers.asStateFlow()

    private val _checklists = MutableStateFlow<List<Checklist>>(emptyList())
    val checklists: StateFlow<List<Checklist>> = _checklists.asStateFlow()

    private val _transcriptionNotes = MutableStateFlow<List<TranscriptionNote>>(emptyList())
    val transcriptionNotes: StateFlow<List<TranscriptionNote>> = _transcriptionNotes.asStateFlow()

    init {
        loadAllData()
    }

    private fun loadAllData() {
        try {
            if (remindersFile.exists()) {
                val json = remindersFile.readText()
                val type = object : TypeToken<List<Reminder>>() {}.type
                _reminders.value = gson.fromJson(json, type) ?: emptyList()
            }
            if (alarmsFile.exists()) {
                val json = alarmsFile.readText()
                val type = object : TypeToken<List<AlarmItem>>() {}.type
                _alarms.value = gson.fromJson(json, type) ?: emptyList()
            }
            if (timersFile.exists()) {
                val json = timersFile.readText()
                val type = object : TypeToken<List<TimerItem>>() {}.type
                _timers.value = gson.fromJson(json, type) ?: emptyList()
            }
            if (checklistsFile.exists()) {
                val json = checklistsFile.readText()
                val type = object : TypeToken<List<Checklist>>() {}.type
                _checklists.value = gson.fromJson(json, type) ?: emptyList()
            }
            if (notesFile.exists()) {
                val json = notesFile.readText()
                val type = object : TypeToken<List<TranscriptionNote>>() {}.type
                _transcriptionNotes.value = gson.fromJson(json, type) ?: emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // --- REMINDERS ---
    fun saveReminder(reminder: Reminder) {
        scope.launch {
            mutex.withLock {
                val current = _reminders.value.toMutableList()
                val index = current.indexOfFirst { it.id == reminder.id }
                if (index != -1) {
                    current[index] = reminder
                } else {
                    current.add(0, reminder)
                }
                _reminders.value = current
                persist(remindersFile, current)
            }
        }
    }

    fun toggleReminderCompletion(id: String) {
        scope.launch {
            mutex.withLock {
                val current = _reminders.value.map {
                    if (it.id == id) it.copy(isCompleted = !it.isCompleted) else it
                }
                _reminders.value = current
                persist(remindersFile, current)
            }
        }
    }

    fun deleteReminder(id: String) {
        scope.launch {
            mutex.withLock {
                val current = _reminders.value.filter { it.id != id }
                _reminders.value = current
                persist(remindersFile, current)
            }
        }
    }

    // --- ALARMS ---
    fun saveAlarm(alarm: AlarmItem) {
        scope.launch {
            mutex.withLock {
                val current = _alarms.value.toMutableList()
                val index = current.indexOfFirst { it.id == alarm.id }
                if (index != -1) {
                    current[index] = alarm
                } else {
                    current.add(0, alarm)
                }
                _alarms.value = current
                persist(alarmsFile, current)
            }
        }
    }

    fun toggleAlarm(id: String, isEnabled: Boolean) {
        scope.launch {
            mutex.withLock {
                val current = _alarms.value.map {
                    if (it.id == id) it.copy(isEnabled = isEnabled) else it
                }
                _alarms.value = current
                persist(alarmsFile, current)
            }
        }
    }

    fun deleteAlarm(id: String) {
        scope.launch {
            mutex.withLock {
                val current = _alarms.value.filter { it.id != id }
                _alarms.value = current
                persist(alarmsFile, current)
            }
        }
    }

    // --- TIMERS ---
    fun saveTimer(timer: TimerItem) {
        scope.launch {
            mutex.withLock {
                val current = _timers.value.toMutableList()
                val index = current.indexOfFirst { it.id == timer.id }
                if (index != -1) {
                    current[index] = timer
                } else {
                    current.add(0, timer)
                }
                _timers.value = current
                persist(timersFile, current)
            }
        }
    }

    fun updateTimerRemaining(id: String, remaining: Long, isRunning: Boolean, isCompleted: Boolean) {
        scope.launch {
            mutex.withLock {
                val current = _timers.value.map {
                    if (it.id == id) it.copy(remainingSeconds = remaining, isRunning = isRunning, isCompleted = isCompleted) else it
                }
                _timers.value = current
                persist(timersFile, current)
            }
        }
    }

    fun deleteTimer(id: String) {
        scope.launch {
            mutex.withLock {
                val current = _timers.value.filter { it.id != id }
                _timers.value = current
                persist(timersFile, current)
            }
        }
    }

    // --- CHECKLISTS & TASKS ---
    fun saveChecklist(checklist: Checklist) {
        scope.launch {
            mutex.withLock {
                val current = _checklists.value.toMutableList()
                val index = current.indexOfFirst { it.id == checklist.id }
                if (index != -1) {
                    current[index] = checklist
                } else {
                    current.add(0, checklist)
                }
                _checklists.value = current
                persist(checklistsFile, current)
            }
        }
    }

    fun toggleTodoItem(listId: String, itemId: String) {
        scope.launch {
            mutex.withLock {
                val current = _checklists.value.map { list ->
                    if (list.id == listId) {
                        val updatedItems = list.items.map { item ->
                            if (item.id == itemId) item.copy(isDone = !item.isDone) else item
                        }
                        list.copy(items = updatedItems, updatedAt = System.currentTimeMillis())
                    } else list
                }
                _checklists.value = current
                persist(checklistsFile, current)
            }
        }
    }

    fun addTodoItem(listId: String, text: String) {
        if (text.isBlank()) return
        scope.launch {
            mutex.withLock {
                val current = _checklists.value.map { list ->
                    if (list.id == listId) {
                        val updatedItems = list.items + TodoItem(text = text.trim())
                        list.copy(items = updatedItems, updatedAt = System.currentTimeMillis())
                    } else list
                }
                _checklists.value = current
                persist(checklistsFile, current)
            }
        }
    }

    fun deleteTodoItem(listId: String, itemId: String) {
        scope.launch {
            mutex.withLock {
                val current = _checklists.value.map { list ->
                    if (list.id == listId) {
                        list.copy(items = list.items.filter { it.id != itemId }, updatedAt = System.currentTimeMillis())
                    } else list
                }
                _checklists.value = current
                persist(checklistsFile, current)
            }
        }
    }

    fun deleteChecklist(id: String) {
        scope.launch {
            mutex.withLock {
                val current = _checklists.value.filter { it.id != id }
                _checklists.value = current
                persist(checklistsFile, current)
            }
        }
    }

    fun createChecklistFromBullets(title: String, bullets: List<String>): Checklist {
        val cleanItems = bullets.map { raw ->
            val cleaned = raw.trim()
                .removePrefix("-")
                .removePrefix("*")
                .removePrefix("•")
                .replace(Regex("^\\d+\\.\\s*"), "")
                .trim()
            TodoItem(text = cleaned)
        }.filter { it.text.isNotBlank() }

        val newChecklist = Checklist(
            id = UUID.randomUUID().toString(),
            title = if (title.isBlank()) "Tasks from Voice Note" else title,
            items = cleanItems
        )
        saveChecklist(newChecklist)
        return newChecklist
    }

    // --- TRANSCRIPTION NOTES ---
    fun saveTranscriptionNote(note: TranscriptionNote) {
        scope.launch {
            mutex.withLock {
                val current = _transcriptionNotes.value.toMutableList()
                val index = current.indexOfFirst { it.id == note.id }
                if (index != -1) {
                    current[index] = note
                } else {
                    current.add(0, note)
                }
                _transcriptionNotes.value = current
                persist(notesFile, current)
            }
        }
    }

    fun deleteTranscriptionNote(id: String) {
        scope.launch {
            mutex.withLock {
                val current = _transcriptionNotes.value.filter { it.id != id }
                _transcriptionNotes.value = current
                persist(notesFile, current)
            }
        }
    }

    private fun <T> persist(file: File, data: List<T>) {
        try {
            val json = gson.toJson(data)
            file.writeText(json)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
