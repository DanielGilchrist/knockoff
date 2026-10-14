package dev.danielgilchrist.knockoff

import android.app.Application
import dev.danielgilchrist.knockoff.settings.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.io.File

class KnockoffApplication : Application() {
    val settingsStore: SettingsStore by lazy {
        SettingsStore.create(File(filesDir, "settings.json"), CoroutineScope(SupervisorJob() + Dispatchers.IO))
    }
}
