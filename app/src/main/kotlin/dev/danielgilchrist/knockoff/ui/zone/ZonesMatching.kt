package dev.danielgilchrist.knockoff.ui.zone

import java.time.ZoneId

private val ZONES: List<ZoneId> =
    ZoneId
        .getAvailableZoneIds()
        .filterNot { it.startsWith("Etc/GMT+") || it.startsWith("Etc/GMT-") }
        .sorted()
        .map(ZoneId::of)

internal fun zonesMatching(query: String): List<ZoneId> {
    val words = query.trim().split(' ', '/', '_').filter { it.isNotEmpty() }

    return ZONES.filter { zone -> words.all { zone.id.contains(it, ignoreCase = true) } }
}
