package dev.danielgilchrist.knockoff.domain.settings

import kotlinx.serialization.Serializable

@Serializable
internal data class StoredRuleV3(
    private val name: String,
    private val state: String,
    private val policy: String,
    private val windows: List<StoredWindow>,
    private val apps: List<String>,
    private val gmailAccounts: List<String>,
    private val chats: List<StoredChat>,
    private val zone: String,
) {
    fun upgraded(): StoredRuleV4 =
        StoredRuleV4(name, state, policy, windows, emptyList(), apps, gmailAccounts, chats, zone)
}
