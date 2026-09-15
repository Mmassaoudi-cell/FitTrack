package com.personal.fittrack.data.seed

import com.personal.fittrack.data.db.entity.ExerciseEntity

object DefaultExercises {
    val list = listOf(
        ExerciseEntity(name = "Bench Press", category = "Chest"),
        ExerciseEntity(name = "Squat", category = "Legs"),
        ExerciseEntity(name = "Deadlift", category = "Back"),
        ExerciseEntity(name = "Shoulder Press", category = "Shoulders"),
        ExerciseEntity(name = "Biceps Curl", category = "Arms"),
        ExerciseEntity(name = "Triceps Extension", category = "Arms"),
        ExerciseEntity(name = "Lat Pulldown", category = "Back"),
        ExerciseEntity(name = "Row", category = "Back"),
        ExerciseEntity(name = "Leg Press", category = "Legs"),
        ExerciseEntity(name = "Leg Extension", category = "Legs"),
        ExerciseEntity(name = "Leg Curl", category = "Legs"),
        ExerciseEntity(name = "Push-ups", category = "Chest", defaultIncrementKg = 0.0),
        ExerciseEntity(name = "Pull-ups", category = "Back", defaultIncrementKg = 0.0),
        ExerciseEntity(name = "Running", category = "Cardio", defaultIncrementKg = 0.0),
        ExerciseEntity(name = "Cycling", category = "Cardio", defaultIncrementKg = 0.0)
    )
}
