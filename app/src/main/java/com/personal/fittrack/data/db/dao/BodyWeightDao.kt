package com.personal.fittrack.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.personal.fittrack.data.db.entity.BodyWeightEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BodyWeightDao {
    @Query("SELECT * FROM body_weight_entries ORDER BY dateEpochDay ASC")
    fun observeAll(): Flow<List<BodyWeightEntity>>

    @Query("SELECT * FROM body_weight_entries ORDER BY dateEpochDay DESC LIMIT 1")
    fun observeLatest(): Flow<BodyWeightEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: BodyWeightEntity)

    @Query("DELETE FROM body_weight_entries WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM body_weight_entries")
    suspend fun getAllSync(): List<BodyWeightEntity>

    @Insert
    suspend fun insertAll(entries: List<BodyWeightEntity>)

    @Query("DELETE FROM body_weight_entries")
    suspend fun clear()
}
