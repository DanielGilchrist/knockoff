package dev.danielgilchrist.knockoff

import android.app.Application
import dev.danielgilchrist.knockoff.channel.SilencedChannelsStore
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

    private val mutableSilencingRequests =
        MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    val silencingRequests: SharedFlow<Unit> = mutableSilencingRequests.asSharedFlow()

    fun requestSilencing() {
        mutableSilencingRequests.tryEmit(Unit)
    }
}
