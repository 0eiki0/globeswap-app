package tech.eiki.globeswap.ui.world

import androidx.annotation.StringRes
import tech.eiki.globeswap.R
import tech.eiki.globeswap.data.Countries
import tech.eiki.globeswap.data.World

/**
 * Ein Abzeichen auf der Weltkarte – erreicht (mit Datum, falls bekannt) oder mit Fortschritt.
 * Titel und Beschreibung sind Texte aus den Sprachdateien; eingesetzt wird [place] (Text aus den Sprachdateien),
 * [text] (z. B. ein Ländername) oder [number].
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
    val text: String? = null,
) {
    val category: AchievementCategory get() = categoryOf(id)
}

/** Kategorien der Erfolgsseite, in dieser Reihenfolge. */
enum class AchievementCategory(val emoji: String, @StringRes val title: Int) {
    COUNTRIES("🌍", R.string.cat_countries),
    DISCOVER("🧭", R.string.cat_discover),
    CONTINENTS("🗺️", R.string.cat_continents),
    TIME("⏰", R.string.cat_time),
    STREAKS("🔥", R.string.cat_streaks),
    CHATS("💬", R.string.cat_chats),
    PHOTOS("📸", R.string.cat_photos),
}

internal fun categoryOf(id: String): AchievementCategory = when {
    id.startsWith("countries-") -> AchievementCategory.COUNTRIES
    id in setOf("neighbors", "antipode", "islands", "hemispheres", "antarctica") -> AchievementCategory.DISCOVER
    id.startsWith("continent-") || id == "all-continents" -> AchievementCategory.CONTINENTS
    id in setOf("night", "sunrise", "otherday", "zones") -> AchievementCategory.TIME
    id.startsWith("streak-") || id.startsWith("theme-") || id == "loyal" || id == "anniversary" -> AchievementCategory.STREAKS
    id in setOf("talkative", "penpal", "hearts", "artist", "mix") -> AchievementCategory.CHATS
    else -> AchievementCategory.PHOTOS
}

