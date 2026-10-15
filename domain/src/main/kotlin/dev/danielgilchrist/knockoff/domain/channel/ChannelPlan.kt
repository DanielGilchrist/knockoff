package dev.danielgilchrist.knockoff.domain.channel

data class ChannelPlan(
    val silence: List<ChannelChange>,
    val restore: List<ChannelChange>,
    val silenced: SilencedChannels,
)
