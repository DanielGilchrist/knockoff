package dev.danielgilchrist.knockoff.group

import dev.danielgilchrist.knockoff.domain.notification.PackageName
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GroupReachTest {
    private val gmail = PackageName.GMAIL
    private val reading = GroupReader { GroupDiscovery.Read }
    private val unlinked = GroupReader { GroupDiscovery.NotLinked("no association") }

    @Test
    fun `without a listener nothing can be read`() =
        runTest {
            val reach = GroupReach()

            assertEquals(ListenerReach.Unbound, reach.state.value)
            assertEquals(GroupDiscovery.Unbound, reach.discover(gmail))
        }

    @Test
    fun `a bound listener answers until it unbinds`() =
        runTest {
            val reach = GroupReach()

            reach.bind(reading)
            assertEquals(ListenerReach.Bound(reading), reach.state.value)
            assertEquals(GroupDiscovery.Read, reach.discover(gmail))

            reach.unbind(reading)
            assertEquals(ListenerReach.Unbound, reach.state.value)
            assertEquals(GroupDiscovery.Unbound, reach.discover(gmail))
        }

    @Test
    fun `an old listener unbinding leaves the newer one bound`() =
        runTest {
            val reach = GroupReach()

            reach.bind(reading)
            reach.bind(unlinked)
            reach.unbind(reading)

            assertEquals(GroupDiscovery.NotLinked("no association"), reach.discover(gmail))
        }
}
