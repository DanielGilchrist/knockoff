package dev.danielgilchrist.knockoff.listener

import android.app.Notification
import dev.danielgilchrist.knockoff.domain.notification.NotificationTrait
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class NotificationTraitsTest {
    @Test
    fun `an ordinary notification has no traits`() {
        assertEquals(
            emptySet<NotificationTrait>(),
            notificationTraits(Notification.FLAG_AUTO_CANCEL, setOf(Notification.EXTRA_SUB_TEXT)),
        )
    }

    @Test
    fun `each flag maps to its trait`() {
        assertEquals(setOf(NotificationTrait.Ongoing), notificationTraits(Notification.FLAG_ONGOING_EVENT, emptySet()))
        assertEquals(
            setOf(NotificationTrait.ForegroundService),
            notificationTraits(Notification.FLAG_FOREGROUND_SERVICE, emptySet()),
        )
        assertEquals(setOf(NotificationTrait.NoClear), notificationTraits(Notification.FLAG_NO_CLEAR, emptySet()))
    }

    @Test
    fun `an autogroup summary has no traits`() {
        assertEquals(
            emptySet<NotificationTrait>(),
            notificationTraits(Notification.FLAG_GROUP_SUMMARY or FLAG_AUTOGROUP_SUMMARY, emptySet()),
        )
    }

    @Test
    fun `a media session extra is a trait`() {
        assertEquals(
            setOf(NotificationTrait.MediaSession),
            notificationTraits(0, setOf(Notification.EXTRA_MEDIA_SESSION)),
        )
    }

    @Test
    fun `a foreground media notification has every matching trait`() {
        val flags =
            Notification.FLAG_ONGOING_EVENT or Notification.FLAG_FOREGROUND_SERVICE or Notification.FLAG_NO_CLEAR

        assertEquals(
            setOf(
                NotificationTrait.Ongoing,
                NotificationTrait.ForegroundService,
                NotificationTrait.NoClear,
                NotificationTrait.MediaSession,
            ),
            notificationTraits(flags, setOf(Notification.EXTRA_MEDIA_SESSION)),
        )
    }
}
