package tech.eiki.globeswap.ui.settings

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import tech.eiki.globeswap.data.Countries
import tech.eiki.globeswap.data.MatchPref
import tech.eiki.globeswap.data.ServiceLocator
import tech.eiki.globeswap.data.UserSettings
import tech.eiki.globeswap.data.userMessage

class SettingsViewModel : ViewModel() {
    private val repo = ServiceLocator.repository
    private val _settings = MutableStateFlow<UserSettings?>(null)
    val settings: StateFlow<UserSettings?> = _settings.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        load()
    }

    fun load() = update { repo.settings() }

    fun setShareCountry(share: Boolean) = update { repo.updateSettings(shareCountry = share) }

    fun setMatchPref(pref: String) = update { repo.updateSettings(matchPref = pref) }

    /** Name setzen oder mit "" löschen; Fehler (z. B. nicht erlaubter Name) kommen in [nameError]. */
    fun setName(name: String) {
        viewModelScope.launch {
            runCatching { repo.updateSettings(displayName = name) }
                .onSuccess { _settings.value = it; _nameError.value = null }
                .onFailure { _nameError.value = it.userMessage() }
        }
    }

    private val _nameError = MutableStateFlow<String?>(null)
    val nameError: StateFlow<String?> = _nameError.asStateFlow()

    private val _backupCode = MutableStateFlow<String?>(null)
    /** Gerade erstellter Code zum Anzeigen (nur einmal, der Server kennt nur seinen Hash). */
    val backupCode: StateFlow<String?> = _backupCode.asStateFlow()

    fun createBackupCode() {
        viewModelScope.launch {
            runCatching { repo.createRecoveryCode() }
                .onSuccess { _backupCode.value = it; _settings.value = _settings.value?.copy(hasRecoveryCode = true) }
                .onFailure { Log.w("SettingsViewModel", "Code", it); _error.value = it.userMessage() }
        }
    }

    fun backupCodeShown() {
        _backupCode.value = null
    }

    private fun update(call: suspend () -> UserSettings) {
        viewModelScope.launch {
            runCatching { call() }
                .onSuccess { _settings.value = it; _error.value = null }
                .onFailure { Log.w("SettingsViewModel", "Einstellungen", it); _error.value = it.userMessage() }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, onOpenWorld: () -> Unit, vm: SettingsViewModel = viewModel()) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    val backupCode by vm.backupCode.collectAsStateWithLifecycle()
    val nameError by vm.nameError.collectAsStateWithLifecycle()
    backupCode?.let { BackupCodeDialog(it, "Your backup code", onDismiss = vm::backupCodeShown) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
            )
        },
    ) { padding ->
        val s = settings
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
            when {
                s != null -> SettingsContent(s, vm::setShareCountry, vm::setMatchPref, onOpenWorld, vm::createBackupCode, vm::setName, nameError)
                error != null -> Column(Modifier.padding(24.dp)) {
                    Text("Could not load your settings: $error")
                    TextButton(onClick = vm::load) { Text("Try again") }
                }
                else -> Row(Modifier.fillMaxWidth().padding(48.dp)) { CircularProgressIndicator() }
            }
        }
    }
}

