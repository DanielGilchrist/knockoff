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
import dev.danielgilchrist.knockoff.channel.GroupDiscoverer
import dev.danielgilchrist.knockoff.channel.GroupLearning
import dev.danielgilchrist.knockoff.channel.GroupNames
import dev.danielgilchrist.knockoff.chat.SeenChatsLoad
import dev.danielgilchrist.knockoff.domain.chat.ChatSighting
import dev.danielgilchrist.knockoff.domain.chat.SeenChats
import dev.danielgilchrist.knockoff.domain.notification.PostedNotification
import dev.danielgilchrist.knockoff.domain.rule.Evaluation
import dev.danielgilchrist.knockoff.domain.settings.AppliedRules
import dev.danielgilchrist.knockoff.settings.SettingsLoad
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.ZoneId

class KnockoffListener : NotificationListenerService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val sweeper = NotificationSweeper(this, scope) { knockoff.knownGroupsStore.current() }
    private var connection: Connection = Connection.Idle

    private val applier by lazy {
        RulesApplier(sweeper, silencer::request, knockoff.appliedRulesStore) { Log.i(LOG_TAG, it) }
    }

    private val applied: Applied
        get() = applier.applied

    private val knockoff
        get() = application as KnockoffApplication

    private val settingsStore
        get() = knockoff.settingsStore

    private val access by lazy { ChannelAccess(this) }

    private val learning by lazy { GroupLearning(GroupNames(this), knockoff.knownGroupsStore) }

    private val discoverer by lazy { GroupDiscoverer(access, learning) }

    private val silencer by lazy {
        ChannelSilencer(
            access,
            BoundaryAlarm(this),
            settingsStore,
            knockoff.silencedChannelsStore,
            knockoff.seenChatsStore,
            learning,
            knockoff.holidayWatch,
        )
    }

    override fun onCreate() {
        super.onCreate()
        scope.launch { silencer.run() }
        scope.launch { knockoff.silencingRequests.collect { silencer.request() } }
        scope.launch { knockoff.boundaries.collect { sweepAtBoundary() } }
    }

    override fun onListenerConnected() {
        Log.i(LOG_TAG, "listener connected")
        knockoff.groupReach.bind(discoverer)
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

                    combine(
                        rules,
                        seen,
                        deviceZones(this@KnockoffListener),
                        knockoff.holidayWatch.holidays,
                    ) { ruleSet, chats, zone, holidays ->
                        Applying(AppliedRules(ruleSet, zone), chats, holidays)
                    }.distinctUntilChangedBy(Applying::evaluation)
                        .collect { applying ->
                            applier.apply(applying)
                        }
                },
            )
    }

    override fun onListenerDisconnected() {
        knockoff.groupReach.unbind(discoverer)
        connection = connection.disconnected()
        Log.w(LOG_TAG, "listener disconnected")
    }

    override fun onNotificationPosted(
        sbn: StatusBarNotification,
        rankingMap: RankingMap,
    ) {
        val posted = sbn.toSnapshot(rankingMap.groupOf(sbn)).parse()

        scope.launch { knockoff.seenChatsStore.record(ChatSighting.of(posted, sbn.shortcutLabels(rankingMap))) }
        scope.launch { knockoff.knownGroupsStore.record(posted.origin) }
        scope.launch {
            when (val current = applied) {
                Applied.Pending -> evaluateBeforeConnected(sbn, posted)
                is Applied.Active -> sweeper.evaluate(sbn, posted, current.evaluation)
            }
        }
    }

    override fun onNotificationRemoved(
        sbn: StatusBarNotification,
        rankingMap: RankingMap,
        reason: Int,
    ) {
        val revisit = SummaryRevisit.afterRemoval(reason)

        when (val current = applied) {
            Applied.Pending -> {}

            is Applied.Active -> {
                sweeper.revisitSummaries(sbn, revisit, current.evaluation)
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
                    Evaluation(
                        currentSeen().expand(load.settings.rules),
                        ZoneId.systemDefault(),
                        knockoff.holidayWatch.current(),
                    ),
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

    override fun onNotificationChannelModified(
        pkg: String,
        user: UserHandle,
        channel: NotificationChannel,
        modificationType: Int,
    ) {
        silencer.request()
    }

    override fun onDestroy() {
        knockoff.groupReach.unbind(discoverer)
        scope.cancel()
        super.onDestroy()
    }

    private fun sweepAtBoundary() {
        Log.i(LOG_TAG, "boundary reached")

        when (val current = applied) {
            Applied.Pending -> {}

            is Applied.Active -> {
                sweeper.sweep(current.evaluation)
            }
        }
    }
}
