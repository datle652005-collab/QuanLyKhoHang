package com.example.warehouse.utils

import android.content.Context
import com.example.warehouse.data.repository.WarehouseRepository
import kotlinx.coroutines.flow.firstOrNull
import kotlin.math.ceil

class NotificationService(
    private val context: Context,
    private val repository: WarehouseRepository,
    private val prefsManager: NotificationPreferencesManager
) {
    private val notificationHelper = NotificationHelper(context)

    suspend fun checkAndSendImmediateNotification(userId: Long) {
        if (!prefsManager.isNotificationEnabled(userId)) return
        if (prefsManager.getSendTiming(userId) != SendTiming.IMMEDIATE) return

        val products = repository.productDao.getAllProductsWithDetails().firstOrNull() ?: return
        
        for (item in products) {
            val product = item.product
            val quantity = repository.inventoryDao.getInventoryByProductId(product.productId)?.quantity ?: 0
            
            checkAndSendSingle(userId, product.productId, product.productName, product.productCode, quantity, product.minStock)
        }
    }
    
    suspend fun checkAndSendSingleProductImmediate(userId: Long, productId: Long) {
        if (!prefsManager.isNotificationEnabled(userId)) return
        if (prefsManager.getSendTiming(userId) != SendTiming.IMMEDIATE) return

        val products = repository.productDao.getAllProductsWithDetails().firstOrNull() ?: return
        val item = products.find { it.product.productId == productId } ?: return
        val product = item.product
        val quantity = repository.inventoryDao.getInventoryByProductId(product.productId)?.quantity ?: 0
            
        checkAndSendSingle(userId, product.productId, product.productName, product.productCode, quantity, product.minStock)
    }

    suspend fun checkAndSendScheduledNotification(userId: Long) {
        if (!prefsManager.isNotificationEnabled(userId)) return
        if (prefsManager.getSendTiming(userId) != SendTiming.SCHEDULED) return

        val products = repository.productDao.getAllProductsWithDetails().firstOrNull() ?: return
        
        var lowStockCount = 0
        var almostOutCount = 0
        var outOfStockCount = 0
        
        for (item in products) {
            val product = item.product
            val quantity = repository.inventoryDao.getInventoryByProductId(product.productId)?.quantity ?: 0
            val minStock = product.minStock
            
            val status = getStatus(quantity, minStock)
            
            if (status != null && shouldSendNotification(userId, product.productId, status)) {
                when (status) {
                    "Đã hết hàng" -> {
                        if (prefsManager.isOutOfStockEnabled(userId)) outOfStockCount++
                    }
                    "Tồn kho gần hết" -> {
                        if (prefsManager.isAlmostOutEnabled(userId)) almostOutCount++
                    }
                    "Tồn kho thấp" -> {
                        if (prefsManager.isLowStockEnabled(userId)) lowStockCount++
                    }
                }
                updateLastNotified(userId, product.productId, status)
            }
        }
        
        notificationHelper.showAggregatedNotification(lowStockCount, almostOutCount, outOfStockCount)
    }

    private fun checkAndSendSingle(
        userId: Long, 
        productId: Long, 
        productName: String, 
        productCode: String, 
        quantity: Int, 
        minStock: Int
    ) {
        val status = getStatus(quantity, minStock)
        
        // Trạng thái bình thường
        if (status == null) {
            // Reset status so if it drops again, we notify
            prefsManager.setLastNotifiedStatus(userId, productId, "Bình thường")
            return
        }

        val isEnabledForType = when (status) {
            "Đã hết hàng" -> prefsManager.isOutOfStockEnabled(userId)
            "Tồn kho gần hết" -> prefsManager.isAlmostOutEnabled(userId)
            "Tồn kho thấp" -> prefsManager.isLowStockEnabled(userId)
            else -> false
        }

        if (isEnabledForType && shouldSendNotification(userId, productId, status)) {
            notificationHelper.showSingleNotification(status, productName, productCode, quantity, productId)
            updateLastNotified(userId, productId, status)
        }
    }

    private fun getStatus(quantity: Int, minStock: Int): String? {
        return when {
            quantity == 0 -> "Đã hết hàng"
            quantity > 0 && quantity <= ceil(minStock * 0.5) -> "Tồn kho gần hết"
            quantity <= minStock -> "Tồn kho thấp"
            else -> null
        }
    }

    private fun shouldSendNotification(userId: Long, productId: Long, newStatus: String): Boolean {
        val lastStatus = prefsManager.getLastNotifiedStatus(userId, productId)
        val lastTime = prefsManager.getLastNotifiedTime(userId, productId)
        val currentTime = System.currentTimeMillis()
        val freq = prefsManager.getRepeatFrequency(userId)
        
        if (lastStatus != newStatus) return true
        
        return when (freq) {
            RepeatFrequency.NO_REPEAT -> false
            RepeatFrequency.ONLY_STATUS_CHANGE -> false
            RepeatFrequency.EVERY_6H -> (currentTime - lastTime) >= 6 * 60 * 60 * 1000L
            RepeatFrequency.EVERY_12H -> (currentTime - lastTime) >= 12 * 60 * 60 * 1000L
            RepeatFrequency.EVERY_DAY -> (currentTime - lastTime) >= 24 * 60 * 60 * 1000L
        }
    }

    private fun updateLastNotified(userId: Long, productId: Long, status: String) {
        prefsManager.setLastNotifiedStatus(userId, productId, status)
        prefsManager.setLastNotifiedTime(userId, productId, System.currentTimeMillis())
    }
}
