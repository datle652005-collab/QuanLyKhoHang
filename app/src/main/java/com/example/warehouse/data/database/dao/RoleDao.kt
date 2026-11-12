package com.example.warehouse.data.database.dao

import androidx.room.*
import com.example.warehouse.data.database.entity.Role
import kotlinx.coroutines.flow.Flow

@Dao
interface RoleDao {
    @Query("SELECT * FROM roles ORDER BY roleId ASC")
    fun getAllRoles(): Flow<List<Role>>

    @Query("SELECT * FROM roles WHERE roleId = :roleId LIMIT 1")
    suspend fun getRoleById(roleId: Long): Role?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRole(role: Role): Long

    @Query("SELECT COUNT(*) FROM roles")
    suspend fun getRoleCount(): Int
}
