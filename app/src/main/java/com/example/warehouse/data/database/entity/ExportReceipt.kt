package com.example.warehouse.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "export_receipts",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["userId"],
            childColumns = ["userId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["exportCode"], unique = true),
        Index(value = ["userId"])
    ]
)
data class ExportReceipt(
    @PrimaryKey(autoGenerate = true)
    val exportId: Long = 0,
    val exportCode: String,
    val userId: Long? = null,
    val exportDate: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val recipient: String = "",
    val totalAmount: Double = 0.0,
    val status: String = "Completed",
    val note: String = "",
    val deleteReason: String? = null
) {
    val id: Long get() = exportId
    val code: String get() = exportCode
}
