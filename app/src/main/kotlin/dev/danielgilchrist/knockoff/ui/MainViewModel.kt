package dev.danielgilchrist.knockoff.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.danielgilchrist.knockoff.KnockoffApplication
import dev.danielgilchrist.knockoff.ui.preferences.PreferencesEditor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn

class MainViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val mutableSetup = MutableStateFlow(Setup.of(application))

    private val knockoff = application as KnockoffApplication
    private val store = knockoff.settingsStore

    val editor = SettingsEditor(store, viewModelScope)
    val preferences = PreferencesEditor(store, viewModelScope)
    val setup: StateFlow<Setup> = mutableSetup.asStateFlow()
    val choices: StateFlow<TargetChoices> =
        combine(
            flow<InstalledApps> {
                emit(
                    InstalledApps.load(application.packageManager),
                )
            }.onStart { emit(InstalledApps.Loading) },
            knockoff.seenChatsStore.loads
                .map(SeenChatsState::of)
                .onStart { emit(SeenChatsState.Loading) },
            ::TargetChoices,
        ).stateIn(viewModelScope, SharingStarted.Eagerly, TargetChoices(InstalledApps.Loading, SeenChatsState.Loading))

    fun refreshSetup() {
        mutableSetup.value = Setup.of(getApplication())
        getApplication<KnockoffApplication>().requestSilencing()
    }
}
