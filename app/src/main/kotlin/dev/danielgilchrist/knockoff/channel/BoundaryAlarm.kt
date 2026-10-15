package dev.danielgilchrist.knockoff.channel

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import dev.danielgilchrist.knockoff.domain.rule.Boundary
import java.time.ZonedDateTime
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaDuration

internal class BoundaryAlarm(
    private val context: Context,
) {
    companion object {
        private val INEXACT_STEP = 15.minutes.toJavaDuration()
    }

    fun schedule(
        boundary: Boundary,
        now: ZonedDateTime,
    ): AlarmSchedule {
        val alarms = context.getSystemService(AlarmManager::class.java)
        val reached =
            PendingIntent.getBroadcast(
                context,
                0,
                Intent(context, BoundaryReceiver::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )

        return when (boundary) {
            Boundary.None -> {
                alarms.cancel(reached)
                AlarmSchedule.Cleared
            }

            is Boundary.At -> {
                try {
                    alarms.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        boundary.time.toInstant().toEpochMilli(),
                        reached,
                    )
                    AlarmSchedule.Exact(boundary.time)
                } catch (exception: SecurityException) {
                    val at = minOf(boundary.time, now.plus(INEXACT_STEP))

                    alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at.toInstant().toEpochMilli(), reached)
                    AlarmSchedule.Inexact(at, exception.toString())
                }
            }
        }
    }
}
