package com.example.warehouse.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "export_details",
    foreignKeys = [
        ForeignKey(
            entity = ExportReceipt::class,
            parentColumns = ["exportId"],
            childColumns = ["exportId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Product::class,
            parentColumns = ["productId"],
            childColumns = ["productId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["exportId"]),
        Index(value = ["productId"])
    ]
)
data class ExportDetail(
    @PrimaryKey(autoGenerate = true)
    val exportDetailId: Long = 0,
    val exportId: Long,
    val productId: Long,
    val quantity: Int,
    val unitPrice: Double,
    val subtotal: Double = quantity * unitPrice
) {
    val id: Long get() = exportDetailId
    val receiptId: Long get() = exportId
    val price: Double get() = unitPrice
}
