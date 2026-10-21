package dev.danielgilchrist.knockoff.ui.zone

import dev.danielgilchrist.knockoff.domain.zone.ZoneChoice
import dev.danielgilchrist.knockoff.ui.SettingsState

sealed interface DefaultZone {
    data object Unknown : DefaultZone

    data class Known(
        val zone: ZoneChoice,
    ) : DefaultZone

    companion object {
        fun of(settings: SettingsState): DefaultZone =
            when (settings) {
                SettingsState.Loading, is SettingsState.Failed -> Unknown
                is SettingsState.Ready -> Known(settings.settings.rules.defaultZone)
            }
    }
}
