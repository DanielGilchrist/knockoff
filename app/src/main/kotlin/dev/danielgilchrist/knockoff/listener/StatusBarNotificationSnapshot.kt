package dev.danielgilchrist.knockoff.listener

import android.service.notification.StatusBarNotification
import dev.danielgilchrist.knockoff.domain.notification.NotificationKey
import dev.danielgilchrist.knockoff.domain.notification.NotificationSnapshot
import dev.danielgilchrist.knockoff.domain.notification.PackageName

internal fun StatusBarNotification.toSnapshot(): NotificationSnapshot =
    NotificationSnapshot(
        key = NotificationKey(key),
        packageName = PackageName(packageName),
        channelId = notification.channelId,
        shortcutId = notification.shortcutId,
        traits = notificationTraits(notification.flags, notification.extras.keySet()),
    )
