package dev.danielgilchrist.knockoff.holiday

import dev.danielgilchrist.knockoff.domain.dayoff.HolidayCalendar
import dev.danielgilchrist.knockoff.domain.dayoff.Holidays

sealed interface HolidaysState {
    val holidays: Holidays

    data object Loading : HolidaysState {
        override val holidays: Holidays get() = Holidays.NONE
    }

    sealed interface Loaded : HolidaysState

    data object NotChosen : Loaded {
        override val holidays: Holidays get() = Holidays.NONE
    }

    @ConsistentCopyVisibility
    data class Chosen private constructor(
        val calendar: HolidayCalendar.Chosen,
        val read: HolidayRead,
        override val holidays: Holidays,
    ) : Loaded {
        companion object {
            fun of(
                calendar: HolidayCalendar.Chosen,
                read: HolidayRead,
                previous: HolidaysState,
            ): Chosen =
                Chosen(
                    calendar,
                    read,
                    when (read) {
                        is HolidayRead.Read -> Holidays.of(read.events, calendar.regions)
                        is HolidayRead.Failed -> kept(calendar, previous)
                        HolidayRead.NoPermission, HolidayRead.Missing -> Holidays.NONE
                    },
                )

            private fun kept(
                calendar: HolidayCalendar.Chosen,
                previous: HolidaysState,
            ): Holidays =
                when (previous) {
                    Loading, NotChosen -> Holidays.NONE
                    is Chosen -> if (previous.calendar == calendar) previous.holidays else Holidays.NONE
                }
        }
    }
}
