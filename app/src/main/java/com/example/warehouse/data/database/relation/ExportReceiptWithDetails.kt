package com.example.warehouse.data.database.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.example.warehouse.data.database.entity.ExportDetail
import com.example.warehouse.data.database.entity.ExportReceipt
import com.example.warehouse.data.database.entity.Product
import com.example.warehouse.data.database.entity.User

data class ExportDetailWithProduct(
    @Embedded val detail: ExportDetail,
    @Relation(
        parentColumn = "productId",
        entityColumn = "productId"
    )
    val product: Product
)

data class ExportReceiptWithDetails(
    @Embedded val receipt: ExportReceipt,
    @Relation(
        parentColumn = "userId",
        entityColumn = "userId"
    )
    val user: User? = null,
    @Relation(
        entity = ExportDetail::class,
        parentColumn = "exportId",
        entityColumn = "exportId"
    )
    val details: List<ExportDetailWithProduct> = emptyList()
)
