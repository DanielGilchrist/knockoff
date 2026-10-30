package dev.danielgilchrist.knockoff.listener

import dev.danielgilchrist.knockoff.domain.notification.NotificationKey
import kotlin.time.TimeSource

internal sealed interface SnoozedSnapshot {
    data object None : SnoozedSnapshot

    data class Taken(
        val at: TimeSource.Monotonic.ValueTimeMark,
        val keys: Set<NotificationKey>,
    ) : SnoozedSnapshot {
        fun lookup(key: NotificationKey): SnoozedLookup =
            when (key) {
                in keys -> SnoozedLookup.Snoozed
                else -> SnoozedLookup.NotSnoozed
            }
    }
}
