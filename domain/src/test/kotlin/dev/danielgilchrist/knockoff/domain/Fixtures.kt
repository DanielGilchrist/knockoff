package dev.danielgilchrist.knockoff.domain

import dev.danielgilchrist.knockoff.domain.schedule.DailyHours
import dev.danielgilchrist.knockoff.domain.schedule.Schedule
import dev.danielgilchrist.knockoff.domain.schedule.ScheduleParse
import dev.danielgilchrist.knockoff.domain.schedule.WeekDays
import dev.danielgilchrist.knockoff.domain.schedule.WeekDaysParse
import dev.danielgilchrist.knockoff.domain.schedule.Window
import java.time.DayOfWeek
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.THURSDAY
import java.time.DayOfWeek.TUESDAY
import java.time.DayOfWeek.WEDNESDAY
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime

val LONDON: ZoneId = ZoneId.of("Europe/London")

fun london(text: String): ZonedDateTime = LocalDateTime.parse(text).atZone(LONDON)

fun londonWithOffset(
    text: String,
    offset: ZoneOffset,
): ZonedDateTime = ZonedDateTime.ofLocal(LocalDateTime.parse(text), LONDON, offset)

fun days(vararg days: DayOfWeek): WeekDays =
    when (val parse = WeekDays.parse(days.toSet())) {
        is WeekDaysParse.Valid -> parse.days
        WeekDaysParse.Empty -> error("Fixture needs at least one day")
    }

fun window(
    days: WeekDays,
    start: String,
    end: String,
): Window = Window(days, DailyHours.between(LocalTime.parse(start), LocalTime.parse(end)))

fun schedule(vararg windows: Window): Schedule =
    when (val parse = Schedule.parse(windows.toList())) {
        is ScheduleParse.Valid -> parse.schedule
        ScheduleParse.Empty -> error("Fixture needs at least one window")
    }

val EVERY_DAY: WeekDays = days(*DayOfWeek.entries.toTypedArray())

val ALL_BUT_SUNDAY: WeekDays = days(*DayOfWeek.entries.filter { it != DayOfWeek.SUNDAY }.toTypedArray())

val WORK_HOURS: Schedule = schedule(window(days(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY), "08:00", "17:00"))
