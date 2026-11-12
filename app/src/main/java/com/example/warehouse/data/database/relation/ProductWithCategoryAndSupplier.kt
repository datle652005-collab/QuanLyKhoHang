package com.example.warehouse.data.database.relation

import androidx.room.Embedded
import androidx.room.Ignore
import androidx.room.Relation
import com.example.warehouse.data.database.entity.Category
import com.example.warehouse.data.database.entity.Inventory
import com.example.warehouse.data.database.entity.Product
import com.example.warehouse.data.database.entity.Supplier

data class ProductWithCategoryAndSupplier(
    @Embedded val product: Product,
    
    @Relation(
        parentColumn = "categoryId",
        entityColumn = "categoryId"
    )
    val category: Category? = null,
    
    @Ignore
    val supplier: Supplier? = null,

    @Relation(
        parentColumn = "productId",
        entityColumn = "productId"
    )
    val inventory: Inventory? = null
) {
    // Secondary constructor for Room ignore mapping
    constructor(
        product: Product,
        category: Category?,
        inventory: Inventory?
    ) : this(product, category, null, inventory)

    val quantityInStock: Int get() = inventory?.quantity ?: 0
}
