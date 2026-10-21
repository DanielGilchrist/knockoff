package dev.danielgilchrist.knockoff.ui

sealed interface LinkState {
    data object Loading : LinkState

    data class Known(
        val link: DeviceLink,
    ) : LinkState
}
