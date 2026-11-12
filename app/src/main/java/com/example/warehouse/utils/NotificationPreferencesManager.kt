package com.example.warehouse.utils

import android.content.Context
import android.content.SharedPreferences

enum class SendTiming {
    IMMEDIATE, SCHEDULED
}

enum class RepeatFrequency {
    NO_REPEAT, EVERY_6H, EVERY_12H, EVERY_DAY, ONLY_STATUS_CHANGE
}

class NotificationPreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("notification_prefs", Context.MODE_PRIVATE)

    fun isNotificationEnabled(userId: Long): Boolean {
        return prefs.getBoolean("notif_enabled_$userId", false)
    }

    fun setNotificationEnabled(userId: Long, enabled: Boolean) {
        prefs.edit().putBoolean("notif_enabled_$userId", enabled).apply()
    }

    fun isLowStockEnabled(userId: Long): Boolean {
        return prefs.getBoolean("low_stock_enabled_$userId", true)
    }

    fun setLowStockEnabled(userId: Long, enabled: Boolean) {
        prefs.edit().putBoolean("low_stock_enabled_$userId", enabled).apply()
    }

    fun isAlmostOutEnabled(userId: Long): Boolean {
        return prefs.getBoolean("almost_out_enabled_$userId", true)
    }

    fun setAlmostOutEnabled(userId: Long, enabled: Boolean) {
        prefs.edit().putBoolean("almost_out_enabled_$userId", enabled).apply()
    }

    fun isOutOfStockEnabled(userId: Long): Boolean {
        return prefs.getBoolean("out_of_stock_enabled_$userId", true)
    }

    fun setOutOfStockEnabled(userId: Long, enabled: Boolean) {
        prefs.edit().putBoolean("out_of_stock_enabled_$userId", enabled).apply()
    }

    fun getSendTiming(userId: Long): SendTiming {
        val timing = prefs.getString("send_timing_$userId", SendTiming.IMMEDIATE.name)
        return SendTiming.valueOf(timing ?: SendTiming.IMMEDIATE.name)
    }

    fun setSendTiming(userId: Long, timing: SendTiming) {
        prefs.edit().putString("send_timing_$userId", timing.name).apply()
    }

    fun getScheduledTimes(userId: Long): Set<String> {
        return prefs.getStringSet("scheduled_times_$userId", emptySet()) ?: emptySet()
    }

    fun setScheduledTimes(userId: Long, times: Set<String>) {
        prefs.edit().putStringSet("scheduled_times_$userId", times).apply()
    }
    
    fun addScheduledTime(userId: Long, time: String) {
        val times = getScheduledTimes(userId).toMutableSet()
        times.add(time)
        setScheduledTimes(userId, times)
    }
    
    fun removeScheduledTime(userId: Long, time: String) {
        val times = getScheduledTimes(userId).toMutableSet()
        times.remove(time)
        setScheduledTimes(userId, times)
    }

    fun getRepeatFrequency(userId: Long): RepeatFrequency {
        val freq = prefs.getString("repeat_freq_$userId", RepeatFrequency.NO_REPEAT.name)
        return RepeatFrequency.valueOf(freq ?: RepeatFrequency.NO_REPEAT.name)
    }

    fun setRepeatFrequency(userId: Long, freq: RepeatFrequency) {
        prefs.edit().putString("repeat_freq_$userId", freq.name).apply()
    }
    
    fun getLastNotifiedTime(userId: Long, productId: Long): Long {
        return prefs.getLong("last_notified_time_${userId}_$productId", 0L)
    }
    
    fun setLastNotifiedTime(userId: Long, productId: Long, timeMillis: Long) {
        prefs.edit().putLong("last_notified_time_${userId}_$productId", timeMillis).apply()
    }
    
    fun getLastNotifiedStatus(userId: Long, productId: Long): String? {
        return prefs.getString("last_notified_status_${userId}_$productId", null)
    }
    
    fun setLastNotifiedStatus(userId: Long, productId: Long, status: String) {
        prefs.edit().putString("last_notified_status_${userId}_$productId", status).apply()
    }
}
