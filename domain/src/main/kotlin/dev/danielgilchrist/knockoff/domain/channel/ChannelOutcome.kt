package dev.danielgilchrist.knockoff.domain.channel

internal sealed interface ChannelOutcome {
    data object Untouched : ChannelOutcome

    data class Kept(
        val original: AlertingImportance,
    ) : ChannelOutcome

    data class Silence(
        val change: ChannelChange,
        val original: AlertingImportance,
    ) : ChannelOutcome

    data class Restore(
        val change: ChannelChange,
        val original: AlertingImportance,
    ) : ChannelOutcome
}
