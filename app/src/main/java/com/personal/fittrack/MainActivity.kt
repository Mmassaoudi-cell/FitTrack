package com.personal.fittrack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.personal.fittrack.data.prefs.AppTheme
import com.personal.fittrack.ui.navigation.FitTrackNavGraph
import com.personal.fittrack.ui.theme.FitTrackTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as FitTrackApp).container
        setContent {
            val settings by container.userPreferences.settings.collectAsState(
                initial = com.personal.fittrack.data.prefs.AppSettings()
            )
            val useDarkTheme = when (settings.theme) {
                AppTheme.LIGHT -> false
                AppTheme.DARK -> true
                AppTheme.SYSTEM -> isSystemInDarkTheme()
            }
            FitTrackTheme(darkTheme = useDarkTheme) {
                FitTrackNavGraph()
            }
        }
    }
}
