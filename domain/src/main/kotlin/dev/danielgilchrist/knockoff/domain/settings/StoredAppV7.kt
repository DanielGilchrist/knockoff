package dev.danielgilchrist.knockoff.domain.settings

import kotlinx.serialization.Serializable

@Serializable
internal data class StoredAppV7(
    private val app: String,
    private val scope: StoredAppScopeV7,
) {
    fun upgraded(): StoredApp = StoredApp(app, scope.upgraded())
}
