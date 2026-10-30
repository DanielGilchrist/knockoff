package dev.danielgilchrist.knockoff.listener

import dev.danielgilchrist.knockoff.domain.notification.NotificationKey

internal sealed interface SnoozedRead {
    data class Keys(
        val keys: Set<NotificationKey>,
    ) : SnoozedRead

    data class Failed(
        val detail: String,
    ) : SnoozedRead
}
