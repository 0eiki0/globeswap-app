package tech.eiki.globeswap.ui.world

import tech.eiki.globeswap.data.World

/** Ein Abzeichen auf der Weltkarte – erreicht (mit Datum, falls bekannt) oder mit Fortschritt. */
data class Achievement(
    val id: String,
    val emoji: String,
    val title: String,
    val description: String,
    val unlocked: Boolean,
    /** Wann erreicht (Millisekunden), wenn bekannt. */
    val unlockedAt: Long? = null,
    val progress: Int = 0,
    val goal: Int = 1,
)

private val COUNTRY_MILESTONES = listOf(
    Triple(1, "🌍", "First stranger" to "Your first photo from another country"),
    Triple(5, "🧭", "Explorer" to "Photos from 5 countries"),
    Triple(10, "✈️", "Globetrotter" to "Photos from 10 countries"),
    Triple(25, "🗺️", "World traveler" to "Photos from 25 countries"),
    Triple(50, "🌐", "Citizen of the world" to "Photos from 50 countries"),
)

/** Die sechs bewohnten Kontinente (ohne Antarktis). */
private val CONTINENTS = listOf(
    Triple("EU", "🏰", "Europe"),
    Triple("AS", "🏯", "Asia"),
    Triple("AF", "🦁", "Africa"),
    Triple("NA", "🗽", "North America"),
    Triple("SA", "🦜", "South America"),
    Triple("OC", "🦘", "Oceania"),
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
            progress = countries.size.coerceAtMost(n), goal = n,
        )
    }

    val continentFirst = countries.filter { it.continent.isNotEmpty() }
        .groupBy { it.continent }.mapValues { (_, cs) -> cs.minOf { it.firstAt } }
    CONTINENTS.forEach { (code, emoji, name) ->
        list += Achievement(
            "continent-$code", emoji, "Hello $name", "Your first photo from $name",
            unlocked = code in continentFirst, unlockedAt = continentFirst[code],
        )
    }
    val reached = CONTINENTS.count { it.first in continentFirst }
    list += Achievement(
        "all-continents", "🏆", "All continents", "Photos from every inhabited continent",
        unlocked = reached == CONTINENTS.size,
        unlockedAt = if (reached == CONTINENTS.size) CONTINENTS.maxOf { continentFirst.getValue(it.first) } else null,
        progress = reached, goal = CONTINENTS.size,
    )

    val photos = countries.sumOf { it.photos }
    PHOTO_MILESTONES.forEach { (n, emoji) ->
        list += Achievement(
            "photos-$n", emoji, "$n photos", "$n photos from strangers abroad",
            unlocked = photos >= n, progress = photos.coerceAtMost(n), goal = n,
        )
    }
    return list
}
