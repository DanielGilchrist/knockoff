package dev.danielgilchrist.knockoff.domain.schedule

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

internal fun LocalDateTime.firstInstantIn(zone: ZoneId): Instant =
    when (val gap = zone.rules.getTransition(this)?.takeIf { it.isGap }) {
        null -> atZone(zone).withEarlierOffsetAtOverlap().toInstant()
        else -> gap.instant
    }

internal fun LocalDateTime.lastInstantIn(zone: ZoneId): Instant =
    when (val gap = zone.rules.getTransition(this)?.takeIf { it.isGap }) {
        null -> atZone(zone).withLaterOffsetAtOverlap().toInstant()
        else -> gap.instant
    }
