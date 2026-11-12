package com.example.warehouse.utils

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.warehouse.worker.NotificationWorker
import java.util.Calendar
import java.util.concurrent.TimeUnit

object NotificationScheduler {
    fun scheduleAll(context: Context, userId: Long, times: Set<String>) {
        val workManager = WorkManager.getInstance(context)
        workManager.cancelAllWorkByTag("notif_$userId")

        for (timeStr in times) {
            val parts = timeStr.split(":")
            if (parts.size != 2) continue
            val hour = parts[0].toIntOrNull() ?: continue
            val minute = parts[1].toIntOrNull() ?: continue

            val now = Calendar.getInstance()
            val target = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            if (target.before(now)) {
                target.add(Calendar.DAY_OF_YEAR, 1)
            }

            val delay = target.timeInMillis - now.timeInMillis

            val inputData = Data.Builder().putLong("USER_ID", userId).build()

            val workRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .addTag("notif_$userId")
                .addTag("notif_${userId}_${timeStr}")
                .setInputData(inputData)
                .build()

            // Unique work per time string to replace it if recreated
            workManager.enqueueUniqueWork(
                "notif_${userId}_${timeStr}",
                ExistingWorkPolicy.REPLACE,
                workRequest
            )
        }
    }
    
    fun cancelAll(context: Context, userId: Long) {
        WorkManager.getInstance(context).cancelAllWorkByTag("notif_$userId")
    }
}
