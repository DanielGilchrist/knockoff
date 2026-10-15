package dev.danielgilchrist.knockoff.channel

import dev.danielgilchrist.knockoff.domain.channel.SilencedChannels

sealed interface SilencedChannelsLoad {
    data class Loaded(
        val silenced: SilencedChannels,
    ) : SilencedChannelsLoad

    data class Failed(
        val detail: String,
    ) : SilencedChannelsLoad
}
