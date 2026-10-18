package dev.danielgilchrist.knockoff.ui.home

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ChatInitialTest {
    @Test
    fun `the initial is the first letter or digit, upper-cased`() {
        assertEquals(ChatInitial.Letter("R"), ChatInitial.of("rheaSylph"))
        assertEquals(ChatInitial.Letter("S"), ChatInitial.of("🔥 Squad"))
        assertEquals(ChatInitial.Letter("F"), ChatInitial.of("👨‍👩‍👧 Family"))
        assertEquals(ChatInitial.Letter("7"), ChatInitial.of("  7 Seas"))
    }

    @Test
    fun `names without letters or digits have no initial`() {
        assertEquals(ChatInitial.None, ChatInitial.of("🔥🔥"))
        assertEquals(ChatInitial.None, ChatInitial.of(""))
    }
}
