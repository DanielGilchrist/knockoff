package dev.danielgilchrist.knockoff.ui

import dev.danielgilchrist.knockoff.domain.dayoff.DayOff
import dev.danielgilchrist.knockoff.domain.dayoff.Holidays
import dev.danielgilchrist.knockoff.domain.rule.Rule
import dev.danielgilchrist.knockoff.domain.zone.ZoneChoice
import dev.danielgilchrist.knockoff.domain.zone.ZoneContext
import java.time.LocalDate
import java.time.ZonedDateTime

data class RuleDay(
    val date: LocalDate,
    val dayOff: DayOff,
) {
    companion object {
        fun of(
            rule: Rule,
            now: ZonedDateTime,
            defaultZone: ZoneChoice,
            holidays: Holidays,
        ): RuleDay {
            val zones = ZoneContext(now.zone, defaultZone)

            return RuleDay(rule.localised(now, zones).toLocalDate(), rule.dayOffAt(now, zones, holidays))
        }
    }
}
