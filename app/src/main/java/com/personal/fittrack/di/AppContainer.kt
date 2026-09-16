package com.personal.fittrack.di

import android.content.Context
import androidx.room.Room
import com.personal.fittrack.data.db.AppDatabase
import com.personal.fittrack.data.export.DataExportImportManager
import com.personal.fittrack.data.prefs.UserPreferences
import com.personal.fittrack.data.repository.BodyWeightRepository
import com.personal.fittrack.data.repository.NutritionRepository
import com.personal.fittrack.data.repository.WorkoutRepository

class AppContainer(context: Context) {
    val database: AppDatabase = Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java,
        "fittrack.db"
    ).addMigrations(com.personal.fittrack.data.db.MIGRATION_1_2).build()

    val userPreferences = UserPreferences(context.applicationContext)

    val workoutRepository = WorkoutRepository(database.exerciseDao(), database.workoutDao())
    val bodyWeightRepository = BodyWeightRepository(database.bodyWeightDao())
    val nutritionRepository = NutritionRepository(database.foodDao())
    val exportImportManager = DataExportImportManager(context.applicationContext, database, userPreferences)
}
