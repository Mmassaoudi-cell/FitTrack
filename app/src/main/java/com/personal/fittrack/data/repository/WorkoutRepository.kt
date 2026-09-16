package com.personal.fittrack.data.repository

import com.personal.fittrack.data.db.dao.ExerciseDao
import com.personal.fittrack.data.db.dao.WorkoutDao
import com.personal.fittrack.data.db.entity.ExerciseEntity
import com.personal.fittrack.data.db.entity.SetEntryEntity
import com.personal.fittrack.data.db.entity.WorkoutSessionEntity
import com.personal.fittrack.data.seed.DefaultExercises
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import com.personal.fittrack.domain.currentDay
import com.personal.fittrack.domain.InputValidation
import java.time.LocalDate
import java.time.ZoneId

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
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

    suspend fun startSession(name: String, exercisePlan: String = ""): Long =
        workoutDao.startOrResume(name, exercisePlan)

    fun observeRoutines() = workoutDao.observeRoutines()
    suspend fun deleteRoutine(id: Long) = workoutDao.deleteRoutine(id)
    suspend fun saveRoutine(name: String, exerciseIds: List<Long>): Long {
        require(name.isNotBlank() && exerciseIds.isNotEmpty()) { "Choose a name and at least one exercise." }
        return workoutDao.insertRoutine(com.personal.fittrack.data.db.entity.WorkoutRoutineEntity(
            name = name.trim(), exerciseIds = exerciseIds.distinct().joinToString(",")
        ))
    }

    fun observeAllSets() = workoutDao.observeAllSets()

    suspend fun appendSet(sessionId: Long, exerciseId: Long, weightKg: Double, reps: Int): Long {
        require(InputValidation.nonNegative(weightKg) && reps in 1..999) { "Enter a valid weight and 1–999 reps." }
        return workoutDao.appendSet(sessionId, exerciseId, weightKg, reps)
    }

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

    suspend fun updateSet(set: SetEntryEntity) {
        require(InputValidation.nonNegative(set.weightKg) && set.reps in 1..999) { "Enter a valid weight and 1–999 reps." }
        workoutDao.updateSet(set)
    }
    suspend fun restoreSet(set: SetEntryEntity) = workoutDao.restoreDeletedSet(set)

    suspend fun deleteSet(id: Long) = workoutDao.deleteSet(id)

    fun observeSetsForExercise(exerciseId: Long): Flow<List<SetEntryEntity>> =
        workoutDao.observeSetsForExercise(exerciseId)

    suspend fun getAllSetsForExercise(exerciseId: Long): List<SetEntryEntity> =
        workoutDao.getAllSetsForExerciseSync(exerciseId)

    /** Suggests the weight/reps used last time this exercise was performed. */
    suspend fun getLastPerformance(exerciseId: Long): SetEntryEntity? =
        workoutDao.getRecentSetsForExercise(exerciseId, 1).firstOrNull()

    fun observeSetsToday(): Flow<List<SetEntryEntity>> = currentDay().flatMapLatest { (day, zone) ->
        val start = day.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
        workoutDao.observeSetsBetween(start, end)
    }
}
