package tech.eiki.globeswap.ui.conversations

import android.text.format.DateUtils
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import tech.eiki.globeswap.ui.StrangerId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationsScreen(
    conversations: List<Conversation>,
    photoUrl: (String) -> String,
    onOpen: (String) -> Unit,
    onDeleteAccount: () -> Unit,
    publicId: String? = null,
    onAboutOpened: () -> Unit = {},
    onOpenWorld: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenChangelog: () -> Unit = {},
) {
    var showAbout by remember { mutableStateOf(false) }
    if (showAbout) AboutDialog(publicId, onDismiss = { showAbout = false })
    var menuOpen by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.chats)) },
                actions = {
                    IconButton(onClick = onOpenWorld) { Icon(Icons.Default.Public, contentDescription = stringResource(R.string.your_world_map)) }
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
                },
            )
        },
    ) { padding ->
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
        if (conversations.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding).padding(32.dp), contentAlignment = Alignment.Center) {
                Text(
                    stringResource(R.string.no_swaps_yet),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            return@Scaffold
        }
        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            items(conversations, key = { it.id }) { conversation ->
                val unread = conversation.unread
                ListItem(
                    modifier = Modifier.clickable { onOpen(conversation.id) },
                    leadingContent = {
                        AsyncImage(
                            model = photoUrl(conversation.lastPhotoId),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(64.dp).clip(RoundedCornerShape(12.dp)),
                        )
                    },
                    headlineContent = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                listOf(conversation.displayName(stringResource(R.string.stranger)), Countries.flag(conversation.country)).filter { it.isNotEmpty() }.joinToString(" "),
                                fontWeight = if (unread) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                DateUtils.getRelativeTimeSpanString(conversation.lastMessageAt).toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                    overlineContent = { StrangerId(conversation.label, maxLines = 1) },
                    supportingContent = {
                        Text(
                            when {
                                unread -> stringResource(R.string.new_photo_for_you)
                                conversation.ended -> stringResource(R.string.chat_ended)
                                conversation.lastFromMe -> stringResource(R.string.waiting_their_photo)
                                else -> stringResource(R.string.your_turn)
                            },
                            color = if (unread) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                )
                HorizontalDivider()
            }
        }
    }
}
