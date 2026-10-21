package dev.danielgilchrist.knockoff.domain.settings

import dev.danielgilchrist.knockoff.domain.notification.PackageName
import dev.danielgilchrist.knockoff.domain.rule.AppScope
import kotlinx.serialization.Serializable

@Serializable
internal data class StoredApp(
    private val app: String,
    private val scope: StoredAppScope,
) {
    companion object {
        fun from(
            packageName: PackageName,
            scope: AppScope,
        ): StoredApp = StoredApp(packageName.value, StoredAppScope.from(scope))
    }

    fun parse(): Parsed<Pair<PackageName, AppScope>> =
        when {
            app.isBlank() -> Parsed.Failed("blank app")
            else -> scope.parse().then { Parsed.Ok(PackageName(app) to it) }.within("\"$app\"")
        }
}
