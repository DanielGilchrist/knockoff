package dev.danielgilchrist.knockoff.domain.group

import dev.danielgilchrist.knockoff.domain.notification.AccountAddress
import dev.danielgilchrist.knockoff.domain.notification.ChannelGroupId
import dev.danielgilchrist.knockoff.domain.notification.ChannelId
import dev.danielgilchrist.knockoff.domain.notification.PackageName

@ConsistentCopyVisibility
data class KnownApp private constructor(
    val packageName: PackageName,
    val channels: Map<ChannelId, ChannelGroupId>,
    val names: Map<ChannelGroupId, GroupName>,
    val addresses: List<AccountAddress>,
) {
    companion object {
        private const val ADDRESS_LIMIT = 20

        fun of(
            packageName: PackageName,
            channels: Map<ChannelId, ChannelGroupId>,
            names: Map<ChannelGroupId, GroupName>,
            addresses: List<AccountAddress>,
        ): KnownApp = KnownApp(packageName, channels, names, addresses.distinct().take(ADDRESS_LIMIT))
    }

    val groups: List<KnownGroup>
        get() =
            (names.keys + channels.values)
                .distinct()
                .map { id ->
                    KnownGroup(
                        id,
                        when (val name = names[id]) {
                            null -> GroupLabel.Unnamed
                            else -> GroupLabel.Named(name)
                        },
                    )
                }

    fun withChannel(
        channel: ChannelId,
        group: ChannelGroupId,
    ): KnownApp = copy(channels = channels + (channel to group))

    fun withAddress(address: AccountAddress): KnownApp =
        when (address) {
            in addresses -> this
            else -> of(packageName, channels, names, listOf(address) + addresses)
        }
}
