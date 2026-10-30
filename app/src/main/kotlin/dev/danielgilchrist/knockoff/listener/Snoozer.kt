package dev.danielgilchrist.knockoff.listener

import android.util.Log
import dev.danielgilchrist.knockoff.LOG_TAG
import dev.danielgilchrist.knockoff.domain.notification.NotificationKey
import dev.danielgilchrist.knockoff.system.SystemCall
import kotlinx.coroutines.delay
import java.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

internal class Snoozer(
    private val calls: NotificationCalls,
) {
    companion object {
        private val CONFIRMATION_DELAY = 1.seconds
        private const val RELEASE_AFTER_MILLIS = 1L
    }

    private val snoozed = SnoozedKeys(::readSnoozed)

    suspend fun snooze(
        key: NotificationKey,
        duration: Duration,
    ) {
        when (val outcome = attemptSnooze(key, duration)) {
            SnoozeOutcome.Confirmed -> Log.i(LOG_TAG, "key=${key.value} snooze confirmed")
            is SnoozeOutcome.Rejected -> Log.e(LOG_TAG, "key=${key.value} snooze rejected: ${outcome.detail}")
            is SnoozeOutcome.Unconfirmed -> Log.w(LOG_TAG, "key=${key.value} snooze unconfirmed: ${outcome.detail}")
        }
    }

    fun resnooze(
        key: NotificationKey,
        duration: Duration,
    ) {
        when (val outcome = attemptResnooze(key, duration)) {
            ResnoozeOutcome.Requested -> Log.i(LOG_TAG, "key=${key.value} re-snooze requested")
            is ResnoozeOutcome.Rejected -> Log.e(LOG_TAG, "key=${key.value} re-snooze rejected: ${outcome.detail}")
            is ResnoozeOutcome.Unavailable -> Log.w(LOG_TAG, "key=${key.value} re-snooze unsent: ${outcome.detail}")
        }
    }

    suspend fun release(key: NotificationKey) {
        when (val outcome = attemptRelease(key)) {
            ReleaseOutcome.Released -> Log.i(LOG_TAG, "key=${key.value} released")
            is ReleaseOutcome.Rejected -> Log.e(LOG_TAG, "key=${key.value} release rejected: ${outcome.detail}")
            is ReleaseOutcome.Unconfirmed -> Log.w(LOG_TAG, "key=${key.value} release unconfirmed: ${outcome.detail}")
        }
    }

    private suspend fun attemptSnooze(
        key: NotificationKey,
        duration: Duration,
    ): SnoozeOutcome =
        when (val call = calls.snooze(key, duration.toMillis())) {
            is SystemCall.Refused -> {
                SnoozeOutcome.Rejected(call.detail)
            }

            is SystemCall.Unavailable -> {
                SnoozeOutcome.Unconfirmed(call.detail)
            }

            is SystemCall.Done -> {
                delay(CONFIRMATION_DELAY)

                when (val lookup = snoozed.lookup(key, TimeSource.Monotonic.markNow())) {
                    SnoozedLookup.Snoozed -> {
                        SnoozeOutcome.Confirmed
                    }

                    SnoozedLookup.NotSnoozed -> {
                        SnoozeOutcome.Unconfirmed(
                            "not in the snoozed list after $CONFIRMATION_DELAY",
                        )
                    }

                    is SnoozedLookup.Unknown -> {
                        SnoozeOutcome.Unconfirmed(lookup.detail)
                    }
                }
            }
        }

    private fun attemptResnooze(
        key: NotificationKey,
        duration: Duration,
    ): ResnoozeOutcome =
        when (val call = calls.snooze(key, duration.toMillis())) {
            is SystemCall.Done -> ResnoozeOutcome.Requested
            is SystemCall.Refused -> ResnoozeOutcome.Rejected(call.detail)
            is SystemCall.Unavailable -> ResnoozeOutcome.Unavailable(call.detail)
        }

    private suspend fun attemptRelease(key: NotificationKey): ReleaseOutcome =
        when (val call = calls.snooze(key, RELEASE_AFTER_MILLIS)) {
            is SystemCall.Refused -> {
                ReleaseOutcome.Rejected(call.detail)
            }

            is SystemCall.Unavailable -> {
                ReleaseOutcome.Unconfirmed(call.detail)
            }

            is SystemCall.Done -> {
                delay(CONFIRMATION_DELAY)

                when (val lookup = snoozed.lookup(key, TimeSource.Monotonic.markNow())) {
                    SnoozedLookup.NotSnoozed -> {
                        ReleaseOutcome.Released
                    }

                    SnoozedLookup.Snoozed -> {
                        ReleaseOutcome.Unconfirmed(
                            "still in the snoozed list after $CONFIRMATION_DELAY",
                        )
                    }

                    is SnoozedLookup.Unknown -> {
                        ReleaseOutcome.Unconfirmed(lookup.detail)
                    }
                }
            }
        }

    private fun readSnoozed(): SnoozedRead =
        when (val read = calls.read(NotificationList.Snoozed)) {
            is NotificationRead.Read -> SnoozedRead.Keys(read.notifications.map { NotificationKey(it.key) }.toSet())
            is NotificationRead.Failure -> SnoozedRead.Failed(read.detail)
        }
}
