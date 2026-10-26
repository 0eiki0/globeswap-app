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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Cake
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import tech.eiki.globeswap.R

const val MIN_AGE = 16

/** Alt genug? Am 16. Geburtstag selbst ist man dabei. */
fun isOldEnough(birth: LocalDate, today: LocalDate = LocalDate.now(), minAge: Int = MIN_AGE): Boolean =
    !birth.plusYears(minAge.toLong()).isAfter(today)

/**
 * Neutrale Altersabfrage vor dem Start (Tag, Monat und Jahr, ohne Vorauswahl). Gespeichert wird nur das
 * Ergebnis auf dem Gerät, nicht das Geburtsdatum. Der Datenschutzerklärung stimmt man danach beim Onboarding zu.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgeGateScreen(onResult: (oldEnough: Boolean) -> Unit) {
    var day by rememberSaveable { mutableStateOf<Int?>(null) }
    var month by rememberSaveable { mutableStateOf<Int?>(null) }
    var year by rememberSaveable { mutableStateOf<Int?>(null) }
    val thisYear = YearMonth.now().year
    // Tage passend zu Monat und Jahr (29. Februar nur im Schaltjahr); ein ungültiger Tag wird verworfen.
    val daysInMonth = month?.let { YearMonth.of(year ?: 2000, it).lengthOfMonth() } ?: 31
    if ((day ?: 0) > daysInMonth) day = null
    Surface(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(Icons.Default.Cake, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.age_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                stringResource(R.string.age_text),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Picker(
                    label = stringResource(R.string.day),
                    value = day?.toString(),
                    options = (1..daysInMonth).map { it to it.toString() },
                    onPick = { day = it },
                    modifier = Modifier.fillMaxWidth(),
                )
                Picker(
                    label = stringResource(R.string.month),
                    value = month?.let { Month.of(it).getDisplayName(TextStyle.FULL, Locale.ENGLISH) },
                    options = (1..12).map { it to Month.of(it).getDisplayName(TextStyle.FULL, Locale.ENGLISH) },
                    onPick = { month = it },
                    modifier = Modifier.fillMaxWidth(),
                )
                Picker(
                    label = stringResource(R.string.year),
                    value = year?.toString(),
                    options = (thisYear downTo thisYear - 100).map { it to it.toString() },
                    onPick = { year = it },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = { onResult(isOldEnough(LocalDate.of(year!!, month!!, day!!))) },
                enabled = day != null && month != null && year != null,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.continue_)) }
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
            Text(stringResource(R.string.sorry), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                stringResource(R.string.too_young, MIN_AGE),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}
