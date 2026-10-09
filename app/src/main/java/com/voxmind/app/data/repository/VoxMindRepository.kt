package com.voxmind.app.data.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.voxmind.app.data.deepseek.DeepSeekClient
import com.voxmind.app.data.models.AlarmItem
import com.voxmind.app.data.models.AutoSortSummary
import com.voxmind.app.data.models.AutoSortedCategory
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
import kotlinx.coroutines.withContext
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

    suspend fun autoSortAllWriting(
        deepSeekClient: DeepSeekClient,
        forceAll: Boolean = false
    ): Result<AutoSortSummary> = withContext(Dispatchers.IO) {
        mutex.withLock {
            try {
                val allNotes = _transcriptionNotes.value
                val notesToSort = if (forceAll) allNotes else allNotes.filter { !it.isAutoSorted }
                if (notesToSort.isEmpty()) {
                    return@withLock Result.success(AutoSortSummary(0, emptyList(), 0))
                }

                val rawTexts = notesToSort.map { note ->
                    buildString {
                        append(note.title).append("\n")
                        if (note.rawTranscript.isNotBlank()) append(note.rawTranscript).append("\n")
                        if (note.organizedThoughts.isNotBlank()) append(note.organizedThoughts).append("\n")
                        if (note.bulletPoints.isNotEmpty()) append(note.bulletPoints.joinToString("\n")).append("\n")
                    }.trim()
                }.filter { it.isNotBlank() }

                if (rawTexts.isEmpty()) {
                    return@withLock Result.success(AutoSortSummary(0, emptyList(), 0))
                }

                val existingLists = _checklists.value
                val existingTitles = existingLists.map { it.title }

                val result = deepSeekClient.autoSortWritingIntoLists(rawTexts, existingTitles)
                if (result.isFailure) {
                    return@withLock Result.failure(result.exceptionOrNull() ?: Exception("Auto-sort failed"))
                }

                val categories = result.getOrNull() ?: emptyList()
                if (categories.isEmpty()) {
                    return@withLock Result.success(AutoSortSummary(0, emptyList(), 0))
                }

                var totalItemsRouted = 0
                var newListsCreated = 0
                val listsAffected = mutableListOf<String>()

                val updatedChecklists = _checklists.value.toMutableList()

                for (category in categories) {
                    val catTitle = category.listTitle.trim()
                    val existingIndex = updatedChecklists.indexOfFirst { it.title.equals(catTitle, ignoreCase = true) }

                    if (existingIndex != -1) {
                        val targetList = updatedChecklists[existingIndex]
                        val existingItemTexts = targetList.items.map { it.text.lowercase().trim() }.toSet()
                        val newItemsToAdd = category.items
                            .map { it.trim() }
                            .filter { it.isNotBlank() && !existingItemTexts.contains(it.lowercase()) }
                            .map { TodoItem(text = it) }

                        if (newItemsToAdd.isNotEmpty()) {
                            totalItemsRouted += newItemsToAdd.size
                            listsAffected.add(targetList.title)
                            updatedChecklists[existingIndex] = targetList.copy(
                                items = targetList.items + newItemsToAdd,
                                updatedAt = System.currentTimeMillis()
                            )
                        }
                    } else {
                        // Create new categorized list with distinctive color
                        val newItems = category.items
                            .map { it.trim() }
                            .filter { it.isNotBlank() }
                            .map { TodoItem(text = it) }

                        if (newItems.isNotEmpty()) {
                            val palette = listOf("#6366F1", "#10B981", "#F59E0B", "#06B6D4", "#A855F7", "#EC4899", "#3B82F6")
                            val chosenColor = palette[Math.abs(catTitle.hashCode()) % palette.size]
                            val newList = Checklist(
                                title = catTitle,
                                items = newItems,
                                colorHex = chosenColor
                            )
                            updatedChecklists.add(0, newList)
                            totalItemsRouted += newItems.size
                            newListsCreated++
                            listsAffected.add(catTitle)
                        }
                    }
                }

                _checklists.value = updatedChecklists
                persist(checklistsFile, updatedChecklists)

                // Mark processed notes as sorted
                val sortedNoteIds = notesToSort.map { it.id }.toSet()
                val updatedNotes = _transcriptionNotes.value.map { note ->
                    if (sortedNoteIds.contains(note.id)) {
                        note.copy(isAutoSorted = true, sortedListCategories = categories.map { it.listTitle })
                    } else note
                }
                _transcriptionNotes.value = updatedNotes
                persist(notesFile, updatedNotes)

                Result.success(
                    AutoSortSummary(
                        totalItemsRouted = totalItemsRouted,
                        listsAffected = listsAffected.distinct(),
                        newListsCreated = newListsCreated
                    )
                )
            } catch (e: Exception) {
                Result.failure(e)
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
