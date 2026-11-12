package com.example.warehouse.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "import_details",
    foreignKeys = [
        ForeignKey(
            entity = ImportReceipt::class,
            parentColumns = ["importId"],
            childColumns = ["importId"],
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
        Index(value = ["importId"]),
        Index(value = ["productId"])
    ]
)
data class ImportDetail(
    @PrimaryKey(autoGenerate = true)
    val importDetailId: Long = 0,
    val importId: Long,
    val productId: Long,
    val quantity: Int,
    val unitPrice: Double,
    val subtotal: Double = quantity * unitPrice
) {
    val id: Long get() = importDetailId
    val receiptId: Long get() = importId
    val price: Double get() = unitPrice
}
