package dev.danielgilchrist.knockoff.listener

import android.service.notification.NotificationListenerService

internal enum class SummaryRevisit {
    AllSummaries,
    AutogroupSummaries,
    ;

    fun covers(position: GroupPosition): Boolean =
        when (this) {
            AllSummaries -> position != GroupPosition.Member
            AutogroupSummaries -> position == GroupPosition.AutogroupSummary
        }

    companion object {
        fun afterRemoval(reason: Int): SummaryRevisit =
            when (reason) {
                NotificationListenerService.REASON_SNOOZED -> AutogroupSummaries
                else -> AllSummaries
            }
    }
}
