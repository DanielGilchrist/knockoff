package dev.danielgilchrist.knockoff.listener

import android.service.notification.NotificationListenerService
import android.util.Log
import dev.danielgilchrist.knockoff.LOG_TAG
import dev.danielgilchrist.knockoff.domain.notification.NotificationKey

internal class SummaryDismisser(
    private val listener: NotificationListenerService,
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
        }
    }

    private fun attempt(key: NotificationKey): DismissOutcome =
        try {
            listener.cancelNotification(key.value)
            DismissOutcome.Dismissed
        } catch (exception: SecurityException) {
            DismissOutcome.Rejected(exception.toString())
        }
}
