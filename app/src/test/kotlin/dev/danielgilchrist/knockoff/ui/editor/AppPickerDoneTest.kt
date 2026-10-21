package dev.danielgilchrist.knockoff.ui.editor

import dev.danielgilchrist.knockoff.ui.components.PickerDone
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AppPickerDoneTest {
    @Test
    fun `adding is unavailable until an app is ticked, then counts them`() {
        assertEquals(PickerDone.Unavailable("Add"), appPickerDone(0))
        assertEquals(PickerDone.Ready("Add 1"), appPickerDone(1))
        assertEquals(PickerDone.Ready("Add 3"), appPickerDone(3))
    }
}
