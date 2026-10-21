package dev.danielgilchrist.knockoff.channel

import android.util.Log
import dev.danielgilchrist.knockoff.LOG_TAG
import dev.danielgilchrist.knockoff.domain.channel.ChannelGrouping
import dev.danielgilchrist.knockoff.domain.channel.ChannelKey
import dev.danielgilchrist.knockoff.domain.channel.ReadChannel
import dev.danielgilchrist.knockoff.domain.notification.ChannelGroupId
import dev.danielgilchrist.knockoff.domain.notification.ChannelId
import dev.danielgilchrist.knockoff.group.GroupRecord
import dev.danielgilchrist.knockoff.group.KnownGroupsStore

internal class GroupLearning(
    private val names: GroupNames,
    private val store: KnownGroupsStore,
) {
    suspend fun learn(read: Map<ChannelKey, ReadChannel>) {
        read.entries.groupBy { it.key.packageName }.forEach { (packageName, channels) ->
            when (val fetch = names.of(packageName)) {
                is GroupNamesFetch.Fetched -> {
                    report(store.update { it.read(packageName, groupsOf(channels), fetch.names) })
                }

                is GroupNamesFetch.NotLinked -> {
                    Log.w(LOG_TAG, "${packageName.value} group names unread, not linked: ${fetch.detail}")
                }

                GroupNamesFetch.Unbound -> {
                    Log.w(LOG_TAG, "${packageName.value} group names unread, listener unbound")
                }
            }
        }
    }

    private fun groupsOf(channels: List<Map.Entry<ChannelKey, ReadChannel>>): Map<ChannelId, ChannelGroupId> =
        channels
            .flatMap { (key, read) ->
                when (val grouping = read.grouping) {
                    is ChannelGrouping.Grouped -> listOf(key.channelId to grouping.id)
                    ChannelGrouping.Ungrouped -> emptyList()
                }
            }.toMap()

    private fun report(record: GroupRecord) {
        when (record) {
            GroupRecord.Recorded, GroupRecord.Unchanged -> {}

            is GroupRecord.Failed -> {
                Log.w(LOG_TAG, "channel groups not recorded: ${record.detail}")
            }
        }
    }
}
