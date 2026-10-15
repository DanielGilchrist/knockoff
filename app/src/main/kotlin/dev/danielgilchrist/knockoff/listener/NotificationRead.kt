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

    data object Missing : Failure {
        override val detail: String get() = "the system did not return the list"
    }
}
