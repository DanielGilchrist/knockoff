package dev.danielgilchrist.knockoff.ui

import android.content.Context

data class Setup(
    val access: ListenerAccess,
    val link: DeviceLink,
    val alarms: ExactAlarms,
    val notifications: NotificationPermission,
) {
    companion object {
        fun of(context: Context): Setup =
            Setup(
                ListenerAccess.of(context),
                DeviceLink.of(context),
                ExactAlarms.of(context),
                NotificationPermission.of(context),
            )
    }
}
