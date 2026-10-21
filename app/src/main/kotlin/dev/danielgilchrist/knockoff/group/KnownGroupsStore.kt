package dev.danielgilchrist.knockoff.group

import dev.danielgilchrist.knockoff.domain.group.KnownGroups
import dev.danielgilchrist.knockoff.domain.group.KnownGroupsDecode
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

class KnownGroupsStore(
    file: File,
) {
    private val file = StagedFile(file)
    private val mutex = Mutex()
    private val writes = MutableStateFlow(0)

    val loads: Flow<KnownGroupsLoad> = writes.map { read() }.distinctUntilChanged()

    suspend fun current(): KnownGroups = read().known

    suspend fun update(change: (KnownGroups) -> KnownGroups): GroupRecord =
        mutex.withLock {
            when (val load = read()) {
                is KnownGroupsLoad.Loaded -> write(load.groups, change(load.groups))
                is KnownGroupsLoad.Corrupt -> write(KnownGroups.NONE, change(KnownGroups.NONE))
                is KnownGroupsLoad.ReadFailed -> GroupRecord.Failed(load.detail)
            }
        }

    private suspend fun write(
        current: KnownGroups,
        updated: KnownGroups,
    ): GroupRecord =
        when {
            updated == current -> {
                GroupRecord.Unchanged
            }

            else -> {
                when (val write = file.write(updated.toJson())) {
                    FileWrite.Written -> {
                        writes.update { it + 1 }
                        GroupRecord.Recorded
                    }

                    is FileWrite.Failed -> {
                        GroupRecord.Failed(write.detail)
                    }
                }
            }
        }

    private suspend fun read(): KnownGroupsLoad =
        when (val read = file.read()) {
            FileRead.Missing -> {
                KnownGroupsLoad.Loaded(KnownGroups.NONE)
            }

            is FileRead.Failed -> {
                KnownGroupsLoad.ReadFailed(read.detail)
            }

            is FileRead.Undecodable -> {
                KnownGroupsLoad.Corrupt(read.detail)
            }

            is FileRead.Text -> {
                when (val decode = KnownGroups.fromJson(read.text)) {
                    is KnownGroupsDecode.Decoded -> KnownGroupsLoad.Loaded(decode.groups)
                    is KnownGroupsDecode.Failure -> KnownGroupsLoad.Corrupt(decode.detail)
                }
            }
        }
}
