package tech.eiki.globeswap.ui.editor

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.text.StaticLayout
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke as DrawStroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tech.eiki.globeswap.R
import tech.eiki.globeswap.data.PhotoInfo
import tech.eiki.globeswap.util.ImageUtils
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class Tool { NONE, DRAW, FILTER }

/** Unterhalb dieser Höhe (relativ zum Bild) wird ein Sticker beim Loslassen gelöscht. */
private const val TRASH_ZONE = 0.86f

/**
 * Bearbeiten vor dem Senden: Text, Zeichnen, Emoji-Sticker und Filter.
 * [onConfirm] bekommt das fertig gerenderte JPEG und seine Merkmale (bearbeitet, Filter) für die Erfolge.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoEditor(
    jpeg: ByteArray,
    confirmLabel: String,
    onRetake: () -> Unit,
    onConfirm: (ByteArray, PhotoInfo) -> Unit,
) {
    val source = remember(jpeg) { BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size) }
    val thumbnail = remember(source) { thumbnailOf(source) }
    val state = remember(jpeg) { EditorState() }
    val scope = rememberCoroutineScope()

    var tool by remember { mutableStateOf(Tool.NONE) }
    var brushColor by remember { mutableStateOf(EditorColors[2]) }
    var brushSize by remember { mutableStateOf(BrushSize.MEDIUM) }
    var textEditing by remember { mutableStateOf<TextDraft?>(null) }
    var showEmojis by remember { mutableStateOf(false) }
    var draggingSticker by remember { mutableStateOf<Long?>(null) }
    var selectedId by remember { mutableStateOf<Long?>(null) }
    if (selectedId != null && state.stickers.none { it.id == selectedId }) selectedId = null
    var exporting by remember { mutableStateOf(false) }

    BackHandler {
        when {
            selectedId != null -> selectedId = null
            tool != Tool.NONE -> tool = Tool.NONE
            else -> onRetake()
        }
    }

    fun export() {
        if (exporting) return
        exporting = true
        scope.launch {
            val result = withContext(Dispatchers.Default) {
                val edited = state.strokes.isNotEmpty() || state.stickers.isNotEmpty() || state.filter != PhotoFilter.ORIGINAL
                if (edited) ImageUtils.encodeJpeg(EditorRenderer.render(source, state)) else jpeg
            }
            exporting = false
            val info = PhotoInfo(
                edited = state.strokes.isNotEmpty() || state.stickers.isNotEmpty(),
                filter = state.filter.takeIf { it != PhotoFilter.ORIGINAL }?.name,
            )
            onConfirm(result, info)
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        // ---------------------------------------------------------------- Foto mit Ebenen
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            BoxWithConstraints(Modifier.aspectRatio(source.width.toFloat() / source.height)) {
                val boxW = constraints.maxWidth.toFloat()
                val boxH = constraints.maxHeight.toFloat()

                Image(
                    bitmap = remember(source) { source.asImageBitmap() },
                    contentDescription = stringResource(R.string.photo),
                    contentScale = ContentScale.FillBounds,
                    colorFilter = state.filter.matrix?.let { ColorFilter.colorMatrix(ColorMatrix(it)) },
                    modifier = Modifier.fillMaxSize(),
                )

                val currentStroke = remember { mutableStateListOf<Offset>() }
                Canvas(Modifier.fillMaxSize()) {
                    drawIntoCanvas { canvas ->
                        state.strokes.forEach { EditorRenderer.drawStroke(canvas.nativeCanvas, it, boxW, boxH) }
                        if (currentStroke.isNotEmpty()) {
                            val live = Stroke(currentStroke.toList(), brushColor, brushSize.width)
                            EditorRenderer.drawStroke(canvas.nativeCanvas, live, boxW, boxH)
                        }
                    }
                }

                val density = LocalDensity.current
                val pad = with(density) { 10.dp.toPx() }
                val handleRadius = with(density) { 15.dp.toPx() }
                val layouts = remember(boxW) { HashMap<Triple<StickerKind, String, Int>, StaticLayout>() }
                fun layoutOf(sticker: Sticker) =
                    layouts.getOrPut(Triple(sticker.kind, sticker.text, sticker.color.toArgb())) { EditorRenderer.stickerLayout(sticker, boxW) }
                fun boxOf(sticker: Sticker) = layoutOf(sticker).let { stickerBox(sticker, it.width, it.height, boxW, boxH) }

                state.stickers.forEach { sticker -> StickerView(sticker, layoutOf(sticker), boxW, boxH) }

                // Rahmen, Anfasser (unten rechts: drehen + Größe) und Löschen (oben links) am ausgewählten Sticker
                val selected = state.stickers.firstOrNull { it.id == selectedId }
                if (selected != null && tool != Tool.DRAW) {
                    val box = boxOf(selected)
                    Canvas(Modifier.fillMaxSize()) {
                        val frame = Path().apply {
                            val corners = box.frame(pad)
                            moveTo(corners[0].x, corners[0].y)
                            corners.drop(1).forEach { lineTo(it.x, it.y) }
                            close()
                        }
                        drawPath(frame, Color.Black.copy(alpha = 0.35f), style = DrawStroke(width = 3.dp.toPx()))
                        drawPath(
                            frame,
                            Color.White,
                            style = DrawStroke(width = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f))),
                        )
                    }
                    FrameButton(box.corner(1f, 1f, pad), handleRadius, Icons.Default.OpenInFull, stringResource(R.string.resize_rotate), MaterialTheme.colorScheme.primary)
                    FrameButton(box.corner(-1f, -1f, pad), handleRadius, Icons.Default.Close, stringResource(R.string.remove), Color(0xFFFF3B30))
                    if (draggingSticker == null) {
                        Text(
                            stringResource(R.string.resize_hint),
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 12.dp, start = 24.dp, end = 24.dp)
                                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(50))
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                    }
                }

                // Eine Gestenerkennung für alle Sticker: Ziehen, Anfasser, zwei Finger überall auf dem Foto.
                val touchSlop = LocalViewConfiguration.current.touchSlop
                if (tool != Tool.DRAW) {
                    Box(
                        Modifier.fillMaxSize().pointerInput(boxW, boxH) {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                val sel = state.stickers.firstOrNull { it.id == selectedId }
                                val selBox = sel?.let(::boxOf)
                                val hit = state.stickers.lastOrNull { boxOf(it).contains(down.position, pad) }
                                val mode = when {
                                    selBox != null && (down.position - selBox.corner(1f, 1f, pad)).getDistance() <= handleRadius * 1.6f -> GestureMode.HANDLE
                                    selBox != null && (down.position - selBox.corner(-1f, -1f, pad)).getDistance() <= handleRadius * 1.6f -> GestureMode.REMOVE
                                    hit != null -> GestureMode.STICKER
                                    else -> GestureMode.BACKGROUND
                                }
                                val target = if (mode == GestureMode.STICKER) hit else sel
                                val handleStart = selBox?.let { it.corner(1f, 1f, pad) - it.center }
                                val startScale = sel?.scale ?: 1f
                                val startRotation = sel?.rotation ?: 0f
                                var moved = 0f
                                var started = false

                                while (true) {
                                    val event = awaitPointerEvent()
                                    val pressed = event.changes.filter { it.pressed }
                                    if (pressed.isEmpty()) break
                                    val multi = pressed.size >= 2
                                    if (target == null) continue

                                    if (mode == GestureMode.HANDLE && !multi && handleStart != null) {
                                        val finger = event.changes.firstOrNull { it.id == down.id } ?: continue
                                        moved = maxOf(moved, (finger.position - down.position).getDistance())
                                        if (!started && moved > touchSlop / 2) started = true
                                        if (started) {
                                            val center = Offset(target.center.x * boxW, target.center.y * boxH)
                                            val now = finger.position - center
                                            if (now.getDistance() > 1f) {
                                                state.updateSticker(target.id) {
                                                    it.copy(
                                                        scale = (startScale * now.getDistance() / handleStart.getDistance())
                                                            .coerceIn(MIN_STICKER_SCALE, MAX_STICKER_SCALE),
                                                        rotation = startRotation + angleDelta(handleStart.angle(), now.angle()),
                                                    )
                                                }
                                            }
                                            finger.consume()
                                        }
                                        continue
                                    }
                                    if (mode == GestureMode.REMOVE) continue

                                    val pan = event.calculatePan()
                                    val zoom = event.calculateZoom()
                                    val rotation = event.calculateRotation()
                                    moved += pan.getDistance()
                                    // Ein Finger auf dem Hintergrund bewegt nichts, zwei Finger wirken auf den ausgewählten Sticker.
                                    val canMove = mode == GestureMode.STICKER || multi
                                    if (!started && canMove && (moved > touchSlop || (multi && (zoom != 1f || rotation != 0f)))) {
                                        started = true
                                        selectedId = target.id
                                        if (mode == GestureMode.STICKER) draggingSticker = target.id
                                    }
                                    if (started) {
                                        state.updateSticker(target.id) {
                                            it.copy(
                                                center = Offset(
                                                    (it.center.x + pan.x / boxW).coerceIn(0f, 1f),
                                                    (it.center.y + pan.y / boxH).coerceIn(0f, 1f),
                                                ),
                                                scale = (it.scale * zoom).coerceIn(MIN_STICKER_SCALE, MAX_STICKER_SCALE),
                                                rotation = it.rotation + rotation,
                                            )
                                        }
                                        event.changes.forEach { if (it.positionChanged()) it.consume() }
                                    }
                                }

                                when {
                                    started && mode == GestureMode.STICKER && target != null -> {
                                        draggingSticker = null
                                        val now = state.stickers.firstOrNull { it.id == target.id }
                                        if (now != null && now.center.y > TRASH_ZONE) state.removeSticker(target.id)
                                    }
                                    started -> Unit
                                    mode == GestureMode.REMOVE && target != null -> state.removeSticker(target.id)
                                    mode == GestureMode.STICKER && hit != null ->
                                        if (hit.id == selectedId && hit.kind == StickerKind.TEXT) {
                                            textEditing = TextDraft(hit.id, hit.text, hit.color)
                                        } else {
                                            selectedId = hit.id
                                        }
                                    mode == GestureMode.BACKGROUND -> selectedId = null
                                }
                                draggingSticker = null
                            }
                        },
                    )
                }

                // Zeichenfläche liegt im Zeichenmodus über allem.
                if (tool == Tool.DRAW) {
                    Box(
                        Modifier.fillMaxSize().pointerInput(brushColor, brushSize) {
                            detectDragGestures(
                                onDragStart = { currentStroke.clear(); currentStroke += Offset(it.x / boxW, it.y / boxH) },
                                onDrag = { change, _ ->
                                    change.consume()
                                    currentStroke += Offset(change.position.x / boxW, change.position.y / boxH)
                                },
                                onDragEnd = {
                                    if (currentStroke.isNotEmpty()) state.addStroke(Stroke(currentStroke.toList(), brushColor, brushSize.width))
                                    currentStroke.clear()
                                },
                                onDragCancel = { currentStroke.clear() },
                            )
                        },
                    )
                }

                val dragged = state.stickers.firstOrNull { it.id == draggingSticker }
                if (dragged != null) {
                    val armed = dragged.center.y > TRASH_ZONE
                    Box(
                        Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 16.dp)
                            .size(if (armed) 64.dp else 52.dp)
                            .background(if (armed) Color(0xFFFF3B30) else Color.Black.copy(alpha = 0.55f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.drag_to_delete), tint = Color.White)
                    }
                }
            }
        }

        // ---------------------------------------------------------------- Bedienelemente
        if (textEditing == null) {
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                RoundButton(Icons.Default.Close, stringResource(R.string.discard_photo), onClick = onRetake)
                Column(verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    RoundButton(Icons.Default.TextFields, stringResource(R.string.add_text)) {
                        tool = Tool.NONE
                        textEditing = TextDraft(null, "", Color.White)
                    }
                    RoundButton(Icons.Default.Brush, stringResource(R.string.draw), selected = tool == Tool.DRAW) {
                        selectedId = null
                        tool = if (tool == Tool.DRAW) Tool.NONE else Tool.DRAW
                    }
                    RoundButton(Icons.Default.EmojiEmotions, stringResource(R.string.add_sticker)) {
                        tool = Tool.NONE
                        showEmojis = true
                    }
                    RoundButton(Icons.Default.AutoAwesome, stringResource(R.string.filters), selected = tool == Tool.FILTER) {
                        tool = if (tool == Tool.FILTER) Tool.NONE else Tool.FILTER
                    }
                    if (state.canUndo) RoundButton(Icons.AutoMirrored.Filled.Undo, stringResource(R.string.undo), onClick = state::undo)
                }
            }

            Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(bottom = 16.dp)) {
                when (tool) {
                    Tool.DRAW -> DrawPanel(brushColor, { brushColor = it }, brushSize, { brushSize = it })
                    Tool.FILTER -> FilterPanel(thumbnail, state.filter) { state.filter = it }
                    Tool.NONE -> Unit
                }
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onRetake) { Text(stringResource(R.string.retake), color = Color.White) }
                    Button(onClick = ::export, enabled = !exporting) {
                        if (exporting) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                        } else {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(confirmLabel)
                    }
                }
            }
        }

        textEditing?.let { draft ->
            TextInput(
                draft = draft,
                onDone = { text, color ->
                    textEditing = null
                    when {
                        draft.stickerId == null && text.isNotBlank() -> selectedId = state.addSticker(StickerKind.TEXT, text.trim(), color)
                        draft.stickerId != null && text.isBlank() -> state.removeSticker(draft.stickerId)
                        draft.stickerId != null -> state.updateSticker(draft.stickerId) { it.copy(text = text.trim(), color = color) }
                    }
                },
            )
        }
    }

    if (showEmojis) {
        ModalBottomSheet(onDismissRequest = { showEmojis = false }) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(56.dp),
                modifier = Modifier.fillMaxWidth().height(360.dp).padding(horizontal = 12.dp),
            ) {
                items(EditorEmojis) { emoji ->
                    Box(
                        Modifier.size(56.dp).clip(CircleShape).clickable {
                            selectedId = state.addSticker(StickerKind.EMOJI, emoji, Color.Black)
                            showEmojis = false
                        },
                        contentAlignment = Alignment.Center,
                    ) { Text(emoji, fontSize = 32.sp) }
                }
            }
        }
    }
}

private data class TextDraft(val stickerId: Long?, val text: String, val color: Color)

private enum class GestureMode { STICKER, HANDLE, REMOVE, BACKGROUND }

/** Zeichnet einen Sticker; Gesten erkennt die Foto-Fläche (siehe oben). */
@Composable
private fun StickerView(sticker: Sticker, layout: StaticLayout, boxW: Float, boxH: Float) {
    // In der endgültigen Größe zeichnen (nicht klein zeichnen und hochskalieren), damit Text scharf bleibt.
    val density = LocalDensity.current
    val w = layout.width * sticker.scale
    val h = layout.height * sticker.scale
    val kind = if (sticker.kind == StickerKind.TEXT) stringResource(R.string.kind_text) else stringResource(R.string.kind_sticker)
    val desc = stringResource(R.string.sticker_desc, kind, sticker.text)
    val stateDesc = stringResource(R.string.sticker_state, (sticker.scale * 100).roundToInt(), normalizedDegrees(sticker.rotation))
    Canvas(
        Modifier
            .offset {
                IntOffset(
                    (sticker.center.x * boxW - w / 2f).roundToInt(),
                    (sticker.center.y * boxH - h / 2f).roundToInt(),
                )
            }
            .requiredSize(with(density) { w.toDp() }, with(density) { h.toDp() })
            .semantics {
                contentDescription = desc
                stateDescription = stateDesc
            }
            .graphicsLayer { rotationZ = sticker.rotation },
    ) {
        drawIntoCanvas {
            val canvas = it.nativeCanvas
            canvas.save()
            canvas.scale(sticker.scale, sticker.scale)
            layout.draw(canvas)
            canvas.restore()
        }
    }
}

