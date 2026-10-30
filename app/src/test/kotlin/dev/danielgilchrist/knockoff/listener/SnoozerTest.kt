package dev.danielgilchrist.knockoff.listener

import android.service.notification.StatusBarNotification
import dev.danielgilchrist.knockoff.domain.notification.NotificationKey
import dev.danielgilchrist.knockoff.domain.notification.OriginGroup
import dev.danielgilchrist.knockoff.system.SystemCall
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Duration

@OptIn(ExperimentalCoroutinesApi::class)
class SnoozerTest {
    private val key = NotificationKey("0|com.Slack|1|null|10001")
    private val hour = Duration.ofHours(1)

    @Test
    fun `a snooze that doesn't show up is asked for again until it does`() =
        runTest {
            val calls = FakeCalls(snoozedAfter = 2)

            assertEquals(SnoozeOutcome.Confirmed, Snoozer(calls).snoozed(key, hour))
            assertEquals(2, calls.snoozes)
        }

    @Test
    fun `a snooze that never shows up stops after its retries`() =
        runTest {
            val calls = FakeCalls(snoozedAfter = Int.MAX_VALUE)

            assertEquals(
                SnoozeOutcome.Unconfirmed("not in the snoozed list after 1s"),
                Snoozer(calls).snoozed(key, hour),
            )
            assertEquals(3, calls.snoozes)
            assertEquals(3_000L + 2_000L + 5_000L, currentTime)
        }

    @Test
    fun `a refused snooze isn't retried`() =
        runTest {
            val calls = FakeCalls(snoozedAfter = 1, outcome = SystemCall.Refused("not allowed"))

            assertEquals(SnoozeOutcome.Rejected("not allowed"), Snoozer(calls).snoozed(key, hour))
            assertEquals(1, calls.snoozes)
        }

    @Test
    fun `a busy system is retried for a snooze, a re-snooze and a release`() =
        runTest {
            val busy = FakeCalls(snoozedAfter = 1, outcome = SystemCall.Unavailable("busy"), busyCalls = 1)

            assertEquals(SnoozeOutcome.Confirmed, Snoozer(busy).snoozed(key, hour))
            assertEquals(
                ResnoozeOutcome.Requested,
                Snoozer(FakeCalls(1, SystemCall.Unavailable("busy"), busyCalls = 1)).resnoozed(key, hour),
            )
            assertEquals(
                ResnoozeOutcome.Unavailable("busy"),
                Snoozer(FakeCalls(1, SystemCall.Unavailable("busy"), busyCalls = 9)).resnoozed(key, hour),
            )
        }

    @Test
    fun `a release that leaves it snoozed is asked for again`() =
        runTest {
            val calls = FakeCalls(snoozedAfter = 0, releasedAfter = 2)

            assertEquals(ReleaseOutcome.Released, Snoozer(calls).released(key))
            assertEquals(2, calls.snoozes)
        }

    private inner class FakeCalls(
        private val snoozedAfter: Int,
        private val outcome: SystemCall<Unit> = SystemCall.Done(Unit),
        private val busyCalls: Int = 0,
        private val releasedAfter: Int = Int.MAX_VALUE,
    ) : NotificationCalls {
        var snoozes = 0

        override fun read(list: NotificationList): NotificationRead = NotificationRead.Read(emptyList())

        override fun snoozedKeys(): SnoozedRead =
            SnoozedRead.Keys(if (snoozes >= snoozedAfter && snoozes < releasedAfter) setOf(key) else emptySet())

        override fun groups(): (StatusBarNotification) -> OriginGroup = { OriginGroup.Unknown }

        override fun snooze(
            key: NotificationKey,
            millis: Long,
        ): SystemCall<Unit> {
            snoozes++

            return when {
                snoozes <= busyCalls -> outcome
                outcome is SystemCall.Refused -> outcome
                else -> SystemCall.Done(Unit)
            }
        }

        override fun cancel(key: NotificationKey): SystemCall<Unit> = SystemCall.Done(Unit)
    }
}
