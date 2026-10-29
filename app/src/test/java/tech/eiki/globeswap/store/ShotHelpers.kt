package tech.eiki.globeswap.store

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import tech.eiki.globeswap.data.AchievementData
import tech.eiki.globeswap.data.Conversation
import tech.eiki.globeswap.data.World
import tech.eiki.globeswap.data.WorldCountry
import tech.eiki.globeswap.ui.FloatingNavBar
import tech.eiki.globeswap.ui.LocalBottomBarSpace
import tech.eiki.globeswap.ui.Tab

/**
 * Wie in der App: Leiste unten schwebt über dem Inhalt. Für Screenshots in Tests. Robolectric kann keine Unschärfe
 * zeichnen, darum hier die fast deckende Glasfläche (wie auf Handys vor Android 12) statt durchscheinendem Text.
 */
@Composable
fun ScreenWithBar(tab: Tab, unread: Int = 2, content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        CompositionLocalProvider(LocalBottomBarSpace provides 98.dp) { content() }
        FloatingNavBar(tab, unread = unread, onSelect = {}, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

/** Beispiel-Chats mit gezeichneten Fotos ("scene0" … "scene4", siehe [Scenes]). */
fun sampleConversations(now: Long = System.currentTimeMillis()): List<Conversation> {
    val ids = listOf(
        "3f2a1c9e-7b4d-4e0a-9c1f-2d8e6b5a4c3f", "b81e0d47-52c6-4f39-a8d2-91c7e3f04b5a", "0c9d5e6f-1a2b-4c3d-8e4f-5a6b7c8d9e0f",
        "7d41a9c2-3e8f-4b6a-9d0c-1f2e3a4b5c6d", "e5f60718-293a-4b4c-8d5e-6f708192a3b4",
    )
    val countries = listOf("JP", "BR", "PT", "CA", "NZ")
    return ids.mapIndexed { i, id ->
        Conversation(
            "c$i", id, now - listOf(2, 14, 55, 190, 900)[i] * 60_000L, i % 2 == 1, "scene$i",
            unread = i < 2, blocked = false, country = countries[i], ended = i == 4,
        )
    }
}

/** Beispiel-Weltkarte: 11 Länder auf 6 Kontinenten. */
fun sampleWorld() = World(
    listOf(
        WorldCountry("JP", "AS", 12), WorldCountry("BR", "SA", 5), WorldCountry("PT", "EU", 4), WorldCountry("CA", "NA", 3),
        WorldCountry("NZ", "OC", 2), WorldCountry("KE", "AF", 2), WorldCountry("IN", "AS", 3), WorldCountry("US", "NA", 6),
        WorldCountry("AU", "OC", 1), WorldCountry("MX", "NA", 2), WorldCountry("NO", "EU", 1),
    ),
    continents = 6,
    percentile = 85,
    achievements = AchievementData(home = "DE"),
)
