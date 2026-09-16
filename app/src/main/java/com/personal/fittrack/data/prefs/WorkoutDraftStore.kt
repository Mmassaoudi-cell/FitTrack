package com.personal.fittrack.data.prefs

import android.content.Context

/** Small recovery state, separate from the durable completed sets in Room. */
class WorkoutDraftStore(context: Context, sessionId: Long) {
    private val prefs = context.getSharedPreferences("workout_draft_$sessionId", Context.MODE_PRIVATE)
    var exerciseId: Long?
        get() = prefs.getLong("exercise", -1).takeIf { it > 0 }
        set(value) { prefs.edit().putLong("exercise", value ?: -1).apply() }
    var weight: Double
        get() = Double.fromBits(prefs.getLong("weight", 20.0.toBits()))
        set(value) { prefs.edit().putLong("weight", value.toBits()).apply() }
    var reps: Int
        get() = prefs.getInt("reps", 10)
        set(value) { prefs.edit().putInt("reps", value).apply() }
    var deadline: Long
        get() = prefs.getLong("deadline", 0)
        set(value) { prefs.edit().putLong("deadline", value).apply() }
    var remaining: Int
        get() = prefs.getInt("remaining", 0)
        set(value) { prefs.edit().putInt("remaining", value).apply() }
    fun clear() { prefs.edit().clear().apply() }
}
