package dev.danielgilchrist.knockoff.domain.channel

import kotlinx.serialization.Serializable

@Serializable
internal data class StoredSilencedChannels(
    private val version: Int,
    private val channels: List<StoredSilencedChannel>,
) {
    companion object {
        const val CURRENT_VERSION = 1

        fun from(originals: Map<ChannelKey, AlertingImportance>): StoredSilencedChannels =
            StoredSilencedChannels(
                CURRENT_VERSION,
                originals.map { (key, original) ->
                    StoredSilencedChannel.from(key, original)
                },
            )
    }

    fun toSilencedChannels(): SilencedChannelsDecode {
        val duplicates =
            channels
                .groupingBy { it.key }
                .eachCount()
                .filterValues { it > 1 }
                .keys

        return when {
            duplicates.isEmpty() -> {
                SilencedChannelsDecode.Decoded(
                    SilencedChannels.of(
                        channels.associate {
                            it.key to
                                it.original
                        },
                    ),
                )
            }

            else -> {
                SilencedChannelsDecode.DuplicateChannels(duplicates)
            }
        }
    }
}
