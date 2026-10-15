package dev.danielgilchrist.knockoff.channel

import android.app.NotificationChannel

internal sealed interface ChannelFetch {
    data class Fetched(
        val channels: List<NotificationChannel>,
    ) : ChannelFetch

    data class NotLinked(
        val detail: String,
    ) : ChannelFetch

    data object Unbound : ChannelFetch
}
