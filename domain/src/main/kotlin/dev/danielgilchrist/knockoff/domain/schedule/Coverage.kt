package dev.danielgilchrist.knockoff.domain.schedule

import java.time.ZonedDateTime

sealed interface Coverage {
    data class Inside(
        val endsAt: ZonedDateTime,
    ) : Coverage

    data class Outside(
        val startsAt: ZonedDateTime,
    ) : Coverage

    data object Always : Coverage
}
