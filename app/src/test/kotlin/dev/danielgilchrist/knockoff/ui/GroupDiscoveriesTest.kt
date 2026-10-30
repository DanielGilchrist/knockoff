package dev.danielgilchrist.knockoff.ui

import dev.danielgilchrist.knockoff.domain.notification.PackageName
import dev.danielgilchrist.knockoff.group.GroupDiscovery
import dev.danielgilchrist.knockoff.group.GroupReader
import dev.danielgilchrist.knockoff.group.ListenerReach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GroupDiscoveriesTest {
    private val gmail = PackageName.GMAIL
    private val slack = PackageName("com.Slack")
    private val bound = ListenerReach.Bound(GroupReader { GroupDiscovery.Read })
    private val linked = DeviceLink.Linked("Watch")

    @Test
    fun `a request starts a read unless one is already running`() {
        val first = GroupDiscoveries.NONE.requested(gmail)
        val again = first.discoveries.requested(gmail)
        val afterwards = first.discoveries.finished(gmail, GroupDiscovery.Read).requested(gmail)

        assertEquals(DiscoveryStart.Start, first.start)
        assertEquals(AppDiscovery.Reading, first.discoveries.of(gmail))
        assertEquals(AppDiscovery.NotAsked, first.discoveries.of(slack))
        assertEquals(DiscoveryRequest(first.discoveries, DiscoveryStart.Skip), again)
        assertEquals(DiscoveryStart.Start, afterwards.start)
        assertEquals(AppDiscovery.Reading, afterwards.discoveries.of(gmail))
    }

    @Test
    fun `a read that failed for want of the listener or link is retried once it's there`() {
        val unbound = GroupDiscoveries.NONE.finished(gmail, GroupDiscovery.Unbound)
        val unlinked = GroupDiscoveries.NONE.finished(gmail, GroupDiscovery.NotLinked("no association"))

        assertEquals(DiscoveryStart.Skip, unbound.retried(gmail, ListenerReach.Unbound, linked).start)
        assertEquals(DiscoveryStart.Start, unbound.retried(gmail, bound, DeviceLink.NotLinked).start)
        assertEquals(DiscoveryStart.Skip, unlinked.retried(gmail, bound, DeviceLink.NotLinked).start)
        assertEquals(DiscoveryStart.Start, unlinked.retried(gmail, bound, linked).start)
        assertEquals(AppDiscovery.Reading, unlinked.retried(gmail, bound, linked).discoveries.of(gmail))
    }

    @Test
    fun `a read the system couldn't send is always retried`() {
        val unavailable = GroupDiscoveries.NONE.finished(gmail, GroupDiscovery.Unavailable("binder full"))

        assertEquals(
            DiscoveryStart.Start,
            unavailable.retried(gmail, ListenerReach.Unbound, DeviceLink.NotLinked).start,
        )
    }

    @Test
    fun `a read that worked, is running or never ran isn't retried`() {
        listOf(
            GroupDiscoveries.NONE,
            GroupDiscoveries.NONE.requested(gmail).discoveries,
            GroupDiscoveries.NONE.finished(gmail, GroupDiscovery.Read),
            GroupDiscoveries.NONE.finished(gmail, GroupDiscovery.Unrecorded("disk full")),
        ).forEach { discoveries ->
            assertEquals(
                DiscoveryRequest(discoveries, DiscoveryStart.Skip),
                discoveries.retried(gmail, bound, linked),
            )
        }
    }
}
