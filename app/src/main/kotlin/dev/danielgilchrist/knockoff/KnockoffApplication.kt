package dev.danielgilchrist.knockoff

import android.app.Application
import dev.danielgilchrist.knockoff.ask.ChatAnswers
import dev.danielgilchrist.knockoff.ask.ChatPrompt
import dev.danielgilchrist.knockoff.ask.ChatQuestionNotifier
import dev.danielgilchrist.knockoff.channel.SilencedChannelsStore
import dev.danielgilchrist.knockoff.chat.ChatDecisionsStore
import dev.danielgilchrist.knockoff.chat.SeenChatsStore
import dev.danielgilchrist.knockoff.group.GroupReach
import dev.danielgilchrist.knockoff.group.KnownGroupsStore
import dev.danielgilchrist.knockoff.holiday.CalendarAccess
import dev.danielgilchrist.knockoff.holiday.HolidayWatch
import dev.danielgilchrist.knockoff.holiday.calendarChanges
import dev.danielgilchrist.knockoff.settings.AppliedRulesStore
import dev.danielgilchrist.knockoff.settings.SettingsStore
import dev.danielgilchrist.knockoff.ui.NotificationPermission
import dev.danielgilchrist.knockoff.ui.appicon.AppIcons
import dev.danielgilchrist.knockoff.ui.appicon.PackageChanges
import dev.danielgilchrist.knockoff.ui.appicon.PackageIconSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.io.File

class KnockoffApplication : Application() {
    val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val settingsStore: SettingsStore by lazy {
        SettingsStore(File(filesDir, "settings.json"))
    }

    val appliedRulesStore: AppliedRulesStore by lazy {
        AppliedRulesStore(File(filesDir, "applied-rules.json"))
    }

    val silencedChannelsStore: SilencedChannelsStore by lazy {
        SilencedChannelsStore(File(filesDir, "silenced-channels.json"))
    }

    val seenChatsStore: SeenChatsStore by lazy {
        SeenChatsStore(File(filesDir, "seen-chats.json"))
    }

    val knownGroupsStore: KnownGroupsStore by lazy {
        KnownGroupsStore(File(filesDir, "channel-groups.json"))
    }

    val chatDecisionsStore: ChatDecisionsStore by lazy {
        ChatDecisionsStore(File(filesDir, "chat-decisions.json"))
    }

    val groupReach: GroupReach = GroupReach()

    internal val calendarAccess: CalendarAccess by lazy { CalendarAccess(this) }

    val holidayWatch: HolidayWatch by lazy {
        HolidayWatch(calendarAccess, settingsStore, { calendarChanges(this) }, scope)
    }

    val chatAnswers: ChatAnswers by lazy { ChatAnswers(settingsStore, chatDecisionsStore) }

    val appIcons: AppIcons by lazy { AppIcons(scope, PackageIconSource(packageManager)) }

    private val notificationPermission: MutableStateFlow<NotificationPermission> by lazy {
        MutableStateFlow(NotificationPermission.of(this))
    }

    private val mutableSilencingRequests =
        MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    val silencingRequests: SharedFlow<Unit> = mutableSilencingRequests.asSharedFlow()

    private val mutableBoundaries =
        MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    val boundaries: SharedFlow<Unit> = mutableBoundaries.asSharedFlow()

    override fun onCreate() {
        super.onCreate()
        val notifier = ChatQuestionNotifier(this)

        PackageChanges.register(this) { app -> appIcons.forget(app) }

        scope.launch {
            combine(
                combine(
                    settingsStore.loads,
                    seenChatsStore.loads,
                    chatDecisionsStore.loads,
                    knownGroupsStore.loads,
                    ChatPrompt::of,
                ),
                notificationPermission,
                ::Pair,
            ).distinctUntilChanged()
                .collect { (prompt, permission) -> notifier.show(prompt, permission) }
        }
    }

    fun refreshNotificationPermission() {
        notificationPermission.value = NotificationPermission.of(this)
    }

    fun reachBoundary() {
        requestSilencing()
        mutableBoundaries.tryEmit(Unit)
    }

    fun requestSilencing() {
        mutableSilencingRequests.tryEmit(Unit)
    }
}
