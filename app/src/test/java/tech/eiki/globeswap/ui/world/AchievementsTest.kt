package tech.eiki.globeswap.ui.world

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.eiki.globeswap.data.AchievementData
import tech.eiki.globeswap.data.World
import tech.eiki.globeswap.data.WorldCountry

class AchievementsTest {
    private fun world(vararg cs: Triple<String, String, Long>, photos: Int = 1) =
        World(cs.map { WorldCountry(it.first, it.second, photos, it.third) })

    @Test
    fun emptyMapHasNothingUnlocked() {
        val list = achievements(World())
        assertTrue(list.none { it.unlocked })
        assertEquals(0, list.first { it.id == "countries-5" }.progress)
    }

    @Test
    fun milestonesAndDates() {
        val w = world(
            Triple("JP", "AS", 3000), Triple("DE", "EU", 1000), Triple("FR", "EU", 2000),
            Triple("BR", "SA", 4000), Triple("US", "NA", 5000),
        )
        val byId = achievements(w).associateBy { it.id }
        assertEquals(1000L, byId.getValue("countries-1").unlockedAt) // das erste Land
        assertTrue(byId.getValue("countries-5").unlocked)
        assertEquals(5000L, byId.getValue("countries-5").unlockedAt) // mit dem fünften Land erreicht
        assertFalse(byId.getValue("countries-10").unlocked)
        assertEquals(5, byId.getValue("countries-10").progress)
        assertEquals(1000L, byId.getValue("continent-EU").unlockedAt) // frühestes Land Europas
        assertFalse(byId.getValue("continent-AF").unlocked)
        assertEquals(4, byId.getValue("all-continents").progress)
        assertNull(byId.getValue("all-continents").unlockedAt)
    }

    @Test
    fun allContinentsAndPhotos() {
        val w = world(
            Triple("DE", "EU", 1), Triple("JP", "AS", 2), Triple("KE", "AF", 3),
            Triple("CA", "NA", 4), Triple("AR", "SA", 5), Triple("AU", "OC", 6),
            photos = 20,
        )
        val byId = achievements(w).associateBy { it.id }
        assertTrue(byId.getValue("all-continents").unlocked)
        assertEquals(6L, byId.getValue("all-continents").unlockedAt) // mit dem letzten Kontinent
        assertTrue(byId.getValue("photos-10").unlocked) // 6 × 20 Fotos
        assertTrue(byId.getValue("photos-100").unlocked)
    }

    @Test
    fun discoverWithGeo() {
        val geo = CountryGeo.parse(
            """
            DE|51.1|10.3|AT,FR
            AT|47.6|14.1|DE
            FR|46.6|2.4|DE
            NZ|-44.0|170.5|
            BR|-10.8|-53.0|
            """.trimIndent(),
        )
        val near = World(
            listOf(WorldCountry("AT", "EU", 1, 1), WorldCountry("FR", "EU", 1, 2)),
            achievements = AchievementData(home = "DE"),
        )
        val a = achievements(near, geo).associateBy { it.id }
        assertTrue("Nachbarn AT und FR", a.getValue("neighbors").unlocked)
        assertFalse(a.getValue("antipode").unlocked)
        assertEquals(2, a.getValue("hemispheres").progress) // nur Norden und Osten
        val far = World(
            listOf(WorldCountry("NZ", "OC", 1, 1), WorldCountry("BR", "SA", 1, 2), WorldCountry("AQ", "AN", 1, 3), WorldCountry("JP", "AS", 1, 4)),
            achievements = AchievementData(home = "DE"),
        )
        val b = achievements(far, geo).associateBy { it.id }
        assertTrue("Neuseeland ist über 15.000 km von Deutschland entfernt", b.getValue("antipode").unlocked)
        assertTrue(b.getValue("antarctica").unlocked)
        assertEquals(2, b.getValue("islands").progress) // NZ und JP
        assertEquals(0, b.getValue("neighbors").progress)
        // Ohne bekanntes eigenes Land gibt es kein Nachbarschafts-Abzeichen.
        assertNull(achievements(World(), geo).find { it.id == "neighbors" })
    }

    @Test
    fun serverValues() {
        val day = 24 * 60 * 60 * 1000L
        val now = 1_000 * day
        val w = World(
            achievements = AchievementData(
                createdAt = now - 120 * day, streak = 2, bestStreak = 8, themeDays = 10,
                stats = mapOf("night" to 1L, "otherday" to 2L, "chat_photos" to 12L, "chat_days" to 3L, "reactions" to 10L, "edited" to 9L, "mix" to 3L),
                flags = listOf("tz:60", "tz:540", "wide") + listOf("VIVID", "WARM", "COOL", "VINTAGE", "FADE", "MONO", "NOIR").map { "filter:$it" },
            ),
        )
        val a = achievements(w, now = now).associateBy { it.id }
        val unlocked = a.values.filter { it.unlocked }.map { it.id }.toSet()
        for (id in listOf("night", "otherday", "streak-3", "streak-7", "loyal", "theme-1", "theme-10", "talkative", "hearts", "mix", "filters", "wide")) {
            assertTrue("$id sollte erreicht sein", id in unlocked)
        }
        for (id in listOf("sunrise", "zones", "streak-30", "anniversary", "theme-50", "penpal", "artist")) {
            assertFalse("$id sollte noch nicht erreicht sein", id in unlocked)
        }
        assertEquals(2, a.getValue("zones").progress)
        assertEquals(9, a.getValue("artist").progress)
    }
}
