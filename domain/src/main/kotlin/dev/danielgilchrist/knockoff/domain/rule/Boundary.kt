package dev.danielgilchrist.knockoff.domain.rule

import java.time.ZonedDateTime

sealed interface Boundary {
    data class At(
        val time: ZonedDateTime,
    ) : Boundary

    data object None : Boundary
}
