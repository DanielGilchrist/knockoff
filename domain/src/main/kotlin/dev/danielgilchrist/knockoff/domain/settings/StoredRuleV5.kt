package dev.danielgilchrist.knockoff.domain.settings

import kotlinx.serialization.Serializable

@Serializable
internal data class StoredRuleV5(
    private val name: String,
    private val state: String,
    private val policy: String,
    private val windows: List<StoredWindow>,
    private val breaks: List<StoredBreak>,
    private val apps: List<String>,
    private val gmailAccounts: List<String>,
    private val chats: List<StoredChat>,
    private val patterns: List<StoredPattern>,
    private val zone: String,
) {
    fun upgraded(): StoredRule =
        StoredRule(name, state, policy, windows, breaks, apps, gmailAccounts, chats, patterns, zone, emptyList())
}
