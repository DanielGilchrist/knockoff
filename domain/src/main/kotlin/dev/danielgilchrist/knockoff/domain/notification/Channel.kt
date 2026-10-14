package dev.danielgilchrist.knockoff.domain.notification

sealed interface Channel {
    data class Named(
        val id: ChannelId,
    ) : Channel

    data object Unnamed : Channel
}
