package com.example.warehouse.data.database.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.example.warehouse.data.database.entity.ImportDetail
import com.example.warehouse.data.database.entity.ImportReceipt
import com.example.warehouse.data.database.entity.Product
import com.example.warehouse.data.database.entity.Supplier
import com.example.warehouse.data.database.entity.User

data class ImportDetailWithProduct(
    @Embedded val detail: ImportDetail,
    @Relation(
        parentColumn = "productId",
        entityColumn = "productId"
    )
    val product: Product
)

data class ImportReceiptWithDetails(
    @Embedded val receipt: ImportReceipt,
    @Relation(
        parentColumn = "supplierId",
        entityColumn = "supplierId"
    )
    val supplier: Supplier? = null,
    @Relation(
        parentColumn = "userId",
        entityColumn = "userId"
    )
    val user: User? = null,
    @Relation(
        entity = ImportDetail::class,
        parentColumn = "importId",
        entityColumn = "importId"
    )
    val details: List<ImportDetailWithProduct> = emptyList()
)
