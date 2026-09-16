package com.personal.fittrack.data.export

import com.personal.fittrack.domain.InputValidation
import com.personal.fittrack.data.repository.MealType

object BackupValidator {
    fun validate(data: BackupData) {
        require(data.version in 1..2) { "This backup version is not supported. Update FitTrack first." }
        fun ids(values: List<Long>) { require(values.all { it > 0 } && values.distinct().size == values.size) { "The backup contains invalid or duplicate IDs." } }
        ids(data.exercises.map { it.id }); ids(data.sessions.map { it.id }); ids(data.sets.map { it.id })
        ids(data.foodItems.map { it.id }); ids(data.foodLogEntries.map { it.id }); ids(data.bodyWeights.map { it.id }); ids(data.routines.map { it.id })
        val exercises = data.exercises.map { it.id }.toSet()
        val sessions = data.sessions.map { it.id }.toSet()
        val foods = data.foodItems.map { it.id }.toSet()
        fun validPlan(plan: String) = plan.isEmpty() || plan.split(",").all { it.toLongOrNull() in exercises }
        data.exercises.forEach { require(it.name.isNotBlank() && InputValidation.positive(it.defaultIncrementKg)) { "Invalid exercise." } }
        data.sessions.forEach { require(it.name.isNotBlank() && it.startTimeEpochMillis >= 0 && (it.endTimeEpochMillis == null || it.endTimeEpochMillis >= it.startTimeEpochMillis) && validPlan(it.exercisePlan)) { "Invalid workout session." } }
        data.routines.forEach { require(it.name.isNotBlank() && it.exerciseIds.isNotBlank() && validPlan(it.exerciseIds)) { "Invalid routine." } }
        data.sets.forEach {
            require(it.sessionId in sessions && it.exerciseId in exercises && it.reps in 0..999 && InputValidation.nonNegative(it.weightKg) && it.setIndex > 0 && it.orderInSession >= 0) { "Invalid workout set or missing exercise." }
        }
        data.bodyWeights.forEach { require(InputValidation.positive(it.weightKg)) { "Invalid body weight." }; java.time.LocalDate.ofEpochDay(it.dateEpochDay) }
        data.foodItems.forEach { InputValidation.food(it.name, 100.0, it.caloriesPer100g, it.proteinPer100g, it.carbsPer100g, it.fatPer100g) }
        data.foodLogEntries.forEach {
            InputValidation.food(it.foodName, it.quantityGrams, it.calories, it.protein, it.carbs, it.fat)
            require(it.foodItemId == null || it.foodItemId in foods) { "Missing food reference." }
            require(MealType.entries.any { meal -> meal.name == it.mealType }) { "Invalid meal type." }
            java.time.LocalDate.ofEpochDay(it.dateEpochDay)
        }
        data.settings?.let {
            require(InputValidation.positive(it.weightIncrementKg) && InputValidation.positive(it.profileHeightCm) && it.profileAgeYears in 1..120 && (it.calorieTargetOverride == null || it.calorieTargetOverride > 0)) { "Invalid profile or settings." }
        }
    }
}
