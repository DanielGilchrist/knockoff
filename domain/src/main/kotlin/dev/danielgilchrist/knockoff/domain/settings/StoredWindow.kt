package dev.danielgilchrist.knockoff.domain.settings

import dev.danielgilchrist.knockoff.domain.schedule.DailyHours
import dev.danielgilchrist.knockoff.domain.schedule.WeekDays
import dev.danielgilchrist.knockoff.domain.schedule.WeekDaysParse
import dev.danielgilchrist.knockoff.domain.schedule.Window
import kotlinx.serialization.Serializable
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeParseException

@Serializable
internal data class StoredWindow(
    private val days: List<String>,
    private val start: String,
    private val end: String,
) {
    companion object {
        fun from(window: Window): StoredWindow =
            StoredWindow(
                days = DayOfWeek.entries.filter { it in window.days }.map { it.name },
                start = window.hours.start.toString(),
                end = window.hours.end.toString(),
            )
    }

    fun parse(): Parsed<Window> =
        weekDays().then { weekDays ->
            time(start).then { startTime ->
                time(end).then { endTime -> Parsed.Ok(Window(weekDays, DailyHours.between(startTime, endTime))) }
            }
        }

    private fun weekDays(): Parsed<WeekDays> {
        val unknownDays = days - DayOfWeek.entries.map { it.name }.toSet()

        return when {
            unknownDays.isNotEmpty() -> {
                Parsed.Failed("unknown days $unknownDays")
            }

            else -> {
                when (val parse = WeekDays.parse(DayOfWeek.entries.filter { it.name in days }.toSet())) {
                    is WeekDaysParse.Valid -> Parsed.Ok(parse.days)
                    WeekDaysParse.Empty -> Parsed.Failed("no days")
                }
            }
        }
    }

    private fun time(text: String): Parsed<LocalTime> =
        try {
            Parsed.Ok(LocalTime.parse(text))
        } catch (_: DateTimeParseException) {
            Parsed.Failed("\"$text\" is not a time of day")
        }
}
