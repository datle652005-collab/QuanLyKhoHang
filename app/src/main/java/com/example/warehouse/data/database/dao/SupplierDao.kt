package com.example.warehouse.data.database.dao

import androidx.room.*
import com.example.warehouse.data.database.entity.Supplier
import kotlinx.coroutines.flow.Flow

@Dao
interface SupplierDao {
    @Query("SELECT * FROM suppliers ORDER BY createdAt DESC, supplierId DESC")
    fun getAllSuppliers(): Flow<List<Supplier>>

    @Query("SELECT * FROM suppliers WHERE UPPER(status) = 'ACTIVE' ORDER BY createdAt DESC, supplierId DESC")
    fun getActiveSuppliers(): Flow<List<Supplier>>

    @Query("SELECT * FROM suppliers WHERE supplierId = :id LIMIT 1")
    suspend fun getSupplierById(id: Long): Supplier?

    @Query("SELECT * FROM suppliers WHERE (supplierName LIKE '%' || :query || '%' OR phone LIKE '%' || :query || '%') ORDER BY createdAt DESC, supplierId DESC")
    fun searchSuppliers(query: String): Flow<List<Supplier>>

    @Query("SELECT COUNT(*) FROM suppliers WHERE LOWER(TRIM(supplierName)) = LOWER(TRIM(:name)) AND supplierId != :excludeId")
    suspend fun countSupplierByName(name: String, excludeId: Long = 0): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSupplier(supplier: Supplier): Long

    @Update
    suspend fun updateSupplier(supplier: Supplier)

    @Delete
    suspend fun deleteSupplier(supplier: Supplier)

    @Query("SELECT COUNT(*) FROM suppliers")
    suspend fun getSupplierCount(): Int
}
