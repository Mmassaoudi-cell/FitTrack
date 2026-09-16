package com.personal.fittrack.domain

import com.personal.fittrack.data.db.entity.SetEntryEntity

object WorkoutDuration {
    /** Estimate from logged sets, summing sessions independently. */
    fun minutes(sets: List<SetEntryEntity>): Double = sets.groupBy { it.sessionId }.values.sumOf { session ->
        if (session.size == 1) 5.0 else
            ((session.maxOf { it.completedAtEpochMillis } - session.minOf { it.completedAtEpochMillis }) / 60_000.0).coerceAtLeast(1.0)
    }
}
