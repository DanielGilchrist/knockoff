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
import dev.danielgilchrist.knockoff.domain.change.Change
import dev.danielgilchrist.knockoff.domain.change.next
import dev.danielgilchrist.knockoff.domain.rule.Evaluation
import dev.danielgilchrist.knockoff.settings.SettingsLoad
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.ZoneId

class KnockoffListener : NotificationListenerService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val sweeper = NotificationSweeper(this, scope)
    private var connection: Connection = Connection.Idle
    private var applied: Change<Evaluation> = Change.Pending

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
        scope.launch { knockoff.silencingRequests.collect { silencer.request() } }
    }

    override fun onListenerConnected() {
        Log.i(LOG_TAG, "listener connected")
        val previous = connection
        connection =
            Connection.Connected(
                scope.launch {
                    withContext(NonCancellable) { previous.join() }
                    val rules =
                        settingsStore.loads.transform { load ->
                            when (load) {
                                is SettingsLoad.Usable -> {
                                    emit(load.settings.rules)
                                }

                                is SettingsLoad.Failure -> {
                                    Log.e(LOG_TAG, "sweep skipped, settings failed to load: ${load.detail}")
                                }
                            }
                        }

                    combine(rules, deviceZones(this@KnockoffListener), ::Evaluation)
                        .scan(applied) { change, evaluation -> change.next(evaluation) }
                        .drop(1)
                        .collect(::apply)
                },
            )
    }

    override fun onListenerDisconnected() {
        connection = connection.disconnected()
        Log.w(LOG_TAG, "listener disconnected")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        scope.launch {
            when (val load = settingsStore.loads.first()) {
                is SettingsLoad.Usable -> {
                    sweeper.evaluate(sbn, Evaluation(load.settings.rules, ZoneId.systemDefault()))
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

    private fun apply(change: Change<Evaluation>) {
        applied = change

        when (change) {
            Change.Pending -> {}

            is Change.Initial -> {
                sweeper.sweep(change.value)
                silencer.request()
            }

            is Change.Changed -> {
                sweeper.sweep(change.after)
                sweeper.recheckSnoozed(change.before, change.after)
                silencer.request()
            }
        }
    }
}
