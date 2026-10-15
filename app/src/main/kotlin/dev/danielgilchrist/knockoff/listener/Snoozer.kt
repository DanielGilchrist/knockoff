package dev.danielgilchrist.knockoff.listener

import android.service.notification.NotificationListenerService
import android.util.Log
import dev.danielgilchrist.knockoff.LOG_TAG
import dev.danielgilchrist.knockoff.domain.notification.NotificationKey
import kotlinx.coroutines.delay
import java.time.Duration
import kotlin.time.Duration.Companion.seconds

internal class Snoozer(
    private val listener: NotificationListenerService,
) {
    companion object {
        private val CONFIRMATION_DELAY = 1.seconds
        private const val RELEASE_AFTER_MILLIS = 1L
    }

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
        try {
            listener.snoozeNotification(key.value, duration.toMillis())
            delay(CONFIRMATION_DELAY)
            when (val lookup = lookup(key)) {
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
        } catch (exception: SecurityException) {
            SnoozeOutcome.Rejected(exception.toString())
        }

    private fun attemptResnooze(
        key: NotificationKey,
        duration: Duration,
    ): ResnoozeOutcome =
        try {
            listener.snoozeNotification(key.value, duration.toMillis())
            ResnoozeOutcome.Requested
        } catch (exception: SecurityException) {
            ResnoozeOutcome.Rejected(exception.toString())
        }

    private suspend fun attemptRelease(key: NotificationKey): ReleaseOutcome =
        try {
            listener.snoozeNotification(key.value, RELEASE_AFTER_MILLIS)
            delay(CONFIRMATION_DELAY)
            when (val lookup = lookup(key)) {
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
        } catch (exception: SecurityException) {
            ReleaseOutcome.Rejected(exception.toString())
        }

    private fun lookup(key: NotificationKey): SnoozedLookup =
        when (val read = listener.read(NotificationList.Snoozed)) {
            is NotificationRead.Read -> {
                when {
                    read.notifications.any { it.key == key.value } -> SnoozedLookup.Snoozed
                    else -> SnoozedLookup.NotSnoozed
                }
            }

            is NotificationRead.Failure -> {
                SnoozedLookup.Unknown(read.detail)
            }
        }
}
