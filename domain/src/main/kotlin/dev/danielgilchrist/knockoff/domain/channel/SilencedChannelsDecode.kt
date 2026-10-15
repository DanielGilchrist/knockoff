package dev.danielgilchrist.knockoff.domain.channel

sealed interface SilencedChannelsDecode {
    data class Decoded(
        val silenced: SilencedChannels,
    ) : SilencedChannelsDecode

    sealed interface Failure : SilencedChannelsDecode {
        val detail: String
    }

    data class Unreadable(
        override val detail: String,
    ) : Failure

    data class UnsupportedVersion(
        val version: Int,
    ) : Failure {
        override val detail: String get() = "Unsupported silenced channels version $version"
    }

    data class DuplicateChannels(
        val keys: Set<ChannelKey>,
    ) : Failure {
        override val detail: String get() = "Channels saved more than once: ${keys.map { it.describe() }}"
    }
}
