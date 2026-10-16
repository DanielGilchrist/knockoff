package dev.danielgilchrist.knockoff.channel

import android.util.Log
import dev.danielgilchrist.knockoff.LOG_TAG
import dev.danielgilchrist.knockoff.domain.channel.ChannelChange
import dev.danielgilchrist.knockoff.domain.channel.ChannelKey
import dev.danielgilchrist.knockoff.domain.channel.ReadChannel
import dev.danielgilchrist.knockoff.domain.channel.SilencedChannels
import dev.danielgilchrist.knockoff.domain.notification.PackageName
import dev.danielgilchrist.knockoff.domain.rule.Boundary
import dev.danielgilchrist.knockoff.domain.rule.RuleSet
import dev.danielgilchrist.knockoff.domain.rule.Target
import dev.danielgilchrist.knockoff.settings.SettingsLoad
import dev.danielgilchrist.knockoff.settings.SettingsStore
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import java.time.ZonedDateTime
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

internal class ChannelSilencer(
    private val access: ChannelAccess,
    private val alarm: BoundaryAlarm,
    private val settings: SettingsStore,
    private val store: SilencedChannelsStore,
) {
    companion object {
        private val RESTORE_GRACE = 5.seconds
        private val RETRY = 15.minutes.toJavaDuration()
    }

    private val requests = Channel<Unit>(Channel.CONFLATED)
    private val chatChannels = ChatChannels(access)

    fun request() {
        requests.trySend(Unit)
    }

    suspend fun run() {
        for (request in requests) {
            pass()
        }
    }

    private suspend fun pass() {
        when (val first = prepare()) {
            is Preparation.Unavailable -> {
                Log.w(LOG_TAG, "channels unchanged: ${first.detail}")
            }

            is Preparation.Failed -> {
                Log.e(LOG_TAG, "channels unchanged: ${first.detail}")
                val now = ZonedDateTime.now()

                alarm.schedule(Boundary.None.noLaterThan(now.plus(RETRY)), now).report()
            }

            is Preparation.Ready -> {
                val silencing = apply(first.plan.silence)
                val settlement =
                    when {
                        first.plan.restore.isEmpty() -> silencing
                        else -> silencing.and(finish())
                    }

                if (first.plan.restore.isNotEmpty() && settlement == Settlement.Settled &&
                    first.waitingChats.isNotEmpty()
                ) {
                    request()
                }

                schedule(first, settlement)
            }
        }
    }

    private suspend fun finish(): Settlement {
        delay(RESTORE_GRACE)

        return when (val second = prepare()) {
            is Preparation.Unavailable -> Settlement.Unsettled(second.detail)
            is Preparation.Failed -> Settlement.Unsettled(second.detail)
            is Preparation.Ready -> apply(second.plan.silence + second.plan.restore)
        }
    }

    private fun schedule(
        ready: Preparation.Ready,
        settlement: Settlement,
    ) {
        val boundary = ready.rules.nextBoundary(ready.now)
        val alarmAt =
            when (settlement) {
                Settlement.Settled -> {
                    boundary
                }

                is Settlement.Unsettled -> {
                    Log.e(LOG_TAG, "channels unsettled, retrying: ${settlement.detail}")
                    boundary.noLaterThan(ready.now.plus(RETRY))
                }
            }

        alarm.schedule(alarmAt, ZonedDateTime.now()).report()
    }

    private suspend fun prepare(): Preparation =
        when (val load = settings.loads.first()) {
            is SettingsLoad.Failure -> Preparation.Failed("settings failed to load: ${load.detail}")
            is SettingsLoad.Usable -> withRules(load.settings.rules)
        }

    private suspend fun withRules(rules: RuleSet): Preparation =
        when (val load = store.load()) {
            is SilencedChannelsLoad.Failed -> Preparation.Failed("silenced channels failed to load: ${load.detail}")
            is SilencedChannelsLoad.Loaded -> withSilenced(rules, load.silenced)
        }

    private suspend fun withSilenced(
        rules: RuleSet,
        silenced: SilencedChannels,
    ): Preparation {
        val packages = rules.channelPackages(silenced)

        return read(packages) { channels ->
            val chats = chatChannels.create(rules.chatChannelNeeds(channels, silenced))

            when (chats.creation) {
                ChatChannelsCreation.SomeCreated -> {
                    read(packages) { created -> planned(rules, silenced, created, chats.waiting) }
                }

                ChatChannelsCreation.NoneCreated -> {
                    planned(rules, silenced, channels, chats.waiting)
                }
            }
        }
    }

    private inline fun read(
        packages: Set<PackageName>,
        onRead: (Map<ChannelKey, ReadChannel>) -> Preparation,
    ): Preparation =
        when (val read = access.read(packages)) {
            is ChannelRead.NotLinked -> Preparation.Unavailable("not linked to a companion device: ${read.detail}")
            ChannelRead.Unbound -> Preparation.Unavailable("listener unbound")
            is ChannelRead.Read -> onRead(read.channels)
        }

    private suspend fun planned(
        rules: RuleSet,
        silenced: SilencedChannels,
        read: Map<ChannelKey, ReadChannel>,
        waitingChats: Set<Target.Chat>,
    ): Preparation {
        val now = ZonedDateTime.now()
        val plan = rules.planChannels(read, silenced, now)

        return when {
            plan.silenced == silenced -> {
                Preparation.Ready(rules, plan, now, waitingChats)
            }

            else -> {
                when (val save = store.save(plan.silenced)) {
                    SilencedChannelsSave.Saved -> {
                        Preparation.Ready(rules, plan, now, waitingChats)
                    }

                    is SilencedChannelsSave.Failed -> {
                        Preparation.Failed(
                            "saving silenced channels failed: ${save.detail}",
                        )
                    }
                }
            }
        }
    }

    private fun apply(changes: List<ChannelChange>): Settlement =
        changes.fold<ChannelChange, Settlement>(Settlement.Settled) { settlement, change ->
            settlement.and(access.apply(change).settle(change))
        }
}
