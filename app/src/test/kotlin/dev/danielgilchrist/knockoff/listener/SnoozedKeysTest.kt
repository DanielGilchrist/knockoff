package dev.danielgilchrist.knockoff.listener

import dev.danielgilchrist.knockoff.domain.notification.NotificationKey
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.time.TimeSource

class SnoozedKeysTest {
    private val held = NotificationKey("0|com.Slack|1|null|10001")
    private val other = NotificationKey("0|com.Slack|2|null|10001")

    @Test
    fun `lookups asked for before a read share it`() =
        runTest {
            var reads = 0
            val snoozed =
                SnoozedKeys {
                    reads++
                    SnoozedRead.Keys(setOf(held))
                }
            val since = TimeSource.Monotonic.markNow()

            val lookups = (1..64).map { async { snoozed.lookup(if (it == 1) other else held, since) } }.awaitAll()

            assertEquals(1, reads)
            assertEquals(SnoozedLookup.NotSnoozed, lookups.first())
            assertEquals(List(63) { SnoozedLookup.Snoozed }, lookups.drop(1))
        }

    @Test
    fun `a lookup asked for after the last read reads again`() =
        runTest {
            var reads = 0
            val snoozed =
                SnoozedKeys {
                    reads++
                    SnoozedRead.Keys(if (reads == 1) emptySet() else setOf(held))
                }

            assertEquals(SnoozedLookup.NotSnoozed, snoozed.lookup(held, TimeSource.Monotonic.markNow()))
            assertEquals(SnoozedLookup.Snoozed, snoozed.lookup(held, TimeSource.Monotonic.markNow()))
            assertEquals(2, reads)
        }

    @Test
    fun `a failed read is shared with lookups waiting on it, and later lookups read again`() =
        runTest {
            var reads = 0
            val snoozed =
                SnoozedKeys {
                    reads++
                    if (reads == 1) SnoozedRead.Failed("binder full") else SnoozedRead.Keys(setOf(held))
                }
            val since = TimeSource.Monotonic.markNow()

            val waiting = (1..10).map { async { snoozed.lookup(held, since) } }.awaitAll()

            assertEquals(List(10) { SnoozedLookup.Unknown("binder full") }, waiting)
            assertEquals(SnoozedLookup.Snoozed, snoozed.lookup(held, TimeSource.Monotonic.markNow()))
            assertEquals(2, reads)
        }
}
