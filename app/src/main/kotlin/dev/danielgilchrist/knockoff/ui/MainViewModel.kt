package dev.danielgilchrist.knockoff.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.danielgilchrist.knockoff.KnockoffApplication
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

class MainViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val mutableSetup = MutableStateFlow(Setup.of(application))

    val editor = SettingsEditor((application as KnockoffApplication).settingsStore, viewModelScope)
    val setup: StateFlow<Setup> = mutableSetup.asStateFlow()
    val apps: StateFlow<InstalledApps> =
        flow { emit(InstalledApps.load(application.packageManager)) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, InstalledApps.Loading)

    fun refreshSetup() {
        mutableSetup.value = Setup.of(getApplication())
        getApplication<KnockoffApplication>().requestSilencing()
    }
}
