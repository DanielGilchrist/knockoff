package dev.danielgilchrist.knockoff.domain.schedule

internal fun List<WeekSpan>.merged(): List<WeekSpan> =
    sortedBy { it.startNano }.fold(emptyList()) { merged, next ->
        val last = merged.lastOrNull()

        when {
            last != null && next.startNano <= last.endNano -> {
                merged.dropLast(1) +
                    WeekSpan(last.startNano, maxOf(last.endNano, next.endNano))
            }

            else -> {
                merged + next
            }
        }
    }

internal fun List<WeekSpan>.without(cuts: List<WeekSpan>): List<WeekSpan> =
    cuts.fold(this) { remaining, cut -> remaining.flatMap { it.without(cut) } }

internal fun List<WeekSpan>.nanosWithinWeek(): Long =
    sumOf { span -> maxOf(0L, minOf(span.endNano, WeekSpan.NANOS_PER_WEEK) - maxOf(span.startNano, 0L)) }
