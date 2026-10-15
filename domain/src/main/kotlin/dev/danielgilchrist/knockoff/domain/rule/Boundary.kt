package dev.danielgilchrist.knockoff.domain.rule

import java.time.ZonedDateTime

sealed interface Boundary {
    data class At(
        val time: ZonedDateTime,
    ) : Boundary

    data object None : Boundary

    fun noLaterThan(latest: ZonedDateTime): Boundary =
        when (this) {
            None -> At(latest)
            is At -> At(minOf(time, latest))
        }
}
