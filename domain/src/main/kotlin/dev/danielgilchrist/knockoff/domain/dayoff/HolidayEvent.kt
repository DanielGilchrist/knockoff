package dev.danielgilchrist.knockoff.domain.dayoff

import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class HolidayEvent(
    val date: LocalDate,
    val kind: HolidayKind,
) {
    companion object {
        fun spanning(
            first: LocalDate,
            endExclusive: LocalDate,
            kind: HolidayKind,
        ): List<HolidayEvent> =
            (0 until ChronoUnit.DAYS.between(first, endExclusive)).map { HolidayEvent(first.plusDays(it), kind) }

        fun regionsIn(events: List<HolidayEvent>): Set<HolidayRegion> =
            events
                .flatMap { event ->
                    when (val kind = event.kind) {
                        is HolidayKind.Regional -> kind.regions
                        HolidayKind.Everywhere, HolidayKind.Observance -> emptySet()
                    }
                }.toSet()
    }
}
