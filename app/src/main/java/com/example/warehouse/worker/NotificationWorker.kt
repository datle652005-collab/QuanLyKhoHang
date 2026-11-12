package com.example.warehouse.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.warehouse.data.database.AppDatabase
import com.example.warehouse.data.repository.WarehouseRepository
import com.example.warehouse.utils.NotificationPreferencesManager
import com.example.warehouse.utils.NotificationService
import kotlinx.coroutines.flow.firstOrNull

class NotificationWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val userId = inputData.getLong("USER_ID", -1L)
        if (userId == -1L) return Result.success()

        val database = AppDatabase.getDatabase(applicationContext)
        val repository = WarehouseRepository(database)
        val prefsManager = NotificationPreferencesManager(applicationContext)
        val notificationService = NotificationService(applicationContext, repository, prefsManager)

        notificationService.checkAndSendScheduledNotification(userId)
        
        // Reschedule for next days
        val times = prefsManager.getScheduledTimes(userId)
        com.example.warehouse.utils.NotificationScheduler.scheduleAll(applicationContext, userId, times)

        return Result.success()
    }
}
