package com.example.warehouse.data.database.dao

data class TopProductStat(
    val productId: Long,
    val productCode: String,
    val productName: String,
    val totalQty: Int
)
