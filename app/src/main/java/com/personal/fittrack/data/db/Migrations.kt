package com.personal.fittrack.data.db

val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
        override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE food_items ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE workout_sessions ADD COLUMN exercisePlan TEXT NOT NULL DEFAULT ''")
            db.execSQL("CREATE TABLE IF NOT EXISTS workout_routines (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, exerciseIds TEXT NOT NULL)")
        }
    }
