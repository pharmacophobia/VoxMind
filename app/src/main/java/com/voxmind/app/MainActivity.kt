package com.voxmind.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.voxmind.app.data.deepseek.DeepSeekClient
import com.voxmind.app.data.repository.SettingsRepository
import com.voxmind.app.data.repository.VoxMindRepository
import com.voxmind.app.notifications.NotificationHelper
import com.voxmind.app.speech.SpeechManager
import com.voxmind.app.ui.screens.MainScreen
import com.voxmind.app.ui.theme.VoxMindTheme
import com.voxmind.app.util.AlarmScheduler

class MainActivity : ComponentActivity() {

    private lateinit var repository: VoxMindRepository
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var notificationHelper: NotificationHelper
    private lateinit var alarmScheduler: AlarmScheduler
    private lateinit var speechManager: SpeechManager
    private lateinit var deepSeekClient: DeepSeekClient

    private val permissionRequestLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        // Permissions handled
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize core Singletons & Helpers
        repository = VoxMindRepository(this)
        settingsRepository = SettingsRepository(this)
        notificationHelper = NotificationHelper(this)
        alarmScheduler = AlarmScheduler(this)
        speechManager = SpeechManager(
            context = this,
            isMuteMicSoundsEnabled = { settingsRepository.muteMicSounds.value },
            isNoiseGateEnabled = { settingsRepository.noiseGateEnabled.value }
        )
        deepSeekClient = DeepSeekClient(
            apiKeyProvider = { settingsRepository.apiKey.value },
            modelProvider = { settingsRepository.model.value }
        )

        // Request initial critical runtime permissions
        checkAndRequestPermissions()

        val navTarget = intent?.getStringExtra("EXTRA_NAV_TARGET")
        val initialTab = when (navTarget) {
            "reminders", "alarms", "timers" -> 1
            "lists" -> 2
            else -> 0
        }

        setContent {
            VoxMindTheme {
                MainScreen(
                    speechManager = speechManager,
                    deepSeekClient = deepSeekClient,
                    repository = repository,
                    settingsRepo = settingsRepository,
                    alarmScheduler = alarmScheduler,
                    notificationHelper = notificationHelper,
                    initialTab = initialTab
                )
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.RECORD_AUDIO)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            permissionRequestLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        speechManager.destroy()
    }
}
