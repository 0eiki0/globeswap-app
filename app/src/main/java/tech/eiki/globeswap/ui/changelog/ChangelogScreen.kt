package tech.eiki.globeswap.ui.changelog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import tech.eiki.globeswap.BuildConfig
import tech.eiki.globeswap.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangelogScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val entries = remember { Changelog.load(context, BuildConfig.VERSION_NAME) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.whats_new)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back)) }
                },
            )
        },
    ) { padding ->
        ChangelogList(entries, Modifier.fillMaxSize().padding(padding))
    }
}

@Composable
internal fun ChangelogList(entries: List<ChangelogEntry>, modifier: Modifier = Modifier) {
    LazyColumn(modifier, contentPadding = PaddingValues(vertical = 8.dp)) {
        items(entries, key = { it.version }) { entry ->
            Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        if (entry.version == Changelog.UPCOMING) entry.version else stringResource(R.string.version_n, entry.version),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    if (entry.current) {
                        Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.small) {
                            Text(stringResource(R.string.installed), style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                }
                if (entry.dateLabel.isNotEmpty()) {
                    Text(entry.dateLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Changes(entry.changes, Modifier.padding(top = 8.dp))
            }
            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
        }
    }
}

@Composable
private fun Changes(changes: List<String>, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        changes.forEach { change ->
            Row {
                Text("•", Modifier.padding(end = 8.dp), color = MaterialTheme.colorScheme.primary)
                Text(change, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

/** Einmal nach einem Update: was ist neu in der installierten Version. */
@Composable
fun WhatsNewDialog(entry: ChangelogEntry, onDismiss: () -> Unit, onSeeAll: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.whats_new_in, entry.version)) },
        text = { Changes(entry.changes) },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.nice)) } },
        dismissButton = { TextButton(onClick = onSeeAll) { Text(stringResource(R.string.all_changes)) } },
    )
}
