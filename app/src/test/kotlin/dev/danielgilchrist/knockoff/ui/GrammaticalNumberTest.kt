package dev.danielgilchrist.knockoff.ui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GrammaticalNumberTest {
    @Test
    fun `only one is singular`() {
        assertEquals(
            listOf(GrammaticalNumber.Many, GrammaticalNumber.One, GrammaticalNumber.Many),
            listOf(0, 1, 2).map(GrammaticalNumber::of),
        )
    }
}
