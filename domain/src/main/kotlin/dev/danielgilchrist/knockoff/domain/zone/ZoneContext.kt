package dev.danielgilchrist.knockoff.domain.zone

import java.time.ZoneId

data class ZoneContext(
    val device: ZoneId,
    val default: ZoneChoice,
) {
    fun zoneFor(rule: RuleZone): ZoneId = rule.resolve(default).zoneFor(device)
}
