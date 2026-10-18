package dev.danielgilchrist.knockoff.ui.components

sealed interface ScreenNavigation {
    data object Root : ScreenNavigation

    data class Back(
        val onBack: () -> Unit,
    ) : ScreenNavigation
}
