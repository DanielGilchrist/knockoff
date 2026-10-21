package dev.danielgilchrist.knockoff.domain.channel

data class ReadChannel(
    val importance: Importance,
    val kind: ChannelKind,
    val grouping: ChannelGrouping,
)
