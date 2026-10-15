package dev.danielgilchrist.knockoff.channel

import dev.danielgilchrist.knockoff.domain.channel.Importance

internal sealed interface ImportanceParse {
    data class Known(
        val importance: Importance,
    ) : ImportanceParse

    data class Unknown(
        val level: Int,
    ) : ImportanceParse
}
