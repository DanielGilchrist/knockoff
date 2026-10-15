package dev.danielgilchrist.knockoff

import android.app.Application
import dev.danielgilchrist.knockoff.settings.SettingsStore
import java.io.File

class KnockoffApplication : Application() {
    val settingsStore: SettingsStore by lazy {
        SettingsStore(File(filesDir, "settings.json"))
    }
}
