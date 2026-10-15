package dev.danielgilchrist.knockoff.ui.preferences

import dev.danielgilchrist.knockoff.domain.settings.Settings
import dev.danielgilchrist.knockoff.domain.settings.Theme
import dev.danielgilchrist.knockoff.domain.zone.ZoneChoice
import dev.danielgilchrist.knockoff.settings.SettingsChange
import dev.danielgilchrist.knockoff.settings.SettingsStore
import dev.danielgilchrist.knockoff.ui.SaveStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PreferencesEditor(
    private val store: SettingsStore,
    private val scope: CoroutineScope,
) {
    private val mutableSaveStatus = MutableStateFlow<SaveStatus>(SaveStatus.Idle)

    val saveStatus: StateFlow<SaveStatus> = mutableSaveStatus.asStateFlow()

    fun setDefaultZone(zone: ZoneChoice) {
        change { it.copy(rules = it.rules.withDefaultZone(zone)) }
    }

    fun setTheme(theme: Theme) {
        change { it.copy(theme = theme) }
    }

    private fun change(change: (Settings) -> Settings) {
        scope.launch {
            mutableSaveStatus.value =
                when (val saved = store.modify(change)) {
                    SettingsChange.Saved -> SaveStatus.Idle
                    is SettingsChange.Failed -> SaveStatus.Failed(saved.detail)
                }
        }
    }
}
