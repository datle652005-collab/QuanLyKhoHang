package com.example.warehouse.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "import_receipts",
    foreignKeys = [
        ForeignKey(
            entity = Supplier::class,
            parentColumns = ["supplierId"],
            childColumns = ["supplierId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = User::class,
            parentColumns = ["userId"],
            childColumns = ["userId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["importCode"], unique = true),
        Index(value = ["supplierId"]),
        Index(value = ["userId"])
    ]
)
data class ImportReceipt(
    @PrimaryKey(autoGenerate = true)
    val importId: Long = 0,
    val importCode: String,
    val supplierId: Long? = null,
    val userId: Long? = null,
    val importDate: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val totalAmount: Double = 0.0,
    val status: String = "Đã lưu",
    val note: String = "",
    val supplierNameText: String? = null,
    val deleteReason: String? = null
) {
    val id: Long get() = importId
    val code: String get() = importCode
}
