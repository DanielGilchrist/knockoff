package dev.danielgilchrist.knockoff.listener

import android.app.Notification
import android.service.notification.StatusBarNotification
import dev.danielgilchrist.knockoff.domain.notification.NotificationKey
import dev.danielgilchrist.knockoff.domain.notification.NotificationSnapshot
import dev.danielgilchrist.knockoff.domain.notification.OriginGroup
import dev.danielgilchrist.knockoff.domain.notification.PackageName

internal fun StatusBarNotification.toSnapshot(group: OriginGroup): NotificationSnapshot =
    NotificationSnapshot(
        key = NotificationKey(key),
        packageName = PackageName(packageName),
        channelId = notification.channelId,
        group = group,
        style = notificationStyle(notification.extras.getString(Notification.EXTRA_TEMPLATE)),
        shortcutId = notification.shortcutId,
        titles = chatTitles(),
        traits = notificationTraits(notification.flags, notification.extras.keySet()),
    )
