package dev.danielgilchrist.knockoff.domain.notification

import dev.danielgilchrist.knockoff.domain.channel.ChannelGrouping

sealed interface OriginGroup {
    data class Known(
        val grouping: ChannelGrouping,
    ) : OriginGroup

    data object Unknown : OriginGroup
}
