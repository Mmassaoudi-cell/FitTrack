package com.personal.fittrack.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Transaction
import com.personal.fittrack.data.db.entity.SetEntryEntity
import com.personal.fittrack.data.db.entity.WorkoutSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {
    @Query("SELECT * FROM workout_routines ORDER BY name")
    fun observeRoutines(): Flow<List<com.personal.fittrack.data.db.entity.WorkoutRoutineEntity>>

    @Query("SELECT * FROM workout_routines")
    suspend fun getRoutines(): List<com.personal.fittrack.data.db.entity.WorkoutRoutineEntity>

    @Insert
    suspend fun insertRoutine(routine: com.personal.fittrack.data.db.entity.WorkoutRoutineEntity): Long

    @Query("DELETE FROM workout_routines WHERE id = :id")
    suspend fun deleteRoutine(id: Long)

    @Query("DELETE FROM workout_routines")
    suspend fun clearRoutines()

    @Query("SELECT * FROM workout_sessions WHERE endTimeEpochMillis IS NULL ORDER BY startTimeEpochMillis DESC LIMIT 1")
    suspend fun getActiveSession(): WorkoutSessionEntity?

    @Query("SELECT * FROM set_entries ORDER BY completedAtEpochMillis DESC")
    fun observeAllSets(): Flow<List<SetEntryEntity>>

    @Transaction
    suspend fun startOrResume(name: String, exercisePlan: String = ""): Long = getActiveSession()?.id ?: insertSession(
        WorkoutSessionEntity(name = name, startTimeEpochMillis = System.currentTimeMillis(), exercisePlan = exercisePlan)
    )

    @Transaction
    suspend fun appendSet(sessionId: Long, exerciseId: Long, weightKg: Double, reps: Int): Long {
        val session = getSession(sessionId) ?: error("This workout no longer exists.")
        check(session.endTimeEpochMillis == null) { "This workout has already finished." }
        val sets = getSetsForSessionSync(sessionId)
        val matching = sets.filter { it.exerciseId == exerciseId }
        return insertSet(SetEntryEntity(
            sessionId = sessionId, exerciseId = exerciseId,
            orderInSession = matching.firstOrNull()?.orderInSession ?: ((sets.maxOfOrNull { it.orderInSession } ?: -1) + 1),
            setIndex = (matching.maxOfOrNull { it.setIndex } ?: 0) + 1,
            weightKg = weightKg, reps = reps, completedAtEpochMillis = System.currentTimeMillis()
        ))
    }

    @Transaction
    suspend fun restoreDeletedSet(set: SetEntryEntity): Long {
        val sets = getSetsForSessionSync(set.sessionId)
        val sameExercise = sets.filter { it.exerciseId == set.exerciseId }
        val index = if (sameExercise.any { it.setIndex == set.setIndex }) (sameExercise.maxOf { it.setIndex } + 1) else set.setIndex
        val order = sameExercise.firstOrNull()?.orderInSession ?: ((sets.maxOfOrNull { it.orderInSession } ?: -1) + 1)
        return insertSet(set.copy(setIndex = index, orderInSession = order))
    }

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
