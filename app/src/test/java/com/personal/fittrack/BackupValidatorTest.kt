package com.personal.fittrack

import com.personal.fittrack.data.export.*
import com.personal.fittrack.data.db.entity.*
import com.personal.fittrack.data.prefs.AppSettings
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class BackupValidatorTest {
    private fun backup() = BackupData(exportedAtEpochMillis = 1, exercises = listOf(ExerciseEntity(1, "Squat", "Legs")),
        sessions = listOf(WorkoutSessionEntity(1, "Workout", 1)), sets = listOf(SetEntryEntity(1, 1, 1, 0, 1, 60.0, 10, 2)),
        foodItems = emptyList(), foodLogEntries = emptyList(), bodyWeights = emptyList())
    @Test fun oldBackupWithoutSettingsStillLoads() {
        val json = Json.encodeToString(BackupData.serializer(), backup())
        val restored = Json.decodeFromString(BackupData.serializer(), json)
        BackupValidator.validate(restored)
        assertEquals(1, restored.version); assertNull(restored.settings)
    }
    @Test fun newBackupRoundTripsRoutinesAndSettings() {
        val original = backup().copy(version = 2, settings = AppSettings(), routines = listOf(WorkoutRoutineEntity(1, "Leg day", "1")))
        val restored = Json.decodeFromString(BackupData.serializer(), Json.encodeToString(BackupData.serializer(), original))
        BackupValidator.validate(restored); assertEquals(original, restored)
    }
    @Test(expected = IllegalArgumentException::class) fun unknownVersionRejected() { BackupValidator.validate(backup().copy(version = 99)) }
    @Test(expected = IllegalArgumentException::class) fun orphanSetRejected() { BackupValidator.validate(backup().copy(exercises = emptyList())) }
    @Test(expected = IllegalArgumentException::class) fun duplicateIdsRejected() { val b = backup(); BackupValidator.validate(b.copy(sets = b.sets + b.sets)) }
    @Test(expected = IllegalArgumentException::class) fun unknownMealRejected() {
        BackupValidator.validate(backup().copy(foodLogEntries = listOf(FoodLogEntryEntity(1, 20000, "INVALID", null, "Rice", 100.0, 100.0, 0.0, 0.0, 0.0, 1))))
    }
    @Test(expected = IllegalArgumentException::class) fun invalidProfileRejected() { BackupValidator.validate(backup().copy(settings = AppSettings(profileAgeYears = -1))) }
    @Test(expected = IllegalArgumentException::class) fun brokenRoutineRejected() { BackupValidator.validate(backup().copy(routines = listOf(WorkoutRoutineEntity(1, "Missing", "99")))) }
}
