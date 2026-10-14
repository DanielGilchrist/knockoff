package dev.danielgilchrist.knockoff.domain.schedule

import java.time.Instant

internal data class Interval(
    val start: Instant,
    val end: Instant,
) {
    init {
        require(start < end) { "Interval must end after it starts: $start..$end" }
    }

    operator fun contains(instant: Instant): Boolean = instant >= start && instant < end

    fun touches(other: Interval): Boolean = other.start <= end && start <= other.end

    fun span(other: Interval): Interval = Interval(minOf(start, other.start), maxOf(end, other.end))
}
