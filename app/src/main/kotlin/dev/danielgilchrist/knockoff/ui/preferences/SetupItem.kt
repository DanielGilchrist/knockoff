package dev.danielgilchrist.knockoff.ui.preferences

import dev.danielgilchrist.knockoff.ui.DeviceLink
import dev.danielgilchrist.knockoff.ui.ExactAlarms
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
            listOf(access(setup.access)) + link(setup) + notifications(setup.notifications)

        private fun access(access: ListenerAccess): SetupItem =
            when (access) {
                ListenerAccess.Granted -> {
                    SetupItem(SetupAction.Access, "On. Knockoff can hold notifications.", SetupStatus.Done)
                }

                ListenerAccess.NotGranted -> {
                    SetupItem(SetupAction.Access, "Off. Nothing is held until it's on.", SetupStatus.Needed("Turn on"))
                }
            }

        private fun link(setup: Setup): List<SetupItem> =
            when (setup.link) {
                DeviceLink.Linked -> {
                    listOf(
                        SetupItem(SetupAction.Link, "On. Held apps don't buzz first.", SetupStatus.Done),
                        alarms(setup.alarms),
                    )
                }

                DeviceLink.NotLinked -> {
                    listOf(
                        SetupItem(
                            SetupAction.Link,
                            "Off. Held notifications buzz once. Needs a linked device such as a watch.",
                            SetupStatus.Needed("Link"),
                        ),
                    )
                }
            }

        private fun alarms(alarms: ExactAlarms): SetupItem =
            when (alarms) {
                ExactAlarms.Allowed -> {
                    SetupItem(SetupAction.ExactAlarms, "On. Silencing starts and ends on time.", SetupStatus.Done)
                }

                ExactAlarms.NotAllowed -> {
                    SetupItem(
                        SetupAction.ExactAlarms,
                        "Off. Silencing may be a few minutes late.",
                        SetupStatus.Needed("Allow"),
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
