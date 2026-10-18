package dev.danielgilchrist.knockoff.ui.components

sealed interface TileNote {
    data object None : TileNote

    data class Shown(
        val text: String,
    ) : TileNote
}
