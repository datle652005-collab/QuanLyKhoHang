package com.example.warehouse.data.database.dao

import androidx.room.*
import com.example.warehouse.data.database.entity.ImportDetail
import com.example.warehouse.data.database.entity.ImportReceipt
import com.example.warehouse.data.database.relation.ImportReceiptWithDetails
import kotlinx.coroutines.flow.Flow

@Dao
interface ImportReceiptDao {
    @Transaction
    @Query("SELECT * FROM import_receipts ORDER BY createdAt DESC, importDate DESC, importId DESC")
    fun getAllImportReceipts(): Flow<List<ImportReceiptWithDetails>>

    @Transaction
    @Query("SELECT * FROM import_receipts WHERE importId = :id LIMIT 1")
    suspend fun getImportReceiptById(id: Long): ImportReceiptWithDetails?

    @Transaction
    @Query("SELECT * FROM import_receipts WHERE importDate >= :startTime AND importDate <= :endTime ORDER BY createdAt DESC, importDate DESC, importId DESC")
    fun getImportReceiptsByDateRange(startTime: Long, endTime: Long): Flow<List<ImportReceiptWithDetails>>

    @Query("SELECT COUNT(*) FROM import_receipts WHERE importDate >= :startOfDay AND status NOT IN ('Đã hủy', 'Đã xóa')")
    fun getTodayImportCount(startOfDay: Long): Flow<Int>

    @Query("""
        SELECT COALESCE(SUM(d.quantity), 0)
        FROM import_details d
        INNER JOIN import_receipts r ON d.importId = r.importId
        WHERE r.importDate >= :startTime AND r.importDate <= :endTime
          AND r.status NOT IN ('Đã hủy', 'Đã xóa')
    """)
    fun getTotalImportQuantityByDateRange(startTime: Long, endTime: Long): Flow<Int>

    @Query("""
        SELECT strftime('%Y-%m-%d', r.importDate / 1000, 'unixepoch', 'localtime') AS dateStr, 
               COALESCE(SUM(d.quantity), 0) AS totalQty
        FROM import_details d
        INNER JOIN import_receipts r ON d.importId = r.importId
        WHERE r.importDate >= :startTime AND r.importDate <= :endTime
          AND r.status NOT IN ('Đã hủy', 'Đã xóa')
        GROUP BY dateStr
        ORDER BY dateStr ASC
    """)
    fun getDailyImportStats(startTime: Long, endTime: Long): Flow<List<DailyQuantityStat>>

    @Query("""
        SELECT p.productId, p.productCode, p.productName, COALESCE(SUM(d.quantity), 0) AS totalQty
        FROM import_details d
        INNER JOIN import_receipts r ON d.importId = r.importId
        INNER JOIN products p ON d.productId = p.productId
        WHERE r.importDate >= :startTime AND r.importDate <= :endTime
          AND r.status NOT IN ('Đã hủy', 'Đã xóa')
        GROUP BY p.productId, p.productCode, p.productName
        ORDER BY totalQty DESC
        LIMIT :limit
    """)
    fun getTopImportedProducts(startTime: Long, endTime: Long, limit: Int = 10): Flow<List<TopProductStat>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImportReceipt(receipt: ImportReceipt): Long

    @Update
    suspend fun updateImportReceipt(receipt: ImportReceipt)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImportDetails(details: List<ImportDetail>)

    @Query("DELETE FROM import_details WHERE importId = :receiptId")
    suspend fun deleteImportDetailsForReceipt(receiptId: Long)

    @Query("SELECT COUNT(*) FROM import_receipts WHERE supplierId = :supplierId")
    suspend fun getImportReceiptCountBySupplier(supplierId: Long): Int

    @Delete
    suspend fun deleteImportReceipt(receipt: ImportReceipt)
}
