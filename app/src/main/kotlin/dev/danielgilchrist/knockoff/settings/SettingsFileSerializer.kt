package dev.danielgilchrist.knockoff.settings

import androidx.datastore.core.Serializer
import dev.danielgilchrist.knockoff.domain.settings.Settings
import dev.danielgilchrist.knockoff.domain.settings.SettingsDecode
import java.io.InputStream
import java.io.OutputStream

object SettingsFileSerializer : Serializer<SettingsFile> {
    override val defaultValue: SettingsFile = SettingsFile.Absent

    override suspend fun readFrom(input: InputStream): SettingsFile {
        val text =
            try {
                input.readBytes().decodeToString(throwOnInvalidSequence = true)
            } catch (exception: CharacterCodingException) {
                return SettingsFile.Corrupt(SettingsDecode.Unreadable(exception.toString()))
            }

        return when (val decode = Settings.fromJson(text)) {
            is SettingsDecode.Decoded -> SettingsFile.Loaded(decode.settings)
            is SettingsDecode.Failure -> SettingsFile.Corrupt(decode)
        }
    }

    override suspend fun writeTo(
        t: SettingsFile,
        output: OutputStream,
    ) {
        when (t) {
            is SettingsFile.Loaded -> output.write(t.settings.toJson().encodeToByteArray())
            SettingsFile.Absent, is SettingsFile.Corrupt -> error("Only loaded settings are written, not $t")
        }
    }
}
