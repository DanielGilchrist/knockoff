package dev.danielgilchrist.knockoff.domain.settings

import kotlinx.serialization.Serializable

@Serializable
internal data class StoredAppliedRulesV1(
    private val version: Int,
    private val settings: String,
    private val zone: String,
) : StoredAppliedRulesDocument {
    companion object {
        const val VERSION = 1
    }

    override fun toBaseline(): AppliedRulesDecode =
        StoredAppliedRules(StoredAppliedRules.CURRENT_VERSION, StoredAppliedEntry(settings, zone), emptyList())
            .toBaseline()
}