@Composable
internal fun SettingsContent(
    settings: UserSettings,
    onShareCountry: (Boolean) -> Unit,
    onMatchPref: (String) -> Unit,
    onOpenWorld: () -> Unit,
    onCreateBackupCode: () -> Unit = {},
    onSetName: (String) -> Unit = {},
    nameError: String? = null,
) {
    var confirmNewCode by remember { mutableStateOf(false) }
    if (confirmNewCode) {
        AlertDialog(
            onDismissRequest = { confirmNewCode = false },
            title = { Text("Create a new backup code?") },
            text = { Text("Your old backup code will stop working.") },
            confirmButton = { TextButton(onClick = { confirmNewCode = false; onCreateBackupCode() }) { Text("Create") } },
            dismissButton = { TextButton(onClick = { confirmNewCode = false }) { Text("Cancel") } },
        )
    }
    SectionTitle("Your name")
    NameEditor(settings.displayName, onSetName, nameError)
    HorizontalDivider(Modifier.padding(top = 8.dp))

    SectionTitle("Your country")
    Row(
        Modifier.fillMaxWidth().clickable(enabled = settings.geoEnabled) { onShareCountry(!settings.shareCountry) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = 16.dp)) {
            Text("Show my country", style = MaterialTheme.typography.bodyLarge)
            Text(
                when {
                    !settings.geoEnabled -> "Not available on this server."
                    settings.shareCountry && settings.country.isNotEmpty() ->
                        "Strangers see “${Countries.label(settings.country)}” with your photos. Only the country, never your location."
                    settings.shareCountry -> "Strangers see your country with your photos. Only the country, never your location."
                    else -> "Your photos are sent without a country. It was also removed from photos you already sent."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = settings.shareCountry && settings.geoEnabled, onCheckedChange = null, enabled = settings.geoEnabled)
    }
    HorizontalDivider()

    SectionTitle("Swap with")
    val canPrefer = settings.geoEnabled && settings.shareCountry
    listOf(
        MatchPref.ANY to ("Anyone" to "The next photo, wherever it comes from."),
        MatchPref.NEAR to ("Nearby" to "Prefer people on your continent."),
        MatchPref.FAR to ("Far away" to "Prefer people on other continents."),
    ).forEach { (value, texts) ->
        val enabled = canPrefer || value == MatchPref.ANY
        Row(
            Modifier.fillMaxWidth()
                .selectable(selected = settings.matchPref == value, enabled = enabled, onClick = { onMatchPref(value) })
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(selected = settings.matchPref == value, onClick = null, enabled = enabled)
            Column(Modifier.padding(start = 12.dp)) {
                Text(texts.first, style = MaterialTheme.typography.bodyLarge)
                Text(texts.second, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
    Text(
        if (canPrefer) {
            "This is a preference: if nobody fitting is waiting, you get the next photo anyway, so you never wait longer."
        } else {
            "Turn on “Show my country” to choose nearby or far away."
        },
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
    )
    HorizontalDivider(Modifier.padding(top = 12.dp))

    SectionTitle("Backup code")
    Text(
        if (settings.hasRecoveryCode) {
            "You have a backup code. With it you get your chats back after reinstalling the app. Lost it? Create a new one."
        } else {
            "Reinstalling the app or getting a new phone? With a backup code you get your chats, ID and world map back."
        },
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp),
    )
    TextButton(
        onClick = { if (settings.hasRecoveryCode) confirmNewCode = true else onCreateBackupCode() },
        modifier = Modifier.padding(horizontal = 8.dp),
    ) { Text(if (settings.hasRecoveryCode) "Create new backup code" else "Create backup code") }
    HorizontalDivider(Modifier.padding(top = 4.dp))
    TextButton(onClick = onOpenWorld, modifier = Modifier.padding(8.dp)) { Text("Open your world map") }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 4.dp),
    )
}

/** Freiwilliger Name: Chat-Partner sehen ihn statt "Stranger". */
@Composable
internal fun NameEditor(current: String, onSetName: (String) -> Unit, error: String?) {
    var text by rememberSaveable(current) { mutableStateOf(current) }
    Column(Modifier.padding(horizontal = 16.dp)) {
        Text(
            "Optional. People you chat with see it instead of “Stranger” – your ID stays the same. " +
                "Better not use your full real name.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = text,
            onValueChange = { text = it.take(24) },
            label = { Text("Name") },
            placeholder = { Text("Stranger") },
            singleLine = true,
            isError = error != null,
            supportingText = { Text(error ?: "${text.length}/24") },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { onSetName(text.trim()) }, enabled = text.trim().isNotEmpty() && text.trim() != current) { Text("Save") }
            if (current.isNotEmpty()) {
                TextButton(onClick = { text = ""; onSetName("") }) { Text("Remove name") }
            }
        }
    }
}
