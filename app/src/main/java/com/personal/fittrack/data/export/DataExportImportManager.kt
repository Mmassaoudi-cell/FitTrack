package com.personal.fittrack.data.export

import android.content.Context
import androidx.core.content.FileProvider
import androidx.room.withTransaction
import com.personal.fittrack.data.db.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.time.format.DateTimeFormatter

class DataExportImportManager(
    private val context: Context,
    private val db: AppDatabase
) {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    suspend fun exportToFile(): File = withContext(Dispatchers.IO) {
        val data = BackupData(
            exportedAtEpochMillis = System.currentTimeMillis(),
            exercises = db.exerciseDao().getAllSync(),
            sessions = db.workoutDao().getAllSessionsSync(),
            sets = db.workoutDao().getAllSetsSync(),
            bodyWeights = db.bodyWeightDao().getAllSync(),
            foodItems = db.foodDao().getAllFoodItemsSync(),
            foodLogEntries = db.foodDao().getAllLogEntriesSync()
        )
        val exportDir = File(context.getExternalFilesDir(null), "exports").apply { mkdirs() }
        val timestamp = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").format(java.time.LocalDateTime.now())
        val file = File(exportDir, "fittrack_backup_$timestamp.json")
        file.writeText(json.encodeToString(BackupData.serializer(), data))
        file
    }

    fun shareUri(file: File) = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    suspend fun importFromFile(file: File): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val data = json.decodeFromString(BackupData.serializer(), file.readText())
            db.withTransaction {
                db.workoutDao().clearSets()
                db.workoutDao().clearSessions()
                db.foodDao().clearLogEntries()
                db.foodDao().clearFoodItems()
                db.bodyWeightDao().clear()
                db.exerciseDao().clear()

                db.exerciseDao().insertAll(data.exercises)
                db.workoutDao().insertSessions(data.sessions)
                db.workoutDao().insertSets(data.sets)
                db.bodyWeightDao().insertAll(data.bodyWeights)
                db.foodDao().insertFoodItems(data.foodItems)
                db.foodDao().insertLogEntries(data.foodLogEntries)
            }
        }
    }
}
