package tech.eiki.globeswap.ui.camera

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tech.eiki.globeswap.R
import tech.eiki.globeswap.ui.editor.PhotoEditor
import tech.eiki.globeswap.util.ImageUtils

/**
 * Kamera mit Sucher im Fotoformat 3:4, danach der Editor (Text, Zeichnen, Sticker, Filter). [onConfirm] erhält das fertige JPEG.
 */
@Composable
fun PhotoCapture(
    confirmLabel: String,
    onConfirm: (ByteArray) -> Unit,
    modifier: Modifier = Modifier,
    hint: String? = null,
    onCancel: (() -> Unit)? = null,
    theme: String = "",
) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { hasPermission = it }
    LaunchedEffect(Unit) { if (!hasPermission) launcher.launch(Manifest.permission.CAMERA) }

    var captured by remember { mutableStateOf<ByteArray?>(null) }

    Box(modifier.fillMaxSize().background(Color.Black)) {
        val photo = captured
        when {
            !hasPermission -> PermissionMissing(onRequest = { launcher.launch(Manifest.permission.CAMERA) })
            photo != null -> PhotoEditor(
                jpeg = photo,
                confirmLabel = confirmLabel,
                onRetake = { captured = null },
                onConfirm = { edited -> onConfirm(edited); captured = null },
            )
            else -> CameraView(hint = hint, theme = theme, onCaptured = { captured = it })
        }
        if (onCancel != null && photo == null) {
            IconButton(onClick = onCancel, modifier = Modifier.statusBarsPadding().padding(8.dp)) {
                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.cancel), tint = Color.White)
            }
        }
    }
}

