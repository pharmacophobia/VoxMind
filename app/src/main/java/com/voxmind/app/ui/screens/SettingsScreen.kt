package com.voxmind.app.ui.screens

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voxmind.app.data.deepseek.DeepSeekClient
import com.voxmind.app.data.models.AlarmItem
import com.voxmind.app.data.models.Reminder
import com.voxmind.app.data.repository.SettingsRepository
import com.voxmind.app.notifications.NotificationHelper
import com.voxmind.app.ui.theme.AmberWarning
import com.voxmind.app.ui.theme.CyanAccent
import com.voxmind.app.ui.theme.EmeraldSuccess
import com.voxmind.app.ui.theme.IndigoPrimary
import com.voxmind.app.ui.theme.RoseError
import com.voxmind.app.ui.theme.Slate800
import com.voxmind.app.ui.theme.Slate900
import com.voxmind.app.ui.theme.Slate950
import com.voxmind.app.util.SmsHelper
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    settingsRepo: SettingsRepository,
    deepSeekClient: DeepSeekClient,
    notificationHelper: NotificationHelper,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val apiKey by settingsRepo.apiKey.collectAsState()
    val model by settingsRepo.model.collectAsState()
    val defaultPhone by settingsRepo.defaultPhone.collectAsState()
    val defaultEmail by settingsRepo.defaultEmail.collectAsState()
    val continuousSpeech by settingsRepo.continuousSpeech.collectAsState()
    val muteMicSounds by settingsRepo.muteMicSounds.collectAsState()
    val noiseGateEnabled by settingsRepo.noiseGateEnabled.collectAsState()

    var inputKey by remember(apiKey) { mutableStateOf(apiKey) }
    var inputPhone by remember(defaultPhone) { mutableStateOf(defaultPhone) }
    var inputEmail by remember(defaultEmail) { mutableStateOf(defaultEmail) }
    var showApiKey by remember { mutableStateOf(false) }

    var isTestingConnection by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<String?>(null) }
    var hasSmsPerm by remember { mutableStateOf(SmsHelper.hasSmsPermission(context)) }

    val smsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasSmsPerm = isGranted
        if (isGranted) {
            Toast.makeText(context, "SMS Permission granted!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "SMS Permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Slate950)
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "⚙️ Settings & Integrations",
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White
        )

        // --- DEEPSEEK AI SECTION ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Slate900),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.SmartToy,
                        contentDescription = null,
                        tint = CyanAccent,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "DeepSeek AI Configuration",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                }

                Text(
                    text = "VoxMind uses DeepSeek API to organize thoughts, generate structured bullet points, and extract schedule dates.",
                    color = Color.Gray,
                    fontSize = 12.sp
                )

                OutlinedTextField(
                    value = inputKey,
                    onValueChange = { inputKey = it },
                    label = { Text("DeepSeek API Key") },
                    singleLine = true,
                    visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showApiKey = !showApiKey }) {
                            Icon(
                                imageVector = if (showApiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle visibility",
                                tint = Color.Gray
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = IndigoPrimary,
                        unfocusedBorderColor = Color.DarkGray
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            settingsRepo.setApiKey(inputKey)
                            Toast.makeText(context, "API Key saved!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save Key", color = Color.White)
                    }

                    OutlinedButton(
                        onClick = {
                            settingsRepo.setApiKey(inputKey)
                            isTestingConnection = true
                            testResult = null
                            scope.launch {
                                deepSeekClient.testConnection().fold(
                                    onSuccess = {
                                        testResult = "Success: Connected to DeepSeek API"
                                    },
                                    onFailure = {
                                        testResult = "Error: ${it.localizedMessage}"
                                    }
                                )
                                isTestingConnection = false
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isTestingConnection) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = CyanAccent,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Test API", color = CyanAccent)
                        }
                    }
                }

                if (testResult != null) {
                    val isSuccess = testResult?.startsWith("Success") == true
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(
                                if (isSuccess) EmeraldSuccess.copy(alpha = 0.15f) else RoseError.copy(alpha = 0.15f),
                                RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = if (isSuccess) Icons.Default.Check else Icons.Default.Error,
                            contentDescription = null,
                            tint = if (isSuccess) EmeraldSuccess else RoseError,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = testResult ?: "",
                            color = if (isSuccess) EmeraldSuccess else RoseError,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Model Selection
                Text("Select DeepSeek Model:", color = Color.LightGray, fontSize = 13.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = model == SettingsRepository.MODEL_CHAT,
                        onClick = { settingsRepo.setModel(SettingsRepository.MODEL_CHAT) },
                        label = { Text("deepseek-chat (Fast)", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = IndigoPrimary,
                            selectedLabelColor = Color.White
                        )
                    )

                    FilterChip(
                        selected = model == SettingsRepository.MODEL_REASONER,
                        onClick = { settingsRepo.setModel(SettingsRepository.MODEL_REASONER) },
                        label = { Text("deepseek-reasoner (Deep)", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AmberWarning,
                            selectedLabelColor = Color.Black
                        )
                    )
                }
            }
        }

        // --- DISPATCH INTEGRATIONS: SMS & EMAIL ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Slate900),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = null,
                        tint = EmeraldSuccess,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SMS & Email Dispatch Settings",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                }

                Text(
                    text = "Configure default numbers and addresses for auto-sending text messages and emails when reminders or timers fire.",
                    color = Color.Gray,
                    fontSize = 12.sp
                )

                OutlinedTextField(
                    value = inputPhone,
                    onValueChange = {
                        inputPhone = it
                        settingsRepo.setDefaultPhone(it)
                    },
                    label = { Text("Default Phone Number (for SMS reminders)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = EmeraldSuccess,
                        unfocusedBorderColor = Color.DarkGray
                    )
                )

                // SMS Permission Check
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("SMS Background Permission:", color = Color.LightGray, fontSize = 13.sp)
                        Text(
                            text = if (hasSmsPerm) "Granted (Ready to send texts)" else "Not Granted",
                            color = if (hasSmsPerm) EmeraldSuccess else RoseError,
                            fontSize = 11.sp
                        )
                    }

                    if (!hasSmsPerm) {
                        Button(
                            onClick = {
                                smsPermissionLauncher.launch(Manifest.permission.SEND_SMS)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess)
                        ) {
                            Text("Grant SMS", color = Color.Black, fontSize = 12.sp)
                        }
                    }
                }

                OutlinedTextField(
                    value = inputEmail,
                    onValueChange = {
                        inputEmail = it
                        settingsRepo.setDefaultEmail(it)
                    },
                    label = { Text("Default Email Address (for email reminders)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = IndigoPrimary,
                        unfocusedBorderColor = Color.DarkGray
                    )
                )
            }
        }

        // --- SPEECH RECOGNITION PREFERENCES ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Slate900),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "🎙️ Voice Dictation & Audio",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium
                )

                // 1. Continuous Dictation
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Continuous Voice Dictation", color = Color.White, fontSize = 14.sp)
                        Text(
                            text = "Keep microphone listening through pauses without stopping",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }

                    Switch(
                        checked = continuousSpeech,
                        onCheckedChange = { settingsRepo.setContinuousSpeech(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CyanAccent,
                            checkedTrackColor = CyanAccent.copy(alpha = 0.4f)
                        )
                    )
                }

                // 2. Silence Mic Noises / Beeps
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Mute Microphone Beeps & Earcons", color = Color.White, fontSize = 14.sp)
                        Text(
                            text = "Silences the loud system start/stop beeps and chime sounds during speech loops",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }

                    Switch(
                        checked = muteMicSounds,
                        onCheckedChange = { settingsRepo.setMuteMicSounds(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = EmeraldSuccess,
                            checkedTrackColor = EmeraldSuccess.copy(alpha = 0.4f)
                        )
                    )
                }

                // 3. Ambient Noise Gate
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Ambient Noise Gate & Filter", color = Color.White, fontSize = 14.sp)
                        Text(
                            text = "Filters out low-level room static, breathing, and background hiss",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }

                    Switch(
                        checked = noiseGateEnabled,
                        onCheckedChange = { settingsRepo.setNoiseGateEnabled(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = IndigoPrimary,
                            checkedTrackColor = IndigoPrimary.copy(alpha = 0.4f)
                        )
                    )
                }
            }
        }

        // --- DIAGNOSTICS & SYSTEM TEST ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Slate900),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = AmberWarning,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("System Alerts & Audio Tests", color = Color.White, style = MaterialTheme.typography.titleMedium)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            notificationHelper.showReminderNotification(
                                Reminder(
                                    title = "Test Reminder Notification",
                                    notes = "Testing high-priority sound and vibration channel",
                                    dueTimestamp = System.currentTimeMillis()
                                )
                            )
                            Toast.makeText(context, "Test notification triggered!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Test Notification", fontSize = 11.sp, color = IndigoPrimary)
                    }

                    OutlinedButton(
                        onClick = {
                            notificationHelper.showAlarmNotification(
                                AlarmItem(
                                    hour = 12,
                                    minute = 0,
                                    label = "Test Alarm Ring",
                                    attachedReminderNotes = "Testing alarm channel and heads-up banner"
                                )
                            )
                            Toast.makeText(context, "Test alarm triggered!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Test Alarm", fontSize = 11.sp, color = AmberWarning)
                    }
                }
            }
        }

        // App info footer
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("VoxMind v1.0.0", color = Color.LightGray, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text("Native Voice AI • DeepSeek Intelligence • Exact Alarms", color = Color.Gray, fontSize = 12.sp)
        }
    }
}
