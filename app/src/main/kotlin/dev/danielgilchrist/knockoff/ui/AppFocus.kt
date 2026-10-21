package dev.danielgilchrist.knockoff.ui

import dev.danielgilchrist.knockoff.domain.notification.PackageName

sealed interface AppFocus {
    data object List : AppFocus

    data class App(
        val app: PackageName,
    ) : AppFocus
}
