package dev.danielgilchrist.knockoff.listener

import android.service.notification.StatusBarNotification
import dev.danielgilchrist.knockoff.domain.notification.NotificationKey
import dev.danielgilchrist.knockoff.domain.notification.OriginGroup
import dev.danielgilchrist.knockoff.system.SystemCall

internal interface NotificationCalls {
    fun read(list: NotificationList): NotificationRead

    fun snoozedKeys(): SnoozedRead

    fun groups(): (StatusBarNotification) -> OriginGroup

    fun snooze(
        key: NotificationKey,
        millis: Long,
    ): SystemCall<Unit>

    fun cancel(key: NotificationKey): SystemCall<Unit>
}
