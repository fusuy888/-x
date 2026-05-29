package com.example.data

import android.content.Context
import android.content.SharedPreferences

class SettingsRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("notification_settings_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_IS_ENABLED = "alarm_is_enabled"
        private const val KEY_TEXT = "alarm_text"
        private const val KEY_START_TIME = "alarm_start_time"
        private const val KEY_INTERVAL = "alarm_interval"
        private const val KEY_IS_DND_ENABLED = "alarm_is_dnd_enabled"
        private const val KEY_DND_START_TIME = "alarm_dnd_start_time"
        private const val KEY_DND_END_TIME = "alarm_dnd_end_time"
        private const val KEY_NEXT_TRIGGER_TIME = "alarm_next_trigger_time"
        private const val KEY_THEME_MODE = "app_theme_mode"

        // Default values
        const val DEFAULT_TEXT = "这是你交易系统的订单吗？"
        const val DEFAULT_START_TIME = "00:12:00"
        const val DEFAULT_INTERVAL = 15
        const val DEFAULT_DND_START_TIME = "01:00:00"
        const val DEFAULT_DND_END_TIME = "06:00:00"
        const val DEFAULT_THEME_MODE = 0 // 0: Auto/System, 1: Light, 2: Dark
    }

    var themeMode: Int
        get() = prefs.getInt(KEY_THEME_MODE, DEFAULT_THEME_MODE)
        set(value) = prefs.edit().putInt(KEY_THEME_MODE, value).apply()

    var isEnabled: Boolean
        get() = prefs.getBoolean(KEY_IS_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_ENABLED, value).apply()

    var notificationText: String
        get() = prefs.getString(KEY_TEXT, DEFAULT_TEXT) ?: DEFAULT_TEXT
        set(value) = prefs.edit().putString(KEY_TEXT, value).apply()

    var startTime: String
        get() = prefs.getString(KEY_START_TIME, DEFAULT_START_TIME) ?: DEFAULT_START_TIME
        set(value) = prefs.edit().putString(KEY_START_TIME, value).apply()

    var intervalMinutes: Int
        get() = prefs.getInt(KEY_INTERVAL, DEFAULT_INTERVAL)
        set(value) = prefs.edit().putInt(KEY_INTERVAL, value).apply()

    var isDndEnabled: Boolean
        get() = prefs.getBoolean(KEY_IS_DND_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_DND_ENABLED, value).apply()

    var dndStartTime: String
        get() = prefs.getString(KEY_DND_START_TIME, DEFAULT_DND_START_TIME) ?: DEFAULT_DND_START_TIME
        set(value) = prefs.edit().putString(KEY_DND_START_TIME, value).apply()

    var dndEndTime: String
        get() = prefs.getString(KEY_DND_END_TIME, DEFAULT_DND_END_TIME) ?: DEFAULT_DND_END_TIME
        set(value) = prefs.edit().putString(KEY_DND_END_TIME, value).apply()

    var nextTriggerTime: Long
        get() = prefs.getLong(KEY_NEXT_TRIGGER_TIME, 0L)
        set(value) = prefs.edit().putLong(KEY_NEXT_TRIGGER_TIME, value).apply()
}
