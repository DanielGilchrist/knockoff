package dev.danielgilchrist.knockoff.domain.settings

import kotlinx.serialization.Serializable

@Serializable
internal data class StoredRuleV2(
    private val name: String,
    private val state: String,
    private val policy: String,
    private val windows: List<StoredWindow>,
    private val apps: List<String>,
    private val gmailAccounts: List<String>,
    private val zone: String,
) {
    fun upgraded(): StoredRuleV3 = StoredRuleV3(name, state, policy, windows, apps, gmailAccounts, emptyList(), zone)
}
