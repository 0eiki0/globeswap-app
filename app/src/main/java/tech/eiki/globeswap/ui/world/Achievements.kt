package tech.eiki.globeswap.ui.world

import androidx.annotation.StringRes
import tech.eiki.globeswap.R
import tech.eiki.globeswap.data.World

/**
 * Ein Abzeichen auf der Weltkarte – erreicht (mit Datum, falls bekannt) oder mit Fortschritt.
 * Titel und Beschreibung sind Texte aus den Sprachdateien; eingesetzt wird [number] oder der Kontinent [place].
 */
data class Achievement(
    val id: String,
    val emoji: String,
    @StringRes val title: Int,
    @StringRes val description: Int,
    val unlocked: Boolean,
    /** Wann erreicht (Millisekunden), wenn bekannt. */
    val unlockedAt: Long? = null,
    val progress: Int = 0,
    val goal: Int = 1,
    val number: Int? = null,
    @StringRes val place: Int? = null,
)

private val COUNTRY_MILESTONES = listOf(
    Triple(1, "🌍", R.string.ach_c1 to R.string.ach_c1_d),
    Triple(5, "🧭", R.string.ach_c5 to R.string.ach_cn_d),
    Triple(10, "✈️", R.string.ach_c10 to R.string.ach_cn_d),
    Triple(25, "🗺️", R.string.ach_c25 to R.string.ach_cn_d),
    Triple(50, "🌐", R.string.ach_c50 to R.string.ach_cn_d),
)

/** Die sechs bewohnten Kontinente (ohne Antarktis). */
private val CONTINENTS = listOf(
    Triple("EU", "🏰", R.string.cont_eu),
    Triple("AS", "🏯", R.string.cont_as),
    Triple("AF", "🦁", R.string.cont_af),
    Triple("NA", "🗽", R.string.cont_na),
    Triple("SA", "🦜", R.string.cont_sa),
    Triple("OC", "🦘", R.string.cont_oc),
)

private val PHOTO_MILESTONES = listOf(10 to "📸", 100 to "🎞️")

/** Alle Abzeichen in fester Reihenfolge: Länder, Kontinente, Fotos. */
fun achievements(world: World): List<Achievement> {
    val countries = world.countries
    val firstSeen = countries.map { it.firstAt }.sorted()
    val list = mutableListOf<Achievement>()

    COUNTRY_MILESTONES.forEach { (n, emoji, texts) ->
        list += Achievement(
            "countries-$n", emoji, texts.first, texts.second,
            unlocked = countries.size >= n, unlockedAt = firstSeen.getOrNull(n - 1),
            progress = countries.size.coerceAtMost(n), goal = n, number = n,
        )
    }

    val continentFirst = countries.filter { it.continent.isNotEmpty() }
        .groupBy { it.continent }.mapValues { (_, cs) -> cs.minOf { it.firstAt } }
    CONTINENTS.forEach { (code, emoji, name) ->
        list += Achievement(
            "continent-$code", emoji, R.string.ach_hello, R.string.ach_hello_d,
            unlocked = code in continentFirst, unlockedAt = continentFirst[code], place = name,
        )
    }
    val reached = CONTINENTS.count { it.first in continentFirst }
    list += Achievement(
        "all-continents", "🏆", R.string.ach_all, R.string.ach_all_d,
        unlocked = reached == CONTINENTS.size,
        unlockedAt = if (reached == CONTINENTS.size) CONTINENTS.maxOf { continentFirst.getValue(it.first) } else null,
        progress = reached, goal = CONTINENTS.size,
    )

    val photos = countries.sumOf { it.photos }
    PHOTO_MILESTONES.forEach { (n, emoji) ->
        list += Achievement(
            "photos-$n", emoji, R.string.ach_photos, R.string.ach_photos_d,
            unlocked = photos >= n, progress = photos.coerceAtMost(n), goal = n, number = n,
        )
    }
    return list
}
