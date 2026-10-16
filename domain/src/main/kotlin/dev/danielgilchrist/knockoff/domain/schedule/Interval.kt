package dev.danielgilchrist.knockoff.domain.schedule

import java.time.Instant

@ConsistentCopyVisibility
internal data class Interval private constructor(
    val start: Instant,
    val end: Instant,
) {
    companion object {
        fun parse(
            start: Instant,
            end: Instant,
        ): IntervalParse =
            when {
                start < end -> IntervalParse.Valid(Interval(start, end))
                else -> IntervalParse.Empty
            }
    }

    operator fun contains(instant: Instant): Boolean = instant >= start && instant < end

    fun touches(other: Interval): Boolean = other.start <= end && start <= other.end

    fun span(other: Interval): Interval = Interval(minOf(start, other.start), maxOf(end, other.end))

    fun without(cut: Interval): List<Interval> =
        listOf(parse(start, minOf(end, cut.start)), parse(maxOf(start, cut.end), end)).flatMap { piece ->
            when (piece) {
                is IntervalParse.Valid -> listOf(piece.interval)
                IntervalParse.Empty -> emptyList()
            }
        }
}
