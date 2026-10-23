package tech.eiki.globeswap.ui.chat

import android.text.format.DateUtils
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import tech.eiki.globeswap.data.Message
import tech.eiki.globeswap.data.ReportReason
import tech.eiki.globeswap.ui.StrangerId
import tech.eiki.globeswap.ui.camera.PhotoCapture
import tech.eiki.globeswap.util.ImageUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(onBack: () -> Unit, vm: ChatViewModel = viewModel()) {
    val context = LocalContext.current
    val state by vm.detail.collectAsStateWithLifecycle()
    val loaded = state as? ChatViewModel.DetailState.Loaded
    val conversation = loaded?.detail?.conversation
    val messages = loaded?.detail?.messages.orEmpty()
    val sending by vm.sending.collectAsStateWithLifecycle()

    var replying by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    var reportTarget by remember { mutableStateOf<ReportTarget?>(null) }
    var confirmBlock by remember { mutableStateOf(false) }
    var viewing by remember { mutableStateOf<Message?>(null) }
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) { vm.toasts.collect { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() } }
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
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
                        Text("Stranger")
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
                    sending -> CircularProgressIndicator()
                    else -> Button(onClick = { replying = true }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text("Reply with a photo")
                    }
                }
            }
        },
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(messages, key = { it.id }) { message ->
                PhotoBubble(
                    message = message,
                    url = vm.photoUrl(message.photoId),
                    onClick = { viewing = message },
                )
            }
        }
    }

    viewing?.let { message ->
        PhotoViewer(
            url = vm.photoUrl(message.photoId),
            canReport = !message.mine,
            onDismiss = { viewing = null },
            onReport = { viewing = null; reportTarget = ReportTarget(message.id) },
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

private data class ReportTarget(val messageId: String?)

@Composable
private fun PhotoBubble(message: Message, url: String, onClick: () -> Unit) {
    val mine = message.mine
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = if (mine) Alignment.End else Alignment.Start,
    ) {
        AsyncImage(
            model = url,
            contentDescription = if (mine) "Your photo" else "Stranger's photo",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .aspectRatio(3f / 4f)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable(onClick = onClick),
        )
        Text(
            buildString {
                append(if (mine) "You" else "Stranger")
                append(" · ").append(DateUtils.getRelativeTimeSpanString(message.createdAt))
            },
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(top = 4.dp, start = 4.dp, end = 4.dp),
        )
    }
}

@Composable
private fun PhotoViewer(url: String, canReport: Boolean, onDismiss: () -> Unit, onReport: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
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
                Row {
                    IconButton(onClick = {
                        scope.launch {
                            val ok = runCatching { ImageUtils.saveToGallery(context, url) }.getOrDefault(false)
                            Toast.makeText(
                                context,
                                if (ok) "Saved to gallery" else "Could not save photo",
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                    }) {
                        Icon(Icons.Default.Download, contentDescription = "Save", tint = Color.White)
                    }
                    if (canReport) {
                        IconButton(onClick = onReport) {
                            Icon(Icons.Default.Flag, contentDescription = "Report photo", tint = Color.White)
                        }
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
