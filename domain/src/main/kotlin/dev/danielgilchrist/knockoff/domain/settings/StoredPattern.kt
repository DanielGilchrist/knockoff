package dev.danielgilchrist.knockoff.domain.settings

import kotlinx.serialization.Serializable

@Serializable
internal data class StoredPattern(
    val app: String,
    private val field: String,
    private val pattern: String,
) {
    fun pick(): StoredPatternPick = StoredPatternPick(field, pattern)
}
