package com.example.warehouse.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "stock_check_details",
    foreignKeys = [
        ForeignKey(
            entity = StockCheck::class,
            parentColumns = ["checkId"],
            childColumns = ["checkId"],
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
        Index(value = ["checkId"]),
        Index(value = ["productId"])
    ]
)
data class StockCheckDetail(
    @PrimaryKey(autoGenerate = true)
    val detailId: Long = 0,
    val checkId: Long,
    val productId: Long,
    val systemQuantity: Int,
    val actualQuantity: Int? = null,
    val difference: Int? = actualQuantity?.minus(systemQuantity),
    val reason: String = ""
) {
    val id: Long get() = detailId
}
