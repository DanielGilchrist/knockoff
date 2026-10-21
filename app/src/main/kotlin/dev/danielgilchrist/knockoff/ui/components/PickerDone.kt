package dev.danielgilchrist.knockoff.ui.components

sealed interface PickerDone {
    val label: String

    data class Ready(
        override val label: String,
    ) : PickerDone

    data class Unavailable(
        override val label: String,
    ) : PickerDone
}
