package dev.danielgilchrist.knockoff.domain.channel

import dev.danielgilchrist.knockoff.domain.notification.ChannelId
import dev.danielgilchrist.knockoff.domain.notification.PackageName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class StoredSilencedChannel(
    @SerialName("package")
    private val packageName: String,
    private val channel: String,
    val original: AlertingImportance,
) {
    companion object {
        fun from(
            key: ChannelKey,
            original: AlertingImportance,
        ): StoredSilencedChannel = StoredSilencedChannel(key.packageName.value, key.channelId.value, original)
    }

    val key: ChannelKey get() = ChannelKey(PackageName(packageName), ChannelId(channel))
}
