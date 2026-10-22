package dev.danielgilchrist.knockoff.domain.settings

import dev.danielgilchrist.knockoff.domain.change.Baseline
import kotlinx.serialization.Serializable

@Serializable
internal data class StoredAppliedRules(
    private val version: Int,
    private val reconciled: StoredAppliedEntry,
    private val since: List<StoredAppliedEntry>,
) : StoredAppliedRulesDocument {
    companion object {
        const val CURRENT_VERSION = 2

        fun from(baseline: Baseline.Known<AppliedRules>): StoredAppliedRules =
            StoredAppliedRules(
                CURRENT_VERSION,
                StoredAppliedEntry.from(baseline.reconciled),
                baseline.since.map(StoredAppliedEntry::from),
            )
    }

    override fun toBaseline(): AppliedRulesDecode =
        when (
            val parsed =
                reconciled.toAppliedRules().within("reconciled").then { first ->
                    since.parseEach("since") { it.toAppliedRules() }.then { later ->
                        Parsed.Ok(Baseline.Known(first, later))
                    }
                }
        ) {
            is Parsed.Ok -> AppliedRulesDecode.Decoded(parsed.value)
            is Parsed.Failed -> AppliedRulesDecode.Unreadable(parsed.detail)
        }
}
