package com.personal.fittrack.ui.settings

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.personal.fittrack.FitTrackApp
import com.personal.fittrack.domain.ActivityLevel
import com.personal.fittrack.domain.BiologicalSex
import com.personal.fittrack.domain.CalorieCalculator
import com.personal.fittrack.domain.FitnessGoal
import com.personal.fittrack.domain.WeightUnit
import com.personal.fittrack.data.prefs.AppTheme
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun SettingsScreen() {
    val app = LocalContext.current.applicationContext as FitTrackApp
    val viewModel: SettingsViewModel = viewModel(factory = viewModelFactory {
        initializer { SettingsViewModel(app.container.userPreferences) }
    })
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var age by remember(settings.profileAgeYears) { mutableStateOf(settings.profileAgeYears.toString()) }
    var height by remember(settings.profileHeightCm) { mutableStateOf(settings.profileHeightCm.toString()) }
    var incrementText by remember(settings.weightIncrementKg) { mutableStateOf(settings.weightIncrementKg.toString()) }
    var overrideText by remember(settings.calorieTargetOverride) { mutableStateOf(settings.calorieTargetOverride?.toString() ?: "") }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val tempFile = File(context.cacheDir, "import_temp.json")
                tempFile.outputStream().use { output -> input.copyTo(output) }
                app.container.exportImportManager.importFromFile(tempFile)
            }
        }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Settings") }) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Text("Units", style = MaterialTheme.typography.titleMedium)
                SingleChoiceSegmentedButtonRow {
                    WeightUnit.entries.forEachIndexed { index, unit ->
                        SegmentedButton(
                            selected = settings.weightUnit == unit,
                            onClick = { viewModel.setWeightUnit(unit) },
                            shape = SegmentedButtonDefaults.itemShape(index, WeightUnit.entries.size)
                        ) { Text(unit.name) }
                    }
                }
            }

            item {
                Text("Weight increment step (kg)", style = MaterialTheme.typography.titleMedium)
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = incrementText,
                        onValueChange = { incrementText = it },
                        modifier = Modifier.weight(1f)
                    )
                    Button(onClick = { incrementText.toDoubleOrNull()?.let { viewModel.setWeightIncrement(it) } }) { Text("Save") }
                }
            }

            item {
                Text("Theme", style = MaterialTheme.typography.titleMedium)
                SingleChoiceSegmentedButtonRow {
                    AppTheme.entries.forEachIndexed { index, theme ->
                        SegmentedButton(
                            selected = settings.theme == theme,
                            onClick = { viewModel.setTheme(theme) },
                            shape = SegmentedButtonDefaults.itemShape(index, AppTheme.entries.size)
                        ) { Text(theme.name) }
                    }
                }
            }

            item { HorizontalDivider() }

            item {
                Text("Calorie calculator profile", style = MaterialTheme.typography.titleMedium)
                Text("Used to estimate your daily calorie target. This is an estimate, not medical advice.", style = MaterialTheme.typography.bodyMedium)
            }

            item {
                SingleChoiceSegmentedButtonRow {
                    BiologicalSex.entries.forEachIndexed { index, sex ->
                        SegmentedButton(
                            selected = settings.profileSex == sex,
                            onClick = {
                                viewModel.setProfile(sex, settings.profileAgeYears, settings.profileHeightCm, settings.profileActivityLevel, settings.profileGoal)
                            },
                            shape = SegmentedButtonDefaults.itemShape(index, BiologicalSex.entries.size)
                        ) { Text(sex.name) }
                    }
                }
            }

            item {
                OutlinedTextField(value = age, onValueChange = { age = it }, label = { Text("Age (years)") }, modifier = Modifier.fillMaxWidth())
            }
            item {
                OutlinedTextField(value = height, onValueChange = { height = it }, label = { Text("Height (cm)") }, modifier = Modifier.fillMaxWidth())
            }

            item {
                Text("Activity level", style = MaterialTheme.typography.labelLarge)
                ActivityLevel.entries.forEach { level ->
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        androidx.compose.material3.RadioButton(
                            selected = settings.profileActivityLevel == level,
                            onClick = {
                                viewModel.setProfile(settings.profileSex, settings.profileAgeYears, settings.profileHeightCm, level, settings.profileGoal)
                            }
                        )
                        Text(level.label)
                    }
                }
            }

            item {
                Text("Goal", style = MaterialTheme.typography.labelLarge)
                SingleChoiceSegmentedButtonRow {
                    FitnessGoal.entries.forEachIndexed { index, goal ->
                        SegmentedButton(
                            selected = settings.profileGoal == goal,
                            onClick = {
                                viewModel.setProfile(settings.profileSex, settings.profileAgeYears, settings.profileHeightCm, settings.profileActivityLevel, goal)
                            },
                            shape = SegmentedButtonDefaults.itemShape(index, FitnessGoal.entries.size)
                        ) { Text(goal.label) }
                    }
                }
            }

            item {
                Button(
                    onClick = {
                        val ageInt = age.toIntOrNull() ?: settings.profileAgeYears
                        val heightVal = height.toDoubleOrNull() ?: settings.profileHeightCm
                        viewModel.setProfile(settings.profileSex, ageInt, heightVal, settings.profileActivityLevel, settings.profileGoal)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Save profile") }
            }

            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Manual calorie target override", style = MaterialTheme.typography.labelLarge)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = overrideText,
                                onValueChange = { overrideText = it },
                                label = { Text("kcal (blank = auto)") },
                                modifier = Modifier.weight(1f)
                            )
                            Button(onClick = { viewModel.setCalorieOverride(overrideText.toIntOrNull()) }) { Text("Save") }
                        }
                    }
                }
            }

            item { HorizontalDivider() }

            item {
                Text("Backup", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = {
                        scope.launch {
                            val file = app.container.exportImportManager.exportToFile()
                            val uri = app.container.exportImportManager.shareUri(file)
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "application/json"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(intent, "Export FitTrack data"))
                        }
                    }) { Text("Export Data") }

                    OutlinedButton(onClick = { importLauncher.launch(arrayOf("application/json")) }) {
                        Text("Import Data")
                    }
                }
            }
        }
    }
}
