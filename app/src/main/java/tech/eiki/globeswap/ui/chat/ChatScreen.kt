package tech.eiki.globeswap.ui.chat

import android.text.format.DateUtils
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoNotDisturbOn
import androidx.compose.material.icons.filled.HideImage
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.SecureFlagPolicy
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import tech.eiki.globeswap.data.Countries
import tech.eiki.globeswap.data.displayName
import tech.eiki.globeswap.data.Message
import tech.eiki.globeswap.data.ReportReason
import tech.eiki.globeswap.ui.StrangerId
import tech.eiki.globeswap.ui.camera.PhotoCapture

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(onBack: () -> Unit, vm: ChatViewModel = viewModel()) {
    val context = LocalContext.current
    val state by vm.detail.collectAsStateWithLifecycle()
    val loaded = state as? ChatViewModel.DetailState.Loaded
    val conversation = loaded?.detail?.conversation
    val messages = loaded?.detail?.messages.orEmpty()
    val maxPhotos = loaded?.detail?.maxPhotos ?: 0
    val sending by vm.sending.collectAsStateWithLifecycle()

    var replying by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    var reportTarget by remember { mutableStateOf<ReportTarget?>(null) }
    var confirmBlock by remember { mutableStateOf(false) }
    var viewing by remember { mutableStateOf<Message?>(null) }
    var confirmDeleteChat by remember { mutableStateOf(false) }
    var confirmEndChat by remember { mutableStateOf(false) }
    var reactTarget by remember { mutableStateOf<Message?>(null) }
    var deleteTarget by remember { mutableStateOf<Message?>(null) }
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) { vm.toasts.collect { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() } }
    // Die Liste ist umgedreht (reverseLayout): Index 0 ist das neueste Foto ganz unten. So steht ein Chat
    // beim Öffnen immer ganz unten, auch bevor die Fotos geladen sind.
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(0)
            vm.markSeen()
        }
    }

    if (replying) {
        PhotoCapture(
            confirmLabel = "Reply",
            onConfirm = { vm.reply(it); replying = false },
            onCancel = { replying = false },
            hint = "Reply with a photo",
            modifier = Modifier.navigationBarsPadding(),
        )
        return
    }

    val blocked = conversation?.blocked == true

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(listOf(conversation?.displayName ?: "Stranger", Countries.label(conversation?.country.orEmpty())).filter { it.isNotEmpty() }.joinToString(" · "))
                        StrangerId(conversation?.label.orEmpty())
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { menuOpen = true }) { Icon(Icons.Default.MoreVert, contentDescription = "More") }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("Report") },
                            leadingIcon = { Icon(Icons.Default.Flag, contentDescription = null) },
                            onClick = { menuOpen = false; reportTarget = ReportTarget(null) },
                        )
                        DropdownMenuItem(
                            text = { Text("Block") },
                            leadingIcon = { Icon(Icons.Default.Block, contentDescription = null) },
                            onClick = { menuOpen = false; confirmBlock = true },
                        )
                        if (conversation?.ended == false) {
                            DropdownMenuItem(
                                text = { Text("End chat") },
                                leadingIcon = { Icon(Icons.Default.DoNotDisturbOn, contentDescription = null) },
                                onClick = { menuOpen = false; confirmEndChat = true },
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Delete chat") },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                            enabled = loaded != null,
                            onClick = { menuOpen = false; confirmDeleteChat = true },
                        )
                    }
                },
            )
        },
        bottomBar = {
            Box(Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp), contentAlignment = Alignment.Center) {
                when {
                    state == ChatViewModel.DetailState.Missing ->
                        Text("This chat no longer exists.", color = MaterialTheme.colorScheme.error)
                    state == ChatViewModel.DetailState.Loading -> CircularProgressIndicator()
                    blocked -> Text("This chat is blocked.", color = MaterialTheme.colorScheme.error)
                    conversation?.ended == true -> ChatEnded(byMe = conversation.endedByMe, otherName = conversation.name)
                    sending -> CircularProgressIndicator()
                    // Immer im Wechsel: erst wieder antworten, wenn das Gegenüber ein Foto geschickt hat.
                    conversation?.lastFromMe == true -> WaitingForReply()
                    else -> Button(onClick = { replying = true }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text("Reply with a photo")
                    }
                }
            }
        },
    ) { padding ->
        MessageList(
            messages = messages,
            maxPhotos = maxPhotos,
            otherName = conversation?.name.orEmpty(),
            photoUrl = vm::photoUrl,
            listState = listState,
            modifier = Modifier.fillMaxSize().padding(padding),
            onOpen = { viewing = it },
            onLongPress = { if (it.mine) deleteTarget = it else reactTarget = it },
            onDoubleTap = { vm.react(it.id, if (it.reaction == HEART) "" else HEART) },
        )
    }

    viewing?.let { message ->
        PhotoViewer(
            url = vm.photoUrl(message.photoId),
            mine = message.mine,
            onDismiss = { viewing = null },
            onReport = { viewing = null; reportTarget = ReportTarget(message.id) },
            onDelete = { viewing = null; deleteTarget = message },
        )
    }

    deleteTarget?.let { message ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete this photo?") },
            text = { Text("It will be removed for both of you.") },
            confirmButton = {
                TextButton(onClick = { deleteTarget = null; vm.deletePhoto(message.id) }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Cancel") } },
        )
    }

    reactTarget?.let { message ->
        ReactionPicker(
            current = message.reaction,
            onPick = { emoji -> reactTarget = null; vm.react(message.id, if (emoji == message.reaction) "" else emoji) },
            onDismiss = { reactTarget = null },
        )
    }

    if (confirmEndChat) {
        AlertDialog(
            onDismissRequest = { confirmEndChat = false },
            title = { Text("End this chat?") },
            text = { Text("Neither of you can send more photos. The chat stays visible, and you might meet again in a future swap.") },
            confirmButton = { TextButton(onClick = { confirmEndChat = false; vm.endChat() }) { Text("End chat") } },
            dismissButton = { TextButton(onClick = { confirmEndChat = false }) { Text("Cancel") } },
        )
    }

    if (confirmDeleteChat) {
        AlertDialog(
            onDismissRequest = { confirmDeleteChat = false },
            title = { Text("Delete this chat?") },
            text = { Text("The chat and all its photos will be deleted for both of you. This can't be undone.") },
            confirmButton = {
                TextButton(onClick = { confirmDeleteChat = false; vm.deleteChat(onDeleted = onBack) }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDeleteChat = false }) { Text("Cancel") } },
        )
    }

    reportTarget?.let { target ->
        ReportDialog(
            onDismiss = { reportTarget = null },
            onSubmit = { reason, alsoBlock ->
                reportTarget = null
                vm.report(reason, target.messageId, alsoBlock, onBlocked = onBack)
            },
        )
    }

    if (confirmBlock) {
        AlertDialog(
            onDismissRequest = { confirmBlock = false },
            title = { Text("Block this person?") },
            text = { Text("You will never get each other's photos again and this chat will be hidden.") },
            confirmButton = {
                TextButton(onClick = { confirmBlock = false; vm.block(onBlocked = onBack) }) { Text("Block") }
            },
            dismissButton = { TextButton(onClick = { confirmBlock = false }) { Text("Cancel") } },
        )
    }
}

