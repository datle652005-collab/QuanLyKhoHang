package com.example.warehouse.utils

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateUtils {
    fun formatDate(timestamp: Long, pattern: String = "dd/MM/yyyy HH:mm"): String {
        val sdf = SimpleDateFormat(pattern, Locale.forLanguageTag("vi-VN"))
        return sdf.format(Date(timestamp))
    }

    fun formatDateOnly(timestamp: Long): String {
        return formatDate(timestamp, "dd/MM/yyyy")
    }

    fun formatDateTime(timestamp: Long): String {
        return formatDate(timestamp, "dd/MM/yyyy HH:mm:ss")
    }

    fun formatDateCode(timestamp: Long): String {
        return formatDate(timestamp, "yyyyMMdd")
    }

    fun getStartOfDay(timestamp: Long = System.currentTimeMillis()): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }

    fun getEndOfDay(timestamp: Long = System.currentTimeMillis()): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return calendar.timeInMillis
    }
}
