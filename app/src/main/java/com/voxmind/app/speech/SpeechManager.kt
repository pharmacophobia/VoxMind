package com.voxmind.app.speech

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class SpeechManager(
    private val context: Context,
    private val isMuteMicSoundsEnabled: () -> Boolean = { true },
    private val isNoiseGateEnabled: () -> Boolean = { true }
) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var speechRecognizer: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _partialTranscript = MutableStateFlow("")
    val partialTranscript: StateFlow<String> = _partialTranscript.asStateFlow()

    private val _accumulatedTranscript = MutableStateFlow("")
    val accumulatedTranscript: StateFlow<String> = _accumulatedTranscript.asStateFlow()

    private val _rmsDbLevel = MutableStateFlow(0f)
    val rmsDbLevel: StateFlow<Float> = _rmsDbLevel.asStateFlow()

    private val _statusText = MutableStateFlow("Ready to dictate")
    val statusText: StateFlow<String> = _statusText.asStateFlow()

    var isContinuousMode: Boolean = true
    private var isAudioMutedByApp: Boolean = false

    init {
        mainHandler.post {
            initRecognizer()
        }
    }

    private fun initRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _statusText.value = "Speech recognition service unavailable on this device"
            return
        }
        speechRecognizer?.destroy()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(createListener())
        }
    }

    /**
     * Temporarily silences system earcons, beeps, and start/stop audio cues.
     */
    private fun silenceMicNoises() {
        if (!isMuteMicSoundsEnabled()) return
        if (isAudioMutedByApp) return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                audioManager.adjustStreamVolume(AudioManager.STREAM_NOTIFICATION, AudioManager.ADJUST_MUTE, 0)
                audioManager.adjustStreamVolume(AudioManager.STREAM_SYSTEM, AudioManager.ADJUST_MUTE, 0)
                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, 0)
            } else {
                @Suppress("DEPRECATION")
                audioManager.setStreamMute(AudioManager.STREAM_NOTIFICATION, true)
                @Suppress("DEPRECATION")
                audioManager.setStreamMute(AudioManager.STREAM_SYSTEM, true)
                @Suppress("DEPRECATION")
                audioManager.setStreamMute(AudioManager.STREAM_MUSIC, true)
            }
            isAudioMutedByApp = true
        } catch (e: Exception) {
            // Ignore SecurityException on policy-restricted streams
        }
    }

    /**
     * Restores normal system volume levels.
     */
    private fun restoreSystemAudio() {
        if (!isAudioMutedByApp) return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                audioManager.adjustStreamVolume(AudioManager.STREAM_NOTIFICATION, AudioManager.ADJUST_UNMUTE, 0)
                audioManager.adjustStreamVolume(AudioManager.STREAM_SYSTEM, AudioManager.ADJUST_UNMUTE, 0)
                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_UNMUTE, 0)
            } else {
                @Suppress("DEPRECATION")
                audioManager.setStreamMute(AudioManager.STREAM_NOTIFICATION, false)
                @Suppress("DEPRECATION")
                audioManager.setStreamMute(AudioManager.STREAM_SYSTEM, false)
                @Suppress("DEPRECATION")
                audioManager.setStreamMute(AudioManager.STREAM_MUSIC, false)
            }
        } catch (e: Exception) {
            // Ignore
        } finally {
            isAudioMutedByApp = false
        }
    }

    private fun createListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                _statusText.value = "Listening... Speak clearly"
            }

            override fun onBeginningOfSpeech() {
                _statusText.value = "Transcribing voice in real time..."
            }

            override fun onRmsChanged(rmsdB: Float) {
                val rawNormalized = (rmsdB.coerceIn(0f, 10f) / 10f)
                val noiseGateFloor = if (isNoiseGateEnabled()) 0.12f else 0.02f

                // Filter out low-level ambient hiss, breathing, and room noise
                if (rawNormalized < noiseGateFloor) {
                    _rmsDbLevel.value = 0f
                } else {
                    _rmsDbLevel.value = ((rawNormalized - noiseGateFloor) / (1f - noiseGateFloor)).coerceIn(0f, 1f)
                }
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                _rmsDbLevel.value = 0f
                _statusText.value = "Processing speech..."
            }

            override fun onError(error: Int) {
                _rmsDbLevel.value = 0f
                val message = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                    SpeechRecognizer.ERROR_CLIENT -> "Speech recognition client error"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required"
                    SpeechRecognizer.ERROR_NETWORK -> "Network connection error"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                    SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech recognizer busy"
                    SpeechRecognizer.ERROR_SERVER -> "Recognition server error"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Silence timeout"
                    else -> "Recognition paused ($error)"
                }

                // If continuous mode is enabled and user hasn't explicitly stopped, rearm silently
                if (_isListening.value && isContinuousMode && (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT)) {
                    silenceMicNoises()
                    mainHandler.postDelayed({
                        if (_isListening.value) {
                            startListeningInternal()
                        }
                    }, 300)
                } else {
                    if (!_isListening.value) {
                        _statusText.value = "Idle"
                        mainHandler.postDelayed({ restoreSystemAudio() }, 400)
                    } else {
                        _statusText.value = message
                    }
                }
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val spokenText = matches[0].trim()
                    if (spokenText.isNotEmpty()) {
                        val current = _accumulatedTranscript.value
                        val newAccum = if (current.isBlank()) spokenText else "$current $spokenText"
                        _accumulatedTranscript.value = newAccum
                    }
                }
                _partialTranscript.value = ""
                _rmsDbLevel.value = 0f

                if (_isListening.value && isContinuousMode) {
                    silenceMicNoises()
                    mainHandler.postDelayed({
                        if (_isListening.value) {
                            startListeningInternal()
                        }
                    }, 250)
                } else {
                    _isListening.value = false
                    _statusText.value = "Dictation finished"
                    // Allow final closing sound to pass into silence before restoring
                    mainHandler.postDelayed({ restoreSystemAudio() }, 400)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val partials = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!partials.isNullOrEmpty()) {
                    _partialTranscript.value = partials[0]
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }

    fun startListening() {
        mainHandler.post {
            silenceMicNoises()
            _isListening.value = true
            _statusText.value = "Initializing microphone..."
            startListeningInternal()
        }
    }

    private fun startListeningInternal() {
        try {
            if (speechRecognizer == null) {
                initRecognizer()
            }
            // Ensure streams are silenced right before engine activates
            silenceMicNoises()

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                // Extended silence tolerances to prevent premature cutting off and rapid re-beeping
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 3000L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 3500L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 4000L)
            }
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            _isListening.value = false
            _statusText.value = "Could not start mic: ${e.localizedMessage}"
            restoreSystemAudio()
        }
    }

    fun stopListening() {
        mainHandler.post {
            _isListening.value = false
            _rmsDbLevel.value = 0f
            _partialTranscript.value = ""
            _statusText.value = "Dictation stopped"
            try {
                speechRecognizer?.stopListening()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            // Delay audio restoration slightly so any trailing shutdown chime is muted
            mainHandler.postDelayed({
                restoreSystemAudio()
            }, 500)
        }
    }

    fun clearTranscript() {
        _accumulatedTranscript.value = ""
        _partialTranscript.value = ""
        _statusText.value = "Ready to dictate"
    }

    fun setTranscript(text: String) {
        _accumulatedTranscript.value = text
        _partialTranscript.value = ""
    }

    fun destroy() {
        mainHandler.post {
            restoreSystemAudio()
            try {
                speechRecognizer?.destroy()
                speechRecognizer = null
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
