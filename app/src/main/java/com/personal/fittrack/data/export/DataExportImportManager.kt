package com.personal.fittrack.data.export

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.room.withTransaction
import com.personal.fittrack.data.db.AppDatabase
import com.personal.fittrack.data.prefs.SettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import java.io.File

class DataExportImportManager(private val context: Context, private val db: AppDatabase, private val preferences: SettingsStore) {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true; encodeDefaults = true }
    private val restoreMutex = Mutex()
    private suspend fun snapshot(): BackupData = db.withTransaction {
        BackupData(version = 2, exportedAtEpochMillis = System.currentTimeMillis(),
            exercises = db.exerciseDao().getAllSync(), sessions = db.workoutDao().getAllSessionsSync(),
            sets = db.workoutDao().getAllSetsSync(), bodyWeights = db.bodyWeightDao().getAllSync(),
            foodItems = db.foodDao().getAllFoodItemsSync(), foodLogEntries = db.foodDao().getAllLogEntriesSync(),
            settings = preferences.settings.first(), routines = db.workoutDao().getRoutines())
    }
    suspend fun exportToFile(): File = withContext(Dispatchers.IO) {
        val file = File(File(context.getExternalFilesDir(null) ?: context.filesDir, "exports").apply { check(isDirectory || mkdirs()) { "Backup storage is unavailable." } }, "fittrack_backup_${System.currentTimeMillis()}.json")
        file.writeText(json.encodeToString(BackupData.serializer(), snapshot()))
        file
    }
    fun shareUri(file: File): Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    suspend fun readBackup(uri: Uri): BackupData = withContext(Dispatchers.IO) {
        val bytes = context.contentResolver.openInputStream(uri)?.use { input ->
            val output = java.io.ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                require(output.size() + count <= 25 * 1024 * 1024) { "Choose a backup smaller than 25 MB." }
                output.write(buffer, 0, count)
            }
            output.toByteArray()
        } ?: error("Could not open the selected file.")
        json.decodeFromString(BackupData.serializer(), bytes.toString(Charsets.UTF_8)).also(BackupValidator::validate)
    }
    suspend fun restore(data: BackupData): File = withContext(Dispatchers.IO) {
        restoreMutex.withLock {
            BackupValidator.validate(data)
            val recovery = exportToFile()
            val oldSettings = preferences.settings.first()
            try {
                db.withTransaction {
                    db.workoutDao().clearSets(); db.workoutDao().clearSessions(); db.workoutDao().clearRoutines()
                    db.foodDao().clearLogEntries(); db.foodDao().clearFoodItems()
                    db.bodyWeightDao().clear(); db.exerciseDao().clear()
                    db.exerciseDao().insertAll(data.exercises)
                    db.workoutDao().insertSessions(data.sessions); db.workoutDao().insertSets(data.sets)
                    db.bodyWeightDao().insertAll(data.bodyWeights)
                    db.foodDao().insertFoodItems(data.foodItems); db.foodDao().insertLogEntries(data.foodLogEntries)
                    data.routines.forEach { db.workoutDao().insertRoutine(it) }
                    data.settings?.let { preferences.restore(it) }
                }
            } catch (e: Exception) {
                withContext(kotlinx.coroutines.NonCancellable) { preferences.restore(oldSettings) }
                throw e
            }
            File(context.applicationInfo.dataDir, "shared_prefs").listFiles()?.filter { it.name.startsWith("workout_draft_") }?.forEach {
                context.deleteSharedPreferences(it.nameWithoutExtension)
            }
            recovery
        }
    }
}
