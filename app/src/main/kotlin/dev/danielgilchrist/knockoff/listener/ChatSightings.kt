package dev.danielgilchrist.knockoff.listener

import android.util.Log
import dev.danielgilchrist.knockoff.LOG_TAG
import dev.danielgilchrist.knockoff.chat.ChatRecord
import dev.danielgilchrist.knockoff.chat.SeenChatsStore
import dev.danielgilchrist.knockoff.domain.chat.ChatSighting

internal suspend fun SeenChatsStore.record(sighting: ChatSighting) {
    when (sighting) {
        ChatSighting.NotAChat -> {}

        is ChatSighting.Seen -> {
            when (val record = record(sighting.chat)) {
                ChatRecord.Recorded, ChatRecord.Unchanged -> {}

                is ChatRecord.Failed -> {
                    Log.w(LOG_TAG, "${sighting.chat.packageName.value} chat not recorded: ${record.detail}")
                }
            }
        }
    }
}
