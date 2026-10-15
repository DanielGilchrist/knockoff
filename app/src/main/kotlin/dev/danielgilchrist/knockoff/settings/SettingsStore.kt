package dev.danielgilchrist.knockoff.settings

import dev.danielgilchrist.knockoff.domain.rule.RuleSetParse
import dev.danielgilchrist.knockoff.domain.settings.Settings
import dev.danielgilchrist.knockoff.domain.settings.SettingsDecode
import dev.danielgilchrist.knockoff.storage.FileRead
import dev.danielgilchrist.knockoff.storage.FileWrite
import dev.danielgilchrist.knockoff.storage.StagedFile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File

class SettingsStore(
    file: File,
) {
    private val file = StagedFile(file)
    private val mutex = Mutex()
    private val writes = MutableStateFlow(0)

    val loads: Flow<SettingsLoad> = writes.map { read() }.distinctUntilChanged()

    suspend fun save(settings: Settings): SettingsSave = mutex.withLock { write(settings) }

    suspend fun update(change: (Settings) -> RuleSetParse): SettingsUpdate =
        mutex.withLock {
            val current =
                when (val load = read()) {
                    is SettingsLoad.Usable -> load.settings
                    is SettingsLoad.Corrupt -> return SettingsUpdate.NotLoaded(load.detail)
                    is SettingsLoad.ReadFailed -> return SettingsUpdate.IoFailed(load.detail)
                }

            when (val parse = change(current)) {
                is RuleSetParse.DuplicateNames -> {
                    SettingsUpdate.DuplicateNames(parse.names)
                }

                is RuleSetParse.Valid -> {
                    when (val save = write(current.copy(rules = parse.rules))) {
                        SettingsSave.Saved -> SettingsUpdate.Saved
                        is SettingsSave.WriteFailed -> SettingsUpdate.IoFailed(save.detail)
                    }
                }
            }
        }

    suspend fun modify(change: (Settings) -> Settings): SettingsChange =
        mutex.withLock {
            when (val load = read()) {
                is SettingsLoad.Usable -> {
                    when (val save = write(change(load.settings))) {
                        SettingsSave.Saved -> SettingsChange.Saved
                        is SettingsSave.WriteFailed -> SettingsChange.Failed(save.detail)
                    }
                }

                is SettingsLoad.Failure -> {
                    SettingsChange.Failed(load.detail)
                }
            }
        }

    private suspend fun read(): SettingsLoad =
        when (val read = file.read()) {
            FileRead.Missing -> {
                SettingsLoad.Empty
            }

            is FileRead.Failed -> {
                SettingsLoad.ReadFailed(read.detail)
            }

            is FileRead.Undecodable -> {
                SettingsLoad.Corrupt(SettingsDecode.Unreadable(read.detail))
            }

            is FileRead.Text -> {
                when (val decode = Settings.fromJson(read.text)) {
                    is SettingsDecode.Decoded -> SettingsLoad.Loaded(decode.settings)
                    is SettingsDecode.Failure -> SettingsLoad.Corrupt(decode)
                }
            }
        }

    private suspend fun write(settings: Settings): SettingsSave =
        when (val write = file.write(settings.toJson())) {
            FileWrite.Written -> {
                writes.update { it + 1 }
                SettingsSave.Saved
            }

            is FileWrite.Failed -> {
                SettingsSave.WriteFailed(write.detail)
            }
        }
}
