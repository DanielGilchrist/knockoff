package dev.danielgilchrist.knockoff.channel

import android.app.NotificationManager
import dev.danielgilchrist.knockoff.domain.channel.Importance

internal object AndroidImportance {
    fun level(importance: Importance): Int =
        when (importance) {
            Importance.None -> NotificationManager.IMPORTANCE_NONE
            Importance.Min -> NotificationManager.IMPORTANCE_MIN
            Importance.Low -> NotificationManager.IMPORTANCE_LOW
            Importance.Default -> NotificationManager.IMPORTANCE_DEFAULT
            Importance.High -> NotificationManager.IMPORTANCE_HIGH
            Importance.Max -> NotificationManager.IMPORTANCE_MAX
        }

    fun parse(level: Int): ImportanceParse =
        Importance.entries
            .firstOrNull { level(it) == level }
            ?.let { ImportanceParse.Known(it) }
            ?: ImportanceParse.Unknown(level)
}
