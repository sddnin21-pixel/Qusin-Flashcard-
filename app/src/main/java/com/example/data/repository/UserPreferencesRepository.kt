package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class UserPreferencesRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("qusin_user_prefs", Context.MODE_PRIVATE)

    companion object {
        const val KEY_API_KEY = "gemini_custom_api_key"
        const val KEY_MODEL = "gemini_selected_model"
        const val KEY_THEME = "app_theme_mode" // SYSTEM, LIGHT, DARK
        const val KEY_TTS_SPEED = "tts_speech_rate"
        const val KEY_TTS_PITCH = "tts_pitch"
        const val KEY_DAILY_CARDS_GOAL = "daily_cards_goal"
        const val KEY_DAILY_MINUTES_GOAL = "daily_minutes_goal"
        const val KEY_NOTIF_ENABLED = "notif_enabled"
        const val KEY_NOTIF_HOUR = "notif_hour"
        const val KEY_NOTIF_MINUTE = "notif_minute"
        const val KEY_LANGUAGE = "app_language"
        const val KEY_MUSIC_VOLUME = "music_volume"

        // Default recommended model per specification
        const val DEFAULT_MODEL = "gemini-3.5-flash"
    }

    private val _languageFlow = MutableStateFlow(prefs.getString(KEY_LANGUAGE, "vi") ?: "vi")
    val languageFlow: StateFlow<String> = _languageFlow.asStateFlow()

    private val _apiKeyFlow = MutableStateFlow(getStoredApiKey())
    val apiKeyFlow: StateFlow<String> = _apiKeyFlow.asStateFlow()

    private val _modelFlow = MutableStateFlow(getStoredModel())
    val modelFlow: StateFlow<String> = _modelFlow.asStateFlow()

    private val _themeFlow = MutableStateFlow(prefs.getString(KEY_THEME, "DARK") ?: "DARK")
    val themeFlow: StateFlow<String> = _themeFlow.asStateFlow()

    private val _ttsSpeedFlow = MutableStateFlow(prefs.getFloat(KEY_TTS_SPEED, 1.0f))
    val ttsSpeedFlow: StateFlow<Float> = _ttsSpeedFlow.asStateFlow()

    private val _ttsPitchFlow = MutableStateFlow(prefs.getFloat(KEY_TTS_PITCH, 1.0f))
    val ttsPitchFlow: StateFlow<Float> = _ttsPitchFlow.asStateFlow()

    private val _dailyCardsGoalFlow = MutableStateFlow(prefs.getInt(KEY_DAILY_CARDS_GOAL, 20))
    val dailyCardsGoalFlow: StateFlow<Int> = _dailyCardsGoalFlow.asStateFlow()

    private val _dailyMinutesGoalFlow = MutableStateFlow(prefs.getInt(KEY_DAILY_MINUTES_GOAL, 15))
    val dailyMinutesGoalFlow: StateFlow<Int> = _dailyMinutesGoalFlow.asStateFlow()

    private val _notifEnabledFlow = MutableStateFlow(prefs.getBoolean(KEY_NOTIF_ENABLED, true))
    val notifEnabledFlow: StateFlow<Boolean> = _notifEnabledFlow.asStateFlow()
    val dailyReminderEnabledFlow: StateFlow<Boolean> = _notifEnabledFlow.asStateFlow()

    private val _notifHourFlow = MutableStateFlow(prefs.getInt(KEY_NOTIF_HOUR, 20))
    val notifHourFlow: StateFlow<Int> = _notifHourFlow.asStateFlow()
    val reminderHourFlow: StateFlow<Int> = _notifHourFlow.asStateFlow()

    private val _notifMinuteFlow = MutableStateFlow(prefs.getInt(KEY_NOTIF_MINUTE, 0))
    val notifMinuteFlow: StateFlow<Int> = _notifMinuteFlow.asStateFlow()

    private val _musicVolumeFlow = MutableStateFlow(prefs.getFloat(KEY_MUSIC_VOLUME, 0.7f))
    val musicVolumeFlow: StateFlow<Float> = _musicVolumeFlow.asStateFlow()

    fun getStoredApiKey(): String {
        val custom = prefs.getString(KEY_API_KEY, "") ?: ""
        if (custom.isNotBlank()) return custom
        // Fallback to BuildConfig if present and not default placeholder
        val buildKey = try { BuildConfig.GEMINI_API_KEY } catch (_: Exception) { "" }
        if (buildKey.isNotBlank() && buildKey != "MY_GEMINI_API_KEY") {
            return buildKey
        }
        return ""
    }

    fun hasCustomKey(): Boolean {
        return (prefs.getString(KEY_API_KEY, "") ?: "").isNotBlank()
    }

    fun saveApiKey(key: String) {
        prefs.edit().putString(KEY_API_KEY, key.trim()).apply()
        _apiKeyFlow.value = key.trim()
    }

    fun clearApiKey() {
        prefs.edit().remove(KEY_API_KEY).apply()
        _apiKeyFlow.value = getStoredApiKey()
    }

    fun getStoredModel(): String {
        return prefs.getString(KEY_MODEL, DEFAULT_MODEL) ?: DEFAULT_MODEL
    }

    fun saveModel(model: String) {
        prefs.edit().putString(KEY_MODEL, model).apply()
        _modelFlow.value = model
    }

    fun saveTheme(theme: String) {
        prefs.edit().putString(KEY_THEME, theme).apply()
        _themeFlow.value = theme
    }

    fun saveTtsSpeed(speed: Float) {
        prefs.edit().putFloat(KEY_TTS_SPEED, speed).apply()
        _ttsSpeedFlow.value = speed
    }

    fun saveTtsPitch(pitch: Float) {
        prefs.edit().putFloat(KEY_TTS_PITCH, pitch).apply()
        _ttsPitchFlow.value = pitch
    }

    fun saveDailyGoals(cards: Int, minutes: Int) {
        prefs.edit()
            .putInt(KEY_DAILY_CARDS_GOAL, cards)
            .putInt(KEY_DAILY_MINUTES_GOAL, minutes)
            .apply()
        _dailyCardsGoalFlow.value = cards
        _dailyMinutesGoalFlow.value = minutes
    }

    fun saveNotificationSettings(enabled: Boolean, hour: Int, minute: Int) {
        prefs.edit()
            .putBoolean(KEY_NOTIF_ENABLED, enabled)
            .putInt(KEY_NOTIF_HOUR, hour)
            .putInt(KEY_NOTIF_MINUTE, minute)
            .apply()
        _notifEnabledFlow.value = enabled
        _notifHourFlow.value = hour
        _notifMinuteFlow.value = minute
    }

    fun saveMusicVolume(volume: Float) {
        prefs.edit().putFloat(KEY_MUSIC_VOLUME, volume).apply()
        _musicVolumeFlow.value = volume
    }

    fun setApiKey(key: String) = saveApiKey(key)
    fun setModel(model: String) = saveModel(model)
    fun setLanguage(lang: String) {
        prefs.edit().putString(KEY_LANGUAGE, lang).apply()
        _languageFlow.value = lang
    }
    fun setTtsSpeed(speed: Float) = saveTtsSpeed(speed)
    fun setDailyReminderEnabled(enabled: Boolean) = saveNotificationSettings(enabled, _notifHourFlow.value, _notifMinuteFlow.value)
    fun setReminderHour(hour: Int) = saveNotificationSettings(_notifEnabledFlow.value, hour, _notifMinuteFlow.value)
}
