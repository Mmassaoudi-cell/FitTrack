package com.personal.fittrack.ui.nutrition

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.personal.fittrack.FitTrackApp
import com.personal.fittrack.data.repository.EstimateConfidence
import com.personal.fittrack.data.repository.FoodSource
import com.personal.fittrack.data.repository.MealType
import com.personal.fittrack.ui.components.ConfidenceBadge
import com.personal.fittrack.ui.nutrition.vision.FoodLabelMapper
import com.personal.fittrack.ui.nutrition.vision.FoodRecognizer
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.roundToInt

private sealed class PhotoStage {
    data object Capturing : PhotoStage()
    data object Analyzing : PhotoStage()
    data class Result(
        val foodName: String,
        val caloriesPer100g: Double,
        val confidence: EstimateConfidence?,
        val matched: Boolean
    ) : PhotoStage()
}

@Composable
fun FoodPhotoScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as FitTrackApp
    val scope = rememberCoroutineScope()

    var hasCameraPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasCameraPermission = granted
    }
    LaunchedEffect(Unit) {
        if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    var stage by remember { mutableStateOf<PhotoStage>(PhotoStage.Capturing) }
    var quantityGrams by remember { mutableStateOf(200.0) }
    var selectedMeal by remember { mutableStateOf(MealType.SNACK) }
    val recognizer = remember { FoodRecognizer() }

    Scaffold(topBar = { TopAppBar(title = { Text("Take Food Photo") }) }) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val currentStage = stage) {
                is PhotoStage.Capturing -> {
                    if (hasCameraPermission) {
                        CameraCaptureView(
                            onCaptured = { file ->
                                stage = PhotoStage.Analyzing
                                scope.launch {
                                    val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                                    val labels = recognizer.recognize(bitmap)
                                    val match = FoodLabelMapper.bestMatch(labels)
                                    if (match != null) {
                                        val (label, foodName) = match
                                        val items = app.container.nutritionRepository.searchFoodItems(foodName)
                                        val item = items.firstOrNull()
                                        stage = PhotoStage.Result(
                                            foodName = foodName,
                                            caloriesPer100g = item?.caloriesPer100g ?: 200.0,
                                            confidence = recognizer.confidenceBucket(label.confidence),
                                            matched = true
                                        )
                                    } else {
                                        stage = PhotoStage.Result(
                                            foodName = "Unknown food",
                                            caloriesPer100g = 200.0,
                                            confidence = EstimateConfidence.LOW,
                                            matched = false
                                        )
                                    }
                                }
                            }
                        )
                    } else {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("Camera permission is required to take a food photo.")
                            Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }, modifier = Modifier.padding(top = 12.dp)) {
                                Text("Grant permission")
                            }
                        }
                    }
                }
                is PhotoStage.Analyzing -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                        Text("Analyzing photo…", modifier = Modifier.padding(top = 12.dp))
                    }
                }
                is PhotoStage.Result -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text("Detected: ${currentStage.foodName}", style = MaterialTheme.typography.titleLarge)
                        if (!currentStage.matched) {
                            Text(
                                "We couldn't confidently recognize this food. Please adjust or search manually from Add Food.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        currentStage.confidence?.let {
                            ConfidenceBadge(it, modifier = Modifier.padding(vertical = 8.dp))
                        }

                        Text("Meal", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MealType.entries.forEach { meal ->
                                OutlinedButton(
                                    onClick = { selectedMeal = meal },
                                    colors = if (selectedMeal == meal) {
                                        androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                                            containerColor = MaterialTheme.colorScheme.primaryContainer
                                        )
                                    } else {
                                        androidx.compose.material3.ButtonDefaults.outlinedButtonColors()
                                    }
                                ) { Text(meal.name.lowercase().replaceFirstChar { it.uppercase() }) }
                            }
                        }

                        Text("Estimated quantity", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 16.dp))
                        Text("${quantityGrams.roundToInt()} g", style = MaterialTheme.typography.headlineMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(-50.0, -10.0, 10.0, 50.0).forEach { delta ->
                                OutlinedButton(onClick = { quantityGrams = (quantityGrams + delta).coerceAtLeast(0.0) }) {
                                    Text(if (delta > 0) "+${delta.roundToInt()} g" else "${delta.roundToInt()} g")
                                }
                            }
                        }

                        val estimatedCalories = currentStage.caloriesPer100g * quantityGrams / 100.0
                        Text(
                            "Estimated calories: ${estimatedCalories.roundToInt()} kcal",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 16.dp)
                        )
                        Text(
                            "This is an estimate. Actual calories depend on portion size, oils and preparation method.",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(onClick = { stage = PhotoStage.Capturing }, modifier = Modifier.weight(1f)) {
                                Text("Retake")
                            }
                            Button(
                                onClick = {
                                    scope.launch {
                                        app.container.nutritionRepository.logFood(
                                            mealType = selectedMeal,
                                            foodItem = null,
                                            foodName = currentStage.foodName,
                                            quantityGrams = quantityGrams,
                                            caloriesPer100g = currentStage.caloriesPer100g,
                                            confidence = currentStage.confidence,
                                            source = FoodSource.PHOTO
                                        )
                                        onDone()
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) { Text("Confirm") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CameraCaptureView(onCaptured: (File) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }
    val imageCapture = remember { ImageCapture.Builder().build() }

    DisposableEffect(Unit) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = androidx.camera.core.Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture)
        }, ContextCompat.getMainExecutor(context))
        onDispose {}
    }

    Column(modifier = Modifier.fillMaxSize()) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxWidth().weight(1f))
        Button(
            onClick = {
                val photoFile = File(context.cacheDir.resolve("food_photos").apply { mkdirs() }, "meal_${System.currentTimeMillis()}.jpg")
                val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
                imageCapture.takePicture(
                    outputOptions,
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                            onCaptured(photoFile)
                        }
                        override fun onError(exception: ImageCaptureException) {
                            // Analyzing screen simply won't be reached; user can retry the capture.
                        }
                    }
                )
            },
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) { Text("Capture") }
    }
}
