package tech.eiki.globeswap.ui.swap

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import tech.eiki.globeswap.R
import tech.eiki.globeswap.data.ServiceLocator
import tech.eiki.globeswap.ui.camera.PhotoCapture

@Composable
fun SwapScreen(onMatched: (String) -> Unit, vm: SwapViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val theme by ServiceLocator.updates.theme.collectAsStateWithLifecycle()

    LaunchedEffect(state) {
        val matched = state as? SwapUiState.Matched ?: return@LaunchedEffect
        vm.reset()
        onMatched(matched.conversationId)
    }

    when (val s = state) {
        SwapUiState.Capturing -> PhotoCapture(
            confirmLabel = stringResource(R.string.swap_confirm),
            onConfirm = { jpeg, info -> vm.swap(jpeg, info) },
            hint = stringResource(R.string.swap_hint),
            theme = theme,
        )
        SwapUiState.Sending -> StatusPanel(title = stringResource(R.string.swap_sending), loading = true)
        is SwapUiState.Waiting -> StatusPanel(
            title = stringResource(R.string.swap_waiting),
            text = stringResource(R.string.swap_waiting_text),
            loading = true,
            secondary = stringResource(R.string.take_back) to vm::cancelWaiting,
        )
        is SwapUiState.Failed -> StatusPanel(
            title = stringResource(R.string.swap_failed),
            text = s.message,
            primary = stringResource(R.string.try_again) to { vm.swap(s.jpeg, s.info) },
            secondary = stringResource(R.string.new_photo) to vm::reset,
        )
        is SwapUiState.Matched -> StatusPanel(title = stringResource(R.string.its_a_match), loading = true)
    }
}

@Composable
private fun StatusPanel(
    title: String,
    text: String? = null,
    loading: Boolean = false,
    primary: Pair<String, () -> Unit>? = null,
    secondary: Pair<String, () -> Unit>? = null,
) {
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface).padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Default.Public, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        if (text != null) {
            Spacer(Modifier.height(12.dp))
            Text(text, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        }
        if (loading) {
            Spacer(Modifier.height(24.dp))
            CircularProgressIndicator()
        }
        Spacer(Modifier.height(24.dp))
        primary?.let { (label, action) -> Button(onClick = action) { Text(label) } }
        secondary?.let { (label, action) -> OutlinedButton(onClick = action) { Text(label) } }
    }
}