private val COUNTRY_MILESTONES = listOf(
    Triple(1, "🌍", R.string.ach_c1 to R.string.ach_c1_d),
    Triple(5, "🧭", R.string.ach_c5 to R.string.ach_cn_d),
    Triple(10, "✈️", R.string.ach_c10 to R.string.ach_cn_d),
    Triple(25, "🗺️", R.string.ach_c25 to R.string.ach_cn_d),
    Triple(50, "🌐", R.string.ach_c50 to R.string.ach_cn_d),
    Triple(100, "🌏", R.string.ach_c100 to R.string.ach_cn_d),
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

private val PHOTO_MILESTONES = listOf(10 to "📸", 100 to "🎞️", 500 to "📷", 1000 to "🏅")

/** Inselstaaten (ganz auf Inseln, ohne Landgrenze zu einem anderen Staat). */
internal val ISLAND_STATES = setOf(
    "AG", "BB", "BH", "BS", "CU", "CV", "CY", "DM", "FJ", "FM", "GB", "GD", "IE", "IS", "JM", "JP", "KI", "KM", "KN", "LC",
    "LK", "MG", "MH", "MT", "MU", "MV", "NR", "NZ", "PH", "PW", "SB", "SC", "SG", "ST", "TO", "TT", "TV", "TW", "VC", "VU", "WS",
)

/** Alle Filter des Editors außer „Original“ (für „Filter-Fan“). */
private const val FILTER_COUNT = 7

private const val DAY_MS = 24 * 60 * 60 * 1000L

/** Alle Abzeichen in fester Reihenfolge. [geo] fehlt nur in Tests ohne Karte. */
fun achievements(world: World, geo: CountryGeo? = null, now: Long = System.currentTimeMillis()): List<Achievement> {
    val countries = world.countries
    val codes = countries.map { it.country }.toSet()
    val firstSeen = countries.map { it.firstAt }.sorted()
    val data = world.achievements
    val stat = { key: String -> data.stats[key]?.toInt() ?: 0 }
    val list = mutableListOf<Achievement>()
    fun count(id: String, emoji: String, @StringRes title: Int, @StringRes desc: Int, value: Int, goal: Int, number: Int? = goal) {
        list += Achievement(id, emoji, title, desc, unlocked = value >= goal, progress = value.coerceIn(0, goal), goal = goal, number = number)
    }

    // Länder
    COUNTRY_MILESTONES.forEach { (n, emoji, texts) ->
        list += Achievement(
            "countries-$n", emoji, texts.first, texts.second,
            unlocked = countries.size >= n, unlockedAt = firstSeen.getOrNull(n - 1),
            progress = countries.size.coerceAtMost(n), goal = n, number = n,
        )
    }

    // Entdecken
    val home = data.home
    val neighbors = geo?.neighbors?.get(home).orEmpty()
    if (home.isNotEmpty() && neighbors.isNotEmpty()) {
        list += Achievement(
            "neighbors", "🏘️", R.string.ach_neighbors, R.string.ach_neighbors_d,
            unlocked = neighbors.all { it in codes }, progress = neighbors.count { it in codes }, goal = neighbors.size,
            text = Countries.name(home),
        )
    }
    val farthest = if (geo != null && home.isNotEmpty()) codes.mapNotNull { geo.distanceKm(home, it) }.maxOrNull() ?: 0.0 else 0.0
    list += Achievement("antipode", "🌗", R.string.ach_antipode, R.string.ach_antipode_d, unlocked = farthest >= 15_000)
    count("islands", "🏝️", R.string.ach_islands, R.string.ach_islands_d, codes.count { it in ISLAND_STATES }, 3)
    val centers = codes.mapNotNull { geo?.centers?.get(it) }
    val directions = listOf(centers.any { it.first > 0 }, centers.any { it.first < 0 }, centers.any { it.second > 0 }, centers.any { it.second < 0 })
    count("hemispheres", "✳️", R.string.ach_hemispheres, R.string.ach_hemispheres_d, directions.count { it }, 4, number = null)
    list += Achievement("antarctica", "🐧", R.string.ach_antarctica, R.string.ach_antarctica_d, unlocked = "AQ" in codes)

    // Kontinente
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

    // Zeit und Zeitzonen
    list += Achievement("night", "🦉", R.string.ach_night, R.string.ach_night_d, unlocked = stat("night") > 0)
    list += Achievement("sunrise", "🌅", R.string.ach_sunrise, R.string.ach_sunrise_d, unlocked = stat("sunrise") > 0)
    list += Achievement("otherday", "📆", R.string.ach_otherday, R.string.ach_otherday_d, unlocked = stat("otherday") > 0)
    count("zones", "🕰️", R.string.ach_zones, R.string.ach_zones_d, data.flags.count { it.startsWith("tz:") }, 12)

    // Dranbleiben
    listOf(3, 7, 30).forEach { n -> count("streak-$n", "🔥", R.string.ach_streak, R.string.ach_streak_d, data.bestStreak, n) }
    val days = if (data.createdAt > 0) ((now - data.createdAt) / DAY_MS).toInt() else 0
    count("loyal", "💙", R.string.ach_loyal, R.string.ach_loyal_d, days, 100)
    count("anniversary", "🎂", R.string.ach_anniversary, R.string.ach_anniversary_d, days, 365, number = null)
    count("theme-1", "🎯", R.string.ach_theme1, R.string.ach_theme1_d, data.themeDays, 1)
    count("theme-10", "🎯", R.string.ach_theme10, R.string.ach_theme_d, data.themeDays, 10)
    count("theme-50", "🎯", R.string.ach_theme50, R.string.ach_theme_d, data.themeDays, 50)

    // Chats und Miteinander
    count("talkative", "💬", R.string.ach_talkative, R.string.ach_talkative_d, stat("chat_photos"), 10)
    count("penpal", "✉️", R.string.ach_penpal, R.string.ach_penpal_d, stat("chat_days"), 7, number = null)
    count("hearts", "❤️", R.string.ach_hearts, R.string.ach_hearts_d, stat("reactions"), 10)
    count("artist", "🎨", R.string.ach_artist, R.string.ach_artist_d, stat("edited"), 10)
    count("mix", "🌈", R.string.ach_mix, R.string.ach_mix_d, stat("mix"), 3)

    // Fotografieren
    val photos = countries.sumOf { it.photos }
    PHOTO_MILESTONES.forEach { (n, emoji) -> count("photos-$n", emoji, R.string.ach_photos, R.string.ach_photos_d, photos, n) }
    count("filters", "🖼️", R.string.ach_filters, R.string.ach_filters_d, data.flags.count { it.startsWith("filter:") }, FILTER_COUNT, number = null)
    list += Achievement("wide", "🔭", R.string.ach_wide, R.string.ach_wide_d, unlocked = "wide" in data.flags)
    return list
}
