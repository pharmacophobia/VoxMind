package com.voxmind.app.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voxmind.app.data.deepseek.DeepSeekClient
import com.voxmind.app.data.repository.SettingsRepository
import com.voxmind.app.data.repository.VoxMindRepository
import com.voxmind.app.notifications.NotificationHelper
import com.voxmind.app.speech.SpeechManager
import com.voxmind.app.ui.theme.CyanAccent
import com.voxmind.app.ui.theme.EmeraldSuccess
import com.voxmind.app.ui.theme.IndigoPrimary
import com.voxmind.app.ui.theme.Slate900
import com.voxmind.app.ui.theme.Slate950
import com.voxmind.app.util.AlarmScheduler

@Composable
fun MainScreen(
    speechManager: SpeechManager,
    deepSeekClient: DeepSeekClient,
    repository: VoxMindRepository,
    settingsRepo: SettingsRepository,
    alarmScheduler: AlarmScheduler,
    notificationHelper: NotificationHelper,
    initialTab: Int = 0
) {
    var selectedIndex by rememberSaveable { mutableIntStateOf(initialTab) }

    val navItems = listOf(
        NavigationItem("Transcribe", Icons.Default.Mic, IndigoPrimary),
        NavigationItem("Reminders", Icons.Default.Alarm, CyanAccent),
        NavigationItem("Lists", Icons.AutoMirrored.Filled.FormatListBulleted, EmeraldSuccess),
        NavigationItem("Settings", Icons.Default.Settings, Color.LightGray)
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Slate950,
        bottomBar = {
            NavigationBar(
                containerColor = Slate900,
                tonalElevation = 8.dp
            ) {
                navItems.forEachIndexed { index, item ->
                    val selected = selectedIndex == index
                    NavigationBarItem(
                        selected = selected,
                        onClick = { selectedIndex = index },
                        icon = {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.title,
                                modifier = Modifier.size(24.dp),
                                tint = if (selected) item.activeColor else Color.Gray
                            )
                        },
                        label = {
                            Text(
                                text = item.title,
                                fontSize = 11.sp,
                                color = if (selected) item.activeColor else Color.Gray
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = item.activeColor.copy(alpha = 0.15f)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedIndex) {
                0 -> TranscriptionScreen(
                    speechManager = speechManager,
                    deepSeekClient = deepSeekClient,
                    repository = repository,
                    settingsRepo = settingsRepo,
                    alarmScheduler = alarmScheduler
                )
                1 -> RemindersAlarmsScreen(
                    repository = repository,
                    settingsRepo = settingsRepo,
                    alarmScheduler = alarmScheduler
                )
                2 -> ListsScreen(
                    repository = repository,
                    deepSeekClient = deepSeekClient,
                    settingsRepo = settingsRepo
                )
                3 -> SettingsScreen(
                    settingsRepo = settingsRepo,
                    deepSeekClient = deepSeekClient,
                    notificationHelper = notificationHelper
                )
            }
        }
    }
}

private data class NavigationItem(
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val activeColor: Color
)
