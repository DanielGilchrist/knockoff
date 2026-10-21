package dev.danielgilchrist.knockoff.ui

import dev.danielgilchrist.knockoff.domain.notification.PackageName
import dev.danielgilchrist.knockoff.group.GroupDiscovery
import dev.danielgilchrist.knockoff.group.ListenerReach

@ConsistentCopyVisibility
data class GroupDiscoveries private constructor(
    private val apps: Map<PackageName, AppDiscovery>,
) {
    companion object {
        val NONE: GroupDiscoveries get() = GroupDiscoveries(emptyMap())
    }

    fun of(app: PackageName): AppDiscovery = apps.getOrDefault(app, AppDiscovery.NotAsked)

    fun requested(app: PackageName): DiscoveryRequest =
        when (of(app)) {
            AppDiscovery.Reading -> DiscoveryRequest(this, DiscoveryStart.Skip)
            AppDiscovery.NotAsked, is AppDiscovery.Finished -> DiscoveryRequest(reading(app), DiscoveryStart.Start)
        }

    fun retried(
        app: PackageName,
        reach: ListenerReach,
        link: DeviceLink,
    ): DiscoveryRequest =
        when (val discovery = of(app)) {
            AppDiscovery.NotAsked, AppDiscovery.Reading -> DiscoveryRequest(this, DiscoveryStart.Skip)
            is AppDiscovery.Finished -> retriedAfter(app, discovery.discovery, reach, link)
        }

    fun finished(
        app: PackageName,
        discovery: GroupDiscovery,
    ): GroupDiscoveries = GroupDiscoveries(apps + (app to AppDiscovery.Finished(discovery)))

    private fun retriedAfter(
        app: PackageName,
        last: GroupDiscovery,
        reach: ListenerReach,
        link: DeviceLink,
    ): DiscoveryRequest {
        val worthRetrying =
            when (last) {
                GroupDiscovery.Unbound -> reach is ListenerReach.Bound
                is GroupDiscovery.NotLinked -> link is DeviceLink.Linked
                GroupDiscovery.Read, is GroupDiscovery.Unrecorded -> false
            }

        return when {
            worthRetrying -> requested(app)
            else -> DiscoveryRequest(this, DiscoveryStart.Skip)
        }
    }

    private fun reading(app: PackageName): GroupDiscoveries = GroupDiscoveries(apps + (app to AppDiscovery.Reading))
}
