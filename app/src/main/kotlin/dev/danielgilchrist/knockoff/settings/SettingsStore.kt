package dev.danielgilchrist.knockoff.settings

import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import dev.danielgilchrist.knockoff.domain.settings.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.File
import java.io.IOException

class SettingsStore(
    private val dataStore: DataStore<SettingsFile>,
) {
    companion object {
        fun create(
            file: File,
            scope: CoroutineScope,
        ): SettingsStore = SettingsStore(DataStoreFactory.create(SettingsFileSerializer, scope = scope) { file })
    }

    val loads: Flow<SettingsLoad> =
        dataStore.data
            .map { file ->
                when (file) {
                    SettingsFile.Absent -> SettingsLoad.Empty
                    is SettingsFile.Loaded -> SettingsLoad.Loaded(file.settings)
                    is SettingsFile.Corrupt -> SettingsLoad.Corrupt(file.failure)
                }
            }.catch { cause ->
                when (cause) {
                    is IOException -> emit(SettingsLoad.ReadFailed(cause.toString()))
                    else -> throw cause
                }
            }

    suspend fun save(settings: Settings): SettingsSave =
        try {
            dataStore.updateData { SettingsFile.Loaded(settings) }
            SettingsSave.Saved
        } catch (exception: IOException) {
            SettingsSave.WriteFailed(exception.toString())
        }
}
