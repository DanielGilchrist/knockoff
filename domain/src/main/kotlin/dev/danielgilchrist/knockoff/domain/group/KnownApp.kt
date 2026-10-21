package dev.danielgilchrist.knockoff.domain.group

import dev.danielgilchrist.knockoff.domain.notification.ChannelGroupId
import dev.danielgilchrist.knockoff.domain.notification.ChannelId
import dev.danielgilchrist.knockoff.domain.notification.PackageName

data class KnownApp(
    val packageName: PackageName,
    val channels: Map<ChannelId, ChannelGroupId>,
    val names: Map<ChannelGroupId, GroupName>,
) {
    val groups: List<KnownGroup>
        get() =
            (names.keys + channels.values)
                .distinct()
                .map { id ->
                    KnownGroup(
                        id,
                        when (val name = names[id]) {
                            null -> GroupLabel.Unnamed
                            else -> GroupLabel.Named(name)
                        },
                    )
                }

    fun withChannel(
        channel: ChannelId,
        group: ChannelGroupId,
    ): KnownApp = copy(channels = channels + (channel to group))
}
