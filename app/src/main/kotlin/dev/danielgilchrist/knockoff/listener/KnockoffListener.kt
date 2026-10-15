package dev.danielgilchrist.knockoff.listener

import android.app.NotificationChannel
import android.os.UserHandle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import dev.danielgilchrist.knockoff.KnockoffApplication
import dev.danielgilchrist.knockoff.LOG_TAG
import dev.danielgilchrist.knockoff.channel.BoundaryAlarm
import dev.danielgilchrist.knockoff.channel.ChannelAccess
import dev.danielgilchrist.knockoff.channel.ChannelSilencer
import dev.danielgilchrist.knockoff.domain.notification.PostedNotification
import dev.danielgilchrist.knockoff.domain.rule.Decision
import dev.danielgilchrist.knockoff.domain.rule.Recheck
import dev.danielgilchrist.knockoff.domain.rule.RuleChange
import dev.danielgilchrist.knockoff.domain.rule.RuleSet
import dev.danielgilchrist.knockoff.settings.SettingsLoad
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.launch
import java.time.ZonedDateTime

class KnockoffListener : NotificationListenerService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val snoozer = Snoozer(this)
    private var connection: Connection = Connection.Disconnected

    private val knockoff
        get() = application as KnockoffApplication

    private val settingsStore
        get() = knockoff.settingsStore

    private val silencer by lazy {
        ChannelSilencer(ChannelAccess(this), BoundaryAlarm(this), settingsStore, knockoff.silencedChannelsStore)
    }

    override fun onCreate() {
        super.onCreate()
        scope.launch { silencer.run() }
        scope.launch { knockoff.boundaries.collect { silencer.request() } }
    }

    override fun onListenerConnected() {
        Log.i(LOG_TAG, "listener connected")
        connection.stop()
        connection =
            Connection.Connected(
                scope.launch {
                    settingsStore.loads
                        .transform { load ->
                            when (load) {
                                is SettingsLoad.Usable -> {
                                    emit(load.settings.rules)
                                }

                                is SettingsLoad.Failure -> {
                                    Log.e(
                                        LOG_TAG,
                                        "sweep skipped, settings failed to load: ${load.detail}",
                                    )
                                }
                            }
                        }.scan<RuleSet, RuleChange>(RuleChange.Pending) { change, rules -> change.next(rules) }
                        .collect(::apply)
                },
            )
    }

    override fun onListenerDisconnected() {
        connection.stop()
        connection = Connection.Disconnected
        Log.w(LOG_TAG, "listener disconnected")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        scope.launch {
            when (val load = settingsStore.loads.first()) {
                is SettingsLoad.Usable -> {
                    evaluate(sbn, load.settings.rules)
                }

                is SettingsLoad.Failure -> {
                    Log.e(
                        LOG_TAG,
                        "key=${sbn.key} not evaluated, settings failed to load: ${load.detail}",
                    )
                }
            }
        }
    }

    override fun onNotificationChannelModified(
        pkg: String,
        user: UserHandle,
        channel: NotificationChannel,
        modificationType: Int,
    ) {
        silencer.request()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun apply(change: RuleChange) {
        when (change) {
            RuleChange.Pending -> {}

            is RuleChange.Initial -> {
                sweep(change.rules)
                silencer.request()
            }

            is RuleChange.Changed -> {
                sweep(change.after)
                recheckSnoozed(change.before, change.after)
                silencer.request()
            }
        }
    }

    private fun sweep(rules: RuleSet) {
        when (val read = read(NotificationList.Active)) {
            is NotificationRead.Read -> {
                Log.i(LOG_TAG, "sweeping ${read.notifications.size} active notifications")
                read.notifications.forEach { sbn -> scope.launch { evaluate(sbn, rules) } }
            }

            is NotificationRead.Failure -> {
                Log.e(LOG_TAG, "sweep skipped, active notifications unavailable: ${read.detail}")
            }
        }
    }

    private fun recheckSnoozed(
        before: RuleSet,
        after: RuleSet,
    ) {
        when (val read = read(NotificationList.Snoozed)) {
            is NotificationRead.Read -> {
                Log.i(LOG_TAG, "rechecking ${read.notifications.size} snoozed notifications")
                read.notifications.forEach { sbn -> scope.launch { recheck(sbn, before, after) } }
            }

            is NotificationRead.Failure -> {
                Log.e(LOG_TAG, "recheck skipped, snoozed notifications unavailable: ${read.detail}")
            }
        }
    }

    private suspend fun evaluate(
        sbn: StatusBarNotification,
        rules: RuleSet,
    ) {
        val now = ZonedDateTime.now()
        val posted = sbn.toSnapshot().parse()

        val decision = rules.decide(posted, now)

        Log.i(LOG_TAG, "${sbn.describe(posted)} -> ${decision.describe()}")

        when (decision) {
            is Decision.Leave -> {}

            is Decision.Snooze -> {
                snoozer.snooze(posted.key, decision.duration)
            }
        }
    }

    private suspend fun recheck(
        sbn: StatusBarNotification,
        before: RuleSet,
        after: RuleSet,
    ) {
        val now = ZonedDateTime.now()
        val snoozed = sbn.toSnapshot().parse()
        val recheck = after.recheck(snoozed, before, now)

        Log.i(LOG_TAG, "snoozed ${sbn.describe(snoozed)} -> ${recheck.describe()}")

        when (recheck) {
            Recheck.Unchanged -> {}

            is Recheck.Resnooze -> {
                snoozer.resnooze(snoozed.key, recheck.snooze.duration)
            }

            is Recheck.Release -> {
                snoozer.release(snoozed.key)
            }
        }
    }
}
