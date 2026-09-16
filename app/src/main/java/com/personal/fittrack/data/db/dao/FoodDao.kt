package com.personal.fittrack.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.personal.fittrack.data.db.entity.FoodItemEntity
import com.personal.fittrack.data.db.entity.FoodLogEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodDao {
    @Query("SELECT * FROM food_items ORDER BY name ASC")
    fun observeAllFoodItems(): Flow<List<FoodItemEntity>>

    @Query("SELECT * FROM food_items WHERE name LIKE '%' || :query || '%' ORDER BY name ASC")
    suspend fun searchFoodItems(query: String): List<FoodItemEntity>

    @Query("SELECT COUNT(*) FROM food_items")
    suspend fun countFoodItems(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertFoodItems(items: List<FoodItemEntity>)

    @Query("UPDATE food_items SET isFavorite = :favorite WHERE id = :id")
    suspend fun setFavorite(id: Long, favorite: Boolean)

    @Insert
    suspend fun insertFoodItem(item: FoodItemEntity): Long

    @Query("SELECT * FROM food_log_entries WHERE dateEpochDay = :epochDay ORDER BY loggedAtEpochMillis ASC")
    fun observeLogForDay(epochDay: Long): Flow<List<FoodLogEntryEntity>>

    @androidx.room.Update
    suspend fun updateLogEntry(entry: FoodLogEntryEntity)

    @androidx.room.Transaction
    suspend fun insertCustomAndLog(item: FoodItemEntity, entry: FoodLogEntryEntity): Long {
        val id = insertFoodItem(item)
        return insertLogEntry(entry.copy(foodItemId = id))
    }

    @Insert
    suspend fun insertLogEntry(entry: FoodLogEntryEntity): Long

    @Query("DELETE FROM food_log_entries WHERE id = :id")
    suspend fun deleteLogEntry(id: Long)

    @Query("SELECT * FROM food_log_entries ORDER BY loggedAtEpochMillis DESC LIMIT :limit")
    fun observeRecentLogEntries(limit: Int): Flow<List<FoodLogEntryEntity>>

    @Query("SELECT * FROM food_items")
    suspend fun getAllFoodItemsSync(): List<FoodItemEntity>

    @Query("SELECT * FROM food_log_entries")
    suspend fun getAllLogEntriesSync(): List<FoodLogEntryEntity>

    @Insert
    suspend fun insertLogEntries(entries: List<FoodLogEntryEntity>)

    @Query("DELETE FROM food_items")
    suspend fun clearFoodItems()

    @Query("DELETE FROM food_log_entries")
    suspend fun clearLogEntries()
}
