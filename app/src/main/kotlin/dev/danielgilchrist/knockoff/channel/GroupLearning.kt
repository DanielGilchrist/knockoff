package dev.danielgilchrist.knockoff.channel

import android.util.Log
import dev.danielgilchrist.knockoff.LOG_TAG
import dev.danielgilchrist.knockoff.domain.channel.ChannelGrouping
import dev.danielgilchrist.knockoff.domain.channel.ChannelKey
import dev.danielgilchrist.knockoff.domain.channel.ReadChannel
import dev.danielgilchrist.knockoff.domain.notification.ChannelGroupId
import dev.danielgilchrist.knockoff.domain.notification.ChannelId
import dev.danielgilchrist.knockoff.domain.notification.PackageName
import dev.danielgilchrist.knockoff.group.GroupDiscovery
import dev.danielgilchrist.knockoff.group.GroupRecord
import dev.danielgilchrist.knockoff.group.KnownGroupsStore

internal class GroupLearning(
    private val names: GroupNameSource,
    private val store: KnownGroupsStore,
) {
    suspend fun learn(read: Map<ChannelKey, ReadChannel>) {
        read.entries.groupBy { it.key.packageName }.forEach { (packageName, channels) ->
            report(packageName, learn(packageName, channels.associate { it.key to it.value }))
        }
    }

    suspend fun learn(
        packageName: PackageName,
        channels: Map<ChannelKey, ReadChannel>,
    ): GroupDiscovery =
        when (val fetch = names.of(packageName)) {
            is GroupNamesFetch.Fetched -> {
                when (val record = store.update { it.read(packageName, groupsOf(channels), fetch.names) }) {
                    GroupRecord.Recorded, GroupRecord.Unchanged -> GroupDiscovery.Read
                    is GroupRecord.Failed -> GroupDiscovery.Unrecorded(record.detail)
                }
            }

            is GroupNamesFetch.NotLinked -> {
                GroupDiscovery.NotLinked(fetch.detail)
            }

            GroupNamesFetch.Unbound -> {
                GroupDiscovery.Unbound
            }
        }

    private fun groupsOf(channels: Map<ChannelKey, ReadChannel>): Map<ChannelId, ChannelGroupId> =
        channels
            .flatMap { (key, read) ->
                when (val grouping = read.grouping) {
                    is ChannelGrouping.Grouped -> listOf(key.channelId to grouping.id)
                    ChannelGrouping.Ungrouped -> emptyList()
                }
            }.toMap()

    private fun report(
        packageName: PackageName,
        discovery: GroupDiscovery,
    ) {
        when (discovery) {
            GroupDiscovery.Read -> {}

            is GroupDiscovery.NotLinked -> {
                Log.w(LOG_TAG, "${packageName.value} group names unread, not linked: ${discovery.detail}")
            }

            GroupDiscovery.Unbound -> {
                Log.w(LOG_TAG, "${packageName.value} group names unread, listener unbound")
            }

            is GroupDiscovery.Unrecorded -> {
                Log.w(LOG_TAG, "channel groups not recorded: ${discovery.detail}")
            }
        }
    }
}
