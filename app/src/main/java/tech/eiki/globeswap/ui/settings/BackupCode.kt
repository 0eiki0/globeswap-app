package tech.eiki.globeswap.ui.settings

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.PersistableBundle
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import tech.eiki.globeswap.data.userMessage

/**
 * Zeigt den Wiederherstellungs-Code. Screenshots sind in der App gesperrt, deshalb Kopieren und Teilen
 * (z. B. in einen Passwort-Manager).
 */
@Composable
fun BackupCodeDialog(code: String, title: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = {},
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    code,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                        .padding(vertical = 14.dp),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(onClick = { copyCode(context, code) }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null)
                        Text("Copy", Modifier.padding(start = 6.dp))
                    }
                    FilledTonalButton(onClick = { shareCode(context, code) }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Share, contentDescription = null)
                        Text("Share", Modifier.padding(start = 6.dp))
                    }
                }
                Text(
                    "Keep this code somewhere safe, like a password manager. With it you get your chats back after " +
                        "reinstalling GlobeSwap – and so would anyone else who has it. We can't show it to you again.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("I saved it") } },
    )
}

private fun copyCode(context: Context, code: String) {
    val clip = ClipData.newPlainText("GlobeSwap backup code", code).apply {
        // Android 13+: Code nicht in der Kopier-Vorschau anzeigen.
        description.extras = PersistableBundle().apply { putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true) }
    }
    context.getSystemService(ClipboardManager::class.java).setPrimaryClip(clip)
    Toast.makeText(context, "Backup code copied", Toast.LENGTH_SHORT).show()
}

private fun shareCode(context: Context, code: String) {
    val intent = Intent(Intent.ACTION_SEND).setType("text/plain")
        .putExtra(Intent.EXTRA_TEXT, "GlobeSwap backup code: $code")
    context.startActivity(Intent.createChooser(intent, "Save backup code"))
}

/** Konto mit dem Code zurückholen (beim ersten Start nach einer Neuinstallation). */
@Composable
fun RestoreDialog(restore: suspend (String) -> String, onRestored: (newCode: String) -> Unit, onDismiss: () -> Unit) {
    var busy by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text("Restore your account") },
        text = { RestoreForm(restore, onRestored, onBusy = { busy = it }) },
        confirmButton = { TextButton(onClick = onDismiss, enabled = !busy) { Text("Cancel") } },
    )
}

/** Eingabe des Codes mit "Restore"-Knopf; eigenständig, damit sie sich ohne Dialog testen lässt. */
@Composable
fun RestoreForm(restore: suspend (String) -> String, onRestored: (newCode: String) -> Unit, onBusy: (Boolean) -> Unit = {}) {
    var code by rememberSaveable { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Enter the backup code you saved. Your chats, ID and world map come back.")
        OutlinedTextField(
            value = code,
            onValueChange = { code = it.uppercase().take(24); error = null },
            label = { Text("Backup code") },
            placeholder = { Text("XXXX-XXXX-XXXX-XXXX") },
            singleLine = true,
            isError = error != null,
            supportingText = error?.let { { Text(it) } },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, autoCorrectEnabled = false),
            textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            enabled = !busy && code.count { it.isLetterOrDigit() } == 16,
            onClick = {
                busy = true
                onBusy(true)
                scope.launch {
                    try {
                        onRestored(restore(code))
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        error = e.userMessage()
                    } finally {
                        busy = false
                        onBusy(false)
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (busy) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Text("Restore")
        }
    }
}
