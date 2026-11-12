package com.example.warehouse.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "stock_checks",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["userId"],
            childColumns = ["userId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["checkCode"], unique = false),
        Index(value = ["userId"])
    ]
)
data class StockCheck(
    @PrimaryKey(autoGenerate = true)
    val checkId: Long = 0,
    val checkCode: String = "",
    val title: String = "",
    val note: String = "",
    val checkDate: Long = System.currentTimeMillis(),
    val userId: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val status: String = "Đang tiến hành",
    val deleteReason: String? = null
) {
    val id: Long get() = checkId
    val code: String get() = checkCode.ifEmpty { "KK-${"%03d".format(checkId)}" }
}
