package dev.danielgilchrist.knockoff.domain.schedule

internal sealed interface Occurrence {
    data class Occurs(
        val interval: Interval,
    ) : Occurrence

    data object SkippedByClockChange : Occurrence
}
