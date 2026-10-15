package dev.danielgilchrist.knockoff.domain.schedule

internal sealed interface IntervalParse {
    data class Valid(
        val interval: Interval,
    ) : IntervalParse

    data object Empty : IntervalParse
}
