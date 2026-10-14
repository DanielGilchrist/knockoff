package dev.danielgilchrist.knockoff.domain.rule

import java.time.ZonedDateTime

sealed interface Release {
    data class At(
        val time: ZonedDateTime,
    ) : Release

    data object Never : Release
}
