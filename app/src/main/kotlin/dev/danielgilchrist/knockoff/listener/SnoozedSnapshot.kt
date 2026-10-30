package dev.danielgilchrist.knockoff.listener

import dev.danielgilchrist.knockoff.domain.notification.NotificationKey
import kotlin.time.TimeSource

internal sealed interface SnoozedSnapshot {
    data object None : SnoozedSnapshot

    sealed interface Taken : SnoozedSnapshot {
        val at: TimeSource.Monotonic.ValueTimeMark

        fun lookup(key: NotificationKey): SnoozedLookup
    }

    data class Read(
        override val at: TimeSource.Monotonic.ValueTimeMark,
        val keys: Set<NotificationKey>,
    ) : Taken {
        override fun lookup(key: NotificationKey): SnoozedLookup =
            when (key) {
                in keys -> SnoozedLookup.Snoozed
                else -> SnoozedLookup.NotSnoozed
            }
    }

    data class Unreadable(
        override val at: TimeSource.Monotonic.ValueTimeMark,
        val detail: String,
    ) : Taken {
        override fun lookup(key: NotificationKey): SnoozedLookup = SnoozedLookup.Unknown(detail)
    }
}
