package com.shakal.camera.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCharacteristics
import android.net.Uri
import android.opengl.GLSurfaceView
import android.os.Environment
import android.provider.MediaStore
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.shakal.camera.core.*
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

private val Context.dataStore by preferencesDataStore(name = "settings")
private val LEVEL_KEY = intPreferencesKey("last_level")
private val FRONT_CAMERA_KEY = booleanPreferencesKey("front_camera")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(activity: MainActivity) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var currentLevel by remember { mutableIntStateOf(1) }
    var isFrontCamera by remember { mutableStateOf(false) }
    var isRecording by remember { mutableStateOf(false) }
    var lastPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var showAbout by remember { mutableStateOf(false) }

    // Load saved preferences
    LaunchedEffect(Unit) {
        context.dataStore.data.collect { prefs ->
            currentLevel = prefs[LEVEL_KEY] ?: 1
            isFrontCamera = prefs[FRONT_CAMERA_KEY] ?: false
        }
    }

    // Save preferences when changed
    LaunchedEffect(currentLevel) {
        context.dataStore.edit { it[LEVEL_KEY] = currentLevel }
    }
    LaunchedEffect(isFrontCamera) {
        context.dataStore.edit { it[FRONT_CAMERA_KEY] = isFrontCamera }
    }

    // Get preset
    val presets = remember { PresetLoader.load(context) }
    val currentPreset = remember(currentLevel) {
        presets.find { it.level == currentLevel } ?: presets.first()
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // Camera preview
        CameraPreview(
            modifier = Modifier.fillMaxSize(),
            lifecycleOwner = lifecycleOwner,
            isFrontCamera = isFrontCamera,
            preset = currentPreset,
            context = context
        )

        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${currentLevel}/10 шакалов",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = { showAbout = true }) {
                Text("ⓘ", color = Color.White, fontSize = 20.sp)
            }
        }

        // Bottom controls
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Level slider
            LevelSlider(
                currentLevel = currentLevel,
                onLevelChange = { currentLevel = it },
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )

            // Capture controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Last photo thumbnail
                LastPhotoThumbnail(
                    uri = lastPhotoUri,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable {
                            lastPhotoUri?.let { uri ->
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(uri, "image/*")
                                }
                                context.startActivity(intent)
                            }
                        }
                )

                // Capture button
                CaptureButton(
                    isRecording = isRecording,
                    onTap = {
                        // Take photo
                        takePhoto(context, currentPreset, currentLevel) { uri ->
                            lastPhotoUri = uri
                        }
                    },
                    onLongPressStart = {
                        // Start video recording
                        isRecording = true
                    },
                    onLongPressEnd = {
                        // Stop video recording
                        isRecording = false
                    }
                )

                // Camera switch
                IconButton(
                    onClick = { isFrontCamera = !isFrontCamera },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                ) {
                    Text("🔄", fontSize = 20.sp)
                }
            }
        }

        // Recording indicator
        AnimatedVisibility(
            visible = isRecording,
            modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 8.dp)
        ) {
            Surface(
                color = Color.Red.copy(alpha = 0.8f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "● REC",
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // About dialog
        if (showAbout) {
            AlertDialog(
                onDismissRequest = { showAbout = false },
                title = { Text("О программе") },
                text = {
                    Column {
                        Text("ШакалCamera v1.0.0")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Делаем ваше фото прекрасным", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Свободное программное обеспечение.\nЛицензия: GPL-3.0")
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showAbout = false }) {
                        Text("Закрыть")
                    }
                }
            )
        }
    }
}

@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    isFrontCamera: Boolean,
    preset: Preset,
    context: Context
) {
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    AndroidView(
        factory = { ctx ->
            PreviewView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                scaleType = PreviewView.ScaleType.FILL_CENTER
                implementationMode = PreviewView.ImplementationMode.PERFORMANCE
            }
        },
        modifier = modifier,
        update = { previewView ->
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                    .build()

                val cameraSelector = if (isFrontCamera) {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                } else {
                    CameraSelector.DEFAULT_BACK_CAMERA
                }

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageCapture
                    )
                } catch (e: Exception) {
                    // Camera binding failed
                }
            }, ContextCompat.getMainExecutor(context))
        }
    )
}

@Composable
fun LevelSlider(
    currentLevel: Int,
    onLevelChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val levels = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 12)
    val currentIndex = levels.indexOf(currentLevel).coerceAtLeast(0)

    Column(modifier = modifier.fillMaxWidth()) {
        Slider(
            value = currentIndex.toFloat(),
            onValueChange = { onLevelChange(levels[it.toInt()]) },
            valueRange = 0f..(levels.size - 1).toFloat(),
            steps = levels.size - 2,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFFFFB300),
                activeTrackColor = Color(0xFFFFB300),
                inactiveTrackColor = Color.White.copy(alpha = 0.3f)
            )
        )
    }
}

@Composable
fun CaptureButton(
    isRecording: Boolean,
    onTap: () -> Unit,
    onLongPressStart: () -> Unit,
    onLongPressEnd: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(
                if (isRecording) Color.Red
                else Color.White
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onTap() },
                    onLongPress = {
                        onLongPressStart()
                        tryAwaitRelease()
                        onLongPressEnd()
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.3f))
        )
    }
}

@Composable
fun LastPhotoThumbnail(uri: Uri?, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(Color.White.copy(alpha = 0.2f))
    ) {
        if (uri != null) {
            // In a real app, load the thumbnail asynchronously
            Text("📷", modifier = Modifier.align(Alignment.Center), fontSize = 20.sp)
        }
    }
}

private fun takePhoto(
    context: Context,
    preset: Preset,
    level: Int,
    onSaved: (Uri) -> Unit
) {
    // This would be connected to ImageCapture in a real implementation
    // For now, create a placeholder that demonstrates the pipeline
    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val filename = "shakal_${level}_${timestamp}.jpg"

    val resolver = context.contentResolver
    val contentValues = android.content.ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/ShakalCamera")
    }

    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
    uri?.let { onSaved(it) }
}
