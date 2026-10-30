package dev.danielgilchrist.knockoff.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.danielgilchrist.knockoff.KnockoffApplication
import dev.danielgilchrist.knockoff.chat.ChatDecisionSave
import dev.danielgilchrist.knockoff.domain.group.KnownGroups
import dev.danielgilchrist.knockoff.domain.notification.PackageName
import dev.danielgilchrist.knockoff.domain.rule.Target
import dev.danielgilchrist.knockoff.holiday.HolidaysState
import dev.danielgilchrist.knockoff.ui.preferences.CalendarChoices
import dev.danielgilchrist.knockoff.ui.preferences.PreferencesEditor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val mutableSetup = MutableStateFlow(Setup.of(application))
    private var nearbyAsk = PermissionAsk.Askable
    private var calendarAsk = PermissionAsk.Askable
    private val mutableCalendars = MutableStateFlow<CalendarChoices>(CalendarChoices.Loading)
    private val installed = MutableStateFlow<InstalledApps>(InstalledApps.Loading)
    private val mutablePaired = MutableStateFlow(PairedDevices.of(application, nearbyAsk))

    private val knockoff = application as KnockoffApplication
    private val store = knockoff.settingsStore

    val editor = SettingsEditor(store, viewModelScope)
    val preferences = PreferencesEditor(store, viewModelScope)
    val setup: StateFlow<Setup> = mutableSetup.asStateFlow()
    val paired: StateFlow<PairedDevices> = mutablePaired.asStateFlow()
    val calendars: StateFlow<CalendarChoices> = mutableCalendars.asStateFlow()
    val holidays: StateFlow<HolidaysState> = knockoff.holidayWatch.state
    private val discoveries = MutableStateFlow(GroupDiscoveries.NONE)

    val choices: StateFlow<TargetChoices> =
        combine(
            installed,
            knockoff.seenChatsStore.loads
                .map(SeenChatsState::of)
                .onStart { emit(SeenChatsState.Loading) },
            knockoff.knownGroupsStore.loads.map { it.known },
            mutableSetup.map { LinkState.Known(it.link) },
            discoveries,
            ::TargetChoices,
        ).stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            TargetChoices(
                InstalledApps.Loading,
                SeenChatsState.Loading,
                KnownGroups.NONE,
                LinkState.Loading,
                GroupDiscoveries.NONE,
            ),
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

    init {
        loadApps()

        val focus = editor.editing.map(AppFocus::of).distinctUntilChanged()

        viewModelScope.launch {
            focus.collect { focused -> whenAppFocused(focused) { app -> discover(app) { it.requested(app) } } }
        }
        viewModelScope.launch {
            val links = mutableSetup.map { it.link }

            combine(focus, knockoff.groupReach.state, links, ::Triple).collect { (focused, reach, link) ->
                whenAppFocused(focused) { app -> discover(app) { it.retried(app, reach, link) } }
            }
        }
    }

    private inline fun whenAppFocused(
        focus: AppFocus,
        act: (PackageName) -> Unit,
    ) {
        when (focus) {
            AppFocus.List -> {}

            is AppFocus.App -> {
                act(focus.app)
            }
        }
    }

    private fun discover(
        app: PackageName,
        request: (GroupDiscoveries) -> DiscoveryRequest,
    ) {
        var start = DiscoveryStart.Skip
        discoveries.update { current -> request(current).also { start = it.start }.discoveries }

        when (start) {
            DiscoveryStart.Skip -> {}

            DiscoveryStart.Start -> {
                viewModelScope.launch {
                    val discovery = knockoff.groupReach.discover(app)
                    discoveries.update { it.finished(app, discovery) }
                }
            }
        }
    }

    fun refreshPaired() {
        mutablePaired.value = PairedDevices.of(getApplication(), nearbyAsk)
    }

    fun nearbyDevicesAnswered(ask: PermissionAsk) {
        nearbyAsk = ask
        refreshPaired()
    }

    fun refreshCalendars() {
        viewModelScope.launch(Dispatchers.IO) {
            mutableCalendars.value = CalendarChoices.of(knockoff.calendarAccess.calendars(), calendarAsk)
        }
    }

    fun calendarAnswered(ask: PermissionAsk) {
        calendarAsk = ask
        refreshCalendars()
        knockoff.holidayWatch.refresh()
    }

    private fun loadApps() {
        viewModelScope.launch { installed.value = InstalledApps.load(getApplication<Application>().packageManager) }
    }

    fun refreshSetup() {
        when (installed.value) {
            is InstalledApps.Unavailable -> {
                loadApps()
            }

            InstalledApps.Loading, is InstalledApps.Loaded -> {}
        }
        mutableSetup.value = Setup.of(getApplication())
        refreshPaired()
        getApplication<KnockoffApplication>().requestSilencing()
        getApplication<KnockoffApplication>().refreshNotificationPermission()
        knockoff.holidayWatch.refresh()
        refreshCalendars()
    }
}
