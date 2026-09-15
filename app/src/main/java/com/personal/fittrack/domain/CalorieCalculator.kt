package com.personal.fittrack.domain

enum class BiologicalSex { MALE, FEMALE }

enum class ActivityLevel(val multiplier: Double, val label: String) {
    SEDENTARY(1.2, "Sedentary (little or no exercise)"),
    LIGHT(1.375, "Light exercise (1-3 days/week)"),
    MODERATE(1.55, "Moderate exercise (3-5 days/week)"),
    ACTIVE(1.725, "Heavy exercise (6-7 days/week)"),
    VERY_ACTIVE(1.9, "Very heavy exercise / physical job")
}

enum class FitnessGoal(val calorieAdjustment: Int, val label: String) {
    LOSE(-500, "Lose weight"),
    MAINTAIN(0, "Maintain weight"),
    GAIN(400, "Gain weight")
}

/**
 * Estimates only - not medical advice. Uses the Mifflin-St Jeor equation, a widely
 * recognized estimate of basal metabolic rate for healthy adults.
 */
object CalorieCalculator {
    fun bmr(sex: BiologicalSex, weightKg: Double, heightCm: Double, ageYears: Int): Double {
        val base = 10 * weightKg + 6.25 * heightCm - 5 * ageYears
        return if (sex == BiologicalSex.MALE) base + 5 else base - 161
    }

    fun tdee(bmr: Double, activityLevel: ActivityLevel): Double = bmr * activityLevel.multiplier

    fun dailyCalorieTarget(
        sex: BiologicalSex,
        weightKg: Double,
        heightCm: Double,
        ageYears: Int,
        activityLevel: ActivityLevel,
        goal: FitnessGoal
    ): Int {
        val estimatedTdee = tdee(bmr(sex, weightKg, heightCm, ageYears), activityLevel)
        return (estimatedTdee + goal.calorieAdjustment).toInt().coerceAtLeast(1000)
    }
}
