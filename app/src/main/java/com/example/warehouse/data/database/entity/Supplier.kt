package com.example.warehouse.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "suppliers")
data class Supplier(
    @PrimaryKey(autoGenerate = true)
    val supplierId: Long = 0,
    val supplierCode: String = "",
    val supplierName: String,
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val contactPerson: String = "",
    val taxCode: String = "",
    val note: String = "",
    val status: String = "ACTIVE",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val id: Long get() = supplierId
    val name: String get() = supplierName
}
