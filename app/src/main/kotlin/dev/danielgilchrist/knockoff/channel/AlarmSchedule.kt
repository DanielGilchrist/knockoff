package dev.danielgilchrist.knockoff.channel

import android.util.Log
import dev.danielgilchrist.knockoff.LOG_TAG
import java.time.ZonedDateTime

sealed interface AlarmSchedule {
    data class Exact(
        val at: ZonedDateTime,
    ) : AlarmSchedule

    data class Inexact(
        val at: ZonedDateTime,
        val detail: String,
    ) : AlarmSchedule

    data object Cleared : AlarmSchedule

    data class Unset(
        val detail: String,
    ) : AlarmSchedule

    fun report() {
        when (this) {
            is Exact -> Log.i(LOG_TAG, "next boundary alarm at $at")
            is Inexact -> Log.w(LOG_TAG, "next boundary alarm near $at, inexact: $detail")
            Cleared -> Log.i(LOG_TAG, "no boundary alarm needed")
            is Unset -> Log.e(LOG_TAG, "boundary alarm not set: $detail")
        }
    }
}
