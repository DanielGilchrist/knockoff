package dev.danielgilchrist.knockoff.domain.settings

import kotlinx.serialization.Serializable

@Serializable
internal data class StoredRuleV7(
    private val name: String,
    private val state: String,
    private val policy: String,
    private val windows: List<StoredWindow>,
    private val breaks: List<StoredBreak>,
    private val zone: String,
    private val apps: List<StoredAppV7>,
) {
    fun upgraded(): StoredRuleV8 =
        StoredRuleV8(name, state, policy, windows, breaks, zone, apps.map(StoredAppV7::upgraded))
}
