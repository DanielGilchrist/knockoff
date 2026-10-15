package dev.danielgilchrist.knockoff.domain.channel

import dev.danielgilchrist.knockoff.domain.notification.Channel
import dev.danielgilchrist.knockoff.domain.notification.ChannelId
import dev.danielgilchrist.knockoff.domain.notification.Origin
import dev.danielgilchrist.knockoff.domain.notification.PackageName

data class ChannelKey(
    val packageName: PackageName,
    val channelId: ChannelId,
) {
    val origin: Origin get() = Origin(packageName, Channel.Named(channelId))

    fun describe(): String = "${packageName.value}/${channelId.value}"
}
