package dev.danielgilchrist.knockoff.ui.preferences

import dev.danielgilchrist.knockoff.ui.DeviceLink
import dev.danielgilchrist.knockoff.ui.ListenerAccess
import dev.danielgilchrist.knockoff.ui.NotificationPermission
import dev.danielgilchrist.knockoff.ui.Setup
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SetupItemTest {
    @Test
    fun `everything set up shows three done rows`() {
        val setup = Setup(ListenerAccess.Granted, DeviceLink.Linked, NotificationPermission.Granted)

        assertEquals(
            listOf(SetupAction.Access, SetupAction.Link, SetupAction.Notifications),
            SetupItem.of(setup).map { it.action },
        )
        assertEquals(List(3) { SetupStatus.Done }, SetupItem.of(setup).map { it.status })
    }

    @Test
    fun `missing setup asks for each step`() {
        val setup = Setup(ListenerAccess.NotGranted, DeviceLink.NotLinked, NotificationPermission.NotGranted)

        assertEquals(
            listOf(
                SetupAction.Access to SetupStatus.Needed("Turn on"),
                SetupAction.Link to SetupStatus.Needed("Link"),
                SetupAction.Notifications to SetupStatus.Needed("Allow"),
            ),
            SetupItem.of(setup).map { it.action to it.status },
        )
    }
}
