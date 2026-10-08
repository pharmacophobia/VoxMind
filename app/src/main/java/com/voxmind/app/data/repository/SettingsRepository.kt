package com.voxmind.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("voxmind_settings", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_DEEPSEEK_API_KEY = "deepseek_api_key"
        private const val KEY_DEEPSEEK_MODEL = "deepseek_model"
        private const val KEY_DEFAULT_PHONE = "default_phone"
        private const val KEY_DEFAULT_EMAIL = "default_email"
        private const val KEY_CONTINUOUS_SPEECH = "continuous_speech"
        private const val KEY_MUTE_MIC_SOUNDS = "mute_mic_sounds"
        private const val KEY_NOISE_GATE = "noise_gate_enabled"
        private const val KEY_DEFAULT_SMS_ENABLED = "default_sms_enabled"
        private const val KEY_DEFAULT_EMAIL_ENABLED = "default_email_enabled"

        // Default preconfigured key from the system environment
        const val DEFAULT_KEY = "sk-b7f85ac6e8cb446ea0556b1fd585f0f3"
        const val MODEL_CHAT = "deepseek-chat"
        const val MODEL_REASONER = "deepseek-reasoner"
    }

    private val _apiKey = MutableStateFlow(prefs.getString(KEY_DEEPSEEK_API_KEY, DEFAULT_KEY) ?: DEFAULT_KEY)
    val apiKey: StateFlow<String> = _apiKey.asStateFlow()

    private val _model = MutableStateFlow(prefs.getString(KEY_DEEPSEEK_MODEL, MODEL_CHAT) ?: MODEL_CHAT)
    val model: StateFlow<String> = _model.asStateFlow()

    private val _defaultPhone = MutableStateFlow(prefs.getString(KEY_DEFAULT_PHONE, "") ?: "")
    val defaultPhone: StateFlow<String> = _defaultPhone.asStateFlow()

    private val _defaultEmail = MutableStateFlow(prefs.getString(KEY_DEFAULT_EMAIL, "") ?: "")
    val defaultEmail: StateFlow<String> = _defaultEmail.asStateFlow()

    private val _continuousSpeech = MutableStateFlow(prefs.getBoolean(KEY_CONTINUOUS_SPEECH, true))
    val continuousSpeech: StateFlow<Boolean> = _continuousSpeech.asStateFlow()

    private val _muteMicSounds = MutableStateFlow(prefs.getBoolean(KEY_MUTE_MIC_SOUNDS, true))
    val muteMicSounds: StateFlow<Boolean> = _muteMicSounds.asStateFlow()

    private val _noiseGateEnabled = MutableStateFlow(prefs.getBoolean(KEY_NOISE_GATE, true))
    val noiseGateEnabled: StateFlow<Boolean> = _noiseGateEnabled.asStateFlow()

    private val _defaultSmsEnabled = MutableStateFlow(prefs.getBoolean(KEY_DEFAULT_SMS_ENABLED, false))
    val defaultSmsEnabled: StateFlow<Boolean> = _defaultSmsEnabled.asStateFlow()

    private val _defaultEmailEnabled = MutableStateFlow(prefs.getBoolean(KEY_DEFAULT_EMAIL_ENABLED, false))
    val defaultEmailEnabled: StateFlow<Boolean> = _defaultEmailEnabled.asStateFlow()

    fun setApiKey(key: String) {
        val trimmed = key.trim()
        prefs.edit().putString(KEY_DEEPSEEK_API_KEY, trimmed).apply()
        _apiKey.value = trimmed
    }

    fun setModel(model: String) {
        prefs.edit().putString(KEY_DEEPSEEK_MODEL, model).apply()
        _model.value = model
    }

    fun setDefaultPhone(phone: String) {
        prefs.edit().putString(KEY_DEFAULT_PHONE, phone.trim()).apply()
        _defaultPhone.value = phone.trim()
    }

    fun setDefaultEmail(email: String) {
        prefs.edit().putString(KEY_DEFAULT_EMAIL, email.trim()).apply()
        _defaultEmail.value = email.trim()
    }

    fun setContinuousSpeech(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_CONTINUOUS_SPEECH, enabled).apply()
        _continuousSpeech.value = enabled
    }

    fun setMuteMicSounds(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_MUTE_MIC_SOUNDS, enabled).apply()
        _muteMicSounds.value = enabled
    }

    fun setNoiseGateEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NOISE_GATE, enabled).apply()
        _noiseGateEnabled.value = enabled
    }

    fun setDefaultSmsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DEFAULT_SMS_ENABLED, enabled).apply()
        _defaultSmsEnabled.value = enabled
    }

    fun setDefaultEmailEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DEFAULT_EMAIL_ENABLED, enabled).apply()
        _defaultEmailEnabled.value = enabled
    }
}
