package dev.danielgilchrist.knockoff.domain.group

import kotlinx.serialization.Serializable

@Serializable
internal data class StoredKnownAppV1(
    private val app: String,
    private val channels: Map<String, String>,
    private val names: Map<String, String>,
) {
    fun upgraded(): StoredKnownApp = StoredKnownApp(app, channels, names, emptyList())
}
