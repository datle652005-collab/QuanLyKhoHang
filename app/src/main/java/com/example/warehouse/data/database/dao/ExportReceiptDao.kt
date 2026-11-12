package com.example.warehouse.data.database.dao

import androidx.room.*
import com.example.warehouse.data.database.entity.ExportDetail
import com.example.warehouse.data.database.entity.ExportReceipt
import com.example.warehouse.data.database.relation.ExportReceiptWithDetails
import kotlinx.coroutines.flow.Flow

@Dao
interface ExportReceiptDao {
    @Transaction
    @Query("SELECT * FROM export_receipts ORDER BY createdAt DESC, exportDate DESC, exportId DESC")
    fun getAllExportReceipts(): Flow<List<ExportReceiptWithDetails>>

    @Transaction
    @Query("SELECT * FROM export_receipts WHERE exportId = :id LIMIT 1")
    suspend fun getExportReceiptById(id: Long): ExportReceiptWithDetails?

    @Transaction
    @Query("SELECT * FROM export_receipts WHERE exportDate >= :startTime AND exportDate <= :endTime ORDER BY createdAt DESC, exportDate DESC, exportId DESC")
    fun getExportReceiptsByDateRange(startTime: Long, endTime: Long): Flow<List<ExportReceiptWithDetails>>

    @Query("SELECT COUNT(*) FROM export_receipts WHERE exportDate >= :startOfDay AND status NOT IN ('Đã hủy', 'Đã xóa')")
    fun getTodayExportCount(startOfDay: Long): Flow<Int>

    @Query("""
        SELECT COALESCE(SUM(d.quantity), 0)
        FROM export_details d
        INNER JOIN export_receipts r ON d.exportId = r.exportId
        WHERE r.exportDate >= :startTime AND r.exportDate <= :endTime
          AND r.status NOT IN ('Đã hủy', 'Đã xóa')
    """)
    fun getTotalExportQuantityByDateRange(startTime: Long, endTime: Long): Flow<Int>

    @Query("""
        SELECT strftime('%Y-%m-%d', r.exportDate / 1000, 'unixepoch', 'localtime') AS dateStr, 
               COALESCE(SUM(d.quantity), 0) AS totalQty
        FROM export_details d
        INNER JOIN export_receipts r ON d.exportId = r.exportId
        WHERE r.exportDate >= :startTime AND r.exportDate <= :endTime
          AND r.status NOT IN ('Đã hủy', 'Đã xóa')
        GROUP BY dateStr
        ORDER BY dateStr ASC
    """)
    fun getDailyExportStats(startTime: Long, endTime: Long): Flow<List<DailyQuantityStat>>

    @Query("""
        SELECT p.productId, p.productCode, p.productName, COALESCE(SUM(d.quantity), 0) AS totalQty
        FROM export_details d
        INNER JOIN export_receipts r ON d.exportId = r.exportId
        INNER JOIN products p ON d.productId = p.productId
        WHERE r.exportDate >= :startTime AND r.exportDate <= :endTime
          AND r.status NOT IN ('Đã hủy', 'Đã xóa')
        GROUP BY p.productId, p.productCode, p.productName
        ORDER BY totalQty DESC
        LIMIT :limit
    """)
    fun getTopExportedProducts(startTime: Long, endTime: Long, limit: Int = 10): Flow<List<TopProductStat>>

    @Query("SELECT COUNT(*) FROM export_receipts WHERE exportDate >= :startOfDay AND exportDate <= :endOfDay")
    suspend fun getExportCountForDay(startOfDay: Long, endOfDay: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExportReceipt(receipt: ExportReceipt): Long

    @Update
    suspend fun updateExportReceipt(receipt: ExportReceipt)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExportDetails(details: List<ExportDetail>)

    @Query("DELETE FROM export_details WHERE exportId = :receiptId")
    suspend fun deleteExportDetailsForReceipt(receiptId: Long)

    @Delete
    suspend fun deleteExportReceipt(receipt: ExportReceipt)
}
