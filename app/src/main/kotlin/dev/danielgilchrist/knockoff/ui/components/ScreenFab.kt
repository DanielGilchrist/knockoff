package dev.danielgilchrist.knockoff.ui.components

import androidx.annotation.DrawableRes

sealed interface ScreenFab {
    data object None : ScreenFab

    data class Extended(
        val text: String,
        @param:DrawableRes val icon: Int,
        val onClick: () -> Unit,
    ) : ScreenFab
}
