package com.voxmind.app.speech

import android.content.Context
import android.content.Intent
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

class SpeechManager(private val context: Context) {

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

    private fun createListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                _statusText.value = "Listening... Speak clearly"
            }

            override fun onBeginningOfSpeech() {
                _statusText.value = "Transcribing voice in real time..."
            }

            override fun onRmsChanged(rmsdB: Float) {
                // rmsdB ranges typically between -2.0 to 10.0
                _rmsDbLevel.value = (rmsdB.coerceIn(0f, 10f) / 10f)
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

                // If continuous mode is enabled and user hasn't explicitly stopped, rearm
                if (_isListening.value && isContinuousMode && (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT)) {
                    mainHandler.postDelayed({
                        if (_isListening.value) {
                            startListeningInternal()
                        }
                    }, 400)
                } else {
                    if (!_isListening.value) {
                        _statusText.value = "Idle"
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
                    mainHandler.postDelayed({
                        if (_isListening.value) {
                            startListeningInternal()
                        }
                    }, 300)
                } else {
                    _isListening.value = false
                    _statusText.value = "Dictation finished"
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
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            }
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            _isListening.value = false
            _statusText.value = "Could not start mic: ${e.localizedMessage}"
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
            try {
                speechRecognizer?.destroy()
                speechRecognizer = null
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
