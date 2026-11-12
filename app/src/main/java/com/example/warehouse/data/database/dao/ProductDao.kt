package com.example.warehouse.data.database.dao

import androidx.room.*
import com.example.warehouse.data.database.entity.Product
import com.example.warehouse.data.database.relation.ProductWithCategoryAndSupplier
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Transaction
    @Query("SELECT * FROM products WHERE UPPER(status) = 'ACTIVE' ORDER BY createdAt DESC, productId DESC")
    fun getActiveProductsWithDetails(): Flow<List<ProductWithCategoryAndSupplier>>

    @Transaction
    @Query("SELECT * FROM products ORDER BY createdAt DESC, productId DESC")
    fun getAllProductsWithDetails(): Flow<List<ProductWithCategoryAndSupplier>>

    @Transaction
    @Query("SELECT * FROM products WHERE productId = :id LIMIT 1")
    suspend fun getProductWithDetailsById(id: Long): ProductWithCategoryAndSupplier?

    @Query("SELECT * FROM products WHERE productId = :id LIMIT 1")
    suspend fun getProductById(id: Long): Product?

    @Transaction
    @Query("SELECT * FROM products WHERE productCode = :code LIMIT 1")
    suspend fun getProductByBarcodeOrCode(code: String): ProductWithCategoryAndSupplier?

    @Transaction
    @Query("""
        SELECT * FROM products 
        WHERE UPPER(status) = 'ACTIVE'
          AND (productName LIKE '%' || :query || '%' 
               OR productCode LIKE '%' || :query || '%')
        ORDER BY createdAt DESC, productId DESC
    """)
    fun searchActiveProducts(query: String): Flow<List<ProductWithCategoryAndSupplier>>

    @Transaction
    @Query("""
        SELECT * FROM products 
        WHERE productName LIKE '%' || :query || '%' 
           OR productCode LIKE '%' || :query || '%'
        ORDER BY createdAt DESC, productId DESC
    """)
    fun searchAllProducts(query: String): Flow<List<ProductWithCategoryAndSupplier>>

    @Transaction
    @Query("SELECT * FROM products WHERE categoryId = :categoryId AND UPPER(status) = 'ACTIVE' ORDER BY createdAt DESC, productId DESC")
    fun getActiveProductsByCategory(categoryId: Long): Flow<List<ProductWithCategoryAndSupplier>>

    @Transaction
    @Query("SELECT * FROM products WHERE categoryId = :categoryId ORDER BY createdAt DESC, productId DESC")
    fun getProductsByCategory(categoryId: Long): Flow<List<ProductWithCategoryAndSupplier>>

    @Query("SELECT COUNT(*) FROM products WHERE categoryId = :categoryId AND UPPER(status) = 'ACTIVE'")
    suspend fun getActiveProductCountByCategory(categoryId: Long): Int

    @Query("SELECT COUNT(*) FROM products WHERE LOWER(productCode) = LOWER(:code) AND UPPER(status) = 'ACTIVE' AND productId != :excludeId")
    suspend fun countActiveProductCode(code: String, excludeId: Long = 0): Int

    @Query("SELECT COUNT(*) FROM products WHERE LOWER(productName) = LOWER(:name) AND UPPER(status) = 'ACTIVE' AND productId != :excludeId")
    suspend fun countActiveProductName(name: String, excludeId: Long = 0): Int

    @Query("SELECT COUNT(*) FROM import_details WHERE productId = :productId")
    suspend fun countImportDetailsByProduct(productId: Long): Int

    @Query("SELECT COUNT(*) FROM export_details WHERE productId = :productId")
    suspend fun countExportDetailsByProduct(productId: Long): Int

    @Query("SELECT COUNT(*) FROM stock_check_details WHERE productId = :productId")
    suspend fun countStockCheckDetailsByProduct(productId: Long): Int

    @Transaction
    @Query("""
        SELECT products.* FROM products 
        LEFT JOIN inventory ON products.productId = inventory.productId 
        WHERE UPPER(products.status) = 'ACTIVE'
          AND COALESCE(inventory.quantity, 0) <= products.minStock 
        ORDER BY 
            CASE WHEN COALESCE(inventory.quantity, 0) = 0 THEN 0 ELSE 1 END ASC,
            COALESCE(inventory.quantity, 0) ASC,
            products.productName ASC
    """)
    fun getLowStockProducts(): Flow<List<ProductWithCategoryAndSupplier>>

    @Transaction
    @Query("""
        SELECT products.* FROM products 
        LEFT JOIN inventory ON products.productId = inventory.productId 
        WHERE UPPER(products.status) = 'ACTIVE'
          AND COALESCE(inventory.quantity, 0) <= products.minStock 
          AND (:categoryId IS NULL OR :categoryId = 0 OR products.categoryId = :categoryId)
        ORDER BY 
            CASE WHEN COALESCE(inventory.quantity, 0) = 0 THEN 0 ELSE 1 END ASC,
            COALESCE(inventory.quantity, 0) ASC,
            products.productName ASC
    """)
    fun getLowStockProductsFiltered(categoryId: Long?): Flow<List<ProductWithCategoryAndSupplier>>

    @Query("SELECT COUNT(*) FROM products WHERE UPPER(status) = 'ACTIVE'")
    fun getProductCount(): Flow<Int>

    @Query("""
        SELECT COALESCE(SUM(i.quantity), 0) 
        FROM inventory i
        INNER JOIN products p ON i.productId = p.productId 
        WHERE UPPER(p.status) = 'ACTIVE'
    """)
    fun getTotalStockQuantity(): Flow<Int>

    @Query("""
        SELECT 
            COALESCE(c.categoryId, 0) AS categoryId,
            COALESCE(c.categoryName, 'Chưa phân loại') AS categoryName,
            COUNT(p.productId) AS productCount,
            COALESCE(SUM(i.quantity), 0) AS totalQuantity
        FROM products p
        LEFT JOIN categories c ON p.categoryId = c.categoryId
        LEFT JOIN inventory i ON p.productId = i.productId
        WHERE UPPER(p.status) = 'ACTIVE'
        GROUP BY COALESCE(c.categoryId, 0), COALESCE(c.categoryName, 'Chưa phân loại')
        ORDER BY totalQuantity DESC
    """)
    fun getCategoryStockStats(): Flow<List<CategoryStockStat>>

    @Query("""
        SELECT COUNT(*) FROM products 
        LEFT JOIN inventory ON products.productId = inventory.productId 
        WHERE UPPER(products.status) = 'ACTIVE'
          AND COALESCE(inventory.quantity, 0) <= products.minStock
    """)
    fun getLowStockCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: Product): Long

    @Update
    suspend fun updateProduct(product: Product)

    @Delete
    suspend fun deleteProduct(product: Product)
}
