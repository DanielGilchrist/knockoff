package dev.danielgilchrist.knockoff.listener

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import dev.danielgilchrist.knockoff.LOG_TAG
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
                read.notifications.forEach { sbn -> scope.launch { evaluate(sbn, evaluation) } }
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
                read.notifications.forEach { sbn -> scope.launch { recheck(sbn, before, after) } }
            }

            is NotificationRead.Failure -> {
                Log.e(LOG_TAG, "recheck skipped, snoozed notifications unavailable: ${read.detail}")
            }
        }
    }

    suspend fun evaluate(
        sbn: StatusBarNotification,
        evaluation: Evaluation,
    ) {
        val posted = sbn.toSnapshot().parse()
        val decision = evaluation.decide(posted, Instant.now())

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
        before: Evaluation,
        after: Evaluation,
    ) {
        val snoozed = sbn.toSnapshot().parse()
        val recheck = after.recheck(snoozed, before, Instant.now())

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
