package com.example.warehouse.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "products",
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["categoryId"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["productCode"], unique = true),
        Index(value = ["categoryId"])
    ]
)
data class Product(
    @PrimaryKey(autoGenerate = true)
    val productId: Long = 0,
    val productCode: String,
    val productName: String,
    val categoryId: Long? = null,
    val importPrice: Double = 0.0,
    val exportPrice: Double = 0.0,
    val minStock: Int = 10,
    val imagePath: String? = null,
    val description: String = "",
    val status: String = "ACTIVE",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val id: Long get() = productId
    val image: String get() = imagePath ?: ""
    val quantityInStock: Int get() = 0
}
