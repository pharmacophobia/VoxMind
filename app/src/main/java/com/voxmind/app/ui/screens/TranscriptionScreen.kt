package com.voxmind.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voxmind.app.data.deepseek.DeepSeekClient
import com.voxmind.app.data.models.ChatMessage
import com.voxmind.app.data.models.ExtractedReminderItem
import com.voxmind.app.data.models.Priority
import com.voxmind.app.data.models.Reminder
import com.voxmind.app.data.models.TranscriptionNote
import com.voxmind.app.data.repository.VoxMindRepository
import com.voxmind.app.speech.SpeechManager
import com.voxmind.app.ui.components.AudioVisualizer
import com.voxmind.app.ui.components.DeepSeekActionButtons
import com.voxmind.app.ui.theme.AmberWarning
import com.voxmind.app.ui.theme.CyanAccent
import com.voxmind.app.ui.theme.EmeraldSuccess
import com.voxmind.app.ui.theme.IndigoDark
import com.voxmind.app.ui.theme.IndigoPrimary
import com.voxmind.app.ui.theme.PurpleAccent
import com.voxmind.app.ui.theme.RoseError
import com.voxmind.app.ui.theme.Slate800
import com.voxmind.app.ui.theme.Slate900
import com.voxmind.app.ui.theme.Slate950
import com.voxmind.app.util.AlarmScheduler
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranscriptionScreen(
    speechManager: SpeechManager,
    deepSeekClient: DeepSeekClient,
    repository: VoxMindRepository,
    alarmScheduler: AlarmScheduler,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    val isListening by speechManager.isListening.collectAsState()
    val partialText by speechManager.partialTranscript.collectAsState()
    val accumulatedText by speechManager.accumulatedTranscript.collectAsState()
    val rmsLevel by speechManager.rmsDbLevel.collectAsState()
    val statusText by speechManager.statusText.collectAsState()

    var aiResultTitle by remember { mutableStateOf("") }
    var organizedThoughts by remember { mutableStateOf("") }
    var bulletPoints by remember { mutableStateOf<List<String>>(emptyList()) }
    var summaryText by remember { mutableStateOf("") }
    var extractedReminders by remember { mutableStateOf<List<ExtractedReminderItem>>(emptyList()) }

    var isAiLoading by remember { mutableStateOf(false) }
    var currentAiAction by remember { mutableStateOf<String?>(null) }
    var activeTab by remember { mutableIntStateOf(0) }

    // Chat modal state
    var showChatSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var chatMessages by remember { mutableStateOf<List<ChatMessage>>(emptyList()) }
    var chatInput by remember { mutableStateOf("") }
    var isChatLoading by remember { mutableStateOf(false) }

    val fullTranscript = if (partialText.isNotBlank()) {
        if (accumulatedText.isNotBlank()) "$accumulatedText $partialText" else partialText
    } else {
        accumulatedText
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Slate950)
            .padding(16.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- TOP HEADER ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Freeflow Transcription",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White
                )
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isListening) CyanAccent else Color.Gray
                )
            }

            IconButton(
                onClick = { showChatSheet = true },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Slate800)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Chat,
                    contentDescription = "Chat with DeepSeek",
                    tint = IndigoPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // --- LIVE AUDIO WAVEFORM VISUALIZER ---
        AudioVisualizer(
            isListening = isListening,
            rmsLevel = rmsLevel,
            modifier = Modifier.padding(vertical = 4.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        // --- BIG GLOWING MIC BUTTON ---
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(vertical = 6.dp)
        ) {
            if (isListening) {
                // Pulsing outer glow ring
                Box(
                    modifier = Modifier
                        .size(86.dp)
                        .clip(CircleShape)
                        .background(IndigoPrimary.copy(alpha = 0.25f))
                )
            }

            IconButton(
                onClick = {
                    if (isListening) {
                        speechManager.stopListening()
                    } else {
                        speechManager.startListening()
                    }
                },
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(if (isListening) RoseError else IndigoPrimary)
            ) {
                Icon(
                    imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                    contentDescription = if (isListening) "Stop Mic" else "Start Mic",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // --- TRANSCRIPTION CARD ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Slate900),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Live Spoken Stream",
                        style = MaterialTheme.typography.labelMedium,
                        color = CyanAccent,
                        fontWeight = FontWeight.Bold
                    )

                    Row {
                        IconButton(
                            onClick = {
                                if (fullTranscript.isNotBlank()) {
                                    clipboardManager.setText(AnnotatedString(fullTranscript))
                                    Toast.makeText(context, "Copied transcript to clipboard", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy text",
                                tint = Color.LightGray,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        IconButton(
                            onClick = { speechManager.clearTranscript() },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear text",
                                tint = Color.Gray,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = fullTranscript,
                    onValueChange = { speechManager.setTranscript(it) },
                    placeholder = {
                        Text(
                            text = "Tap the microphone above and speak your thoughts freely, or type here...",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Slate950,
                        unfocusedContainerColor = Slate950,
                        focusedBorderColor = IndigoPrimary,
                        unfocusedBorderColor = Color.DarkGray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Save Note action
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    ElevatedButton(
                        onClick = {
                            if (fullTranscript.isNotBlank()) {
                                val note = TranscriptionNote(
                                    title = if (aiResultTitle.isNotBlank()) aiResultTitle else "Voice Note ${System.currentTimeMillis() % 10000}",
                                    rawTranscript = fullTranscript,
                                    organizedThoughts = organizedThoughts,
                                    bulletPoints = bulletPoints,
                                    summary = summaryText,
                                    extractedReminders = extractedReminders
                                )
                                repository.saveTranscriptionNote(note)
                                Toast.makeText(context, "Saved to Notes", Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = fullTranscript.isNotBlank(),
                        colors = ButtonDefaults.elevatedButtonColors(
                            containerColor = Slate800,
                            contentColor = EmeraldSuccess
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = "Save",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Note", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // --- DEEPSEEK ACTION BUTTONS ---
        Text(
            text = "DEEPSEEK AI PROCESSING",
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray,
            modifier = Modifier.align(Alignment.Start)
        )

        DeepSeekActionButtons(
            enabled = fullTranscript.isNotBlank(),
            isLoading = isAiLoading,
            currentAction = currentAiAction,
            onOrganizeThoughts = {
                scope.launch {
                    isAiLoading = true
                    currentAiAction = "organize"
                    deepSeekClient.organizeThoughts(fullTranscript).fold(
                        onSuccess = {
                            organizedThoughts = it
                            activeTab = 0
                            Toast.makeText(context, "Thoughts organized!", Toast.LENGTH_SHORT).show()
                        },
                        onFailure = {
                            Toast.makeText(context, "AI error: ${it.localizedMessage}", Toast.LENGTH_LONG).show()
                        }
                    )
                    isAiLoading = false
                    currentAiAction = null
                }
            },
            onGenerateBullets = {
                scope.launch {
                    isAiLoading = true
                    currentAiAction = "bullets"
                    deepSeekClient.generateBulletPoints(fullTranscript).fold(
                        onSuccess = {
                            bulletPoints = it
                            activeTab = 1
                            Toast.makeText(context, "Bullet points generated!", Toast.LENGTH_SHORT).show()
                        },
                        onFailure = {
                            Toast.makeText(context, "AI error: ${it.localizedMessage}", Toast.LENGTH_LONG).show()
                        }
                    )
                    isAiLoading = false
                    currentAiAction = null
                }
            },
            onSummarize = {
                scope.launch {
                    isAiLoading = true
                    currentAiAction = "summary"
                    deepSeekClient.summarize(fullTranscript).fold(
                        onSuccess = {
                            summaryText = it
                            activeTab = 2
                            Toast.makeText(context, "Summary generated!", Toast.LENGTH_SHORT).show()
                        },
                        onFailure = {
                            Toast.makeText(context, "AI error: ${it.localizedMessage}", Toast.LENGTH_LONG).show()
                        }
                    )
                    isAiLoading = false
                    currentAiAction = null
                }
            },
            onExtractReminders = {
                scope.launch {
                    isAiLoading = true
                    currentAiAction = "extract"
                    deepSeekClient.extractReminders(fullTranscript).fold(
                        onSuccess = {
                            extractedReminders = it
                            activeTab = 3
                            Toast.makeText(context, "Found ${it.size} reminder(s)!", Toast.LENGTH_SHORT).show()
                        },
                        onFailure = {
                            Toast.makeText(context, "AI error: ${it.localizedMessage}", Toast.LENGTH_LONG).show()
                        }
                    )
                    isAiLoading = false
                    currentAiAction = null
                }
            }
        )

        Spacer(modifier = Modifier.height(14.dp))

        // --- AI RESULTS SECTION ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Slate900),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                val tabs = listOf("🧠 Thoughts", "📋 Bullets", "📝 Summary", "⏰ Reminders")

                TabRow(
                    selectedTabIndex = activeTab,
                    containerColor = Slate900,
                    contentColor = IndigoPrimary,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[activeTab]),
                            color = IndigoPrimary
                        )
                    }
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = activeTab == index,
                            onClick = { activeTab = index },
                            text = {
                                Text(
                                    text = title,
                                    fontSize = 12.sp,
                                    fontWeight = if (activeTab == index) FontWeight.Bold else FontWeight.Normal,
                                    color = if (activeTab == index) IndigoPrimary else Color.Gray
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                when (activeTab) {
                    0 -> { // Organized Thoughts
                        if (organizedThoughts.isBlank()) {
                            Text(
                                text = "Tap '🧠 Organize Thoughts' to structure your spoken ideas.",
                                color = Color.Gray,
                                fontSize = 13.sp
                            )
                        } else {
                            Text(
                                text = organizedThoughts,
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    1 -> { // Bullet Points
                        if (bulletPoints.isEmpty()) {
                            Text(
                                text = "Tap '📋 Bullet Points' to extract concise action items.",
                                color = Color.Gray,
                                fontSize = 13.sp
                            )
                        } else {
                            Column {
                                bulletPoints.forEach { point ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 3.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Text(text = "• ", color = EmeraldSuccess, fontWeight = FontWeight.Bold)
                                        Text(text = point, color = Color.White, fontSize = 14.sp)
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Convert to Checklist button!
                                Button(
                                    onClick = {
                                        repository.createChecklistFromBullets(
                                            title = "Voice Tasks (${bulletPoints.size})",
                                            bullets = bulletPoints
                                        )
                                        Toast.makeText(context, "Created new Checklist in Tasks tab!", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = Color.Black)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Save as Interactive Checklist", color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    2 -> { // Summary
                        if (summaryText.isBlank()) {
                            Text(
                                text = "Tap '📝 Summarize' to get an executive summary.",
                                color = Color.Gray,
                                fontSize = 13.sp
                            )
                        } else {
                            Text(
                                text = summaryText,
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    3 -> { // Extracted Reminders
                        if (extractedReminders.isEmpty()) {
                            Text(
                                text = "Tap '⏰ Extract Reminders' to detect dates, times, and alarms from speech.",
                                color = Color.Gray,
                                fontSize = 13.sp
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                extractedReminders.forEach { item ->
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = Slate800),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = item.taskTitle,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    color = Color.White
                                                )
                                                Text(
                                                    text = "When: ${item.detectedDateOrTime}",
                                                    color = AmberWarning,
                                                    fontSize = 12.sp
                                                )
                                                if (item.notes.isNotBlank()) {
                                                    Text(
                                                        text = item.notes,
                                                        color = Color.LightGray,
                                                        fontSize = 11.sp
                                                    )
                                                }
                                            }

                                            FilledTonalButton(
                                                onClick = {
                                                    // Schedule reminder in 1 hour by default if relative, or at due timestamp
                                                    val due = System.currentTimeMillis() + (60 * 60 * 1000L)
                                                    val reminder = Reminder(
                                                        title = item.taskTitle,
                                                        notes = "Spoken note: ${item.notes} (${item.detectedDateOrTime})",
                                                        dueTimestamp = due,
                                                        priority = Priority.HIGH
                                                    )
                                                    repository.saveReminder(reminder)
                                                    alarmScheduler.scheduleReminder(reminder)
                                                    Toast.makeText(context, "Scheduled reminder for '${item.taskTitle}'!", Toast.LENGTH_SHORT).show()
                                                },
                                                colors = ButtonDefaults.filledTonalButtonColors(
                                                    containerColor = AmberWarning.copy(alpha = 0.2f),
                                                    contentColor = AmberWarning
                                                )
                                            ) {
                                                Text("Add Reminder", fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- DEEPSEEK CHAT MODAL BOTTOM SHEET ---
    if (showChatSheet) {
        ModalBottomSheet(
            onDismissRequest = { showChatSheet = false },
            sheetState = sheetState,
            containerColor = Slate900
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "💬 DeepSeek Voice Brain Chat",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                Text(
                    text = "Ask questions or refine your transcribed thoughts with DeepSeek AI",
                    fontSize = 12.sp,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Chat Messages Container
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .background(Slate950, RoundedCornerShape(10.dp))
                        .padding(8.dp)
                ) {
                    val chatScroll = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(chatScroll)
                    ) {
                        if (chatMessages.isEmpty()) {
                            Text(
                                text = "Chat is empty. Try: 'Draft an email from this transcript', 'Expand on idea #1', or 'Make this more concise'.",
                                color = Color.Gray,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(8.dp)
                            )
                        }

                        chatMessages.forEach { msg ->
                            val isUser = msg.role == "user"
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isUser) IndigoDark else Slate800)
                                        .padding(8.dp)
                                ) {
                                    Text(
                                        text = msg.content,
                                        color = Color.White,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }

                        if (isChatLoading) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = IndigoPrimary,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("DeepSeek thinking...", color = Color.Gray, fontSize = 12.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Input bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = chatInput,
                        onValueChange = { chatInput = it },
                        placeholder = { Text("Ask DeepSeek...", fontSize = 13.sp, color = Color.Gray) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Slate950,
                            unfocusedContainerColor = Slate950,
                            focusedBorderColor = IndigoPrimary,
                            unfocusedBorderColor = Color.DarkGray,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            if (chatInput.isNotBlank() && !isChatLoading) {
                                val userMsg = ChatMessage(role = "user", content = chatInput)
                                val updated = chatMessages + userMsg
                                chatMessages = updated
                                val promptText = chatInput
                                chatInput = ""
                                isChatLoading = true

                                scope.launch {
                                    // Include transcript context if available
                                    val contextList = if (fullTranscript.isNotBlank()) {
                                        listOf(ChatMessage(role = "user", content = "Context transcript:\n$fullTranscript")) + updated
                                    } else updated

                                    deepSeekClient.chat(contextList).fold(
                                        onSuccess = { reply ->
                                            chatMessages = updated + ChatMessage(role = "assistant", content = reply)
                                        },
                                        onFailure = { err ->
                                            chatMessages = updated + ChatMessage(role = "assistant", content = "Error: ${err.localizedMessage}")
                                        }
                                    )
                                    isChatLoading = false
                                }
                            }
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(IndigoPrimary)
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.White)
                    }
                }
            }
        }
    }
}
