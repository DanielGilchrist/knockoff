package dev.danielgilchrist.knockoff.listener

import android.app.Notification
import dev.danielgilchrist.knockoff.domain.notification.NotificationTrait

internal fun notificationTraits(
    flags: Int,
    extraKeys: Set<String>,
): Set<NotificationTrait> =
    buildSet {
        if (flags and Notification.FLAG_ONGOING_EVENT != 0) add(NotificationTrait.Ongoing)
        if (flags and Notification.FLAG_FOREGROUND_SERVICE != 0) add(NotificationTrait.ForegroundService)
        if (flags and Notification.FLAG_NO_CLEAR != 0) add(NotificationTrait.NoClear)
        if (Notification.EXTRA_MEDIA_SESSION in extraKeys) add(NotificationTrait.MediaSession)
    }
