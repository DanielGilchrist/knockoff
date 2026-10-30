package dev.danielgilchrist.knockoff.ui.editor

import dev.danielgilchrist.knockoff.domain.rule.Policy
import dev.danielgilchrist.knockoff.domain.zone.ZoneChoice
import java.time.LocalDate

data class SectionFacts(
    val defaultZone: ZoneChoice,
    val today: LocalDate,
    val policy: Policy,
    val holidaySource: String,
)
