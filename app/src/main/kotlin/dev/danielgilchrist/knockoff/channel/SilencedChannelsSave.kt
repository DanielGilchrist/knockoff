package dev.danielgilchrist.knockoff.channel

sealed interface SilencedChannelsSave {
    data object Saved : SilencedChannelsSave

    data class Failed(
        val detail: String,
    ) : SilencedChannelsSave
}
