package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.ValuationRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface ValuationDao {
    @Query("SELECT * FROM valuation_records ORDER BY timestamp DESC")
    fun getAllValuations(): Flow<List<ValuationRecord>>

    @Query("SELECT * FROM valuation_records ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestValuation(): ValuationRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertValuation(record: ValuationRecord): Long

    @Query("DELETE FROM valuation_records WHERE id = :id")
    suspend fun deleteValuationById(id: Long)

    @Query("SELECT * FROM valuation_records ORDER BY timestamp DESC")
    suspend fun getAllValuationsSnapshot(): List<ValuationRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertValuations(records: List<ValuationRecord>)

    @Query("DELETE FROM valuation_records")
    suspend fun clearValuations()
}
