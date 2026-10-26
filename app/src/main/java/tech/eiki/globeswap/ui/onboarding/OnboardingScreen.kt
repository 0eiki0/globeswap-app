package tech.eiki.globeswap.ui.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import tech.eiki.globeswap.R
import tech.eiki.globeswap.data.ServiceLocator
import tech.eiki.globeswap.ui.LegalLinks
import tech.eiki.globeswap.ui.settings.BackupCodeDialog
import tech.eiki.globeswap.ui.settings.RestoreDialog

@Composable
fun OnboardingScreen(
    onAccept: () -> Unit,
    restore: suspend (String) -> String = { ServiceLocator.repository.restore(it) },
) {
    var accepted by rememberSaveable { mutableStateOf(false) }
    var restoring by rememberSaveable { mutableStateOf(false) }
    var restoredCode by rememberSaveable { mutableStateOf<String?>(null) }
    // Benachrichtigungen für neue Fotos (Android 13+); die Antwort ist egal, es geht in jedem Fall weiter.
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { onAccept() }
    val accept = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            onAccept()
        }
    }
    if (restoring) {
        RestoreDialog(restore, onRestored = { restoring = false; restoredCode = it }, onDismiss = { restoring = false })
    }
    restoredCode?.let { code ->
        // Willkommen zurück: der alte Code gilt nicht mehr, also gleich den neuen sichern lassen.
        BackupCodeDialog(code, stringResource(R.string.welcome_back_code), onDismiss = { restoredCode = null; accept() })
    }
    Surface(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Spacer(Modifier.height(24.dp))
            Text("GlobeSwap", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.onb_intro))
            Step(Icons.Default.CameraAlt, stringResource(R.string.onb_step1))
            Step(Icons.Default.SwapHoriz, stringResource(R.string.onb_step2))
            Step(Icons.Default.Forum, stringResource(R.string.onb_step3))
            Text(
                stringResource(R.string.onb_rules),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                Modifier.fillMaxWidth().clickable { accepted = !accepted },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(checked = accepted, onCheckedChange = null)
                Text(stringResource(R.string.onb_follow_rules), Modifier.padding(start = 12.dp))
            }
            Text(
                buildAnnotatedString {
                    append(stringResource(R.string.onb_agree_1))
                    withLink(LinkAnnotation.Url(LegalLinks.terms, linkStyle())) { append(stringResource(R.string.terms)) }
                    append(stringResource(R.string.onb_agree_2))
                    withLink(LinkAnnotation.Url(LegalLinks.privacy, linkStyle())) { append(stringResource(R.string.privacy_policy)) }
                    append(stringResource(R.string.onb_agree_3))
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = accept, enabled = accepted, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.lets_go))
            }
            TextButton(onClick = { restoring = true }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.onb_restore))
            }
        }
    }
}

@Composable
private fun linkStyle() = TextLinkStyles(
    SpanStyle(color = MaterialTheme.colorScheme.primary, textDecoration = TextDecoration.Underline),
)

@Composable
private fun Step(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
        Text(text, Modifier.padding(start = 16.dp))
    }
}
