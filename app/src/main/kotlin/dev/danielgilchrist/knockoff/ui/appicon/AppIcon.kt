package dev.danielgilchrist.knockoff.ui.appicon

import androidx.compose.ui.graphics.ImageBitmap

sealed interface AppIcon {
    data object Loading : AppIcon

    sealed interface Settled : AppIcon

    data class Loaded(
        val image: ImageBitmap,
    ) : Settled

    data object Missing : Settled
}
