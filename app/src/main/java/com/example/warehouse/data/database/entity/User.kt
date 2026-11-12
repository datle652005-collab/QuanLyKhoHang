package com.example.warehouse.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "users",
    foreignKeys = [
        ForeignKey(
            entity = Role::class,
            parentColumns = ["roleId"],
            childColumns = ["roleId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["username"], unique = true),
        Index(value = ["roleId"])
    ]
)
data class User(
    @PrimaryKey(autoGenerate = true)
    val userId: Long = 0,
    val username: String,
    val password: String,
    val fullName: String,
    val phone: String = "",
    val email: String = "",
    val avatar: String? = null,
    val roleId: Long? = null,
    val status: String = "ACTIVE",
    val createdBy: Long? = null,
    val lastLoginAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val id: Long get() = userId
    val passwordHash: String get() = password
    val role: String get() = if (roleId == 1L) "ADMIN" else "STAFF"
}
