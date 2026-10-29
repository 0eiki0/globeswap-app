package tech.eiki.globeswap.ui.conversations

import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import tech.eiki.globeswap.R
import tech.eiki.globeswap.data.Conversation
import tech.eiki.globeswap.data.Countries
import tech.eiki.globeswap.data.displayName
import tech.eiki.globeswap.ui.AppCard
import tech.eiki.globeswap.ui.ScreenTitle
import tech.eiki.globeswap.ui.StrangerId

private enum class Filter { ALL, NEW, YOUR_TURN }

@Composable
fun ConversationsScreen(
    conversations: List<Conversation>,
    photoUrl: (String) -> String,
    onOpen: (String) -> Unit,
    onDeleteAccount: () -> Unit,
    publicId: String? = null,
    onAboutOpened: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenChangelog: () -> Unit = {},
) {
    var showAbout by remember { mutableStateOf(false) }
    if (showAbout) AboutDialog(publicId, onDismiss = { showAbout = false })
    var menuOpen by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var filter by rememberSaveable { mutableStateOf(Filter.ALL) }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.delete_all_q)) },
            text = { Text(stringResource(R.string.delete_all_text)) },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onDeleteAccount() }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
    val newCount = conversations.count { it.unread }
    val yourTurn: (Conversation) -> Boolean = { !it.unread && !it.ended && !it.lastFromMe }
    val shown = when (filter) {
        Filter.ALL -> conversations
        Filter.NEW -> conversations.filter { it.unread }
        Filter.YOUR_TURN -> conversations.filter(yourTurn)
    }
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        ScreenTitle(stringResource(R.string.chats)) {
            IconButton(onClick = onOpenSettings) { Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings)) }
            Box {
                IconButton(onClick = { menuOpen = true }) { Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.more)) }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.settings)) },
                        leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) },
                        onClick = { menuOpen = false; onOpenSettings() },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.whats_new)) },
                        leadingIcon = { Icon(Icons.Default.NewReleases, contentDescription = null) },
                        onClick = { menuOpen = false; onOpenChangelog() },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.about_privacy)) },
                        leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) },
                        onClick = { menuOpen = false; showAbout = true; onAboutOpened() },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.delete_my_data)) },
                        leadingIcon = { Icon(Icons.Default.DeleteForever, contentDescription = null) },
                        onClick = { menuOpen = false; confirmDelete = true },
                    )
                }
            }
        }
        if (conversations.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Text(
                    stringResource(R.string.no_swaps_yet),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return@Column
        }
        Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterPill(stringResource(R.string.filter_all), filter == Filter.ALL) { filter = Filter.ALL }
            FilterPill(stringResource(R.string.filter_new, newCount), filter == Filter.NEW) { filter = Filter.NEW }
            FilterPill(stringResource(R.string.your_turn), filter == Filter.YOUR_TURN) { filter = Filter.YOUR_TURN }
        }
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(shown, key = { it.id }) { conversation -> ChatCard(conversation, photoUrl) { onOpen(conversation.id) } }
        }
    }
    }
}

/** Filter-Chip: aktiv dunkel gefüllt, sonst als helle Karte. */
@Composable
private fun FilterPill(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (selected) MaterialTheme.colorScheme.inverseSurface else MaterialTheme.colorScheme.surfaceContainerLowest,
        contentColor = if (selected) MaterialTheme.colorScheme.inverseOnSurface else MaterialTheme.colorScheme.onSurface,
        shadowElevation = if (selected) 0.dp else 1.dp,
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
    }
}

/** Ein Chat als Karte: großes Foto, Name mit Flagge, Status; neue Fotos mit gelbem Punkt. */
@Composable
private fun ChatCard(conversation: Conversation, photoUrl: (String) -> String, onClick: () -> Unit) {
    val unread = conversation.unread
    AppCard(onClick = onClick) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = photoUrl(conversation.lastPhotoId),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(64.dp).clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
            )
            Column(Modifier.weight(1f).padding(start = 14.dp)) {
                StrangerId(conversation.label, maxLines = 1)
                Text(
                    listOf(conversation.displayName(stringResource(R.string.stranger)), Countries.flag(conversation.country)).filter { it.isNotEmpty() }.joinToString(" "),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                )
                Text(
                    when {
                        unread -> stringResource(R.string.new_photo_for_you)
                        conversation.ended -> stringResource(R.string.chat_ended)
                        conversation.lastFromMe -> stringResource(R.string.waiting_their_photo)
                        else -> stringResource(R.string.your_turn)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (unread) FontWeight.Bold else FontWeight.Normal,
                    color = if (unread) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(Modifier.padding(start = 8.dp, end = 4.dp), horizontalAlignment = Alignment.End) {
                Text(
                    DateUtils.getRelativeTimeSpanString(conversation.lastMessageAt).toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (unread) Box(Modifier.padding(top = 8.dp).size(10.dp).background(MaterialTheme.colorScheme.tertiary, CircleShape))
            }
        }
    }
}
