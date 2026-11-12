package com.example.warehouse.data.database.entity

data class StockHistory(
    val id: Long = 0,
    val productId: Long,
    val action: String, // "IMPORT", "EXPORT", "INVENTORY_ADJUST", "INITIAL"
    val quantityBefore: Int = 0,
    val quantityChange: Int,
    val quantityAfter: Int = 0,
    val referenceId: Long? = null,
    val referenceType: String? = null,
    val createdBy: String = "Admin",
    val createdAt: Long = System.currentTimeMillis(),
    val referenceCode: String = "",
    val partnerName: String? = null,
    val status: String = "",
    val unitPrice: Double = 0.0
)
