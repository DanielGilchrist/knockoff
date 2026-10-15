package dev.danielgilchrist.knockoff.listener

import android.app.Notification
import android.service.notification.StatusBarNotification
import dev.danielgilchrist.knockoff.domain.notification.PostedNotification

internal fun StatusBarNotification.describe(posted: PostedNotification): String =
    "${posted.describe()} subText=${notification.extras.getCharSequence(Notification.EXTRA_SUB_TEXT)}"
