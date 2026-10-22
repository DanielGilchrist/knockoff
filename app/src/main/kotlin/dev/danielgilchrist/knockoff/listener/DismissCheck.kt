package dev.danielgilchrist.knockoff.listener

internal sealed interface DismissCheck {
    data object Alone : DismissCheck

    data class Occupied(
        val active: Int,
    ) : DismissCheck
}
