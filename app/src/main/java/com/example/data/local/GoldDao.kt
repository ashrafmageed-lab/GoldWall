package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.GoldItem
import com.example.data.model.GoldType
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for managing GoldItem (Bullion and Jewelry) in Room database.
 */
@Dao
interface GoldDao {

    @Query("SELECT * FROM gold_items ORDER BY weight DESC, createdAt DESC")
    fun getAllItems(): Flow<List<GoldItem>>

    @Query("SELECT * FROM gold_items WHERE type = :type ORDER BY weight DESC, createdAt DESC")
    fun getItemsByType(type: GoldType): Flow<List<GoldItem>>

    @Query("SELECT * FROM gold_items WHERE type = 'BULLION' ORDER BY weight DESC, createdAt DESC")
    fun getBullions(): Flow<List<GoldItem>>

    @Query("SELECT * FROM gold_items WHERE type = 'JEWELRY' ORDER BY weight DESC, createdAt DESC")
    fun getJewelry(): Flow<List<GoldItem>>

    @Query("SELECT * FROM gold_items WHERE id = :id LIMIT 1")
    suspend fun getItemById(id: Long): GoldItem?

    @Query("SELECT COUNT(*) FROM gold_items")
    suspend fun getItemsCount(): Int

    @Query("SELECT COUNT(*) FROM gold_items WHERE type = :type")
    suspend fun getCountByType(type: GoldType): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: GoldItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<GoldItem>): List<Long>

    @Update
    suspend fun updateItem(item: GoldItem)

    @Delete
    suspend fun deleteItem(item: GoldItem)

    @Query("DELETE FROM gold_items WHERE id = :id")
    suspend fun deleteItemById(id: Long)

    @Query("DELETE FROM gold_items WHERE type = :type")
    suspend fun deleteItemsByType(type: GoldType)

    @Query("DELETE FROM gold_items")
    suspend fun clearAllItems()

    @Query("SELECT SUM(weight) FROM gold_items WHERE type = :type")
    fun getTotalWeightByType(type: GoldType): Flow<Double?>

    @Query("SELECT SUM(cost) FROM gold_items WHERE type = :type")
    fun getTotalCostByType(type: GoldType): Flow<Double?>
}
