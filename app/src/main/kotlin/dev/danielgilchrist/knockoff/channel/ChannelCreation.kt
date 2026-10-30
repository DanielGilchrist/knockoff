package dev.danielgilchrist.knockoff.channel

internal sealed interface ChannelCreation {
    data object Created : ChannelCreation

    data object Refused : ChannelCreation

    data object Unsupported : ChannelCreation

    data class NotLinked(
        val detail: String,
    ) : ChannelCreation

    data class Unavailable(
        val detail: String,
    ) : ChannelCreation
}
