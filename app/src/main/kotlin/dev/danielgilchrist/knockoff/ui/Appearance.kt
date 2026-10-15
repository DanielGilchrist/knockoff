package dev.danielgilchrist.knockoff.ui

import dev.danielgilchrist.knockoff.domain.settings.Theme

internal sealed interface Appearance {
    data object Pending : Appearance

    data class Themed(
        val theme: Theme,
    ) : Appearance

    companion object {
        fun of(settings: SettingsState): Appearance =
            when (settings) {
                SettingsState.Loading -> Pending
                is SettingsState.Ready -> Themed(settings.settings.theme)
                is SettingsState.Failed -> Themed(Theme.System)
            }
    }
}
