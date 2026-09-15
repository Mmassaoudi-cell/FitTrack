package com.personal.fittrack.data.repository

import com.personal.fittrack.data.db.dao.ExerciseDao
import com.personal.fittrack.data.db.dao.WorkoutDao
import com.personal.fittrack.data.db.entity.ExerciseEntity
import com.personal.fittrack.data.db.entity.SetEntryEntity
import com.personal.fittrack.data.db.entity.WorkoutSessionEntity
import com.personal.fittrack.data.seed.DefaultExercises
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.ZoneId

class WorkoutRepository(
    private val exerciseDao: ExerciseDao,
    private val workoutDao: WorkoutDao
) {
    suspend fun ensureSeeded() {
        if (exerciseDao.count() == 0) {
            exerciseDao.insertAll(DefaultExercises.list)
        }
    }

    fun observeExercises(): Flow<List<ExerciseEntity>> = exerciseDao.observeAll()

    suspend fun getExercise(id: Long): ExerciseEntity? = exerciseDao.getById(id)

    suspend fun addCustomExercise(name: String, category: String, incrementKg: Double = 2.5): Long =
        exerciseDao.insert(ExerciseEntity(name = name, category = category, isCustom = true, defaultIncrementKg = incrementKg))

    fun observeSessions(): Flow<List<WorkoutSessionEntity>> = workoutDao.observeSessions()

    suspend fun startSession(name: String): Long =
        workoutDao.insertSession(WorkoutSessionEntity(name = name, startTimeEpochMillis = System.currentTimeMillis()))

    suspend fun endSession(session: WorkoutSessionEntity) {
        workoutDao.updateSession(session.copy(endTimeEpochMillis = System.currentTimeMillis()))
    }

    suspend fun getSession(id: Long): WorkoutSessionEntity? = workoutDao.getSession(id)

    fun observeSetsForSession(sessionId: Long): Flow<List<SetEntryEntity>> =
        workoutDao.observeSetsForSession(sessionId)

    suspend fun getSetsForSessionSync(sessionId: Long): List<SetEntryEntity> =
        workoutDao.getSetsForSessionSync(sessionId)

    suspend fun addSet(
        sessionId: Long,
        exerciseId: Long,
        orderInSession: Int,
        setIndex: Int,
        weightKg: Double,
        reps: Int
    ): Long = workoutDao.insertSet(
        SetEntryEntity(
            sessionId = sessionId,
            exerciseId = exerciseId,
            orderInSession = orderInSession,
            setIndex = setIndex,
            weightKg = weightKg,
            reps = reps,
            completedAtEpochMillis = System.currentTimeMillis()
        )
    )

    suspend fun updateSet(set: SetEntryEntity) = workoutDao.updateSet(set)

    suspend fun deleteSet(id: Long) = workoutDao.deleteSet(id)

    fun observeSetsForExercise(exerciseId: Long): Flow<List<SetEntryEntity>> =
        workoutDao.observeSetsForExercise(exerciseId)

    suspend fun getAllSetsForExercise(exerciseId: Long): List<SetEntryEntity> =
        workoutDao.getAllSetsForExerciseSync(exerciseId)

    /** Suggests the weight/reps used last time this exercise was performed. */
    suspend fun getLastPerformance(exerciseId: Long): SetEntryEntity? =
        workoutDao.getRecentSetsForExercise(exerciseId, 1).firstOrNull()

    fun observeSetsToday(): Flow<List<SetEntryEntity>> {
        val zone = ZoneId.systemDefault()
        val startOfDay = LocalDate.now(zone).atStartOfDay(zone).toInstant().toEpochMilli()
        val endOfDay = LocalDate.now(zone).plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
        return workoutDao.observeSetsBetween(startOfDay, endOfDay)
    }
}
