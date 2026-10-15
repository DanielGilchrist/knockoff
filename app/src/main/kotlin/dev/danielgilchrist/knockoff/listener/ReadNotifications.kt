package dev.danielgilchrist.knockoff.listener

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

internal fun NotificationListenerService.read(list: NotificationList): NotificationRead {
    val notifications: Array<StatusBarNotification>? =
        try {
            when (list) {
                NotificationList.Active -> activeNotifications
                NotificationList.Snoozed -> snoozedNotifications
            }
        } catch (exception: SecurityException) {
            return NotificationRead.Refused(exception.toString())
        }

    return when (notifications) {
        null -> NotificationRead.Missing
        else -> NotificationRead.Read(notifications.toList())
    }
}
