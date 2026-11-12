package com.example.warehouse.data.database.dao

data class CategoryStockStat(
    val categoryId: Long,
    val categoryName: String,
    val productCount: Int,
    val totalQuantity: Int
)
