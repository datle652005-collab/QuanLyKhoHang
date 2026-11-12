package com.example.warehouse.data.database.dao

import androidx.room.*
import com.example.warehouse.data.database.entity.Inventory
import kotlinx.coroutines.flow.Flow

@Dao
interface InventoryDao {
    @Query("SELECT * FROM inventory WHERE productId = :productId LIMIT 1")
    suspend fun getInventoryByProductId(productId: Long): Inventory?

    @Query("SELECT * FROM inventory")
    fun getAllInventory(): Flow<List<Inventory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateInventory(inventory: Inventory): Long

    @Query("UPDATE inventory SET quantity = :quantity, updatedAt = :updatedAt WHERE productId = :productId")
    suspend fun updateStock(productId: Long, quantity: Int, updatedAt: Long = System.currentTimeMillis())

    @Delete
    suspend fun deleteInventory(inventory: Inventory)

    @Query("DELETE FROM inventory WHERE productId = :productId")
    suspend fun deleteInventoryByProductId(productId: Long)

    @Query("UPDATE inventory SET quantity = quantity - :qty, updatedAt = :updatedAt WHERE productId = :productId AND quantity >= :qty")
    suspend fun decreaseStockSecure(productId: Long, qty: Int, updatedAt: Long = System.currentTimeMillis()): Int

    @Query("UPDATE inventory SET quantity = quantity + :qty, updatedAt = :updatedAt WHERE productId = :productId")
    suspend fun increaseStockSecure(productId: Long, qty: Int, updatedAt: Long = System.currentTimeMillis()): Int
}
