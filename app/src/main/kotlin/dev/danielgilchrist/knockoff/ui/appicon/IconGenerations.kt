package dev.danielgilchrist.knockoff.ui.appicon

import dev.danielgilchrist.knockoff.domain.notification.PackageName

data class IconGenerations(
    private val changed: Map<PackageName, Int>,
) {
    fun of(app: PackageName): Int = changed.getOrDefault(app, 0)

    fun bumped(app: PackageName): IconGenerations = IconGenerations(changed + (app to of(app) + 1))

    companion object {
        val NONE = IconGenerations(emptyMap())
    }
}
