package com.example.warehouse.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "roles")
data class Role(
    @PrimaryKey(autoGenerate = true)
    val roleId: Long = 0,
    val roleName: String,
    val description: String = ""
)
