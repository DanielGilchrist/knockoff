package dev.danielgilchrist.knockoff.settings

import dev.danielgilchrist.knockoff.domain.rule.RuleSetParse
import dev.danielgilchrist.knockoff.domain.settings.Settings
import dev.danielgilchrist.knockoff.domain.settings.SettingsDecode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.NoSuchFileException
import java.nio.file.StandardCopyOption

class SettingsStore(
    private val file: File,
) {
    private val staged = File(file.parentFile, "${file.name}.tmp")
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
                    when (val save = write(Settings(parse.rules))) {
                        SettingsSave.Saved -> SettingsUpdate.Saved
                        is SettingsSave.WriteFailed -> SettingsUpdate.IoFailed(save.detail)
                    }
                }
            }
        }

    private suspend fun read(): SettingsLoad =
        withContext(Dispatchers.IO) {
            val bytes =
                try {
                    Files.readAllBytes(file.toPath())
                } catch (_: NoSuchFileException) {
                    return@withContext SettingsLoad.Empty
                } catch (exception: IOException) {
                    return@withContext SettingsLoad.ReadFailed(exception.toString())
                }

            val text =
                try {
                    bytes.decodeToString(throwOnInvalidSequence = true)
                } catch (exception: CharacterCodingException) {
                    return@withContext SettingsLoad.Corrupt(SettingsDecode.Unreadable(exception.toString()))
                }

            when (val decode = Settings.fromJson(text)) {
                is SettingsDecode.Decoded -> SettingsLoad.Loaded(decode.settings)
                is SettingsDecode.Failure -> SettingsLoad.Corrupt(decode)
            }
        }

    private suspend fun write(settings: Settings): SettingsSave =
        withContext(Dispatchers.IO) {
            try {
                FileOutputStream(staged).use { output ->
                    output.write(settings.toJson().encodeToByteArray())
                    output.fd.sync()
                }
                Files.move(staged.toPath(), file.toPath(), StandardCopyOption.ATOMIC_MOVE)

                writes.update { it + 1 }
                SettingsSave.Saved
            } catch (exception: IOException) {
                SettingsSave.WriteFailed(exception.toString())
            }
        }
}
