package com.example.warehouse.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.warehouse.utils.NotificationPreferencesManager
import com.example.warehouse.utils.RepeatFrequency
import com.example.warehouse.utils.SendTiming
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NotificationSettingsState(
    val isEnabled: Boolean = false,
    val lowStockEnabled: Boolean = true,
    val almostOutEnabled: Boolean = true,
    val outOfStockEnabled: Boolean = true,
    val sendTiming: SendTiming = SendTiming.IMMEDIATE,
    val scheduledTimes: Set<String> = emptySet(),
    val repeatFrequency: RepeatFrequency = RepeatFrequency.NO_REPEAT
)

class NotificationViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val prefsManager = NotificationPreferencesManager(application)
    
    private val _state = MutableStateFlow(NotificationSettingsState())
    val state: StateFlow<NotificationSettingsState> = _state.asStateFlow()

    private var currentUserId: Long = -1L

    fun loadSettings(userId: Long) {
        currentUserId = userId
        if (userId == -1L) return
        
        _state.value = NotificationSettingsState(
            isEnabled = prefsManager.isNotificationEnabled(userId),
            lowStockEnabled = prefsManager.isLowStockEnabled(userId),
            almostOutEnabled = prefsManager.isAlmostOutEnabled(userId),
            outOfStockEnabled = prefsManager.isOutOfStockEnabled(userId),
            sendTiming = prefsManager.getSendTiming(userId),
            scheduledTimes = prefsManager.getScheduledTimes(userId),
            repeatFrequency = prefsManager.getRepeatFrequency(userId)
        )
    }

    fun setNotificationEnabled(enabled: Boolean) {
        if (currentUserId == -1L) return
        prefsManager.setNotificationEnabled(currentUserId, enabled)
        _state.update { it.copy(isEnabled = enabled) }
        scheduleWorkerIfNecessary()
    }

    fun setLowStockEnabled(enabled: Boolean) {
        if (currentUserId == -1L) return
        prefsManager.setLowStockEnabled(currentUserId, enabled)
        _state.update { it.copy(lowStockEnabled = enabled) }
    }

    fun setAlmostOutEnabled(enabled: Boolean) {
        if (currentUserId == -1L) return
        prefsManager.setAlmostOutEnabled(currentUserId, enabled)
        _state.update { it.copy(almostOutEnabled = enabled) }
    }

    fun setOutOfStockEnabled(enabled: Boolean) {
        if (currentUserId == -1L) return
        prefsManager.setOutOfStockEnabled(currentUserId, enabled)
        _state.update { it.copy(outOfStockEnabled = enabled) }
    }

    fun setSendTiming(timing: SendTiming) {
        if (currentUserId == -1L) return
        prefsManager.setSendTiming(currentUserId, timing)
        _state.update { it.copy(sendTiming = timing) }
        scheduleWorkerIfNecessary()
    }

    fun addScheduledTime(time: String) {
        if (currentUserId == -1L) return
        prefsManager.addScheduledTime(currentUserId, time)
        _state.update { it.copy(scheduledTimes = prefsManager.getScheduledTimes(currentUserId)) }
        scheduleWorkerIfNecessary()
    }

    fun removeScheduledTime(time: String) {
        if (currentUserId == -1L) return
        prefsManager.removeScheduledTime(currentUserId, time)
        _state.update { it.copy(scheduledTimes = prefsManager.getScheduledTimes(currentUserId)) }
        scheduleWorkerIfNecessary()
    }

    fun setRepeatFrequency(frequency: RepeatFrequency) {
        if (currentUserId == -1L) return
        prefsManager.setRepeatFrequency(currentUserId, frequency)
        _state.update { it.copy(repeatFrequency = frequency) }
    }

    private fun scheduleWorkerIfNecessary() {
        if (currentUserId == -1L) return
        val timing = prefsManager.getSendTiming(currentUserId)
        if (timing == SendTiming.SCHEDULED && prefsManager.isNotificationEnabled(currentUserId)) {
            val times = prefsManager.getScheduledTimes(currentUserId)
            com.example.warehouse.utils.NotificationScheduler.scheduleAll(getApplication(), currentUserId, times)
        } else {
            com.example.warehouse.utils.NotificationScheduler.cancelAll(getApplication(), currentUserId)
        }
    }
}
