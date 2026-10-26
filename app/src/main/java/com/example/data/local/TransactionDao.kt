package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.TransactionRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transaction_records ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<TransactionRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(record: TransactionRecord): Long

    @Query("SELECT * FROM transaction_records ORDER BY timestamp DESC")
    suspend fun getAllTransactionsSnapshot(): List<TransactionRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(records: List<TransactionRecord>)

    @Query("DELETE FROM transaction_records")
    suspend fun clearTransactions()
}
