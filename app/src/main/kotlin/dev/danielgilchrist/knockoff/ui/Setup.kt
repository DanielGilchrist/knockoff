package dev.danielgilchrist.knockoff.ui

import android.content.Context

data class Setup(
    val access: ListenerAccess,
    val link: DeviceLink,
    val notifications: NotificationPermission,
) {
    companion object {
        fun of(context: Context): Setup =
            Setup(
                ListenerAccess.of(context),
                DeviceLink.of(context),
                NotificationPermission.of(context),
            )
    }
}
