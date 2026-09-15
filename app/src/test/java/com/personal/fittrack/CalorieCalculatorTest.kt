package com.personal.fittrack

import com.personal.fittrack.domain.ActivityLevel
import com.personal.fittrack.domain.BiologicalSex
import com.personal.fittrack.domain.CalorieCalculator
import com.personal.fittrack.domain.FitnessGoal
import org.junit.Assert.assertEquals
import org.junit.Test

class CalorieCalculatorTest {

    @Test
    fun `bmr for male matches Mifflin-St Jeor formula`() {
        // 10*80 + 6.25*180 - 5*30 + 5 = 800 + 1125 - 150 + 5 = 1780
        val bmr = CalorieCalculator.bmr(BiologicalSex.MALE, weightKg = 80.0, heightCm = 180.0, ageYears = 30)
        assertEquals(1780.0, bmr, 0.001)
    }

    @Test
    fun `bmr for female matches Mifflin-St Jeor formula`() {
        // 10*60 + 6.25*165 - 5*25 - 161 = 600 + 1031.25 - 125 - 161 = 1345.25
        val bmr = CalorieCalculator.bmr(BiologicalSex.FEMALE, weightKg = 60.0, heightCm = 165.0, ageYears = 25)
        assertEquals(1345.25, bmr, 0.001)
    }

    @Test
    fun `tdee applies activity multiplier`() {
        val tdee = CalorieCalculator.tdee(2000.0, ActivityLevel.MODERATE)
        assertEquals(3100.0, tdee, 0.001)
    }

    @Test
    fun `daily target applies goal adjustment on top of tdee`() {
        val target = CalorieCalculator.dailyCalorieTarget(
            sex = BiologicalSex.MALE,
            weightKg = 80.0,
            heightCm = 180.0,
            ageYears = 30,
            activityLevel = ActivityLevel.SEDENTARY,
            goal = FitnessGoal.LOSE
        )
        // bmr=1780, tdee=1780*1.2=2136, target=2136-500=1636
        assertEquals(1636, target)
    }

    @Test
    fun `daily target never drops below the safety floor`() {
        val target = CalorieCalculator.dailyCalorieTarget(
            sex = BiologicalSex.FEMALE,
            weightKg = 45.0,
            heightCm = 150.0,
            ageYears = 20,
            activityLevel = ActivityLevel.SEDENTARY,
            goal = FitnessGoal.LOSE
        )
        assert(target >= 1000)
    }
}
