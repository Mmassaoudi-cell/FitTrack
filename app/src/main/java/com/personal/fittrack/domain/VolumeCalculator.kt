package com.personal.fittrack.domain

import com.personal.fittrack.data.db.entity.SetEntryEntity

/** Training volume = sets x reps x weight, summed per individual set. */
object VolumeCalculator {
    fun setVolume(weightKg: Double, reps: Int): Double = weightKg * reps

    fun totalVolume(sets: List<SetEntryEntity>): Double =
        sets.sumOf { setVolume(it.weightKg, it.reps) }

    fun totalReps(sets: List<SetEntryEntity>): Int = sets.sumOf { it.reps }

    fun distinctExerciseCount(sets: List<SetEntryEntity>): Int =
        sets.map { it.exerciseId }.distinct().size
}
