package dev.danielgilchrist.knockoff.domain.channel

import dev.danielgilchrist.knockoff.domain.notification.ChannelGroupId

sealed interface ChannelGrouping {
    data class Grouped(
        val id: ChannelGroupId,
    ) : ChannelGrouping

    data object Ungrouped : ChannelGrouping

    companion object {
        fun of(group: String?): ChannelGrouping =
            when {
                group.isNullOrBlank() -> Ungrouped
                else -> Grouped(ChannelGroupId(group))
            }
    }
}
