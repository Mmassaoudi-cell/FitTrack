package com.personal.fittrack.ui.nutrition

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.io.File

@Composable
fun FoodPhotoScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    val vm = foodEntryViewModel()
    val result by vm.photoResult.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    var manual by remember { mutableStateOf(false) }
    var permission by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { permission = it }
    // Refresh permission after returning from Android's app settings.
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) permission = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    if (result || manual) { FoodEntryForm(vm, onDone, if (result) { { vm.retake() } } else null); return }
    Scaffold(contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0), topBar = { TopAppBar(title = { Text("Food photo") }, navigationIcon = { IconButton(onClick = onDone) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Find a food suggestion, then confirm the food and portion. Photos stay on your device.")
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            when {
                busy -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { CircularProgressIndicator(); Text("Finding food suggestions…", Modifier.padding(top = 12.dp)) } }
                permission -> CameraCaptureView(vm::analyze, vm::reportError, Modifier.weight(1f))
                else -> Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                    Text("Allow camera access to take a photo. You can also log food manually.")
                    Button(onClick = { launcher.launch(Manifest.permission.CAMERA) }) { Text("Allow camera") }
                    TextButton(onClick = { context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))) }) { Text("Open app permissions") }
                }
            }
            OutlinedButton(onClick = { manual = true }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("Search or enter food manually") }
        }
    }
}

@Composable
private fun CameraCaptureView(onCaptured: (File) -> Unit, onError: (String) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }
    val capture = remember { ImageCapture.Builder().setTargetResolution(android.util.Size(1280, 960)).build() }
    var ready by remember { mutableStateOf(false) }
    var capturing by remember { mutableStateOf(false) }
    val captureCallback by rememberUpdatedState(onCaptured)
    val errorCallback by rememberUpdatedState(onError)
    val alive = remember { java.util.concurrent.atomic.AtomicBoolean(true) }
    DisposableEffect(owner) {
        alive.set(true)
        var provider: ProcessCameraProvider? = null
        val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            if (alive.get()) try {
                provider = future.get()
                provider!!.bindToLifecycle(owner, CameraSelector.DEFAULT_BACK_CAMERA, preview, capture)
                ready = true
            } catch (e: Exception) { errorCallback("Camera unavailable. Try manual food entry or reopen this screen.") }
        }, ContextCompat.getMainExecutor(context))
        onDispose { alive.set(false); provider?.unbind(preview, capture) }
    }
    Column(modifier) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxWidth().weight(1f))
        Button(enabled = ready && !capturing, onClick = {
            capturing = true
            capture.targetRotation = previewView.display?.rotation ?: android.view.Surface.ROTATION_0
            val file = File(context.cacheDir.resolve("food_photos").apply { mkdirs() }, "meal_${System.currentTimeMillis()}.jpg")
            capture.takePicture(ImageCapture.OutputFileOptions.Builder(file).build(), ContextCompat.getMainExecutor(context), object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    capturing = false
                    if (alive.get()) captureCallback(file) else file.delete()
                }
                override fun onError(exception: ImageCaptureException) {
                    capturing = false; file.delete()
                    if (alive.get()) errorCallback("Could not capture the photo. Please try again.")
                }
            })
        }, modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) { Text(if (capturing) "Capturing…" else "Take photo") }
    }
}
