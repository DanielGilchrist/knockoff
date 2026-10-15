package dev.danielgilchrist.knockoff.channel

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
}
