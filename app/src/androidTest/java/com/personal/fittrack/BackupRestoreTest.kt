package com.personal.fittrack

import android.net.Uri
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.personal.fittrack.data.db.AppDatabase
import com.personal.fittrack.data.db.entity.*
import com.personal.fittrack.data.export.*
import com.personal.fittrack.data.prefs.*
import com.personal.fittrack.domain.WeightUnit
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BackupRestoreTest {
    private lateinit var testDirectory: java.io.File
    private lateinit var db: AppDatabase
    private lateinit var manager: DataExportImportManager
    private lateinit var preferences: FakeSettings
    private class FakeSettings : SettingsStore {
        override val settings = MutableStateFlow(AppSettings())
        var failNext = false
        override suspend fun restore(settings: AppSettings) {
            if (failNext) { failNext = false; error("Simulated settings write failure") }
            this.settings.value = settings
        }
    }
    @Before fun setup() {
        val target = InstrumentationRegistry.getInstrumentation().targetContext
        testDirectory = java.io.File(target.cacheDir, "backup-test-${System.nanoTime()}").apply { mkdirs() }
        val context = object : android.content.ContextWrapper(target) {
            override fun getFilesDir() = testDirectory
            override fun getExternalFilesDir(type: String?) = testDirectory
            override fun getApplicationInfo() = android.content.pm.ApplicationInfo(target.applicationInfo).apply { dataDir = testDirectory.path }
        }
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        preferences = FakeSettings()
        manager = DataExportImportManager(context, db, preferences)
    }
    @After fun close() { db.close(); testDirectory.deleteRecursively() }
    private suspend fun seed() {
        db.exerciseDao().insert(ExerciseEntity(1, "Squat", "Legs"))
        val session = db.workoutDao().startOrResume("Leg day", "1")
        db.workoutDao().appendSet(session, 1, 80.0, 8)
        db.workoutDao().insertRoutine(WorkoutRoutineEntity(1, "Leg day", "1"))
        db.foodDao().insertFoodItem(FoodItemEntity(1, "Rice", 130.0, 2.0, 28.0, 0.2, isFavorite = true))
    }
    @Test fun exportRestoreRoundTripIncludesPreferencesAndRecoveryCopy() = runBlocking<Unit> {
        seed()
        preferences.settings.value = AppSettings(weightUnit = WeightUnit.LB)
        val original = manager.exportToFile()
        try {
            val backup = manager.readBackup(Uri.fromFile(original))
            db.bodyWeightDao().insert(BodyWeightEntity(dateEpochDay = 20000, weightKg = 70.0))
            preferences.settings.value = AppSettings()
            val recovery = manager.restore(backup)
            try {
                assertEquals(WeightUnit.LB, preferences.settings.value.weightUnit)
                assertTrue(db.bodyWeightDao().getAllSync().isEmpty())
                assertEquals(80.0, db.workoutDao().getAllSetsSync().single().weightKg, 0.001)
                assertTrue(db.foodDao().getAllFoodItemsSync().single().isFavorite)
                assertEquals("Leg day", db.workoutDao().getRoutines().single().name)
                assertEquals(1, manager.readBackup(Uri.fromFile(recovery)).bodyWeights.size)
            } finally { recovery.delete() }
        } finally { original.delete() }
    }
    @Test fun invalidImportLeavesExistingRecordsUntouched() = runBlocking<Unit> {
        seed()
        val file = manager.exportToFile()
        try {
            val invalid = manager.readBackup(Uri.fromFile(file)).copy(version = 100)
            try { manager.restore(invalid); fail("Expected invalid version") } catch (_: IllegalArgumentException) { }
            assertEquals(1, db.workoutDao().getAllSetsSync().size)
        } finally { file.delete() }
    }
    @Test fun settingsFailureRollsBackDatabaseReplacement() = runBlocking<Unit> {
        seed()
        val file = manager.exportToFile()
        try {
            val backup = manager.readBackup(Uri.fromFile(file)).copy(bodyWeights = listOf(BodyWeightEntity(1, 20000, 65.0)))
            preferences.failNext = true
            try { manager.restore(backup); fail("Expected simulated failure") } catch (_: IllegalStateException) { }
            assertTrue(db.bodyWeightDao().getAllSync().isEmpty())
            assertEquals(1, db.workoutDao().getAllSetsSync().size)
        } finally { file.delete() }
    }
}
