package dev.danielgilchrist.knockoff.ui.editor

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DraftProblemsTest {
    @Test
    fun `one problem reads as a sentence and several as bullets`() {
        assertEquals("Give the rule a name.", problemsBody(listOf("Give the rule a name.")))
        assertEquals("• a\n• b", problemsBody(listOf("a", "b")))
    }
}