@Composable
private fun CameraView(hint: String?, theme: String, onCaptured: (ByteArray) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    var lensFacing by rememberSaveable { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var flashOn by rememberSaveable { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var provider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    // Zoom: aktueller Wert und Bereich der Kamera; Weitwinkel als eigene Kamera, falls das Handy sie so meldet.
    var zoomRatio by remember { mutableFloatStateOf(1f) }
    var zoomRange by remember { mutableStateOf(1f to 1f) }
    var useWide by rememberSaveable { mutableStateOf(false) }
    var wideSelector by remember { mutableStateOf<CameraSelector?>(null) }
    var pendingZoom by remember { mutableStateOf<Float?>(null) }
    var focusPoint by remember { mutableStateOf<Offset?>(null) }

    // Sucher und Foto haben dasselbe Format 3:4 (hochkant): Was man im Sucher sieht, ist genau das Foto.
    val previewView = remember {
        PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
    }
    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setResolutionSelector(FOUR_THREE)
            .build()
    }

    LaunchedEffect(Unit) { provider = ProcessCameraProvider.awaitInstance(context) }

    DisposableEffect(provider, lensFacing, useWide, lifecycleOwner) {
        val cameraProvider = provider
        var bound: Camera? = null
        if (cameraProvider != null) {
            val preview = Preview.Builder().setResolutionSelector(FOUR_THREE).build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
            val wide = wideSelector.takeIf { useWide && lensFacing == CameraSelector.LENS_FACING_BACK }
            val selector = wide ?: CameraSelector.Builder().requireLensFacing(lensFacing).build()
            try {
                cameraProvider.unbindAll()
                bound = cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview, imageCapture)
                camera = bound
                bound.cameraInfo.zoomState.observe(lifecycleOwner) { state ->
                    zoomRatio = state.zoomRatio
                    if (wide == null) zoomRange = state.minZoomRatio to state.maxZoomRatio
                }
                if (wide == null && lensFacing == CameraSelector.LENS_FACING_BACK && bound.cameraInfo.zoomState.value?.minZoomRatio?.let { it >= 0.95f } != false) {
                    wideSelector = wideCameraSelector(cameraProvider.availableCameraInfos, bound.cameraInfo)
                }
                pendingZoom?.let { bound.cameraControl.setZoomRatio(it); pendingZoom = null }
            } catch (e: Exception) {
                Log.e("PhotoCapture", "Kamera konnte nicht gestartet werden", e)
            }
        }
        onDispose {
            bound?.cameraInfo?.zoomState?.removeObservers(lifecycleOwner)
            cameraProvider?.unbindAll()
        }
    }

    val separateWide = wideSelector != null && lensFacing == CameraSelector.LENS_FACING_BACK
    val presets = zoomPresets(zoomRange.first, zoomRange.second, separateWide)
    val shownZoom = if (useWide) WIDE * zoomRatio else zoomRatio
    fun selectZoom(preset: Float) {
        when {
            separateWide && preset == WIDE -> useWide = true
            useWide -> { pendingZoom = preset; useWide = false }
            else -> camera?.cameraControl?.setZoomRatio(preset)
        }
    }

    LaunchedEffect(focusPoint) {
        if (focusPoint != null) {
            delay(900)
            focusPoint = null
        }
    }

    LaunchedEffect(flashOn) {
        imageCapture.flashMode = if (flashOn) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF
    }

    fun takePhoto() {
        if (busy) return
        busy = true
        val mirror = lensFacing == CameraSelector.LENS_FACING_FRONT
        imageCapture.takePicture(
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    scope.launch {
                        val jpeg = withContext(Dispatchers.Default) {
                            image.use { ImageUtils.toJpeg(it.toBitmap(), it.imageInfo.rotationDegrees, mirror) }
                        }
                        busy = false
                        onCaptured(jpeg)
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    busy = false
                    Toast.makeText(context, context.getString(R.string.photo_failed, exception.message.orEmpty()), Toast.LENGTH_SHORT).show()
                }
            },
        )
    }

    Column(Modifier.fillMaxSize()) {
        // Oben: Tagesthema und Blitz auf dunklem Grund, nicht über dem Bild
        Box(Modifier.fillMaxWidth().statusBarsPadding().heightIn(min = 56.dp).padding(horizontal = 8.dp, vertical = 4.dp)) {
            if (theme.isNotBlank()) {
                DailyTheme(theme, Modifier.align(Alignment.Center).padding(horizontal = 48.dp))
            }
            IconButton(onClick = { flashOn = !flashOn }, modifier = Modifier.align(Alignment.CenterEnd)) {
                Icon(
                    if (flashOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                    contentDescription = stringResource(R.string.flash),
                    tint = Color.White,
                )
            }
        }

        // Sucher im Fotoformat 3:4 – auf kleinen Bildschirmen schmaler statt abgeschnitten
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Box(Modifier.aspectRatio(3f / 4f).clip(RectangleShape).background(Color(0xFF1A1A1A)).testTag("viewfinder")) {
                AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
                // Zwei Finger: zoomen. Antippen: dort scharfstellen.
                Box(
                    Modifier
                        .fillMaxSize()
                        .pointerInput(camera) {
                            detectTransformGestures { _, _, zoom, _ ->
                                val cam = camera ?: return@detectTransformGestures
                                if (zoom != 1f) {
                                    val state = cam.cameraInfo.zoomState.value ?: return@detectTransformGestures
                                    cam.cameraControl.setZoomRatio((state.zoomRatio * zoom).coerceIn(state.minZoomRatio, state.maxZoomRatio))
                                }
                            }
                        }
                        .pointerInput(camera) {
                            detectTapGestures { offset ->
                                val cam = camera ?: return@detectTapGestures
                                val point = previewView.meteringPointFactory.createPoint(offset.x, offset.y)
                                cam.cameraControl.startFocusAndMetering(FocusMeteringAction.Builder(point).build())
                                focusPoint = offset
                            }
                        },
                )
                focusPoint?.let { p ->
                    Box(
                        Modifier
                            .offset { IntOffset(p.x.roundToInt() - 32.dp.roundToPx(), p.y.roundToInt() - 32.dp.roundToPx()) }
                            .size(64.dp)
                            .border(2.dp, Color.White, CircleShape),
                    )
                }
                Column(
                    Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(bottom = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if (hint != null) {
                        Text(
                            hint,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .padding(horizontal = 24.dp, vertical = 8.dp)
                                .background(Color.Black.copy(alpha = 0.45f), MaterialTheme.shapes.medium)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                        )
                    }
                    ZoomChips(presets, shownZoom, onSelect = ::selectZoom)
                }
            }
        }

        // Unten: Auslöser und Kamera wechseln
        Row(
            Modifier.fillMaxWidth().padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(Modifier.size(48.dp))
            Box(
                Modifier
                    .size(76.dp)
                    .border(4.dp, Color.White, CircleShape)
                    .padding(8.dp)
                    .background(if (busy) Color.Gray else Color.White, CircleShape)
                    .clickable(enabled = !busy) { takePhoto() },
                contentAlignment = Alignment.Center,
            ) {
                if (busy) CircularProgressIndicator(Modifier.size(28.dp), color = Color.Black)
            }
            IconButton(onClick = {
                useWide = false
                lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                    CameraSelector.LENS_FACING_FRONT
                } else {
                    CameraSelector.LENS_FACING_BACK
                }
            }) {
                Icon(Icons.Default.FlipCameraAndroid, contentDescription = stringResource(R.string.switch_camera), tint = Color.White)
            }
        }
    }
}

/** Vorschau und Foto im Format 4:3 (hochkant 3:4), notfalls das nächstbeste Format der Kamera. */
private val FOUR_THREE = ResolutionSelector.Builder()
    .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
    .build()

@Composable
private fun PermissionMissing(onRequest: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            stringResource(R.string.camera_needed),
            color = Color.White,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.size(16.dp))
        Button(onClick = onRequest) { Text(stringResource(R.string.allow_camera)) }
    }
}

/** Tagesthema vom Server als kleine Anregung über dem Kamerabild. */
@Composable
internal fun DailyTheme(theme: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .background(Color.Black.copy(alpha = 0.5f), MaterialTheme.shapes.large)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.todays_theme), color = Color.White.copy(alpha = 0.75f), style = MaterialTheme.typography.labelSmall)
        Text(theme, color = Color.White, textAlign = TextAlign.Center, style = MaterialTheme.typography.titleMedium)
    }
}
