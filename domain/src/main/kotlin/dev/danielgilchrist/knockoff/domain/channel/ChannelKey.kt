package dev.danielgilchrist.knockoff.domain.channel

import dev.danielgilchrist.knockoff.domain.notification.ChannelId
import dev.danielgilchrist.knockoff.domain.notification.PackageName

data class ChannelKey(
    val packageName: PackageName,
    val channelId: ChannelId,
) {
    fun describe(): String = "${packageName.value}/${channelId.value}"
}
