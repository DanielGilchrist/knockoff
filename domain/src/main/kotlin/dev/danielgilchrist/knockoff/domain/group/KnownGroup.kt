package dev.danielgilchrist.knockoff.domain.group

import dev.danielgilchrist.knockoff.domain.notification.ChannelGroupId

data class KnownGroup(
    val id: ChannelGroupId,
    val label: GroupLabel,
)
