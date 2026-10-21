package dev.danielgilchrist.knockoff.listener

import android.util.Log
import dev.danielgilchrist.knockoff.LOG_TAG
import dev.danielgilchrist.knockoff.domain.notification.Origin
import dev.danielgilchrist.knockoff.group.GroupRecord
import dev.danielgilchrist.knockoff.group.KnownGroupsStore

internal suspend fun KnownGroupsStore.record(origin: Origin) {
    when (val record = update { it.sighted(origin) }) {
        GroupRecord.Recorded, GroupRecord.Unchanged -> {}

        is GroupRecord.Failed -> {
            Log.w(LOG_TAG, "${origin.packageName.value} channel group not recorded: ${record.detail}")
        }
    }
}
