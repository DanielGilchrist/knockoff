package dev.danielgilchrist.knockoff.channel

import dev.danielgilchrist.knockoff.domain.channel.ChannelKey
import dev.danielgilchrist.knockoff.domain.channel.Importance

internal sealed interface ChannelRead {
    data class Read(
        val importances: Map<ChannelKey, Importance>,
    ) : ChannelRead

    data class NotLinked(
        val detail: String,
    ) : ChannelRead

    data object Unbound : ChannelRead
}
