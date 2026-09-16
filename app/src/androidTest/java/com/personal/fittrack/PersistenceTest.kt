package com.personal.fittrack

import android.content.Context
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.personal.fittrack.data.db.*
import com.personal.fittrack.data.db.entity.*
import kotlinx.coroutines.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.json.JSONObject

@RunWith(AndroidJUnit4::class)
class PersistenceTest {
    private lateinit var db: AppDatabase
    private val context: Context get() = InstrumentationRegistry.getInstrumentation().targetContext
    @Before fun setup() { db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build() }
    @After fun close() { db.close() }
    @Test fun concurrentSetLoggingAllocatesDistinctIndices() = runBlocking<Unit> {
        val exercise = db.exerciseDao().insert(ExerciseEntity(name = "Squat", category = "Legs"))
        val session = db.workoutDao().startOrResume("Training")
        coroutineScope { (1..20).map { async(Dispatchers.IO) { db.workoutDao().appendSet(session, exercise, 60.0, 10) } }.awaitAll() }
        val sets = db.workoutDao().getSetsForSessionSync(session)
        assertEquals((1..20).toList(), sets.map { it.setIndex }.sorted())
        assertEquals(1, sets.map { it.orderInSession }.distinct().size)
    }
    @Test fun startResumesExistingSessionAndFinishAllowsANewOne() = runBlocking<Unit> {
        val first = db.workoutDao().startOrResume("First")
        assertEquals(first, db.workoutDao().startOrResume("Second"))
        db.workoutDao().updateSession(db.workoutDao().getSession(first)!!.copy(endTimeEpochMillis = System.currentTimeMillis()))
        assertNotEquals(first, db.workoutDao().startOrResume("New workout"))
    }
    @Test fun finishedWorkoutRejectsNewSets() = runBlocking<Unit> {
        val exercise = db.exerciseDao().insert(ExerciseEntity(name = "Squat", category = "Legs"))
        val id = db.workoutDao().startOrResume("Training")
        db.workoutDao().updateSession(db.workoutDao().getSession(id)!!.copy(endTimeEpochMillis = System.currentTimeMillis()))
        try { db.workoutDao().appendSet(id, exercise, 20.0, 10); fail("Expected closed workout rejection") } catch (_: IllegalStateException) { }
    }
    @Test fun undoAfterAnotherSetDoesNotDuplicateSetNumbers() = runBlocking<Unit> {
        val exercise = db.exerciseDao().insert(ExerciseEntity(name = "Squat", category = "Legs"))
        val session = db.workoutDao().startOrResume("Training")
        db.workoutDao().appendSet(session, exercise, 60.0, 10)
        val second = db.workoutDao().appendSet(session, exercise, 65.0, 8)
        val deleted = db.workoutDao().getAllSetsSync().first { it.id == second }
        db.workoutDao().deleteSet(second)
        db.workoutDao().appendSet(session, exercise, 70.0, 6)
        db.workoutDao().restoreDeletedSet(deleted)
        assertEquals(listOf(1, 2, 3), db.workoutDao().getAllSetsSync().map { it.setIndex }.sorted())
    }
    @Test fun versionOneMigrationPreservesRecords() = runBlocking<Unit> {
        val name = "migration-test-${System.nanoTime()}.db"
        val testContext = InstrumentationRegistry.getInstrumentation().context
        val schema = JSONObject(testContext.assets.open("com.personal.fittrack.data.db.AppDatabase/1.json").bufferedReader().use { it.readText() }).getJSONObject("database")
        context.openOrCreateDatabase(name, Context.MODE_PRIVATE, null).use { sqlite ->
            val entities = schema.getJSONArray("entities")
            for (i in 0 until entities.length()) {
                val entity = entities.getJSONObject(i)
                sqlite.execSQL(entity.getString("createSql").replace("${'$'}{TABLE_NAME}", entity.getString("tableName")))
                val indexes = entity.getJSONArray("indices")
                for (j in 0 until indexes.length()) sqlite.execSQL(indexes.getJSONObject(j).getString("createSql").replace("${'$'}{TABLE_NAME}", entity.getString("tableName")))
            }
            val queries = schema.getJSONArray("setupQueries")
            for (i in 0 until queries.length()) sqlite.execSQL(queries.getString(i))
            sqlite.execSQL("INSERT INTO exercises VALUES (1, 'Squat', 'Legs', 0, 2.5)")
            sqlite.execSQL("INSERT INTO workout_sessions VALUES (1, 'Legacy workout', 1, 2)")
            sqlite.execSQL("INSERT INTO set_entries VALUES (1, 1, 1, 0, 1, 60.0, 10, 2)")
            sqlite.execSQL("INSERT INTO food_items VALUES (1, 'Rice', 130.0, 2.0, 28.0, 0.2, 0)")
            sqlite.version = 1
        }
        try {
            val migrated = Room.databaseBuilder(context, AppDatabase::class.java, name).addMigrations(MIGRATION_1_2).build()
            try {
                assertEquals(60.0, migrated.workoutDao().getAllSetsSync().single().weightKg, 0.001)
                assertEquals("", migrated.workoutDao().getSession(1)!!.exercisePlan)
                assertFalse(migrated.foodDao().getAllFoodItemsSync().single().isFavorite)
                assertTrue(migrated.workoutDao().getRoutines().isEmpty())
            } finally { migrated.close() }
        } finally { context.deleteDatabase(name) }
    }
}
