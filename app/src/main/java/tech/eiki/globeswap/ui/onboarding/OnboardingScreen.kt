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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import tech.eiki.globeswap.ui.LegalLinks

@Composable
fun OnboardingScreen(onAccept: () -> Unit) {
    var accepted by rememberSaveable { mutableStateOf(false) }
    // Benachrichtigungen für neue Fotos (Android 13+); die Antwort ist egal, es geht in jedem Fall weiter.
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { onAccept() }
    val accept = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            onAccept()
        }
    }
    Surface(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Spacer(Modifier.height(24.dp))
            Text("GlobeSwap", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
            Text("Swap photos with strangers all over the world. No sign-up needed.")
            Step(Icons.Default.CameraAlt, "Take a photo of whatever you see right now.")
            Step(Icons.Default.SwapHoriz, "It goes to someone somewhere in the world who took a photo at the same time – and you get theirs.")
            Step(Icons.Default.Forum, "Reply with a photo and start a conversation made only of pictures.")
            Text(
                "Rules: no nudity, no violence, no harassment, no personal information. " +
                    "You can report inappropriate photos and block people at any time.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                Modifier.fillMaxWidth().clickable { accepted = !accepted },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(checked = accepted, onCheckedChange = null)
                Text("I will follow the rules.", Modifier.padding(start = 12.dp))
            }
            Text(
                buildAnnotatedString {
                    append("By continuing you agree to the ")
                    withLink(LinkAnnotation.Url(LegalLinks.terms, linkStyle())) { append("Terms of Use") }
                    append(" and acknowledge the ")
                    withLink(LinkAnnotation.Url(LegalLinks.privacy, linkStyle())) { append("Privacy Policy") }
                    append(".")
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = accept, enabled = accepted, modifier = Modifier.fillMaxWidth()) {
                Text("Let's go")
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