/**
 * Die Fotos eines Chats. Die Liste ist umgedreht (reverseLayout), damit sie beim Öffnen immer ganz unten
 * beim neuesten Foto steht – auch bevor die Bilder geladen sind.
 */
@Composable
internal fun MessageList(
    messages: List<Message>,
    maxPhotos: Int,
    photoUrl: (String) -> String,
    listState: LazyListState,
    otherName: String = "",
    modifier: Modifier = Modifier,
    onOpen: (Message) -> Unit = {},
    onLongPress: (Message) -> Unit = {},
    onDoubleTap: (Message) -> Unit = {},
) {
    LazyColumn(
        state = listState,
        modifier = modifier,
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        reverseLayout = true,
    ) {
        items(messages.asReversed(), key = { it.id }) { message ->
            if (message.deleted) {
                DeletedPhoto(message)
            } else {
                PhotoBubble(
                    message = message,
                    otherName = otherName,
                    url = photoUrl(message.photoId),
                    onClick = { onOpen(message) },
                    onLongClick = { onLongPress(message) },
                    onDoubleClick = if (message.mine) null else ({ onDoubleTap(message) }),
                )
            }
        }
        // Ganz oben (wegen reverseLayout zuletzt): Hinweis auf die Grenze.
        if (maxPhotos > 0) {
            item(key = "limit") { PhotoLimitHint(maxPhotos) }
        }
    }
}

private data class ReportTarget(val messageId: String?)

private const val HEART = "❤️"

/** Dieselben Emojis wie auf dem Server. */
internal val REACTIONS = listOf("❤️", "😂", "😮", "😢", "👍", "🔥")

@Composable
internal fun ReactionBadge(emoji: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        modifier = modifier.padding(horizontal = 10.dp),
    ) {
        Text(emoji, fontSize = 18.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
    }
}

