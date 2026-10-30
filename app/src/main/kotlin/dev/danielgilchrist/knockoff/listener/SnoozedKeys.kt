package dev.danielgilchrist.knockoff.listener

import dev.danielgilchrist.knockoff.domain.notification.NotificationKey
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.TimeSource

internal class SnoozedKeys(
    private val read: () -> SnoozedRead,
) {
    private val mutex = Mutex()
    private var latest: SnoozedSnapshot = SnoozedSnapshot.None

    suspend fun lookup(
        key: NotificationKey,
        since: TimeSource.Monotonic.ValueTimeMark,
    ): SnoozedLookup =
        mutex.withLock {
            when (val snapshot = latest) {
                is SnoozedSnapshot.Taken -> if (snapshot.at >= since) snapshot.lookup(key) else readAndLookup(key)
                SnoozedSnapshot.None -> readAndLookup(key)
            }
        }

    private fun readAndLookup(key: NotificationKey): SnoozedLookup {
        val at = TimeSource.Monotonic.markNow()
        val snapshot =
            when (val snoozed = read()) {
                is SnoozedRead.Keys -> SnoozedSnapshot.Read(at, snoozed.keys)
                is SnoozedRead.Failed -> SnoozedSnapshot.Unreadable(at, snoozed.detail)
            }

        latest = snapshot

        return snapshot.lookup(key)
    }
}
