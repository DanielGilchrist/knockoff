package dev.danielgilchrist.knockoff.ui

import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import dev.danielgilchrist.knockoff.listener.KnockoffListener

enum class ListenerAccess {
    Granted,
    NotGranted,
    ;

    companion object {
        fun of(context: Context): ListenerAccess {
            val manager = context.getSystemService(NotificationManager::class.java)
            val listener = ComponentName(context, KnockoffListener::class.java)

            return when {
                manager.isNotificationListenerAccessGranted(listener) -> Granted
                else -> NotGranted
            }
        }
    }
}
