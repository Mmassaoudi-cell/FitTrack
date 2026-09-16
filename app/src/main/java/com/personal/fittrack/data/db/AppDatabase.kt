package com.personal.fittrack.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.personal.fittrack.data.db.dao.BodyWeightDao
import com.personal.fittrack.data.db.dao.ExerciseDao
import com.personal.fittrack.data.db.dao.FoodDao
import com.personal.fittrack.data.db.dao.WorkoutDao
import com.personal.fittrack.data.db.entity.BodyWeightEntity
import com.personal.fittrack.data.db.entity.ExerciseEntity
import com.personal.fittrack.data.db.entity.FoodItemEntity
import com.personal.fittrack.data.db.entity.FoodLogEntryEntity
import com.personal.fittrack.data.db.entity.SetEntryEntity
import com.personal.fittrack.data.db.entity.WorkoutSessionEntity

@Database(
    entities = [
        ExerciseEntity::class,
        WorkoutSessionEntity::class,
        SetEntryEntity::class,
        BodyWeightEntity::class,
        FoodItemEntity::class,
        com.personal.fittrack.data.db.entity.WorkoutRoutineEntity::class,
        FoodLogEntryEntity::class
    ],
    version = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun bodyWeightDao(): BodyWeightDao
    abstract fun foodDao(): FoodDao
}
