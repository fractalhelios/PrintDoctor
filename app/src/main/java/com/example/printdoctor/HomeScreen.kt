package com.example.printdoctor

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun HomeScreen(
    onOpenSettings: () -> Unit,
    // Se llama cuando la foto ya quedó guardada. Entrega la ruta, la imagen ya cargada y
    // los rectángulos (en píxeles) de origen y destino para la animación de vuelo.
    onPhotoCaptured: (path: String, bitmap: Bitmap?, from: Rect, to: Rect) -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    // ¿Ya tenemos permiso de cámara?
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasPermission = granted }

    // Al abrir la pantalla, pedimos el permiso si todavía no lo tenemos
    LaunchedEffect(Unit) {
        if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    // Posiciones que necesita la animación de la foto
    var previewBounds by remember { mutableStateOf<Rect?>(null) }
    var contentOrigin by remember { mutableStateOf<Offset?>(null) }
    var capturing by remember { mutableStateOf(false) }

    // Objeto de CameraX que se encarga de tomar la foto
    val imageCapture = remember { ImageCapture.Builder().build() }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(16.dp)
        ) {
            // Marcador invisible: marca dónde empieza el contenido.
            // Ahí aterrizará la miniatura de la foto en el formulario.
            Spacer(
                modifier = Modifier
                    .size(0.dp)
                    .onGloballyPositioned { contentOrigin = it.positionInRoot() }
            )

            // Visor de la cámara (ocupa todo el espacio disponible)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .onGloballyPositioned { previewBounds = it.boundsInRoot() }
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (hasPermission) {
                    CameraPreview(
                        imageCapture = imageCapture,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Se necesita permiso para usar la cámara")
                        Spacer(modifier = Modifier.height(12.dp))
                        AppButton(onClick = {
                            permissionLauncher.launch(Manifest.permission.CAMERA)
                        }) {
                            Text("Dar permiso")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Botones de abajo (aquí puedes agregar más después)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AppButton(
                    onClick = {
                        if (!capturing) {
                            capturing = true
                            takePhoto(
                                context = context,
                                imageCapture = imageCapture,
                                onSaved = { path ->
                                    scope.launch {
                                        // Cargamos la foto en segundo plano para la animación
                                        val bitmap = withContext(Dispatchers.IO) {
                                            runCatching { loadScaledBitmap(path) }.getOrNull()
                                        }
                                        val from = previewBounds ?: Rect.Zero
                                        val origin = contentOrigin ?: Offset.Zero
                                        val to = Rect(
                                            origin,
                                            Size(
                                                with(density) { PhotoThumbWidth.toPx() },
                                                with(density) { PhotoThumbHeight.toPx() }
                                            )
                                        )
                                        onPhotoCaptured(path, bitmap, from, to)
                                    }
                                },
                                onError = { capturing = false }
                            )
                        }
                    },
                    enabled = hasPermission && !capturing,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Tomar foto")
                }
                AppOutlinedButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Configuración")
                }
            }
        }
    }
}

@Composable
private fun CameraPreview(imageCapture: ImageCapture, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    // MainActivity es un LifecycleOwner: la cámara se enlaza a su ciclo de vida
    val lifecycleOwner = context as LifecycleOwner
    val providerHolder = remember { mutableStateOf<ProcessCameraProvider?>(null) }

    // La cámara aparece con un fundido suave poco después de abrirse la pantalla
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(250)
        visible = true
    }
    val cameraAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 500),
        label = "cameraFade"
    )

    // Al salir de esta pantalla, soltamos la cámara
    DisposableEffect(Unit) {
        onDispose { providerHolder.value?.unbindAll() }
    }

    AndroidView(
        modifier = modifier.alpha(cameraAlpha),
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                // COMPATIBLE permite recortar esquinas y animar la vista
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            }
            val providerFuture = ProcessCameraProvider.getInstance(ctx)
            providerFuture.addListener({
                val cameraProvider = providerFuture.get()
                providerHolder.value = cameraProvider

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageCapture
                    )
                } catch (e: Exception) {
                    Log.e("PrintDoctor", "No se pudo iniciar la cámara", e)
                }
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        }
    )
}

private fun takePhoto(
    context: Context,
    imageCapture: ImageCapture,
    onSaved: (String) -> Unit,
    onError: () -> Unit
) {
    // Carpeta de fotos propia de la app (no se borra sola, a diferencia del caché).
    // Ruta típica: /sdcard/Android/data/<paquete>/files/Pictures/
    val folder = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: context.filesDir
    val photoFile = File(folder, "print_${System.currentTimeMillis()}.jpg")
    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

    imageCapture.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                Log.d("PrintDoctor", "Foto guardada en: ${photoFile.absolutePath}")
                onSaved(photoFile.absolutePath)
            }

            override fun onError(exception: ImageCaptureException) {
                Log.e("PrintDoctor", "Error al tomar la foto", exception)
                Toast.makeText(context, "No se pudo tomar la foto", Toast.LENGTH_SHORT).show()
                onError()
            }
        }
    )
}
