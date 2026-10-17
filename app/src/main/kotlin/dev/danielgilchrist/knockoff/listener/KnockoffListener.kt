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
import dev.danielgilchrist.knockoff.chat.ChatRecord
import dev.danielgilchrist.knockoff.chat.SeenChatsLoad
import dev.danielgilchrist.knockoff.domain.change.Change
import dev.danielgilchrist.knockoff.domain.change.next
import dev.danielgilchrist.knockoff.domain.chat.ChatSighting
import dev.danielgilchrist.knockoff.domain.chat.SeenChats
import dev.danielgilchrist.knockoff.domain.notification.PostedNotification
import dev.danielgilchrist.knockoff.domain.rule.Evaluation
import dev.danielgilchrist.knockoff.settings.SettingsLoad
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
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

    @Volatile
    private var applied: Change<Evaluation> = Change.Pending

    private val knockoff
        get() = application as KnockoffApplication

    private val settingsStore
        get() = knockoff.settingsStore

    private val silencer by lazy {
        ChannelSilencer(
            ChannelAccess(this),
            BoundaryAlarm(this),
            settingsStore,
            knockoff.silencedChannelsStore,
            knockoff.seenChatsStore,
        )
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

                    val seen =
                        knockoff.seenChatsStore.loads
                            .scan(SeenChats.NONE) { last, load ->
                                when (load) {
                                    is SeenChatsLoad.Loaded -> {
                                        load.seen
                                    }

                                    is SeenChatsLoad.Corrupt -> {
                                        SeenChats.NONE
                                    }

                                    is SeenChatsLoad.ReadFailed -> {
                                        Log.w(LOG_TAG, "seen chats unreadable, keeping the last: ${load.detail}")
                                        last
                                    }
                                }
                            }.drop(1)

                    combine(rules, seen, deviceZones(this@KnockoffListener)) { ruleSet, chats, zone ->
                        Evaluation(chats.expand(ruleSet), zone)
                    }.distinctUntilChanged()
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

    override fun onNotificationPosted(
        sbn: StatusBarNotification,
        rankingMap: RankingMap,
    ) {
        val posted = sbn.toSnapshot().parse()

        scope.launch { record(ChatSighting.of(posted, sbn.shortcutLabels(rankingMap))) }
        scope.launch {
            when (val change = applied) {
                Change.Pending -> evaluateBeforeConnected(sbn, posted)
                is Change.Initial -> sweeper.evaluate(sbn, posted, change.value)
                is Change.Changed -> sweeper.evaluate(sbn, posted, change.after)
            }
        }
    }

    private suspend fun evaluateBeforeConnected(
        sbn: StatusBarNotification,
        posted: PostedNotification,
    ) {
        when (val load = settingsStore.loads.first()) {
            is SettingsLoad.Usable -> {
                sweeper.evaluate(
                    sbn,
                    posted,
                    Evaluation(currentSeen().expand(load.settings.rules), ZoneId.systemDefault()),
                )
            }

            is SettingsLoad.Failure -> {
                Log.e(LOG_TAG, "key=${sbn.key} not evaluated, settings failed to load: ${load.detail}")
            }
        }
    }

    private suspend fun currentSeen(): SeenChats =
        when (val load = knockoff.seenChatsStore.loads.first()) {
            is SeenChatsLoad.Loaded -> {
                load.seen
            }

            is SeenChatsLoad.Corrupt -> {
                SeenChats.NONE
            }

            is SeenChatsLoad.ReadFailed -> {
                Log.w(LOG_TAG, "seen chats unreadable, deciding without them: ${load.detail}")
                SeenChats.NONE
            }
        }

    private suspend fun record(sighting: ChatSighting) {
        when (sighting) {
            ChatSighting.NotAChat -> {}

            is ChatSighting.Seen -> {
                when (val record = knockoff.seenChatsStore.record(sighting.chat)) {
                    ChatRecord.Recorded, ChatRecord.Unchanged -> {}

                    is ChatRecord.Failed -> {
                        Log.w(LOG_TAG, "${sighting.chat.packageName.value} chat not recorded: ${record.detail}")
                    }
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
