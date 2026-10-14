package dev.danielgilchrist.knockoff.listener

import android.app.Notification
import dev.danielgilchrist.knockoff.domain.notification.NotificationTrait

// Notification.FLAG_AUTOGROUP_SUMMARY is @hide, so its value is copied from AOSP.
internal const val FLAG_AUTOGROUP_SUMMARY = 0x00000400

internal fun notificationTraits(
    flags: Int,
    extraKeys: Set<String>,
): Set<NotificationTrait> =
    buildSet {
        if (flags and Notification.FLAG_ONGOING_EVENT != 0) add(NotificationTrait.Ongoing)
        if (flags and Notification.FLAG_FOREGROUND_SERVICE != 0) add(NotificationTrait.ForegroundService)
        if (flags and Notification.FLAG_NO_CLEAR != 0) add(NotificationTrait.NoClear)
        if (flags and FLAG_AUTOGROUP_SUMMARY != 0) add(NotificationTrait.AutogroupSummary)
        if (Notification.EXTRA_MEDIA_SESSION in extraKeys) add(NotificationTrait.MediaSession)
    }
