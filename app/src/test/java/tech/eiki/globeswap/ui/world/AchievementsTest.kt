package tech.eiki.globeswap.ui.world

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
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
}
