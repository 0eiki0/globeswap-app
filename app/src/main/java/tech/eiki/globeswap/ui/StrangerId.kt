package tech.eiki.globeswap.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp

/** Eindeutige Kennung (UUID) der anderen Person, klein in Festbreitenschrift. */
@Composable
fun StrangerId(
    id: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    maxLines: Int = Int.MAX_VALUE,
) {
    if (id.isEmpty()) return
    Text(
        id,
        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.sp),
        fontFamily = FontFamily.Monospace,
        color = color,
        maxLines = maxLines,
        softWrap = maxLines > 1,
        modifier = modifier,
    )
}
