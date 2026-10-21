package dev.danielgilchrist.knockoff.group

sealed interface ListenerReach {
    data class Bound(
        val reader: GroupReader,
    ) : ListenerReach

    data object Unbound : ListenerReach
}
