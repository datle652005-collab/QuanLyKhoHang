package com.example.warehouse.data.database.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.example.warehouse.data.database.entity.StockCheck
import com.example.warehouse.data.database.entity.StockCheckDetail
import com.example.warehouse.data.database.entity.Product
import com.example.warehouse.data.database.entity.User

data class InventoryDetailWithProduct(
    @Embedded val detail: StockCheckDetail,
    @Relation(
        parentColumn = "productId",
        entityColumn = "productId"
    )
    val product: Product
)

data class InventoryCheckWithDetails(
    @Embedded val check: StockCheck,
    @Relation(
        parentColumn = "userId",
        entityColumn = "userId"
    )
    val user: User? = null,
    @Relation(
        entity = StockCheckDetail::class,
        parentColumn = "checkId",
        entityColumn = "checkId"
    )
    val details: List<InventoryDetailWithProduct> = emptyList()
)

typealias StockCheckWithDetails = InventoryCheckWithDetails
