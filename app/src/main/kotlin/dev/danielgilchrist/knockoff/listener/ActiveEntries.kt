package dev.danielgilchrist.knockoff.listener

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

internal class ActiveEntries(
    private val listener: NotificationListenerService,
) {
    fun groups(): ActiveGroups =
        when (val read = listener.read(NotificationList.Active)) {
            is NotificationRead.Read -> ActiveGroups.Read(of(read.notifications))
            is NotificationRead.Failure -> ActiveGroups.Unavailable(read.detail)
        }

    fun of(notifications: List<StatusBarNotification>): List<GroupEntry> {
        val groupOf = listener.groupsNow()

        return notifications.map { GroupEntry.of(it, groupOf(it)) }
    }
}
