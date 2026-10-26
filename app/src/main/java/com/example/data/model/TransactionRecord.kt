package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transaction_records")
data class TransactionRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val actionType: String, // إضافة قطعة، تعديل، حذف، تقييم، تحديث كاش باك
    val title: String,
    val details: String,
    val amountOrWeight: String = ""
)
