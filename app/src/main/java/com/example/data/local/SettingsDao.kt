package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.PortfolioSettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SettingsDao {
    @Query("SELECT * FROM portfolio_settings WHERE id = 1 LIMIT 1")
    fun getSettings(): Flow<PortfolioSettingsEntity?>

    @Query("SELECT * FROM portfolio_settings WHERE id = 1 LIMIT 1")
    suspend fun getSettingsSnapshot(): PortfolioSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(settings: PortfolioSettingsEntity)

    @Update
    suspend fun update(settings: PortfolioSettingsEntity)
}
