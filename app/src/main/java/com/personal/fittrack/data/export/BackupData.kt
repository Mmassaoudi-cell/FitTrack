package com.personal.fittrack.data.export

import com.personal.fittrack.data.db.entity.BodyWeightEntity
import com.personal.fittrack.data.db.entity.ExerciseEntity
import com.personal.fittrack.data.db.entity.FoodItemEntity
import com.personal.fittrack.data.db.entity.FoodLogEntryEntity
import com.personal.fittrack.data.db.entity.SetEntryEntity
import com.personal.fittrack.data.db.entity.WorkoutSessionEntity
import kotlinx.serialization.Serializable

@Serializable
data class BackupData(
    val version: Int = 1,
    val exportedAtEpochMillis: Long,
    val exercises: List<ExerciseEntity>,
    val sessions: List<WorkoutSessionEntity>,
    val sets: List<SetEntryEntity>,
    val bodyWeights: List<BodyWeightEntity>,
    val foodItems: List<FoodItemEntity>,
    val foodLogEntries: List<FoodLogEntryEntity>
)
