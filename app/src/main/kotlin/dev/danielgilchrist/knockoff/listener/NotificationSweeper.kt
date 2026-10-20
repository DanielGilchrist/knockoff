package dev.danielgilchrist.knockoff.listener

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import dev.danielgilchrist.knockoff.LOG_TAG
import dev.danielgilchrist.knockoff.domain.notification.GroupRole
import dev.danielgilchrist.knockoff.domain.notification.PostedNotification
import dev.danielgilchrist.knockoff.domain.rule.Decision
import dev.danielgilchrist.knockoff.domain.rule.Evaluation
import dev.danielgilchrist.knockoff.domain.rule.Recheck
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.Instant

internal class NotificationSweeper(
    private val listener: NotificationListenerService,
    private val scope: CoroutineScope,
) {
    private val snoozer = Snoozer(listener)

    fun sweep(evaluation: Evaluation) {
        when (val read = listener.read(NotificationList.Active)) {
            is NotificationRead.Read -> {
                Log.i(LOG_TAG, "sweeping ${read.notifications.size} active notifications")
                val active = ActiveGroups.Read(read.notifications.map(GroupEntry::of))

                read.notifications.zip(active.entries).forEach { (sbn, entry) ->
                    scope.launch { decide(sbn, entry.posted, entry.role { active }, evaluation) }
                }
            }

            is NotificationRead.Failure -> {
                Log.e(LOG_TAG, "sweep skipped, active notifications unavailable: ${read.detail}")
            }
        }
    }

    fun recheckSnoozed(
        before: Evaluation,
        after: Evaluation,
    ) {
        when (val read = listener.read(NotificationList.Snoozed)) {
            is NotificationRead.Read -> {
                Log.i(LOG_TAG, "rechecking ${read.notifications.size} snoozed notifications")
                val active = lazy { activeGroups() }

                read.notifications.forEach { sbn -> scope.launch { recheck(sbn, active, before, after) } }
            }

            is NotificationRead.Failure -> {
                Log.e(LOG_TAG, "recheck skipped, snoozed notifications unavailable: ${read.detail}")
            }
        }
    }

    suspend fun evaluate(
        sbn: StatusBarNotification,
        posted: PostedNotification,
        evaluation: Evaluation,
    ) {
        decide(sbn, posted, GroupEntry.of(sbn, posted).role(::activeGroups), evaluation)
    }

    fun revisitSummaries(
        removed: StatusBarNotification,
        evaluation: Evaluation,
    ) {
        when (val group = removed.notification.group) {
            null -> {}

            else -> {
                when (GroupEntry.positionOf(group, removed.notification.flags)) {
                    GroupPosition.Summary -> {}

                    GroupPosition.Member -> {
                        scope.launch { revisitSummariesOf(GroupEntry.of(removed), evaluation) }
                    }
                }
            }
        }
    }

    private fun revisitSummariesOf(
        gone: GroupEntry,
        evaluation: Evaluation,
    ) {
        when (val read = listener.read(NotificationList.Active)) {
            is NotificationRead.Read -> {
                read.notifications
                    .map { it to GroupEntry.of(it) }
                    .filter { (_, entry) -> entry.position == GroupPosition.Summary && entry.sharesGroupWith(gone) }
                    .forEach { (sbn, entry) -> scope.launch { evaluate(sbn, entry.posted, evaluation) } }
            }

            is NotificationRead.Failure -> {
                Log.w(LOG_TAG, "summaries not revisited, active notifications unavailable: ${read.detail}")
            }
        }
    }

    private suspend fun decide(
        sbn: StatusBarNotification,
        posted: PostedNotification,
        lookup: GroupRoleLookup,
        evaluation: Evaluation,
    ) {
        when (lookup) {
            is GroupRoleLookup.Found -> {
                decide(sbn, posted, lookup.role, evaluation)
            }

            is GroupRoleLookup.Unknown -> {
                Log.w(LOG_TAG, "${sbn.describe(posted)} -> leave: its group couldn't be read: ${lookup.detail}")
            }
        }
    }

    private suspend fun decide(
        sbn: StatusBarNotification,
        posted: PostedNotification,
        role: GroupRole,
        evaluation: Evaluation,
    ) {
        val decision = evaluation.decide(posted, role, Instant.now())

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
        active: Lazy<ActiveGroups>,
        before: Evaluation,
        after: Evaluation,
    ) {
        val entry = GroupEntry.of(sbn)
        val snoozed = entry.posted

        when (val lookup = entry.role { active.value }) {
            is GroupRoleLookup.Found -> {
                recheck(sbn, snoozed, lookup.role, before, after)
            }

            is GroupRoleLookup.Unknown -> {
                Log.w(
                    LOG_TAG,
                    "snoozed ${sbn.describe(snoozed)} not rechecked, its group couldn't be read: ${lookup.detail}",
                )
            }
        }
    }

    private suspend fun recheck(
        sbn: StatusBarNotification,
        snoozed: PostedNotification,
        role: GroupRole,
        before: Evaluation,
        after: Evaluation,
    ) {
        val recheck = after.recheck(snoozed, role, before, Instant.now())

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

    private fun activeGroups(): ActiveGroups =
        when (val read = listener.read(NotificationList.Active)) {
            is NotificationRead.Read -> ActiveGroups.Read(read.notifications.map(GroupEntry::of))
            is NotificationRead.Failure -> ActiveGroups.Unavailable(read.detail)
        }
}
