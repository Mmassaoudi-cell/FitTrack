package com.personal.fittrack

import com.personal.fittrack.data.db.entity.SetEntryEntity
import com.personal.fittrack.domain.VolumeCalculator
import org.junit.Assert.assertEquals
import org.junit.Test

class VolumeCalculatorTest {

    private fun set(exerciseId: Long, weightKg: Double, reps: Int) = SetEntryEntity(
        sessionId = 1,
        exerciseId = exerciseId,
        orderInSession = 0,
        setIndex = 1,
        weightKg = weightKg,
        reps = reps,
        completedAtEpochMillis = 0L
    )

    @Test
    fun `single set volume is weight times reps`() {
        assertEquals(800.0, VolumeCalculator.setVolume(80.0, 10), 0.001)
    }

    @Test
    fun `total volume sums each set separately`() {
        val sets = listOf(
            set(exerciseId = 1, weightKg = 80.0, reps = 10),
            set(exerciseId = 1, weightKg = 80.0, reps = 10),
            set(exerciseId = 1, weightKg = 82.5, reps = 8)
        )
        // 800 + 800 + 660 = 2260
        assertEquals(2260.0, VolumeCalculator.totalVolume(sets), 0.001)
    }

    @Test
    fun `total reps sums reps across all sets`() {
        val sets = listOf(set(1, 80.0, 10), set(1, 80.0, 8), set(2, 100.0, 5))
        assertEquals(23, VolumeCalculator.totalReps(sets))
    }

    @Test
    fun `distinct exercise count ignores duplicate exercise ids`() {
        val sets = listOf(set(1, 80.0, 10), set(1, 80.0, 8), set(2, 100.0, 5))
        assertEquals(2, VolumeCalculator.distinctExerciseCount(sets))
    }
}
