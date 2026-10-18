package dev.danielgilchrist.knockoff.ui.editor

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class WindowPositionTest {
    @Test
    fun `a lone window is unnumbered and the rest are numbered from one`() {
        assertEquals(WindowPosition.Only, WindowPosition.of(0, 1))
        assertEquals(listOf(WindowPosition.OneOf(1), WindowPosition.OneOf(2)), (0..1).map { WindowPosition.of(it, 2) })
    }
}
