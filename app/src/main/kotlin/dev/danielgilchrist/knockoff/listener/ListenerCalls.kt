package dev.danielgilchrist.knockoff.listener

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import dev.danielgilchrist.knockoff.domain.notification.NotificationKey
import dev.danielgilchrist.knockoff.domain.notification.OriginGroup
import dev.danielgilchrist.knockoff.system.SystemCall
import dev.danielgilchrist.knockoff.system.systemCall

internal class ListenerCalls(
    private val listener: NotificationListenerService,
) : NotificationCalls {
    override fun read(list: NotificationList): NotificationRead =
        when (
            val call =
                systemCall {
                    when (list) {
                        NotificationList.Active -> listener.activeNotifications
                        NotificationList.Snoozed -> listener.snoozedNotifications
                    }
                }
        ) {
            is SystemCall.Done -> readOf(call.value)
            is SystemCall.Refused -> NotificationRead.Refused(call.detail)
            is SystemCall.Unavailable -> NotificationRead.Unavailable(call.detail)
        }

    override fun groups(): (StatusBarNotification) -> OriginGroup =
        when (val call = systemCall { listener.currentRanking }) {
            is SystemCall.Done -> call.value?.let { it::groupOf } ?: { _ -> OriginGroup.Unknown }
            is SystemCall.Failed -> { _ -> OriginGroup.Unknown }
        }

    override fun snooze(
        key: NotificationKey,
        millis: Long,
    ): SystemCall<Unit> = systemCall { listener.snoozeNotification(key.value, millis) }

    override fun cancel(key: NotificationKey): SystemCall<Unit> = systemCall { listener.cancelNotification(key.value) }

    private fun readOf(notifications: Array<StatusBarNotification>?): NotificationRead =
        when (notifications) {
            null -> NotificationRead.Unavailable("the system returned no list")
            else -> NotificationRead.Read(notifications.toList())
        }
}
