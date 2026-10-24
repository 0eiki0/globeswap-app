package tech.eiki.globeswap.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import tech.eiki.globeswap.data.InstallChannel
import tech.eiki.globeswap.data.LatestRelease
import tech.eiki.globeswap.update.SelfUpdateState
import kotlin.math.roundToInt

/** Diese Version ist zu alt – die App ist gesperrt, bis sie aktualisiert ist. */
@Composable
fun ForcedUpdateScreen(
    message: String,
    installedVersion: String,
    onUpdate: () -> Unit,
    onCheckAgain: () -> Unit,
    selfUpdate: SelfUpdateState = SelfUpdateState.Idle,
) {
    Surface(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().safeDrawingPadding().padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(Icons.Default.SystemUpdate, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(72.dp))
            Spacer(Modifier.height(24.dp))
            Text("Update required", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Text(
                message.ifBlank { "This version of GlobeSwap is no longer supported. Please update to continue." },
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(32.dp))
            Button(onClick = onUpdate, enabled = !selfUpdate.busy, modifier = Modifier.fillMaxWidth()) {
                Text(if (selfUpdate is SelfUpdateState.Failed) "Try again" else "Update now")
            }
            SelfUpdateProgress(selfUpdate, Modifier.padding(top = 12.dp))
            TextButton(onClick = onCheckAgain) { Text("Check again") }
            Spacer(Modifier.height(16.dp))
            Text(
                "Installed version: $installedVersion",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Hinweis auf eine neue Version, über der unteren Navigationsleiste. */
@Composable
fun UpdateBanner(
    release: LatestRelease,
    channel: InstallChannel,
    onUpdate: () -> Unit,
    onLater: () -> Unit,
    selfUpdate: SelfUpdateState = SelfUpdateState.Idle,
) {
    Surface(color = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer) {
      Column {
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.SystemUpdate, contentDescription = null)
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text("GlobeSwap ${release.versionName} is available", style = MaterialTheme.typography.titleSmall)
                if (channel == InstallChannel.FDROID) {
                    Text(
                        "It may take a day to show up in your app store.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            TextButton(onClick = onLater, enabled = !selfUpdate.busy) { Text("Later") }
            Button(onClick = onUpdate, enabled = !selfUpdate.busy) {
                Text(if (selfUpdate is SelfUpdateState.Failed) "Retry" else "Update")
            }
        }
        SelfUpdateProgress(selfUpdate, Modifier.padding(start = 16.dp, end = 16.dp, bottom = 10.dp))
      }
    }
}

private val SelfUpdateState.busy get() = this is SelfUpdateState.Downloading || this == SelfUpdateState.Installing

/** Fortschritt des Selbst-Updates (nur in der GitHub-Variante zu sehen). */
@Composable
fun SelfUpdateProgress(state: SelfUpdateState, modifier: Modifier = Modifier) {
    when (state) {
        SelfUpdateState.Idle -> Unit
        is SelfUpdateState.Downloading -> Column(modifier.fillMaxWidth()) {
            LinearProgressIndicator(progress = { state.progress }, modifier = Modifier.fillMaxWidth())
            Text("Downloading update… ${(state.progress * 100).roundToInt()} %", style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp))
        }
        SelfUpdateState.Installing -> Column(modifier.fillMaxWidth()) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Text("Installing… tap “Update” if Android asks.", style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp))
        }
        SelfUpdateState.NeedsPermission -> Text(
            "Allow GlobeSwap to install updates in the settings that just opened, then come back here.",
            style = MaterialTheme.typography.bodySmall,
            modifier = modifier,
        )
        is SelfUpdateState.Failed -> Text(
            state.message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            modifier = modifier,
        )
    }
}
