package dev.danielgilchrist.knockoff.listener

import android.util.Log
import dev.danielgilchrist.knockoff.LOG_TAG
import dev.danielgilchrist.knockoff.domain.notification.NotificationKey
import dev.danielgilchrist.knockoff.system.SystemCall

internal class SummaryDismisser(
    private val calls: NotificationCalls,
) {
    fun dismiss(key: NotificationKey) {
        when (val outcome = attempt(key)) {
            DismissOutcome.Dismissed -> {
                Log.i(
                    LOG_TAG,
                    "key=${key.value} dismissed autogroup summary; Android recreates it when its notifications return",
                )
            }

            is DismissOutcome.Rejected -> {
                Log.e(LOG_TAG, "key=${key.value} autogroup summary dismissal rejected: ${outcome.detail}")
            }

            is DismissOutcome.Unavailable -> {
                Log.w(
                    LOG_TAG,
                    "key=${key.value} autogroup summary not dismissed, the system was busy: ${outcome.detail}",
                )
            }
        }
    }

    private fun attempt(key: NotificationKey): DismissOutcome =
        when (val call = calls.cancel(key)) {
            is SystemCall.Done -> DismissOutcome.Dismissed
            is SystemCall.Refused -> DismissOutcome.Rejected(call.detail)
            is SystemCall.Unavailable -> DismissOutcome.Unavailable(call.detail)
        }
}
