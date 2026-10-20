package dev.danielgilchrist.knockoff.ui.preferences

import dev.danielgilchrist.knockoff.ui.DeviceLink
import dev.danielgilchrist.knockoff.ui.ListenerAccess
import dev.danielgilchrist.knockoff.ui.NotificationPermission
import dev.danielgilchrist.knockoff.ui.Setup

data class SetupItem(
    val action: SetupAction,
    val supporting: String,
    val status: SetupStatus,
) {
    companion object {
        fun of(setup: Setup): List<SetupItem> =
            listOf(access(setup.access), link(setup.link), notifications(setup.notifications))

        private fun access(access: ListenerAccess): SetupItem =
            when (access) {
                ListenerAccess.Granted -> {
                    SetupItem(SetupAction.Access, "On. Knockoff can hold notifications.", SetupStatus.Done)
                }

                ListenerAccess.NotGranted -> {
                    SetupItem(SetupAction.Access, "Off. Nothing is held until it's on.", SetupStatus.Needed("Turn on"))
                }
            }

        private fun link(link: DeviceLink): SetupItem =
            when (link) {
                DeviceLink.Linked -> {
                    SetupItem(SetupAction.Link, "On. Held apps don't buzz first.", SetupStatus.Done)
                }

                DeviceLink.NotLinked -> {
                    SetupItem(
                        SetupAction.Link,
                        "Off. Held notifications buzz once. Needs a linked device such as a watch.",
                        SetupStatus.Needed("Link"),
                    )
                }
            }

        private fun notifications(permission: NotificationPermission): SetupItem =
            when (permission) {
                NotificationPermission.Granted -> {
                    SetupItem(SetupAction.Notifications, "On. Rules can ask about new chats.", SetupStatus.Done)
                }

                NotificationPermission.NotGranted -> {
                    SetupItem(
                        SetupAction.Notifications,
                        "Off. New chats are only listed in Knockoff.",
                        SetupStatus.Needed("Allow"),
                    )
                }
            }
    }
}
