package dev.danielgilchrist.knockoff.listener

import android.util.Log
import dev.danielgilchrist.knockoff.LOG_TAG
import dev.danielgilchrist.knockoff.domain.rule.Decision

internal class NotificationHolder(
    private val snoozer: Snoozer,
    private val dismisser: SummaryDismisser,
    private val activeGroups: () -> ActiveGroups,
) {
    suspend fun hold(
        entry: GroupEntry,
        decision: Decision.Snooze,
    ) {
        when (entry.position) {
            GroupPosition.Member, GroupPosition.Summary -> snoozer.snooze(entry.posted.key, decision.duration)
            GroupPosition.AutogroupSummary -> dismiss(entry)
        }
    }

    private fun dismiss(summary: GroupEntry) {
        val key = summary.posted.key.value

        when (val active = activeGroups()) {
            is ActiveGroups.Read -> {
                when (val check = summary.dismissCheck(active.entries)) {
                    DismissCheck.Alone -> {
                        dismisser.dismiss(summary.posted.key)
                    }

                    is DismissCheck.Occupied -> {
                        Log.i(
                            LOG_TAG,
                            "key=$key autogroup summary kept while ${check.active} notifications under it " +
                                "are active; it's revisited as they go",
                        )
                    }
                }
            }

            is ActiveGroups.Unavailable -> {
                Log.w(LOG_TAG, "key=$key autogroup summary kept, active notifications unavailable: ${active.detail}")
            }
        }
    }
}
