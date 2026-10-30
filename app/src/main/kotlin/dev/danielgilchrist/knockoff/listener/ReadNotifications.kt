package dev.danielgilchrist.knockoff.listener

import android.os.BadParcelableException
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

internal fun NotificationListenerService.read(list: NotificationList): NotificationRead =
    try {
        readOf(list)
    } catch (exception: SecurityException) {
        NotificationRead.Refused(exception.toString())
    } catch (exception: BadParcelableException) {
        NotificationRead.Unavailable(exception.toString())
    }

private fun NotificationListenerService.readOf(list: NotificationList): NotificationRead {
    val notifications: Array<StatusBarNotification>? =
        when (list) {
            NotificationList.Active -> activeNotifications
            NotificationList.Snoozed -> snoozedNotifications
        }

    return when (notifications) {
        null -> NotificationRead.Unavailable("the system returned no list")
        else -> NotificationRead.Read(notifications.toList())
    }
}
