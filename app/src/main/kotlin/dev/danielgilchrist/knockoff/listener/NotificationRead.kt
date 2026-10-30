package dev.danielgilchrist.knockoff.listener

import android.service.notification.StatusBarNotification

sealed interface NotificationRead {
    data class Read(
        val notifications: List<StatusBarNotification>,
    ) : NotificationRead

    sealed interface Failure : NotificationRead {
        val detail: String
    }

    data class Refused(
        override val detail: String,
    ) : Failure

    data class Unavailable(
        override val detail: String,
    ) : Failure
}
