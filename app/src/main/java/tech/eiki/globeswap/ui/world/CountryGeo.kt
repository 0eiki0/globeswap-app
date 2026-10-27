package tech.eiki.globeswap.ui.world

import android.content.Context
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** Mittelpunkt und Nachbarländer je Land (assets/geo.txt, erzeugt von tools/make-geo.py) – für einige Erfolge. */
class CountryGeo(
    val centers: Map<String, Pair<Double, Double>>,
    val neighbors: Map<String, Set<String>>,
) {
    /** Entfernung der Ländermittelpunkte in Kilometern, null wenn ein Land unbekannt ist. */
    fun distanceKm(a: String, b: String): Double? {
        val (lat1, lon1) = centers[a] ?: return null
        val (lat2, lon2) = centers[b] ?: return null
        val p1 = Math.toRadians(lat1)
        val p2 = Math.toRadians(lat2)
        val dp = p2 - p1
        val dl = Math.toRadians(lon2 - lon1)
        val h = sin(dp / 2) * sin(dp / 2) + cos(p1) * cos(p2) * sin(dl / 2) * sin(dl / 2)
        return 2 * 6371.0 * asin(sqrt(h))
    }

    companion object {
        fun parse(text: String): CountryGeo {
            val centers = mutableMapOf<String, Pair<Double, Double>>()
            val neighbors = mutableMapOf<String, Set<String>>()
            text.lineSequence().filter { it.isNotBlank() && !it.startsWith("#") }.forEach { line ->
                val f = line.split('|')
                centers[f[0]] = f[1].toDouble() to f[2].toDouble()
                neighbors[f[0]] = f.getOrElse(3) { "" }.split(',').filter { it.isNotEmpty() }.toSet()
            }
            // Antarktis fehlt auf der Karte, kann aber als Herkunftsland vorkommen.
            centers.putIfAbsent("AQ", -80.0 to 0.0)
            return CountryGeo(centers, neighbors)
        }

        @Volatile private var cached: CountryGeo? = null

        fun load(context: Context): CountryGeo =
            cached ?: parse(context.assets.open("geo.txt").bufferedReader().use { it.readText() }).also { cached = it }
    }
}
