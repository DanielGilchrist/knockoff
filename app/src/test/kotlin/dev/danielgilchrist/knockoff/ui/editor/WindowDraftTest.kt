package dev.danielgilchrist.knockoff.ui.editor

import dev.danielgilchrist.knockoff.ui.components.TileNote
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalTime

class WindowDraftTest {
    @Test
    fun `the end time notes when it falls on the next day`() {
        val hours = { start: Int, end: Int ->
            WindowDraft.WORKING_HOURS.copy(start = LocalTime.of(start, 0), end = LocalTime.of(end, 0))
        }

        assertEquals(TileNote.None, hours(8, 17).endNote())
        assertEquals(TileNote.Shown("Next day"), hours(22, 6).endNote())
        assertEquals(TileNote.Shown("24 hours later"), hours(9, 9).endNote())
    }
}
