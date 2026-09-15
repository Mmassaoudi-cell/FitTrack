package com.personal.fittrack.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.personal.fittrack.data.db.entity.SetEntryEntity
import com.personal.fittrack.data.db.entity.WorkoutSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {
    @Insert
    suspend fun insertSession(session: WorkoutSessionEntity): Long

    @Update
    suspend fun updateSession(session: WorkoutSessionEntity)

    @Query("SELECT * FROM workout_sessions ORDER BY startTimeEpochMillis DESC")
    fun observeSessions(): Flow<List<WorkoutSessionEntity>>

    @Query("SELECT * FROM workout_sessions WHERE id = :id")
    suspend fun getSession(id: Long): WorkoutSessionEntity?

    @Insert
    suspend fun insertSet(set: SetEntryEntity): Long

    @Update
    suspend fun updateSet(set: SetEntryEntity)

    @Query("DELETE FROM set_entries WHERE id = :id")
    suspend fun deleteSet(id: Long)

    @Query("SELECT * FROM set_entries WHERE sessionId = :sessionId ORDER BY orderInSession ASC, setIndex ASC")
    fun observeSetsForSession(sessionId: Long): Flow<List<SetEntryEntity>>

    @Query("SELECT * FROM set_entries WHERE exerciseId = :exerciseId ORDER BY completedAtEpochMillis DESC")
    fun observeSetsForExercise(exerciseId: Long): Flow<List<SetEntryEntity>>

    @Query("SELECT * FROM set_entries WHERE exerciseId = :exerciseId ORDER BY completedAtEpochMillis DESC LIMIT :limit")
    suspend fun getRecentSetsForExercise(exerciseId: Long, limit: Int): List<SetEntryEntity>

    @Query("SELECT * FROM set_entries WHERE exerciseId = :exerciseId ORDER BY completedAtEpochMillis ASC")
    suspend fun getAllSetsForExerciseSync(exerciseId: Long): List<SetEntryEntity>

    @Query("SELECT * FROM set_entries WHERE completedAtEpochMillis BETWEEN :startMillis AND :endMillis")
    fun observeSetsBetween(startMillis: Long, endMillis: Long): Flow<List<SetEntryEntity>>

    @Query("SELECT * FROM set_entries WHERE sessionId = :sessionId")
    suspend fun getSetsForSessionSync(sessionId: Long): List<SetEntryEntity>

    @Query("SELECT * FROM workout_sessions")
    suspend fun getAllSessionsSync(): List<WorkoutSessionEntity>

    @Query("SELECT * FROM set_entries")
    suspend fun getAllSetsSync(): List<SetEntryEntity>

    @Insert
    suspend fun insertSessions(sessions: List<WorkoutSessionEntity>)

    @Insert
    suspend fun insertSets(sets: List<SetEntryEntity>)

    @Query("DELETE FROM set_entries")
    suspend fun clearSets()

    @Query("DELETE FROM workout_sessions")
    suspend fun clearSessions()
}
