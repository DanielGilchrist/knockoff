package dev.danielgilchrist.knockoff.ui.zone

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ZonesMatchingTest {
    @Test
    fun `a city finds its zone whatever its case`() {
        assertEquals(listOf("Europe/London"), zonesMatching("london").map { it.id })
    }

    @Test
    fun `every word must match, and spaces stand for underscores`() {
        assertEquals(listOf("America/New_York"), zonesMatching("america new york").map { it.id })
    }

    @Test
    fun `an empty search lists every zone in order`() {
        val all = zonesMatching("  ").map { it.id }

        assertTrue(all.size > 400)
        assertEquals(all.sorted(), all)
    }

    @Test
    fun `an unknown place finds nothing`() {
        assertEquals(emptyList<String>(), zonesMatching("Atlantis").map { it.id })
    }

    @Test
    fun `Etc zones with inverted GMT signs are left out`() {
        assertEquals(emptyList<String>(), zonesMatching("Etc/GMT+5").map { it.id })
        assertEquals(emptyList<String>(), zonesMatching("Etc/GMT-5").map { it.id })
    }
}
