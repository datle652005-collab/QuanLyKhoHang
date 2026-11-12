package com.example.warehouse.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "inventory",
    foreignKeys = [
        ForeignKey(
            entity = Product::class,
            parentColumns = ["productId"],
            childColumns = ["productId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["productId"], unique = true)
    ]
)
data class Inventory(
    @PrimaryKey(autoGenerate = true)
    val inventoryId: Long = 0,
    val productId: Long,
    val quantity: Int = 0,
    val updatedAt: Long = System.currentTimeMillis()
)
