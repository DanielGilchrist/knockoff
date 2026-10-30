package dev.danielgilchrist.knockoff.channel

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import dev.danielgilchrist.knockoff.domain.rule.Boundary
import dev.danielgilchrist.knockoff.system.SystemCall
import dev.danielgilchrist.knockoff.system.systemCall
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
                when (val call = systemCall { alarms.cancel(reached) }) {
                    is SystemCall.Done -> AlarmSchedule.Cleared
                    is SystemCall.Failed -> AlarmSchedule.Unset(call.detail)
                }
            }

            is Boundary.At -> {
                val exact =
                    systemCall {
                        alarms.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            boundary.time.toInstant().toEpochMilli(),
                            reached,
                        )
                    }

                when (exact) {
                    is SystemCall.Done -> {
                        AlarmSchedule.Exact(boundary.time)
                    }

                    is SystemCall.Refused -> {
                        inexact(
                            alarms,
                            minOf(boundary.time, now.plus(INEXACT_STEP)),
                            reached,
                            exact,
                        )
                    }

                    is SystemCall.Unavailable -> {
                        AlarmSchedule.Unset(exact.detail)
                    }
                }
            }
        }
    }

    private fun inexact(
        alarms: AlarmManager,
        at: ZonedDateTime,
        reached: PendingIntent,
        refused: SystemCall.Refused,
    ): AlarmSchedule =
        when (
            val call =
                systemCall {
                    alarms.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        at.toInstant().toEpochMilli(),
                        reached,
                    )
                }
        ) {
            is SystemCall.Done -> AlarmSchedule.Inexact(at, refused.detail)
            is SystemCall.Failed -> AlarmSchedule.Unset(call.detail)
        }
}
