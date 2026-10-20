package dev.danielgilchrist.knockoff.listener

import dev.danielgilchrist.knockoff.domain.notification.NotificationStyle
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class NotificationStylesTest {
    @Test
    fun `only the messaging template is a messaging notification`() {
        assertEquals(NotificationStyle.Messaging, notificationStyle("android.app.Notification\$MessagingStyle"))
        assertEquals(NotificationStyle.Other, notificationStyle("android.app.Notification\$InboxStyle"))
        assertEquals(NotificationStyle.Other, notificationStyle(null))
    }
}
