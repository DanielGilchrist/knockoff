package dev.danielgilchrist.knockoff.ui.components

sealed interface CardRemoval {
    data object None : CardRemoval

    data class Remove(
        val label: String,
        val onRemove: () -> Unit,
    ) : CardRemoval
}
