package dev.danielgilchrist.knockoff.domain.channel

import dev.danielgilchrist.knockoff.domain.json.VersionedDecode
import dev.danielgilchrist.knockoff.domain.json.VersionedJson
import dev.danielgilchrist.knockoff.domain.notification.PackageName

@ConsistentCopyVisibility
data class SilencedChannels private constructor(
    private val originals: Map<ChannelKey, AlertingImportance>,
) {
    companion object {
        val NONE: SilencedChannels get() = SilencedChannels(emptyMap())

        internal fun of(originals: Map<ChannelKey, AlertingImportance>): SilencedChannels =
            SilencedChannels(originals.toMap())

        fun fromJson(text: String): SilencedChannelsDecode =
            when (
                val decode =
                    VersionedJson.decode(
                        text,
                        mapOf(StoredSilencedChannels.CURRENT_VERSION to StoredSilencedChannels.serializer()),
                    )
            ) {
                is VersionedDecode.Decoded -> decode.value.toSilencedChannels()
                is VersionedDecode.Unreadable -> SilencedChannelsDecode.Unreadable(decode.detail)
                is VersionedDecode.UnsupportedVersion -> SilencedChannelsDecode.UnsupportedVersion(decode.version)
            }
    }

    val packages: Set<PackageName> get() = originals.keys.map { it.packageName }.toSet()

    fun toJson(): String = VersionedJson.json.encodeToString(StoredSilencedChannels.from(originals))

    internal fun plan(
        read: Map<ChannelKey, Importance>,
        stateOf: (ChannelKey) -> ChannelState,
    ): ChannelPlan {
        val outcomes = read.map { (key, current) -> key to outcome(key, current, stateOf(key)) }

        return ChannelPlan(
            silence = outcomes.map { it.second }.filterIsInstance<ChannelOutcome.Silence>().map { it.change },
            restore = outcomes.map { it.second }.filterIsInstance<ChannelOutcome.Restore>().map { it.change },
            silenced =
                SilencedChannels(
                    originals.filterKeys { it !in read } + outcomes.flatMap { (key, outcome) -> saved(key, outcome) },
                ),
        )
    }

    private fun outcome(
        key: ChannelKey,
        current: Importance,
        state: ChannelState,
    ): ChannelOutcome {
        val original = originals[key]

        return when {
            original != null && current == Importance.Low -> {
                when (state) {
                    ChannelState.Held -> {
                        ChannelOutcome.Kept(original)
                    }

                    ChannelState.Permitted -> {
                        ChannelOutcome.Restore(
                            ChannelChange(key, current, original.importance),
                            original,
                        )
                    }
                }
            }

            else -> {
                when (state) {
                    ChannelState.Held -> silenceIfAlerting(key, current)
                    ChannelState.Permitted -> ChannelOutcome.Untouched
                }
            }
        }
    }

    private fun silenceIfAlerting(
        key: ChannelKey,
        current: Importance,
    ): ChannelOutcome =
        when (val loudness = current.loudness()) {
            Loudness.Quiet -> {
                ChannelOutcome.Untouched
            }

            is Loudness.Alerting -> {
                ChannelOutcome.Silence(
                    ChannelChange(key, current, Importance.Low),
                    loudness.importance,
                )
            }
        }

    private fun saved(
        key: ChannelKey,
        outcome: ChannelOutcome,
    ): List<Pair<ChannelKey, AlertingImportance>> =
        when (outcome) {
            ChannelOutcome.Untouched -> emptyList()
            is ChannelOutcome.Kept -> listOf(key to outcome.original)
            is ChannelOutcome.Silence -> listOf(key to outcome.original)
            is ChannelOutcome.Restore -> listOf(key to outcome.original)
        }
}
