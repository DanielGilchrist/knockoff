package dev.danielgilchrist.knockoff.domain.settings

import dev.danielgilchrist.knockoff.domain.dayoff.PublicHolidays
import kotlinx.serialization.Serializable

@Serializable
internal data class StoredRuleV8(
    private val name: String,
    private val state: String,
    private val policy: String,
    private val windows: List<StoredWindow>,
    private val breaks: List<StoredBreak>,
    private val zone: String,
    private val apps: List<StoredApp>,
) {
    fun upgraded(): StoredRule =
        StoredRule(name, state, policy, windows, breaks, zone, apps, emptyList(), PublicHolidays.AsUsual.name)
}
