package dev.danielgilchrist.knockoff.channel

import dev.danielgilchrist.knockoff.domain.group.GroupName
import dev.danielgilchrist.knockoff.domain.notification.ChannelGroupId

internal sealed interface GroupNamesFetch {
    data class Fetched(
        val names: Map<ChannelGroupId, GroupName>,
    ) : GroupNamesFetch

    data class NotLinked(
        val detail: String,
    ) : GroupNamesFetch

    data object Unbound : GroupNamesFetch
}
