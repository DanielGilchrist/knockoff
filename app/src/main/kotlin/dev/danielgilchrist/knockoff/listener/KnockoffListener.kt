package dev.danielgilchrist.knockoff.listener

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import dev.danielgilchrist.knockoff.KnockoffApplication
import dev.danielgilchrist.knockoff.LOG_TAG
import dev.danielgilchrist.knockoff.domain.notification.NotificationKey
import dev.danielgilchrist.knockoff.domain.notification.PostedNotification
import dev.danielgilchrist.knockoff.domain.rule.Decision
import dev.danielgilchrist.knockoff.domain.settings.Settings
import dev.danielgilchrist.knockoff.settings.SettingsLoad
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.ZonedDateTime
import kotlin.time.Duration.Companion.seconds

class KnockoffListener : NotificationListenerService() {
    companion object {
        private val CONFIRMATION_DELAY = 1.seconds
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var sweeps: Job? = null

    private val settingsStore
        get() = (application as KnockoffApplication).settingsStore

    override fun onListenerConnected() {
        Log.i(LOG_TAG, "listener connected")
        sweeps?.cancel()
        sweeps =
            scope.launch {
                settingsStore.loads.collect(::sweep)
                Log.e(LOG_TAG, "settings stopped updating, sweeps resume on the next connect")
            }
    }

    override fun onListenerDisconnected() {
        sweeps?.cancel()
        sweeps = null
        Log.w(LOG_TAG, "listener disconnected")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        scope.launch { evaluate(sbn, settingsStore.loads.first()) }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun sweep(load: SettingsLoad) {
        val active: Array<StatusBarNotification>? =
            try {
                activeNotifications
            } catch (exception: SecurityException) {
                Log.e(LOG_TAG, "reading active notifications was refused: $exception")
                return
            }

        if (active == null) {
            Log.w(LOG_TAG, "sweep skipped, the system did not return the active notifications")
            return
        }

        Log.i(LOG_TAG, "sweeping ${active.size} active notifications")
        active.forEach { sbn -> scope.launch { evaluate(sbn, load) } }
    }

    private suspend fun evaluate(
        sbn: StatusBarNotification,
        load: SettingsLoad,
    ) {
        val now = ZonedDateTime.now()
        val posted = sbn.toSnapshot().parse()
        val subText = sbn.notification.extras.getCharSequence(Notification.EXTRA_SUB_TEXT)
        val described = "${posted.describe()} subText=$subText"

        val rules =
            when (load) {
                is SettingsLoad.Loaded -> {
                    load.settings.rules
                }

                SettingsLoad.Empty -> {
                    Settings.DEFAULT.rules
                }

                is SettingsLoad.Failure -> {
                    Log.e(LOG_TAG, "$described not evaluated, settings failed to load: ${load.detail}")
                    return
                }
            }

        act(posted, rules.decide(posted, now), described)
    }

    private suspend fun act(
        posted: PostedNotification,
        decision: Decision,
        described: String,
    ) {
        Log.i(LOG_TAG, "$described -> ${decision.describe()}")

        when (decision) {
            is Decision.Leave -> {}

            is Decision.Snooze -> {
                report(posted.key, snooze(posted.key, decision.duration))
            }
        }
    }

    private fun report(
        key: NotificationKey,
        outcome: SnoozeOutcome,
    ) {
        when (outcome) {
            SnoozeOutcome.Confirmed -> Log.i(LOG_TAG, "key=${key.value} snooze confirmed")
            is SnoozeOutcome.Rejected -> Log.e(LOG_TAG, "key=${key.value} snooze rejected: ${outcome.detail}")
            is SnoozeOutcome.Unconfirmed -> Log.w(LOG_TAG, "key=${key.value} snooze unconfirmed: ${outcome.detail}")
        }
    }

    private suspend fun snooze(
        key: NotificationKey,
        duration: Duration,
    ): SnoozeOutcome =
        try {
            snoozeNotification(key.value, duration.toMillis())
            delay(CONFIRMATION_DELAY)
            confirmation(key)
        } catch (exception: SecurityException) {
            SnoozeOutcome.Rejected(exception.toString())
        }

    private fun confirmation(key: NotificationKey): SnoozeOutcome =
        try {
            val snoozed: Array<StatusBarNotification>? = snoozedNotifications

            when {
                snoozed == null -> SnoozeOutcome.Unconfirmed("the system did not return the snoozed list")
                snoozed.any { it.key == key.value } -> SnoozeOutcome.Confirmed
                else -> SnoozeOutcome.Unconfirmed("not in the snoozed list after $CONFIRMATION_DELAY")
            }
        } catch (exception: SecurityException) {
            SnoozeOutcome.Unconfirmed(exception.toString())
        }
}
