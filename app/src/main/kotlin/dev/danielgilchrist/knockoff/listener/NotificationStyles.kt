package dev.danielgilchrist.knockoff.listener

import android.app.Notification
import dev.danielgilchrist.knockoff.domain.notification.NotificationStyle

internal fun notificationStyle(template: String?): NotificationStyle =
    when (template) {
        Notification.MessagingStyle::class.java.name -> NotificationStyle.Messaging
        else -> NotificationStyle.Other
    }
