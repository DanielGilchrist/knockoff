package dev.danielgilchrist.knockoff.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.danielgilchrist.knockoff.KnockoffApplication
import dev.danielgilchrist.knockoff.chat.ChatDecisionSave
import dev.danielgilchrist.knockoff.domain.group.KnownGroups
import dev.danielgilchrist.knockoff.domain.rule.Target
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
import kotlinx.coroutines.launch

class MainViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val mutableSetup = MutableStateFlow(Setup.of(application))
    private var nearbyAsk = NearbyAsk.Askable
    private val mutablePaired = MutableStateFlow(PairedDevices.of(application, nearbyAsk))

    private val knockoff = application as KnockoffApplication
    private val store = knockoff.settingsStore

    val editor = SettingsEditor(store, viewModelScope)
    val preferences = PreferencesEditor(store, viewModelScope)
    val setup: StateFlow<Setup> = mutableSetup.asStateFlow()
    val paired: StateFlow<PairedDevices> = mutablePaired.asStateFlow()
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
            knockoff.knownGroupsStore.loads.map { it.known },
            mutableSetup.map { LinkState.Known(it.link) },
            ::TargetChoices,
        ).stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            TargetChoices(InstalledApps.Loading, SeenChatsState.Loading, KnownGroups.NONE, LinkState.Loading),
        )

    private val chatSaveStatus = MutableStateFlow<SaveStatus>(SaveStatus.Idle)

    val newChats: StateFlow<NewChatsView> =
        combine(
            combine(
                editor.settings,
                knockoff.seenChatsStore.loads,
                knockoff.chatDecisionsStore.loads,
                knockoff.knownGroupsStore.loads,
                NewChats::of,
            ),
            chatSaveStatus,
            ::NewChatsView,
        ).stateIn(viewModelScope, SharingStarted.Eagerly, NewChatsView(NewChats.Loading, SaveStatus.Idle))

    fun letChatThrough(chat: Target.Chat) {
        viewModelScope.launch {
            chatSaveStatus.value =
                when (val save = knockoff.chatDecisionsStore.letThrough(chat.key)) {
                    ChatDecisionSave.Saved -> SaveStatus.Idle
                    is ChatDecisionSave.Failed -> SaveStatus.Failed(save.detail)
                }
        }
    }

    fun refreshPaired() {
        mutablePaired.value = PairedDevices.of(getApplication(), nearbyAsk)
    }

    fun nearbyDevicesAnswered(ask: NearbyAsk) {
        nearbyAsk = ask
        refreshPaired()
    }

    fun refreshSetup() {
        mutableSetup.value = Setup.of(getApplication())
        refreshPaired()
        getApplication<KnockoffApplication>().requestSilencing()
        getApplication<KnockoffApplication>().refreshNotificationPermission()
    }
}
