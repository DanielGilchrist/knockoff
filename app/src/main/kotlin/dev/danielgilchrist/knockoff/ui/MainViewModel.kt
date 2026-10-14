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
    private val mutableAccess = MutableStateFlow(ListenerAccess.of(application))

    val editor = SettingsEditor((application as KnockoffApplication).settingsStore, viewModelScope)
    val access: StateFlow<ListenerAccess> = mutableAccess.asStateFlow()
    val apps: StateFlow<InstalledApps> =
        flow { emit(InstalledApps.load(application.packageManager)) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, InstalledApps.Loading)

    fun refreshAccess() {
        mutableAccess.value = ListenerAccess.of(getApplication())
    }
}
