package dev.danielgilchrist.knockoff.chat

import dev.danielgilchrist.knockoff.domain.chat.SeenChats
import dev.danielgilchrist.knockoff.domain.chat.SeenChatsDecode
import dev.danielgilchrist.knockoff.domain.rule.Target
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

class SeenChatsStore(
    file: File,
) {
    private val file = StagedFile(file)
    private val mutex = Mutex()
    private val writes = MutableStateFlow(0)

    val loads: Flow<SeenChatsLoad> = writes.map { read() }.distinctUntilChanged()

    suspend fun record(chat: Target.Chat): ChatRecord =
        mutex.withLock {
            when (val load = read()) {
                is SeenChatsLoad.Loaded -> write(load.seen, load.seen.seen(chat))
                is SeenChatsLoad.Corrupt -> write(SeenChats.NONE, SeenChats.NONE.seen(chat))
                is SeenChatsLoad.ReadFailed -> ChatRecord.Failed(load.detail)
            }
        }

    private suspend fun write(
        current: SeenChats,
        updated: SeenChats,
    ): ChatRecord =
        when {
            updated == current -> {
                ChatRecord.Unchanged
            }

            else -> {
                when (val write = file.write(updated.toJson())) {
                    FileWrite.Written -> {
                        writes.update { it + 1 }
                        ChatRecord.Recorded
                    }

                    is FileWrite.Failed -> {
                        ChatRecord.Failed(write.detail)
                    }
                }
            }
        }

    private suspend fun read(): SeenChatsLoad =
        when (val read = file.read()) {
            FileRead.Missing -> {
                SeenChatsLoad.Loaded(SeenChats.NONE)
            }

            is FileRead.Failed -> {
                SeenChatsLoad.ReadFailed(read.detail)
            }

            is FileRead.Undecodable -> {
                SeenChatsLoad.Corrupt(read.detail)
            }

            is FileRead.Text -> {
                when (val decode = SeenChats.fromJson(read.text)) {
                    is SeenChatsDecode.Decoded -> SeenChatsLoad.Loaded(decode.seen)
                    is SeenChatsDecode.Failure -> SeenChatsLoad.Corrupt(decode.detail)
                }
            }
        }
}