@Composable
internal fun ReactionPicker(current: String, onPick: (String) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("React to this photo") },
        text = {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                REACTIONS.forEach { emoji ->
                    Box(
                        Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(50))
                            .background(if (emoji == current) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                            .clickable { onPick(emoji) },
                        contentAlignment = Alignment.Center,
                    ) { Text(emoji, fontSize = 26.sp) }
                }
            }
        },
        confirmButton = {
            if (current.isNotEmpty()) TextButton(onClick = { onPick(current) }) { Text("Remove reaction") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
internal fun ChatEnded(byMe: Boolean, otherName: String = "") {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(if (byMe) "You ended this chat" else "${otherName.ifBlank { "The stranger" }} ended this chat", style = MaterialTheme.typography.titleSmall)
        Text(
            "No more photos can be sent here. Take a new photo to meet someone else.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/** Platzhalter für ein gelöschtes Foto, auf der Seite der Person, die es geschickt hatte. */
@Composable
internal fun DeletedPhoto(message: Message) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        horizontalArrangement = if (message.mine) Arrangement.End else Arrangement.Start,
    ) {
        Row(
            Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.HideImage,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
            Text(
                if (message.mine) "You deleted a photo" else "The stranger deleted a photo",
                style = MaterialTheme.typography.labelMedium,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 6.dp),
            )
        }
    }
}

/** Nur die neuesten Fotos bleiben – ältere verschwinden bei beiden. */
@Composable
internal fun PhotoLimitHint(maxPhotos: Int) {
    Text(
        "Only the latest $maxPhotos photos are kept – older ones disappear for both of you.",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
    )
}

@Composable
private fun WaitingForReply() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Waiting for the stranger's photo", style = MaterialTheme.typography.titleSmall)
        Text(
            "You take turns – you can reply once they've sent one.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun PhotoBubble(
    message: Message,
    url: String,
    onClick: () -> Unit,
    otherName: String = "",
    onLongClick: () -> Unit,
    onDoubleClick: (() -> Unit)? = null,
) {
    val mine = message.mine
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = if (mine) Alignment.End else Alignment.Start,
    ) {
        Box(Modifier.fillMaxWidth(0.8f)) {
            AsyncImage(
                model = url,
                contentDescription = if (mine) "Your photo" else "Stranger's photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(3f / 4f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .combinedClickable(onClick = onClick, onLongClick = onLongClick, onDoubleClick = onDoubleClick),
            )
            if (message.reaction.isNotEmpty()) {
                ReactionBadge(
                    message.reaction,
                    Modifier.align(if (mine) Alignment.BottomStart else Alignment.BottomEnd).offset(y = 10.dp),
                )
            }
        }
        Text(
            buildString {
                append(if (mine) "You" else otherName.ifBlank { "Stranger" })
                if (!mine && message.country.isNotEmpty()) append(" · ").append(Countries.label(message.country))
                append(" · ").append(DateUtils.getRelativeTimeSpanString(message.createdAt))
            },
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(top = if (message.reaction.isNotEmpty()) 14.dp else 4.dp, start = 4.dp, end = 4.dp),
        )
    }
}

@Composable
private fun PhotoViewer(url: String, mine: Boolean, onDismiss: () -> Unit, onReport: () -> Unit, onDelete: () -> Unit) {
    // Kein Speichern; Screenshots sind auch im Vollbild gesperrt (eigenes Fenster, daher ausdrücklich SecureOn).
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, securePolicy = SecureFlagPolicy.SecureOn),
    ) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
                if (mine) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete photo", tint = Color.White)
                    }
                } else {
                    IconButton(onClick = onReport) {
                        Icon(Icons.Default.Flag, contentDescription = "Report photo", tint = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportDialog(onDismiss: () -> Unit, onSubmit: (ReportReason, Boolean) -> Unit) {
    var reason by remember { mutableStateOf(ReportReason.NUDITY) }
    var alsoBlock by remember { mutableStateOf(true) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Report") },
        text = {
            Column {
                ReportReason.entries.forEach { option ->
                    Row(
                        Modifier.fillMaxWidth().selectable(selected = reason == option, onClick = { reason = option }),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = reason == option, onClick = null)
                        Text(option.label, Modifier.padding(start = 8.dp, top = 8.dp, bottom = 8.dp))
                    }
                }
                Row(
                    Modifier.fillMaxWidth().clickable { alsoBlock = !alsoBlock },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = alsoBlock, onCheckedChange = null)
                    Text("Also block this person", Modifier.padding(start = 8.dp, top = 12.dp, bottom = 12.dp))
                }
            }
        },
        confirmButton = { TextButton(onClick = { onSubmit(reason, alsoBlock) }) { Text("Report") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
