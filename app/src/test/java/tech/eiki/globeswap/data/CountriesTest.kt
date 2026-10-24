package tech.eiki.globeswap.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.eiki.globeswap.ui.onboarding.isOldEnough
import java.time.YearMonth

class CountriesTest {
    @Test
    fun flagsAndNames() {
        assertEquals("🇯🇵", Countries.flag("JP"))
        assertEquals("🇩🇪 Germany", Countries.label("DE"))
        assertEquals("", Countries.label(""))
        assertEquals("", Countries.flag("xx1"))
        assertEquals("South America", Countries.continent("SA"))
    }

    @Test
    fun ageCheckIsConservative() {
        val now = YearMonth.of(2026, 10)
        assertTrue(isOldEnough(YearMonth.of(2010, 9), now)) // 16 und einen Monat
        assertFalse(isOldEnough(YearMonth.of(2010, 10), now)) // Geburtsmonat: Tag unbekannt → noch nicht
        assertFalse(isOldEnough(YearMonth.of(2010, 11), now)) // 15
        assertTrue(isOldEnough(YearMonth.of(1960, 1), now))
        assertFalse(isOldEnough(YearMonth.of(2026, 1), now))
    }
}
