package dev.danielgilchrist.knockoff.ui

import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import dev.danielgilchrist.knockoff.listener.KnockoffListener
import dev.danielgilchrist.knockoff.system.SystemCall
import dev.danielgilchrist.knockoff.system.systemCall

enum class ListenerAccess {
    Granted,
    NotGranted,
    ;

    companion object {
        fun of(context: Context): ListenerAccess {
            val manager = context.getSystemService(NotificationManager::class.java)
            val listener = ComponentName(context, KnockoffListener::class.java)

            return when (val call = systemCall { manager.isNotificationListenerAccessGranted(listener) }) {
                is SystemCall.Done -> if (call.value) Granted else NotGranted
                is SystemCall.Failed -> NotGranted
            }
        }
    }
}
