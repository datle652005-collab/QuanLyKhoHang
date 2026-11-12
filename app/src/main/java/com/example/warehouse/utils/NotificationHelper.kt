package com.example.warehouse.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity

class NotificationHelper(private val context: Context) {
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val channelId = "warehouse_alerts_channel"

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Cảnh báo tồn kho",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Thông báo khi hàng hóa sắp hết hoặc đã hết"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showSingleNotification(
        alertType: String,
        productName: String,
        productCode: String,
        quantity: Int,
        productId: Long
    ) {
        val title = alertType
        val message = if (quantity == 0) {
            "Sản phẩm $productName ($productCode) đã hết hàng."
        } else {
            "Sản phẩm $productName ($productCode) chỉ còn $quantity sản phẩm trong kho."
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("NAVIGATE_TO", "inventory_alerts")
            putExtra("ALERT_TYPE_FILTER", alertType)
            putExtra("SCROLL_TO_PRODUCT_ID", productId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            productId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(productId.toInt(), notification)
    }

    fun showAggregatedNotification(lowStockCount: Int, almostOutCount: Int, outOfStockCount: Int) {
        val total = lowStockCount + almostOutCount + outOfStockCount
        if (total == 0) return

        val title = "Có $total sản phẩm cần chú ý"
        val messageBuilder = StringBuilder()
        if (lowStockCount > 0) messageBuilder.append("• $lowStockCount tồn kho thấp\n")
        if (almostOutCount > 0) messageBuilder.append("• $almostOutCount gần hết\n")
        if (outOfStockCount > 0) messageBuilder.append("• $outOfStockCount hết hàng")

        val message = messageBuilder.toString().trimEnd()

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("NAVIGATE_TO", "inventory_alerts")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            9999, // Static ID for aggregated notification
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(message.replace("\n", ", "))
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(9999, notification)
    }
}
