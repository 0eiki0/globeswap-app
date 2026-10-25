package tech.eiki.globeswap.ui.camera

import android.annotation.SuppressLint
import android.hardware.camera2.CameraCharacteristics
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import kotlin.math.abs
import kotlin.math.atan

/** Zoomstufen als Knöpfe: Weitwinkel (unter 1×, wenn vorhanden), 1×, 2×, 5×, 10× – soweit das Handy sie kann. */
fun zoomPresets(min: Float, max: Float, separateWide: Boolean = false): List<Float> = buildList {
    if (min < 0.95f) add(roundZoom(min)) else if (separateWide) add(WIDE)
    add(1f)
    listOf(2f, 5f, 10f).filter { it <= max + 0.01f }.forEach(::add)
}.take(4)

/** Platzhalter für "auf die eigene Weitwinkelkamera umschalten" (Handys ohne Zoom unter 1×). */
const val WIDE = 0.5f

private fun roundZoom(z: Float) = (Math.round(z * 10) / 10f)

/** "0.6", "1", "2", "1.4" */
fun formatZoom(z: Float): String =
    if (abs(z - Math.round(z)) < 0.05f) Math.round(z).toString() else String.format(Locale.US, "%.1f", z)

/** Welcher Knopf leuchtet: der nächstliegende, der nicht über dem aktuellen Zoom liegt. */
fun activePreset(presets: List<Float>, current: Float): Float =
    presets.lastOrNull { it <= current + 0.05f } ?: presets.first()

/** Horizontaler Bildwinkel in Grad aus Brennweite und Sensorbreite (mm). */
fun horizontalFov(focalLength: Float, sensorWidth: Float): Double =
    Math.toDegrees(2 * atan(sensorWidth / (2.0 * focalLength)))

/**
 * Manche Handys melden die Weitwinkelkamera nicht als Zoomstufe unter 1×, sondern als eigene Kamera.
 * Dann suchen wir die hintere Kamera mit dem deutlich größten Bildwinkel und wählen sie direkt.
 */
@SuppressLint("RestrictedApi")
@androidx.annotation.OptIn(ExperimentalCamera2Interop::class)
fun wideCameraSelector(available: List<CameraInfo>, current: CameraInfo?): CameraSelector? {
    fun fov(info: CameraInfo): Double? = runCatching {
        val c2 = Camera2CameraInfo.from(info)
        val focal = c2.getCameraCharacteristic(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)?.minOrNull()
        val sensor = c2.getCameraCharacteristic(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)
        if (focal == null || sensor == null || focal <= 0f) null else horizontalFov(focal, sensor.width)
    }.getOrNull()

    val currentFov = current?.let(::fov) ?: return null
    val wide = available
        .filter { it.lensFacing == CameraSelector.LENS_FACING_BACK && it != current }
        .mapNotNull { info -> fov(info)?.let { info to it } }
        .filter { (_, f) -> f > currentFov * 1.25 }
        .maxByOrNull { (_, f) -> f }?.first ?: return null
    val id = Camera2CameraInfo.from(wide).cameraId
    return CameraSelector.Builder()
        .addCameraFilter { infos -> infos.filter { Camera2CameraInfo.from(it).cameraId == id } }
        .build()
}

/** Zoom-Knöpfe über dem Auslöser; der aktive zeigt den genauen Zoom ("1.4×"). */
@Composable
fun ZoomChips(presets: List<Float>, current: Float, onSelect: (Float) -> Unit, modifier: Modifier = Modifier) {
    if (presets.size < 2) return
    val active = activePreset(presets, current)
    Row(
        modifier.background(Color.Black.copy(alpha = 0.35f), CircleShape).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        presets.forEach { preset ->
            val selected = preset == active
            val label = if (selected) "${formatZoom(current)}×" else formatZoom(preset)
            Box(
                Modifier
                    .size(if (selected) 44.dp else 36.dp)
                    .background(if (selected) Color.White.copy(alpha = 0.25f) else Color.Transparent, CircleShape)
                    .clickable { onSelect(preset) }
                    .semantics { contentDescription = "Zoom ${formatZoom(preset)}×"; this.selected = selected },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    color = if (selected) Color(0xFFFFD54F) else Color.White,
                    fontSize = if (selected) 13.sp else 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}
