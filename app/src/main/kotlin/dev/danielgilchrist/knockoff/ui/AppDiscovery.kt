package dev.danielgilchrist.knockoff.ui

import dev.danielgilchrist.knockoff.group.GroupDiscovery

sealed interface AppDiscovery {
    data object NotAsked : AppDiscovery

    data object Reading : AppDiscovery

    data class Finished(
        val discovery: GroupDiscovery,
    ) : AppDiscovery
}
