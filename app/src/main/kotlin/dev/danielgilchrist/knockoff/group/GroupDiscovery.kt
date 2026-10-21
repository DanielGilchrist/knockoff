package dev.danielgilchrist.knockoff.group

sealed interface GroupDiscovery {
    data object Read : GroupDiscovery

    data class NotLinked(
        val detail: String,
    ) : GroupDiscovery

    data object Unbound : GroupDiscovery

    data class Unrecorded(
        val detail: String,
    ) : GroupDiscovery
}
