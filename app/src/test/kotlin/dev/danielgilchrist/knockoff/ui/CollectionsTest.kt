package dev.danielgilchrist.knockoff.ui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CollectionsTest {
    @Test
    fun `toggling a set adds a missing item and removes a present one`() {
        assertEquals(setOf(1, 2), setOf(1).toggled(2))
        assertEquals(setOf(1), setOf(1, 2).toggled(2))
    }

    @Test
    fun `toggling a map adds a missing key and removes a present one`() {
        assertEquals(mapOf(1 to "a", 2 to "b"), mapOf(1 to "a").toggled(2, "b"))
        assertEquals(mapOf(1 to "a"), mapOf(1 to "a", 2 to "b").toggled(2, "changed"))
    }
}
