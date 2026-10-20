package dev.danielgilchrist.knockoff.ui.preferences

import dev.danielgilchrist.knockoff.ui.DeviceLink
import dev.danielgilchrist.knockoff.ui.ListenerAccess
import dev.danielgilchrist.knockoff.ui.NotificationPermission
import dev.danielgilchrist.knockoff.ui.Setup
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SetupItemTest {
    @Test
    fun `everything set up shows done rows and the link explanation`() {
        val setup = Setup(ListenerAccess.Granted, DeviceLink.Linked("Pixel Buds"), NotificationPermission.Granted)

        assertEquals(
            listOf(
                SetupAction.Access to SetupStatus.Done,
                SetupAction.Link to SetupStatus.Explained,
                SetupAction.Notifications to SetupStatus.Done,
            ),
            SetupItem.of(setup).map { it.action to it.status },
        )
    }

    @Test
    fun `a link names its device`() {
        val setup = Setup(ListenerAccess.Granted, DeviceLink.Linked("Pixel Buds"), NotificationPermission.Granted)

        assertEquals(
            "On. Linked to Pixel Buds. Held notifications stay quiet.",
            SetupItem.of(setup).single { it.action == SetupAction.Link }.supporting,
        )
    }

    @Test
    fun `missing setup asks for each step`() {
        val setup = Setup(ListenerAccess.NotGranted, DeviceLink.NotLinked, NotificationPermission.NotGranted)

        assertEquals(
            listOf(
                SetupAction.Access to SetupStatus.Needed("Turn on"),
                SetupAction.Link to SetupStatus.Needed("Set up"),
                SetupAction.Notifications to SetupStatus.Needed("Allow"),
            ),
            SetupItem.of(setup).map { it.action to it.status },
        )
    }
}
