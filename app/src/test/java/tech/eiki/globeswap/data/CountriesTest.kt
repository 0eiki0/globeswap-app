package tech.eiki.globeswap.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.eiki.globeswap.R
import tech.eiki.globeswap.ui.onboarding.isOldEnough
import java.time.LocalDate

class CountriesTest {
    @Test
    fun flagsAndNames() {
        assertEquals("🇯🇵", Countries.flag("JP"))
        assertEquals("🇩🇪 Germany", Countries.label("DE"))
        assertEquals("", Countries.label(""))
        assertEquals("", Countries.flag("xx1"))
        assertEquals(R.string.cont_sa, Countries.continent("SA"))
    }

    @Test
    fun ageCheck() {
        val today = LocalDate.of(2026, 10, 24)
        assertTrue(isOldEnough(LocalDate.of(2010, 10, 24), today)) // heute 16. Geburtstag
        assertFalse(isOldEnough(LocalDate.of(2010, 10, 25), today)) // morgen erst 16
        assertTrue(isOldEnough(LocalDate.of(2010, 1, 1), today))
        assertFalse(isOldEnough(LocalDate.of(2012, 2, 29), today))
        assertTrue(isOldEnough(LocalDate.of(1960, 5, 5), today))
    }
}
