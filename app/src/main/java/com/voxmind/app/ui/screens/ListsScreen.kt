package com.voxmind.app.ui.screens

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
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voxmind.app.data.deepseek.DeepSeekClient
import com.voxmind.app.data.models.Checklist
import com.voxmind.app.data.repository.SettingsRepository
import com.voxmind.app.data.repository.VoxMindRepository
import com.voxmind.app.ui.components.ChecklistCard
import com.voxmind.app.ui.theme.CyanAccent
import com.voxmind.app.ui.theme.EmeraldSuccess
import com.voxmind.app.ui.theme.IndigoPrimary
import com.voxmind.app.ui.theme.Slate800
import com.voxmind.app.ui.theme.Slate900
import com.voxmind.app.ui.theme.Slate950
import kotlinx.coroutines.launch

@Composable
fun ListsScreen(
    repository: VoxMindRepository,
    deepSeekClient: DeepSeekClient,
    settingsRepo: SettingsRepository,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val checklists by repository.checklists.collectAsState()
    val transcriptionNotes by repository.transcriptionNotes.collectAsState()
    val autoSortEnabled by settingsRepo.autoSortEnabled.collectAsState()

    val unsortedNotesCount = transcriptionNotes.count { !it.isAutoSorted }

    var showCreateDialog by remember { mutableStateOf(false) }
    var isSorting by remember { mutableStateOf(false) }
    var lastSortStatus by remember { mutableStateOf<String?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Slate950,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = EmeraldSuccess,
                contentColor = Color.Black
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add List")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📋 Lists & Tasks",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White
                )
                Text(
                    text = "${checklists.size} lists • ${transcriptionNotes.size} voice notes",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // --- DEEPSEEK AUTO-ORGANIZER CARD ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Slate900),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = IndigoPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "DeepSeek Auto-Organizer",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                        }

                        if (autoSortEnabled) {
                            Box(
                                modifier = Modifier
                                    .background(EmeraldSuccess.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("Auto-Sort ON", color = EmeraldSuccess, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "DeepSeek continuously parses your writing & voice streams over time, routing distinct tasks, ideas, and items into relevant lists automatically.",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )

                    if (lastSortStatus != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = lastSortStatus ?: "",
                            color = CyanAccent,
                            fontSize = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                scope.launch {
                                    isSorting = true
                                    repository.autoSortAllWriting(deepSeekClient, forceAll = false).fold(
                                        onSuccess = { summary ->
                                            if (summary.totalItemsRouted == 0) {
                                                lastSortStatus = "All current writing is already categorized!"
                                                Toast.makeText(context, "All writings are already sorted!", Toast.LENGTH_SHORT).show()
                                            } else {
                                                lastSortStatus = "Sorted ${summary.totalItemsRouted} items into: ${summary.listsAffected.joinToString(", ")}"
                                                Toast.makeText(
                                                    context,
                                                    "DeepSeek sorted ${summary.totalItemsRouted} items across ${summary.listsAffected.size} lists!",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }
                                        },
                                        onFailure = { err ->
                                            lastSortStatus = "Error sorting: ${err.localizedMessage}"
                                            Toast.makeText(context, "Auto-sort error: ${err.localizedMessage}", Toast.LENGTH_LONG).show()
                                        }
                                    )
                                    isSorting = false
                                }
                            },
                            enabled = !isSorting,
                            colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                            modifier = Modifier.weight(1f)
                        ) {
                            if (isSorting) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Sorting...", color = Color.White, fontSize = 12.sp)
                            } else {
                                Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (unsortedNotesCount > 0) "Auto-Sort ($unsortedNotesCount new)" else "Auto-Sort Now",
                                    color = Color.White,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        if (transcriptionNotes.isNotEmpty()) {
                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        isSorting = true
                                        repository.autoSortAllWriting(deepSeekClient, forceAll = true).fold(
                                            onSuccess = { summary ->
                                                lastSortStatus = "Re-sorted ${summary.totalItemsRouted} items across ${summary.listsAffected.size} lists!"
                                                Toast.makeText(
                                                    context,
                                                    "DeepSeek re-sorted ${summary.totalItemsRouted} items!",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            },
                                            onFailure = { err ->
                                                lastSortStatus = "Error: ${err.localizedMessage}"
                                                Toast.makeText(context, "Error: ${err.localizedMessage}", Toast.LENGTH_LONG).show()
                                            }
                                        )
                                        isSorting = false
                                    }
                                },
                                enabled = !isSorting
                            ) {
                                Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp), tint = CyanAccent)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Re-sort All", fontSize = 11.sp, color = CyanAccent)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (checklists.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.FormatListBulleted,
                            contentDescription = null,
                            tint = Color.DarkGray,
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No Lists Created Yet",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Dictate freely in Transcribe tab and tap 'Auto-Sort', or tap + to create one",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(checklists, key = { it.id }) { checklist ->
                        ChecklistCard(
                            checklist = checklist,
                            onToggleItem = { itemId ->
                                repository.toggleTodoItem(checklist.id, itemId)
                            },
                            onAddItem = { text ->
                                repository.addTodoItem(checklist.id, text)
                            },
                            onDeleteItem = { itemId ->
                                repository.deleteTodoItem(checklist.id, itemId)
                            },
                            onDeleteList = {
                                repository.deleteChecklist(checklist.id)
                                Toast.makeText(context, "Deleted list '${checklist.title}'", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        var listTitle by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            containerColor = Slate900,
            title = { Text("New Task List", color = Color.White) },
            text = {
                OutlinedTextField(
                    value = listTitle,
                    onValueChange = { listTitle = it },
                    label = { Text("List Title (e.g. Groceries, Sprint Goals)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (listTitle.isNotBlank()) {
                            repository.saveChecklist(Checklist(title = listTitle.trim()))
                            showCreateDialog = false
                            Toast.makeText(context, "List created!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess)
                ) {
                    Text("Create", color = Color.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }
}
