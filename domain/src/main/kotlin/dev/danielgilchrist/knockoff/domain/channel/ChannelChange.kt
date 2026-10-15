package dev.danielgilchrist.knockoff.domain.channel

data class ChannelChange(
    val key: ChannelKey,
    val from: Importance,
    val to: Importance,
) {
    fun describe(): String = "${key.describe()} $from -> $to"
}
