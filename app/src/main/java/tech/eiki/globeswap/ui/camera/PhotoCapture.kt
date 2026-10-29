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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import tech.eiki.globeswap.R
import tech.eiki.globeswap.data.PhotoInfo
import tech.eiki.globeswap.ui.LocalBottomBarSpace
import tech.eiki.globeswap.ui.editor.PhotoEditor
import tech.eiki.globeswap.ui.theme.GlobeBlue
import tech.eiki.globeswap.ui.theme.GlobeGradient
import tech.eiki.globeswap.ui.theme.GlobeYellow
import tech.eiki.globeswap.util.ImageUtils
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Kamera mit Sucher im Fotoformat 3:4, danach der Editor (Text, Zeichnen, Sticker, Filter). [onConfirm] erhält das fertige JPEG.
 */
@Composable
fun PhotoCapture(
    confirmLabel: String,
    onConfirm: (ByteArray, PhotoInfo) -> Unit,
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
    var capturedWide by remember { mutableStateOf(false) }
    // Beim Tagesthema mitmachen: Antippen des Themas, gilt für das nächste Foto (Erfolge „Tagesthema“)
    var themeJoined by rememberSaveable { mutableStateOf(false) }

    // Hintergrund folgt dem Design (hell/dunkel); unten Platz für die Leiste, damit Auslöser und Editor-Knöpfe frei bleiben
    Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(bottom = LocalBottomBarSpace.current)) {
        val photo = captured
        when {
            !hasPermission -> PermissionMissing(onRequest = { launcher.launch(Manifest.permission.CAMERA) })
            photo != null -> PhotoEditor(
                jpeg = photo,
                confirmLabel = confirmLabel,
                onRetake = { captured = null },
                onConfirm = { edited, info ->
                    onConfirm(edited, info.copy(theme = themeJoined && theme.isNotBlank(), wide = capturedWide))
                    captured = null
                },
            )
            else -> CameraView(
                hint = hint,
                theme = theme,
                themeJoined = themeJoined,
                onToggleTheme = { themeJoined = !themeJoined },
                onCaptured = { jpeg, wide -> captured = jpeg; capturedWide = wide },
            )
        }
        if (onCancel != null && photo == null) {
            IconButton(onClick = onCancel, modifier = Modifier.statusBarsPadding().padding(8.dp)) {
                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.cancel), tint = MaterialTheme.colorScheme.onBackground)
            }
        }
    }
}

@Composable
private fun CameraView(
    hint: String?,
    theme: String,
    themeJoined: Boolean,
    onToggleTheme: () -> Unit,
    onCaptured: (jpeg: ByteArray, wide: Boolean) -> Unit,
) {
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
        val wide = shownZoom < 0.95f // Ultraweitwinkel (eigene Kamera oder Zoom unter 1×)
        imageCapture.takePicture(
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    scope.launch {
                        val jpeg = withContext(Dispatchers.Default) {
                            image.use { ImageUtils.toJpeg(it.toBitmap(), it.imageInfo.rotationDegrees, mirror) }
                        }
                        busy = false
                        onCaptured(jpeg, wide)
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
        // Oben: Tagesthema als Leiste auf dunklem Grund, nicht über dem Bild
        Box(Modifier.fillMaxWidth().statusBarsPadding().heightIn(min = 56.dp).padding(horizontal = 12.dp, vertical = 6.dp)) {
            if (theme.isNotBlank()) {
                DailyTheme(theme, Modifier.fillMaxWidth(), joined = themeJoined, onToggle = onToggleTheme)
            }
        }

        // Sucher im Fotoformat 3:4 mit runden Ecken – auf kleinen Bildschirmen schmaler statt abgeschnitten
        Box(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 8.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.aspectRatio(3f / 4f).clip(RoundedCornerShape(28.dp)).background(Color(0xFF1A1A1A)).testTag("viewfinder")) {
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

        // Unten: Blitz, Auslöser (weißer Ring, blauer Verlauf) und Kamera wechseln
        Row(
            Modifier.fillMaxWidth().padding(vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CameraButton(
                if (flashOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                stringResource(R.string.flash),
                highlighted = flashOn,
            ) { flashOn = !flashOn }
            Box(
                Modifier
                    .size(80.dp)
                    .border(5.dp, MaterialTheme.colorScheme.onBackground, CircleShape)
                    .padding(9.dp)
                    .clip(CircleShape)
                    .background(if (busy) SolidColor(Color.Gray) else GlobeGradient)
                    .clickable(enabled = !busy) { takePhoto() },
                contentAlignment = Alignment.Center,
            ) {
                if (busy) CircularProgressIndicator(Modifier.size(28.dp), color = Color.White)
            }
            CameraButton(Icons.Default.FlipCameraAndroid, stringResource(R.string.switch_camera)) {
                useWide = false
                lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                    CameraSelector.LENS_FACING_FRONT
                } else {
                    CameraSelector.LENS_FACING_BACK
                }
            }
        }
    }
}

/** Runder Knopf neben dem Auslöser (Blitz, Kamera wechseln). */
@Composable
private fun CameraButton(icon: ImageVector, description: String, highlighted: Boolean = false, onClick: () -> Unit) {
    val fg = MaterialTheme.colorScheme.onBackground
    Box(
        Modifier.size(52.dp).clip(RoundedCornerShape(16.dp))
            .background(if (highlighted) GlobeYellow else fg.copy(alpha = 0.1f))
            .clickable(onClick = onClick)
            .semantics { contentDescription = description; role = Role.Button },
        contentAlignment = Alignment.Center,
    ) { Icon(icon, contentDescription = null, tint = if (highlighted) Color.Black else fg) }
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
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.size(16.dp))
        Button(onClick = onRequest) { Text(stringResource(R.string.allow_camera)) }
    }
}

/**
 * Tagesthema vom Server als Leiste über dem Kamerabild (gelbes „THEMA DES TAGES“, Thema fett);
 * mit [onToggle] rechts ein Knopf zum Mitmachen.
 */
@Composable
internal fun DailyTheme(theme: String, modifier: Modifier = Modifier, joined: Boolean = false, onToggle: (() -> Unit)? = null) {
    val joinLabel = stringResource(if (joined) R.string.theme_joined else R.string.theme_join)
    // Hell: weiße Karte mit blauer Beschriftung; dunkel: Glasfläche mit gelber Beschriftung; mitgemacht: blau
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val text = if (joined || dark) Color.White else MaterialTheme.colorScheme.onSurface
    val label = when {
        joined -> Color.White.copy(alpha = 0.8f)
        dark -> GlobeYellow
        else -> MaterialTheme.colorScheme.primary
    }
    Row(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                when {
                    joined -> GlobeBlue.copy(alpha = 0.9f)
                    dark -> Color.White.copy(alpha = 0.12f)
                    else -> MaterialTheme.colorScheme.surfaceContainerLowest
                },
            )
            .then(if (onToggle != null) Modifier.clickable(onClick = onToggle).semantics { stateDescription = joinLabel } else Modifier)
            .padding(start = 16.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f, fill = false).padding(end = 12.dp)) {
            Text(stringResource(R.string.todays_theme), color = label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            Text(theme, color = text, style = MaterialTheme.typography.titleMedium)
        }
        if (onToggle != null) {
            Spacer(Modifier.weight(1f))
            Text(
                stringResource(if (joined) R.string.joined_short else R.string.join_short),
                color = if (joined) Color.White else Color.Black,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier
                    .background(if (joined) Color.White.copy(alpha = 0.2f) else GlobeYellow, CircleShape)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            )
        }
    }
}
