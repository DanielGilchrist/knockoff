package dev.danielgilchrist.knockoff.channel

import dev.danielgilchrist.knockoff.domain.channel.ChannelKey
import dev.danielgilchrist.knockoff.domain.channel.ReadChannel

internal sealed interface ChannelRead {
    data class Read(
        val channels: Map<ChannelKey, ReadChannel>,
    ) : ChannelRead

    data class NotLinked(
        val detail: String,
    ) : ChannelRead

    data object Unbound : ChannelRead

    data class Unavailable(
        val detail: String,
    ) : ChannelRead
}
