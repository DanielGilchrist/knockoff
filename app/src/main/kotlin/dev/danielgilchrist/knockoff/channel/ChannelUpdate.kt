package dev.danielgilchrist.knockoff.channel

internal sealed interface ChannelUpdate {
    data object Applied : ChannelUpdate

    data class Unconfirmed(
        val detail: String,
    ) : ChannelUpdate

    data class NotLinked(
        val detail: String,
    ) : ChannelUpdate

    data object Missing : ChannelUpdate

    data object Unbound : ChannelUpdate
}
