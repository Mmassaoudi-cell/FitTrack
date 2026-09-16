package com.personal.fittrack

import android.app.Application
import com.personal.fittrack.di.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class FitTrackApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            cacheDir.resolve("food_photos").listFiles()?.filter { it.isFile && it.name.startsWith("meal_") }?.forEach { it.delete() }
            container.workoutRepository.ensureSeeded()
            container.nutritionRepository.ensureSeeded()
        }
    }
}
