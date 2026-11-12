package com.example.warehouse.data.database.dao

import androidx.room.*
import com.example.warehouse.data.database.entity.StockCheck
import com.example.warehouse.data.database.entity.StockCheckDetail
import com.example.warehouse.data.database.relation.InventoryCheckWithDetails
import kotlinx.coroutines.flow.Flow

@Dao
interface InventoryCheckDao {
    @Transaction
    @Query("SELECT * FROM stock_checks ORDER BY checkDate DESC, checkId DESC")
    fun getAllInventoryChecks(): Flow<List<InventoryCheckWithDetails>>

    @Transaction
    @Query("SELECT * FROM stock_checks WHERE checkId = :id LIMIT 1")
    suspend fun getInventoryCheckById(id: Long): InventoryCheckWithDetails?

    @Transaction
    @Query("""
        SELECT DISTINCT sc.* FROM stock_checks sc
        INNER JOIN stock_check_details scd ON sc.checkId = scd.checkId
        WHERE scd.productId IN (:productIds) 
        AND sc.status IN ('Đang tiến hành', 'Yêu cầu kiểm kê lại', 'Đang chờ xét duyệt')
    """)
    suspend fun getActiveChecksForProducts(productIds: List<Long>): List<InventoryCheckWithDetails>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInventoryCheck(check: StockCheck): Long

    @Update
    suspend fun updateInventoryCheck(check: StockCheck)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInventoryDetails(details: List<StockCheckDetail>)

    @Update
    suspend fun updateInventoryDetails(details: List<StockCheckDetail>)

    @Query("DELETE FROM stock_check_details WHERE checkId = :checkId")
    suspend fun deleteInventoryDetailsByCheckId(checkId: Long)

    @Delete
    suspend fun deleteInventoryCheck(check: StockCheck)

    @Query("DELETE FROM stock_checks WHERE checkId = :checkId")
    suspend fun deleteInventoryCheckById(checkId: Long)
}

typealias StockCheckDao = InventoryCheckDao
