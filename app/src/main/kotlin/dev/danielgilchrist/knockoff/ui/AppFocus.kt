package dev.danielgilchrist.knockoff.ui

import dev.danielgilchrist.knockoff.domain.notification.PackageName

sealed interface AppFocus {
    data object List : AppFocus

    data class App(
        val app: PackageName,
    ) : AppFocus

    companion object {
        fun of(editing: Editing): AppFocus =
            when (editing) {
                Editing.Closed, is Editing.Viewing, is Editing.Renaming -> List
                is Editing.Section -> editing.focus
                is Editing.Guided -> editing.focus
            }
    }
}
