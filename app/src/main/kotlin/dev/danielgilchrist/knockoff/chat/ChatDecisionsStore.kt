package dev.danielgilchrist.knockoff.chat

import dev.danielgilchrist.knockoff.domain.chat.ChatDecisions
import dev.danielgilchrist.knockoff.domain.chat.ChatDecisionsDecode
import dev.danielgilchrist.knockoff.domain.rule.ChatKey
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

class ChatDecisionsStore(
    file: File,
) {
    private val file = StagedFile(file)
    private val mutex = Mutex()
    private val writes = MutableStateFlow(0)

    val loads: Flow<ChatDecisionsLoad> = writes.map { read() }.distinctUntilChanged()

    suspend fun letThrough(key: ChatKey): ChatDecisionSave =
        mutex.withLock {
            when (val load = read()) {
                is ChatDecisionsLoad.Loaded -> write(load.decisions.lettingThrough(key))
                is ChatDecisionsLoad.Corrupt -> write(ChatDecisions.NONE.lettingThrough(key))
                is ChatDecisionsLoad.ReadFailed -> ChatDecisionSave.Failed(load.detail)
            }
        }

    private suspend fun write(decisions: ChatDecisions): ChatDecisionSave =
        when (val write = file.write(decisions.toJson())) {
            FileWrite.Written -> {
                writes.update { it + 1 }
                ChatDecisionSave.Saved
            }

            is FileWrite.Failed -> {
                ChatDecisionSave.Failed(write.detail)
            }
        }

    private suspend fun read(): ChatDecisionsLoad =
        when (val read = file.read()) {
            FileRead.Missing -> {
                ChatDecisionsLoad.Loaded(ChatDecisions.NONE)
            }

            is FileRead.Failed -> {
                ChatDecisionsLoad.ReadFailed(read.detail)
            }

            is FileRead.Undecodable -> {
                ChatDecisionsLoad.Corrupt(read.detail)
            }

            is FileRead.Text -> {
                when (val decode = ChatDecisions.fromJson(read.text)) {
                    is ChatDecisionsDecode.Decoded -> ChatDecisionsLoad.Loaded(decode.decisions)
                    is ChatDecisionsDecode.Failure -> ChatDecisionsLoad.Corrupt(decode.detail)
                }
            }
        }
}
