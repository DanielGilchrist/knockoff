package dev.danielgilchrist.knockoff.listener

import android.service.notification.StatusBarNotification
import android.util.Log
import dev.danielgilchrist.knockoff.LOG_TAG
import dev.danielgilchrist.knockoff.domain.change.Baseline
import dev.danielgilchrist.knockoff.domain.group.KnownGroups
import dev.danielgilchrist.knockoff.domain.notification.GroupRole
import dev.danielgilchrist.knockoff.domain.notification.OriginGroup
import dev.danielgilchrist.knockoff.domain.notification.PostedNotification
import dev.danielgilchrist.knockoff.domain.rule.Decision
import dev.danielgilchrist.knockoff.domain.rule.Evaluation
import dev.danielgilchrist.knockoff.domain.rule.Recheck
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.Instant

internal class NotificationSweeper(
    private val calls: NotificationCalls,
    private val scope: CoroutineScope,
    private val knownGroups: suspend () -> KnownGroups,
) : HeldNotifications {
    private val snoozer = Snoozer(calls)
    private val entries = ActiveEntries(calls)
    private val holder = NotificationHolder(snoozer, SummaryDismisser(calls), entries::groups)
    private val revisits = SummaryRevisits(scope, revisit = ::revisitBatch)
    private val sweeps =
        MutableSharedFlow<Evaluation>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST).also {
            scope.launch { it.collectLatest(::sweepNow) }
        }

    override fun sweep(evaluation: Evaluation) {
        sweeps.tryEmit(evaluation)
    }

    override suspend fun recheckSnoozed(
        before: Baseline.Known<Evaluation>,
        after: Evaluation,
    ): RecheckStart =
        when (val read = recovering { calls.read(NotificationList.Snoozed) }) {
            is NotificationRead.Read -> {
                Log.i(LOG_TAG, "rechecking ${read.notifications.size} snoozed notifications")
                val active = lazy { entries.groups() }

                RecheckStart.Started(
                    scope.launch {
                        val known = knownGroups()

                        read.notifications.forEach { sbn -> launch { recheck(sbn, known, active, before, after) } }
                    },
                )
            }

            is NotificationRead.Failure -> {
                Log.e(LOG_TAG, "recheck skipped, snoozed notifications unavailable: ${read.detail}")
                RecheckStart.Skipped(read.detail)
            }
        }

    suspend fun evaluate(
        sbn: StatusBarNotification,
        posted: PostedNotification,
        evaluation: Evaluation,
    ) {
        val entry = GroupEntry.of(sbn, posted)

        decide(entry, entry.role(entries::groups), evaluation)
    }

    fun revisitSummaries(
        removed: StatusBarNotification,
        revisit: SummaryRevisit,
        evaluation: Evaluation,
    ) {
        when {
            !removed.isGroup -> {}

            else -> {
                when (GroupEntry.positionOf(removed.notification.group, removed.notification.flags)) {
                    GroupPosition.Summary, GroupPosition.AutogroupSummary -> {}

                    GroupPosition.Member -> {
                        revisits.add(PendingRevisit(GroupEntry.of(removed, OriginGroup.Unknown), revisit, evaluation))
                    }
                }
            }
        }
    }

    private suspend fun sweepNow(evaluation: Evaluation) {
        when (val read = persisting { calls.read(NotificationList.Active) }) {
            is NotificationRead.Read -> {
                Log.i(LOG_TAG, "sweeping ${read.notifications.size} active notifications")
                val active = ActiveGroups.Read(entries.of(read.notifications))

                active.entries.forEach { entry -> scope.launch { decide(entry, entry.role { active }, evaluation) } }
            }

            is NotificationRead.Failure -> {
                Log.e(LOG_TAG, "sweep skipped, active notifications unavailable: ${read.detail}")
            }
        }
    }

    private suspend fun revisitBatch(batch: List<PendingRevisit>) {
        when (val read = recovering { calls.read(NotificationList.Active) }) {
            is NotificationRead.Read -> {
                val active = ActiveGroups.Read(entries.of(read.notifications))
                val evaluation = batch.last().evaluation

                active.entries
                    .filter { entry ->
                        batch.any { it.revisit.covers(entry.position) && entry.sharesGroupWith(it.gone) }
                    }.forEach { entry -> scope.launch { decide(entry, entry.role { active }, evaluation) } }
            }

            is NotificationRead.Failure -> {
                Log.w(
                    LOG_TAG,
                    "${batch.size} summary revisits skipped, active notifications unavailable: ${read.detail}",
                )
            }
        }
    }

    private suspend fun decide(
        entry: GroupEntry,
        lookup: GroupRoleLookup,
        evaluation: Evaluation,
    ) {
        when (lookup) {
            is GroupRoleLookup.Found -> {
                decide(entry, lookup.role, evaluation)
            }

            is GroupRoleLookup.Unknown -> {
                Log.w(LOG_TAG, "${entry.posted.describe()} -> leave: its group couldn't be read: ${lookup.detail}")
            }
        }
    }

    private suspend fun decide(
        entry: GroupEntry,
        role: GroupRole,
        evaluation: Evaluation,
    ) {
        val decision = evaluation.decide(entry.posted, role, Instant.now())

        Log.i(LOG_TAG, "${entry.posted.describe()} -> ${decision.describe()}")

        when (decision) {
            is Decision.Leave -> {}

            is Decision.Snooze -> {
                holder.hold(entry, decision)
            }
        }
    }

    private suspend fun recheck(
        sbn: StatusBarNotification,
        known: KnownGroups,
        active: Lazy<ActiveGroups>,
        before: Baseline.Known<Evaluation>,
        after: Evaluation,
    ) {
        val entry = GroupEntry.of(sbn, known.groupOf(sbn))
        val snoozed = entry.posted

        when (val lookup = entry.role { active.value }) {
            is GroupRoleLookup.Found -> {
                recheck(snoozed, lookup.role, before, after)
            }

            is GroupRoleLookup.Unknown -> {
                Log.w(
                    LOG_TAG,
                    "snoozed ${snoozed.describe()} not rechecked, its group couldn't be read: ${lookup.detail}",
                )
            }
        }
    }

    private suspend fun recheck(
        snoozed: PostedNotification,
        role: GroupRole,
        before: Baseline.Known<Evaluation>,
        after: Evaluation,
    ) {
        val recheck = after.recheck(snoozed, role, before, Instant.now())

        Log.i(LOG_TAG, "snoozed ${snoozed.describe()} -> ${recheck.describe()}")

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
