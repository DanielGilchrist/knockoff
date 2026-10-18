package dev.danielgilchrist.knockoff.ui

import dev.danielgilchrist.knockoff.domain.settings.Settings

internal sealed interface ShownScreen {
    val screen: Screen

    data object Home : ShownScreen {
        override val screen = Screen.Home
    }

    data class Preferences(
        val settings: Settings,
    ) : ShownScreen {
        override val screen = Screen.Preferences
    }

    data class NewChats(
        val settings: Settings,
    ) : ShownScreen {
        override val screen = Screen.NewChats
    }

    companion object {
        fun of(
            screen: Screen,
            settings: SettingsState,
        ): ShownScreen =
            when (settings) {
                is SettingsState.Ready -> {
                    when (screen) {
                        Screen.Home -> Home
                        Screen.Preferences -> Preferences(settings.settings)
                        Screen.NewChats -> NewChats(settings.settings)
                    }
                }

                SettingsState.Loading, is SettingsState.Failed -> {
                    Home
                }
            }
    }
}
