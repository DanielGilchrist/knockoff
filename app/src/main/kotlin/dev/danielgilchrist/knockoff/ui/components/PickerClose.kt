package dev.danielgilchrist.knockoff.ui.components

sealed interface PickerClose {
    data object None : PickerClose

    data class Discard(
        val onClose: () -> Unit,
    ) : PickerClose
}