private fun normalizedDegrees(degrees: Float): Int = ((degrees.roundToInt() % 360) + 360) % 360

/** Runder Knopf am Rahmen (nur Anzeige; angetippt wird über die Gestenerkennung der Foto-Fläche). */
@Composable
private fun FrameButton(position: Offset, radius: Float, icon: ImageVector, description: String, color: Color) {
    val density = LocalDensity.current
    val size = with(density) { (radius * 2).toDp() }
    Box(
        Modifier
            .offset { IntOffset((position.x - radius).roundToInt(), (position.y - radius).roundToInt()) }
            .requiredSize(size)
            .semantics { contentDescription = description }
            .background(Color.White, CircleShape)
            .border(2.dp, color, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(size * 0.6f))
    }
}

@Composable
private fun RoundButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    selected: Boolean = false,
    onClick: () -> Unit,
) {
    Box(
        Modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(if (selected) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.45f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = description, tint = Color.White)
    }
}

@Composable
private fun ColorRow(selected: Color, onSelect: (Color) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EditorColors.forEach { color ->
            val isSelected = color == selected
            Box(
                Modifier
                    .size(if (isSelected) 34.dp else 28.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(if (isSelected) 3.dp else 1.5.dp, Color.White, CircleShape)
                    .clickable { onSelect(color) },
            )
        }
    }
}

