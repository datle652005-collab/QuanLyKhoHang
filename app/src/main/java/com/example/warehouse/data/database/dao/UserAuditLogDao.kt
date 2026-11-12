package com.example.warehouse.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.warehouse.data.database.entity.UserAuditLog
import kotlinx.coroutines.flow.Flow

@Dao
interface UserAuditLogDao {
    @Insert
    suspend fun insertLog(log: UserAuditLog): Long

    @Query("SELECT * FROM user_audit_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<UserAuditLog>>
}
