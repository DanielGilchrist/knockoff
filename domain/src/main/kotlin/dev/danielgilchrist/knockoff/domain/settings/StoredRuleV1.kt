package dev.danielgilchrist.knockoff.domain.settings

import dev.danielgilchrist.knockoff.domain.zone.RuleZone
import kotlinx.serialization.Serializable

@Serializable
internal data class StoredRuleV1(
    private val name: String,
    private val state: String,
    private val policy: String,
    private val windows: List<StoredWindow>,
    private val apps: List<String>,
    private val gmailAccounts: List<String>,
) {
    fun upgraded(): StoredRule =
        StoredRule(name, state, policy, windows, apps, gmailAccounts, RuleZone.Default.stored())
}