@Composable
private fun DrawPanel(color: Color, onColor: (Color) -> Unit, size: BrushSize, onSize: (BrushSize) -> Unit) {
    Column(Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.45f)).padding(vertical = 6.dp)) {
        ColorRow(color, onColor)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            BrushSize.entries.forEach { option ->
                val dot = when (option) {
                    BrushSize.SMALL -> 8.dp
                    BrushSize.MEDIUM -> 14.dp
                    BrushSize.LARGE -> 22.dp
                }
                Box(
                    Modifier
                        .padding(horizontal = 10.dp)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (option == size) Color.White.copy(alpha = 0.25f) else Color.Transparent)
                        .clickable { onSize(option) },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(Modifier.size(dot).background(color, CircleShape).border(1.dp, Color.White, CircleShape))
                }
            }
        }
    }
}

@Composable
private fun FilterPanel(thumbnail: Bitmap, selected: PhotoFilter, onSelect: (PhotoFilter) -> Unit) {
    val image = remember(thumbnail) { thumbnail.asImageBitmap() }
    LazyRow(
        Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.45f)).padding(vertical = 10.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(PhotoFilter.entries) { filter ->
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onSelect(filter) }) {
                Image(
                    bitmap = image,
                    contentDescription = stringResource(filter.label),
                    contentScale = ContentScale.Crop,
                    colorFilter = filter.matrix?.let { ColorFilter.colorMatrix(ColorMatrix(it)) },
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(
                            if (filter == selected) 3.dp else 0.dp,
                            if (filter == selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                            RoundedCornerShape(12.dp),
                        ),
                )
                Text(stringResource(filter.label), color = Color.White, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

@Composable
private fun TextInput(draft: TextDraft, onDone: (String, Color) -> Unit) {
    var text by remember(draft) { mutableStateOf(draft.text) }
    var color by remember(draft) { mutableStateOf(draft.color) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    BackHandler { onDone(text, color) }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable(interactionSource = null, indication = null) { onDone(text, color) }
            .imePadding(),
    ) {
        TextButton(
            onClick = { onDone(text, color) },
            modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(8.dp),
        ) { Text(stringResource(R.string.done), color = Color.White, fontWeight = FontWeight.Bold) }

        BasicTextField(
            value = text,
            onValueChange = { text = it },
            textStyle = TextStyle(color = color, fontSize = 30.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
            cursorBrush = SolidColor(Color.White),
            modifier = Modifier.align(Alignment.Center).fillMaxWidth().padding(24.dp).focusRequester(focus),
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.Center) {
                    if (text.isEmpty()) {
                        Text(stringResource(R.string.type_something), color = Color.White.copy(alpha = 0.5f), fontSize = 30.sp, fontWeight = FontWeight.Bold)
                    }
                    inner()
                }
            },
        )

        Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp)) {
            ColorRow(color) { color = it }
        }
    }
}

private fun thumbnailOf(source: Bitmap): Bitmap {
    val scale = 160f / maxOf(source.width, source.height)
    return Bitmap.createScaledBitmap(source, (source.width * scale).roundToInt().coerceAtLeast(1), (source.height * scale).roundToInt().coerceAtLeast(1), true)
}

