package dev.danielgilchrist.knockoff.ui.preferences

import dev.danielgilchrist.knockoff.ui.DeviceLink
import dev.danielgilchrist.knockoff.ui.ExactAlarms
import dev.danielgilchrist.knockoff.ui.ListenerAccess
import dev.danielgilchrist.knockoff.ui.NotificationPermission
import dev.danielgilchrist.knockoff.ui.Setup
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SetupItemTest {
    @Test
    fun `everything set up shows four done rows`() {
        val setup =
            Setup(ListenerAccess.Granted, DeviceLink.Linked, ExactAlarms.Allowed, NotificationPermission.Granted)

        assertEquals(
            listOf(SetupAction.Access, SetupAction.Link, SetupAction.ExactAlarms, SetupAction.Notifications),
            SetupItem.of(setup).map { it.action },
        )
        assertEquals(List(4) { SetupStatus.Done }, SetupItem.of(setup).map { it.status })
    }

    @Test
    fun `exact alarms only matter once a device is linked`() {
        val setup =
            Setup(
                ListenerAccess.NotGranted,
                DeviceLink.NotLinked,
                ExactAlarms.NotAllowed,
                NotificationPermission.NotGranted,
            )

        assertEquals(
            listOf(
                SetupAction.Access to SetupStatus.Needed("Turn on"),
                SetupAction.Link to SetupStatus.Needed("Link"),
                SetupAction.Notifications to SetupStatus.Needed("Allow"),
            ),
            SetupItem.of(setup).map { it.action to it.status },
        )
        assertEquals(
            SetupStatus.Needed("Allow"),
            SetupItem.of(setup.copy(link = DeviceLink.Linked)).single { it.action == SetupAction.ExactAlarms }.status,
        )
    }
}
