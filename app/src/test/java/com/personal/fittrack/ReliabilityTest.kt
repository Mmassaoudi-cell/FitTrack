package com.personal.fittrack

import com.personal.fittrack.data.db.entity.*
import com.personal.fittrack.domain.*
import com.personal.fittrack.ui.nutrition.vision.*
import org.junit.Assert.*
import org.junit.Test

class ReliabilityTest {
    private fun set(session: Long, minutes: Long) = SetEntryEntity(sessionId = session, exerciseId = 1, orderInSession = 0, setIndex = 1, weightKg = 50.0, reps = 10, completedAtEpochMillis = minutes * 60000)
    @Test fun separatedWorkoutsDoNotCountTheGap() {
        assertEquals(20.0, WorkoutDuration.minutes(listOf(set(1, 0), set(1, 10), set(2, 480), set(2, 490))), 0.001)
    }
    @Test fun emptyWorkoutHasNoDuration() { assertEquals(0.0, WorkoutDuration.minutes(emptyList()), 0.0) }
    @Test fun localeDecimalIsAccepted() { assertEquals(72.5, InputValidation.number("72,5")!!, 0.001) }
    @Test fun nonFiniteInputIsRejected() { listOf("NaN", "Infinity", "1e999", "bad").forEach { assertNull(InputValidation.number(it)) } }
    @Test fun poundsRoundTrip() { assertEquals(80.0, WeightUnit.LB.toKg(WeightUnit.LB.display(80.0)), 0.0001) }
    @Test fun uncertainPhotoIsNotGuessed() { assertNull(FoodLabelMapper.bestMatch(listOf(RecognizedLabel("Pizza", 0.4f)))) }
    @Test fun genericDessertDoesNotMeanIceCream() { assertNull(FoodLabelMapper.bestMatch(listOf(RecognizedLabel("Dessert", 0.99f)))) }
    @Test fun substringDoesNotConfusePineappleWithApple() { assertNull(FoodLabelMapper.bestMatch(listOf(RecognizedLabel("Pineapple", 0.99f)))) }
    @Test fun specificLabelCanSuggestFood() { assertEquals("Pizza", FoodLabelMapper.bestMatch(listOf(RecognizedLabel("Pizza", 0.95f)))?.second) }
    @Test(expected = IllegalArgumentException::class) fun negativeCaloriesRejected() { InputValidation.food("Food", 100.0, -1.0) }
    @Test(expected = IllegalArgumentException::class) fun emptyPortionRejected() { InputValidation.food("Food", 0.0, 100.0) }
}
