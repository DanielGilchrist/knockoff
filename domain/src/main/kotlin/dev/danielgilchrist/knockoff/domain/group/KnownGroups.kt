package dev.danielgilchrist.knockoff.domain.group

import dev.danielgilchrist.knockoff.domain.channel.ChannelGrouping
import dev.danielgilchrist.knockoff.domain.channel.ChannelKey
import dev.danielgilchrist.knockoff.domain.json.VersionedDecode
import dev.danielgilchrist.knockoff.domain.json.VersionedJson
import dev.danielgilchrist.knockoff.domain.notification.AccountAddress
import dev.danielgilchrist.knockoff.domain.notification.Channel
import dev.danielgilchrist.knockoff.domain.notification.ChannelGroupId
import dev.danielgilchrist.knockoff.domain.notification.ChannelId
import dev.danielgilchrist.knockoff.domain.notification.NotificationCategory
import dev.danielgilchrist.knockoff.domain.notification.Origin
import dev.danielgilchrist.knockoff.domain.notification.OriginAddress
import dev.danielgilchrist.knockoff.domain.notification.OriginGroup
import dev.danielgilchrist.knockoff.domain.notification.PackageName
import dev.danielgilchrist.knockoff.domain.rule.Target

@ConsistentCopyVisibility
data class KnownGroups private constructor(
    val apps: List<KnownApp>,
) {
    companion object {
        const val LIMIT = 200

        val NONE: KnownGroups get() = KnownGroups(emptyList())

        internal fun of(apps: List<KnownApp>): KnownGroups = KnownGroups(apps.distinctBy { it.packageName }.take(LIMIT))

        fun fromJson(text: String): KnownGroupsDecode =
            when (
                val decode =
                    VersionedJson.decode<StoredKnownGroupsDocument>(
                        text,
                        mapOf(
                            StoredKnownGroupsV1.VERSION to StoredKnownGroupsV1.serializer(),
                            StoredKnownGroups.CURRENT_VERSION to StoredKnownGroups.serializer(),
                        ),
                    )
            ) {
                is VersionedDecode.Decoded -> decode.value.toKnownGroups()
                is VersionedDecode.Unreadable -> KnownGroupsDecode.Unreadable(decode.detail)
                is VersionedDecode.UnsupportedVersion -> KnownGroupsDecode.UnsupportedVersion(decode.version)
            }
    }

    fun sighted(origin: Origin): KnownGroups = sightedGroup(origin).sightedAddress(origin)

    private fun sightedAddress(origin: Origin): KnownGroups =
        when (val address = origin.address) {
            OriginAddress.Unaddressed -> {
                this
            }

            is OriginAddress.Addressed -> {
                val current = appOf(origin.packageName)

                when (origin.category) {
                    NotificationCategory.Email -> {
                        when (val learned = current.withAddress(address.address)) {
                            current -> this
                            else -> of(listOf(learned) + apps)
                        }
                    }

                    NotificationCategory.Other -> {
                        this
                    }
                }
            }
        }

    private fun sightedGroup(origin: Origin): KnownGroups =
        when (val channel = origin.channel) {
            Channel.Unnamed -> {
                this
            }

            is Channel.Named -> {
                when (val group = origin.group) {
                    OriginGroup.Unknown -> this
                    is OriginGroup.Known -> sighted(ChannelKey(origin.packageName, channel.id), group.grouping)
                }
            }
        }

    fun sighted(
        channel: ChannelKey,
        grouping: ChannelGrouping,
    ): KnownGroups =
        when (grouping) {
            ChannelGrouping.Ungrouped -> {
                this
            }

            is ChannelGrouping.Grouped -> {
                when (groupOf(channel)) {
                    OriginGroup.Known(grouping) -> this
                    else -> of(listOf(appOf(channel.packageName).withChannel(channel.channelId, grouping.id)) + apps)
                }
            }
        }

    fun read(
        packageName: PackageName,
        channels: Map<ChannelId, ChannelGroupId>,
        names: Map<ChannelGroupId, GroupName>,
    ): KnownGroups {
        val current = appOf(packageName)
        val learned = KnownApp.of(packageName, current.channels + channels, names, current.addresses)

        return when (learned) {
            current -> this
            else -> of(listOf(learned) + apps)
        }
    }

    fun groupOf(channel: ChannelKey): OriginGroup =
        when (val group = appOf(channel.packageName).channels[channel.channelId]) {
            null -> OriginGroup.Unknown
            else -> OriginGroup.Known(ChannelGrouping.Grouped(group))
        }

    fun groupingOf(chat: Target.Chat): ChannelGrouping =
        when (val group = groupOf(ChannelKey(chat.packageName, chat.channel))) {
            OriginGroup.Unknown -> ChannelGrouping.Ungrouped
            is OriginGroup.Known -> group.grouping
        }

    fun groupsOf(packageName: PackageName): List<KnownGroup> = appOf(packageName).groups

    fun addressesOf(packageName: PackageName): List<AccountAddress> = appOf(packageName).addresses

    fun toJson(): String = VersionedJson.json.encodeToString(StoredKnownGroups.from(this))

    private fun appOf(packageName: PackageName): KnownApp =
        apps.firstOrNull { it.packageName == packageName }
            ?: KnownApp.of(packageName, emptyMap(), emptyMap(), emptyList())
}
