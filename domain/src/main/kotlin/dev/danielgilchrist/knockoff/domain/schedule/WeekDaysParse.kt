package dev.danielgilchrist.knockoff.domain.schedule

sealed interface WeekDaysParse {
    data class Valid(
        val days: WeekDays,
    ) : WeekDaysParse

    data object Empty : WeekDaysParse
}
