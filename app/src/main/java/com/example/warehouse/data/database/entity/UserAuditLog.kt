package com.example.warehouse.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_audit_logs")
data class UserAuditLog(
    @PrimaryKey(autoGenerate = true)
    val logId: Long = 0,
    val action: String, // "CREATE_ACCOUNT", "LOCK_ACCOUNT", "UNLOCK_ACCOUNT", "CHANGE_ROLE", "CHANGE_PASSWORD", "DELETE_ACCOUNT", "EDIT_PROFILE"
    val targetUserId: Long?,
    val targetUsername: String,
    val performedByUserId: Long?,
    val details: String,
    val timestamp: Long = System.currentTimeMillis()
)
