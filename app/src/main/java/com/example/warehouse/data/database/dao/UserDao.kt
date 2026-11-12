package com.example.warehouse.data.database.dao

import androidx.room.*
import com.example.warehouse.data.database.entity.User
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users ORDER BY userId DESC")
    fun getAllUsers(): Flow<List<User>>

    @Query("SELECT * FROM users WHERE LOWER(username) = LOWER(:username) LIMIT 1")
    suspend fun getUserByUsername(username: String): User?

    @Query("SELECT * FROM users WHERE userId = :id LIMIT 1")
    suspend fun getUserById(id: Long): User?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User): Long

    @Update
    suspend fun updateUser(user: User)


    @Query("SELECT COUNT(*) FROM users")
    suspend fun getUserCount(): Int

    @Query("SELECT COUNT(*) FROM users WHERE roleId = 1 AND UPPER(status) = 'ACTIVE' AND userId != :excludeUserId")
    suspend fun countActiveAdminsExcluding(excludeUserId: Long): Int

    @Query("SELECT COUNT(*) FROM users WHERE roleId = 1 AND UPPER(status) = 'ACTIVE'")
    suspend fun countActiveAdmins(): Int

    @Query("UPDATE users SET lastLoginAt = :timestamp WHERE userId = :userId")
    suspend fun updateLastLoginAt(userId: Long, timestamp: Long)

    @Query("SELECT COUNT(*) FROM import_receipts WHERE userId = :userId")
    suspend fun countImportReceiptsByUser(userId: Long): Int

    @Query("SELECT COUNT(*) FROM export_receipts WHERE userId = :userId")
    suspend fun countExportReceiptsByUser(userId: Long): Int

    @Query("SELECT COUNT(*) FROM stock_checks WHERE userId = :userId")
    suspend fun countStockChecksByUser(userId: Long): Int
}
