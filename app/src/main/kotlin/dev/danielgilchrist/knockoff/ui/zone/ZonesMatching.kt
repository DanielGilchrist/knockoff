package dev.danielgilchrist.knockoff.ui.zone

import java.time.ZoneId

private val REGIONS =
    setOf("Africa", "America", "Antarctica", "Arctic", "Asia", "Atlantic", "Australia", "Europe", "Indian", "Pacific")

private val ZONES: List<ZoneId> =
    ZoneId
        .getAvailableZoneIds()
        .filter { id -> id == "UTC" || REGIONS.any { id.startsWith("$it/") } }
        .sorted()
        .map(ZoneId::of)

internal fun zonesMatching(query: String): List<ZoneId> {
    val words = query.trim().split(' ', '/', '_').filter { it.isNotEmpty() }

    return ZONES.filter { zone -> words.all { zone.id.contains(it, ignoreCase = true) } }
}
