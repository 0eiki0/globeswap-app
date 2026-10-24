package tech.eiki.globeswap.ui.onboarding

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
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Block
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.Month
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

const val MIN_AGE = 16

/**
 * Alt genug? Nur Monat und Jahr sind bekannt, deshalb zählt der Geburtsmonat selbst noch nicht:
 * wer im Juni 2010 geboren ist, ist ab Juli 2026 dabei.
 */
fun isOldEnough(birth: YearMonth, now: YearMonth = YearMonth.now(), minAge: Int = MIN_AGE): Boolean {
    val months = (now.year - birth.year) * 12 + (now.monthValue - birth.monthValue)
    return months > minAge * 12
}

/**
 * Neutrale Altersabfrage vor dem Start (Monat und Jahr, ohne Vorauswahl). Gespeichert wird nur das
 * Ergebnis auf dem Gerät, nicht das Geburtsdatum.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgeGateScreen(onResult: (oldEnough: Boolean) -> Unit) {
    var month by rememberSaveable { mutableStateOf<Int?>(null) }
    var year by rememberSaveable { mutableStateOf<Int?>(null) }
    val thisYear = YearMonth.now().year
    Surface(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(Icons.Default.Cake, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(56.dp))
            Spacer(Modifier.height(16.dp))
            Text("When were you born?", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                "We only use this to check your age. It is not stored or sent anywhere.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Picker(
                    label = "Month",
                    value = month?.let { Month.of(it).getDisplayName(TextStyle.FULL, Locale.ENGLISH) },
                    options = (1..12).map { it to Month.of(it).getDisplayName(TextStyle.FULL, Locale.ENGLISH) },
                    onPick = { month = it },
                    modifier = Modifier.weight(1.3f),
                )
                Picker(
                    label = "Year",
                    value = year?.toString(),
                    options = (thisYear downTo thisYear - 100).map { it to it.toString() },
                    onPick = { year = it },
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { onResult(isOldEnough(YearMonth.of(year!!, month!!))) },
                enabled = month != null && year != null,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Continue") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Picker(label: String, value: String?, options: List<Pair<Int, String>>, onPick: (Int) -> Unit, modifier: Modifier) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(
            value = value.orEmpty(),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            singleLine = true,
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (key, text) ->
                DropdownMenuItem(text = { Text(text) }, onClick = { onPick(key); expanded = false })
            }
        }
    }
}

/** Wer zu jung ist, bleibt hier (bis die App-Daten gelöscht werden). */
@Composable
fun TooYoungScreen() {
    Surface(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().safeDrawingPadding().padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(Icons.Default.Block, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(56.dp))
            Spacer(Modifier.height(16.dp))
            Text("Sorry!", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                "GlobeSwap is only for people aged $MIN_AGE and over, because you swap photos with strangers.",
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}
