package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.NotificationDatabase
import com.example.data.NotificationHistory
import com.example.data.NotificationRepository
import com.example.data.SettingsRepository
import com.example.util.AlarmScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsRepo = SettingsRepository(application)
    private val database = NotificationDatabase.getDatabase(application)
    private val notificationRepo = NotificationRepository(database.notificationDao)

    // State flows for settings
    private val _isEnabled = MutableStateFlow(settingsRepo.isEnabled)
    val isEnabled: StateFlow<Boolean> = _isEnabled.asStateFlow()

    private val _notificationText = MutableStateFlow(settingsRepo.notificationText)
    val notificationText: StateFlow<String> = _notificationText.asStateFlow()

    private val _startTime = MutableStateFlow(settingsRepo.startTime)
    val startTime: StateFlow<String> = _startTime.asStateFlow()

    private val _intervalMinutes = MutableStateFlow(settingsRepo.intervalMinutes)
    val intervalMinutes: StateFlow<Int> = _intervalMinutes.asStateFlow()

    private val _isDndEnabled = MutableStateFlow(settingsRepo.isDndEnabled)
    val isDndEnabled: StateFlow<Boolean> = _isDndEnabled.asStateFlow()

    private val _dndStartTime = MutableStateFlow(settingsRepo.dndStartTime)
    val dndStartTime: StateFlow<String> = _dndStartTime.asStateFlow()

    private val _dndEndTime = MutableStateFlow(settingsRepo.dndEndTime)
    val dndEndTime: StateFlow<String> = _dndEndTime.asStateFlow()

    private val _nextTriggerTime = MutableStateFlow(settingsRepo.nextTriggerTime)
    val nextTriggerTime: StateFlow<Long> = _nextTriggerTime.asStateFlow()

    private val _themeMode = MutableStateFlow(settingsRepo.themeMode)
    val themeMode: StateFlow<Int> = _themeMode.asStateFlow()

    private val _activeAlertToShow = MutableStateFlow<String?>(null)
    val activeAlertToShow: StateFlow<String?> = _activeAlertToShow.asStateFlow()

    fun showAlertDetails(text: String?) {
        _activeAlertToShow.value = text
    }

    fun dismissAlertDetails() {
        _activeAlertToShow.value = null
    }

    // History Flow from Room Database
    val historyList: StateFlow<List<NotificationHistory>> = notificationRepo.allHistory
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun setEnabled(enabled: Boolean) {
        settingsRepo.isEnabled = enabled
        _isEnabled.value = enabled
        
        if (enabled) {
            AlarmScheduler.scheduleNextAlarm(getApplication())
        } else {
            AlarmScheduler.cancelAlarm(getApplication())
        }
        updateNextTriggerTimeState()
    }

    fun updateNotificationText(text: String) {
        settingsRepo.notificationText = text
        _notificationText.value = text
        autoRescheduleIfEnabled()
    }

    fun updateStartTime(time: String) {
        settingsRepo.startTime = time
        _startTime.value = time
        autoRescheduleIfEnabled()
    }

    fun updateInterval(minutes: Int) {
        settingsRepo.intervalMinutes = minutes
        _intervalMinutes.value = minutes
        autoRescheduleIfEnabled()
    }

    fun setDndEnabled(enabled: Boolean) {
        settingsRepo.isDndEnabled = enabled
        _isDndEnabled.value = enabled
        autoRescheduleIfEnabled()
    }

    fun updateDndStartTime(time: String) {
        settingsRepo.dndStartTime = time
        _dndStartTime.value = time
        autoRescheduleIfEnabled()
    }

    fun updateDndEndTime(time: String) {
        settingsRepo.dndEndTime = time
        _dndEndTime.value = time
        autoRescheduleIfEnabled()
    }

    // Force calibrate the time alignment immediately
    fun calibrateTimesNow() {
        if (settingsRepo.isEnabled) {
            AlarmScheduler.scheduleNextAlarm(getApplication())
            updateNextTriggerTimeState()
        }
    }

    fun deleteHistoryItem(id: Int) {
        viewModelScope.launch {
            notificationRepo.deleteById(id)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            notificationRepo.clearAll()
        }
    }

    fun updateThemeMode(mode: Int) {
        settingsRepo.themeMode = mode
        _themeMode.value = mode
    }

    private fun autoRescheduleIfEnabled() {
        if (settingsRepo.isEnabled) {
            AlarmScheduler.scheduleNextAlarm(getApplication())
        }
        updateNextTriggerTimeState()
    }

    fun updateNextTriggerTimeState() {
        _nextTriggerTime.value = settingsRepo.nextTriggerTime
    }

    init {
        // Calibrate once on launch to ensure correct display sync
        updateNextTriggerTimeState()
    }
}
