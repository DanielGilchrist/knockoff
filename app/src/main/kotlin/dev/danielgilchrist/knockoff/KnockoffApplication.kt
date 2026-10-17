package dev.danielgilchrist.knockoff

import android.app.Application
import dev.danielgilchrist.knockoff.channel.SilencedChannelsStore
import dev.danielgilchrist.knockoff.chat.ChatDecisionsStore
import dev.danielgilchrist.knockoff.chat.SeenChatsStore
import dev.danielgilchrist.knockoff.settings.SettingsStore
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.io.File

class KnockoffApplication : Application() {
    val settingsStore: SettingsStore by lazy {
        SettingsStore(File(filesDir, "settings.json"))
    }

    val silencedChannelsStore: SilencedChannelsStore by lazy {
        SilencedChannelsStore(File(filesDir, "silenced-channels.json"))
    }

    val seenChatsStore: SeenChatsStore by lazy {
        SeenChatsStore(File(filesDir, "seen-chats.json"))
    }

    val chatDecisionsStore: ChatDecisionsStore by lazy {
        ChatDecisionsStore(File(filesDir, "chat-decisions.json"))
    }

    private val mutableSilencingRequests =
        MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    val silencingRequests: SharedFlow<Unit> = mutableSilencingRequests.asSharedFlow()

    fun requestSilencing() {
        mutableSilencingRequests.tryEmit(Unit)
    }
}
