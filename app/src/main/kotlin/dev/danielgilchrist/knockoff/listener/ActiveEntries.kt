package dev.danielgilchrist.knockoff.listener

import android.service.notification.StatusBarNotification

internal class ActiveEntries(
    private val calls: NotificationCalls,
) {
    fun groups(): ActiveGroups =
        when (val read = calls.read(NotificationList.Active)) {
            is NotificationRead.Read -> ActiveGroups.Read(of(read.notifications))
            is NotificationRead.Failure -> ActiveGroups.Unavailable(read.detail)
        }

    fun of(notifications: List<StatusBarNotification>): List<GroupEntry> {
        val groupOf = calls.groups()

        return notifications.map { GroupEntry.of(it, groupOf(it)) }
    }
}
