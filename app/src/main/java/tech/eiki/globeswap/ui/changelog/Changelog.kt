package tech.eiki.globeswap.ui.changelog

import android.content.Context
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Eine Version im Changelog; [current] ist die installierte. */
data class ChangelogEntry(val version: String, val date: LocalDate?, val changes: List<String>, val current: Boolean = false) {
    val dateLabel: String get() = date?.format(DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH)).orEmpty()
}

/**
 * Liest assets/changelog.md. "## Next" sind die Änderungen ohne Versionsnummer: In einem Release-Build gehören sie
 * zur installierten Version (der Release-Lauf benennt die Überschrift erst danach um); hat die installierte Version
 * schon eine eigene Überschrift (Entwicklungs-Build), erscheinen sie als "Coming next".
 */
object Changelog {
    private val heading = Regex("""^##\s+(\S+)(?:\s+\((\d{4}-\d{2}-\d{2})\))?\s*$""")

    /** Überschrift für "Next", wenn die installierte Version schon eine eigene hat (Entwicklungs-Builds). */
    const val UPCOMING = "Coming next"

    fun parse(text: String, installedVersion: String): List<ChangelogEntry> {
        val raw = mutableListOf<Triple<String, LocalDate?, MutableList<String>>>()
        text.lineSequence().map { it.trimEnd() }.forEach { line ->
            val match = heading.matchEntire(line)
            when {
                match != null -> raw += Triple(
                    match.groupValues[1],
                    match.groupValues[2].takeIf { it.isNotEmpty() }?.let(LocalDate::parse),
                    mutableListOf(),
                )
                line.startsWith("- ") && raw.isNotEmpty() -> raw.last().third += line.removePrefix("- ").trim()
            }
        }
        val installedNamed = raw.any { it.first == installedVersion }
        return raw.filter { it.third.isNotEmpty() }.map { (version, date, changes) ->
            val isNext = version.equals("Next", ignoreCase = true)
            val shown = when {
                !isNext -> version
                installedNamed -> UPCOMING
                else -> installedVersion
            }
            ChangelogEntry(shown, date, changes, current = shown == installedVersion)
        }
    }

    @Volatile private var cached: List<ChangelogEntry>? = null

    fun load(context: Context, installedVersion: String): List<ChangelogEntry> = cached ?: runCatching {
        parse(context.assets.open("changelog.md").bufferedReader().use { it.readText() }, installedVersion)
    }.getOrDefault(emptyList()).also { cached = it }
}
