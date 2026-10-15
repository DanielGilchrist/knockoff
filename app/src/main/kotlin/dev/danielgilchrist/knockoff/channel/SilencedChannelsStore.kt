package dev.danielgilchrist.knockoff.channel

import dev.danielgilchrist.knockoff.domain.channel.SilencedChannels
import dev.danielgilchrist.knockoff.domain.channel.SilencedChannelsDecode
import dev.danielgilchrist.knockoff.storage.FileRead
import dev.danielgilchrist.knockoff.storage.FileWrite
import dev.danielgilchrist.knockoff.storage.StagedFile
import java.io.File

class SilencedChannelsStore(
    file: File,
) {
    private val file = StagedFile(file)

    suspend fun load(): SilencedChannelsLoad =
        when (val read = file.read()) {
            FileRead.Missing -> {
                SilencedChannelsLoad.Loaded(SilencedChannels.NONE)
            }

            is FileRead.Failed -> {
                SilencedChannelsLoad.Failed(read.detail)
            }

            is FileRead.Undecodable -> {
                SilencedChannelsLoad.Failed(read.detail)
            }

            is FileRead.Text -> {
                when (val decode = SilencedChannels.fromJson(read.text)) {
                    is SilencedChannelsDecode.Decoded -> SilencedChannelsLoad.Loaded(decode.silenced)
                    is SilencedChannelsDecode.Failure -> SilencedChannelsLoad.Failed(decode.detail)
                }
            }
        }

    suspend fun save(silenced: SilencedChannels): SilencedChannelsSave =
        when (val write = file.write(silenced.toJson())) {
            FileWrite.Written -> SilencedChannelsSave.Saved
            is FileWrite.Failed -> SilencedChannelsSave.Failed(write.detail)
        }
}
