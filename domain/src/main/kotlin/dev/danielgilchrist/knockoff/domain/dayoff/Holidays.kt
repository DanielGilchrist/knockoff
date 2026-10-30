package dev.danielgilchrist.knockoff.domain.dayoff

import java.time.LocalDate

data class Holidays(
    val dates: Set<LocalDate>,
) {
    companion object {
        val NONE: Holidays get() = Holidays(emptySet())

        fun of(
            events: List<HolidayEvent>,
            regions: Set<HolidayRegion>,
        ): Holidays = Holidays(events.filter { it.kind.observedIn(regions) }.map { it.date }.toSet())
    }
}
