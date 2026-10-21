package dev.danielgilchrist.knockoff.ui.editor

import dev.danielgilchrist.knockoff.ui.components.PickerDone

internal fun appPickerDone(selected: Int): PickerDone =
    when (selected) {
        0 -> PickerDone.Unavailable("Add")
        else -> PickerDone.Ready("Add $selected")
    }
