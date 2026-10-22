package dev.danielgilchrist.knockoff.domain.group

import dev.danielgilchrist.knockoff.domain.notification.AccountAddress
import dev.danielgilchrist.knockoff.domain.notification.AccountAddressParse
import dev.danielgilchrist.knockoff.domain.notification.ChannelGroupId
import dev.danielgilchrist.knockoff.domain.notification.ChannelId
import dev.danielgilchrist.knockoff.domain.notification.PackageName
import kotlinx.serialization.Serializable

@Serializable
internal data class StoredKnownApp(
    private val app: String,
    private val channels: Map<String, String>,
    private val names: Map<String, String>,
    private val addresses: List<String>,
) {
    companion object {
        fun from(app: KnownApp): StoredKnownApp =
            StoredKnownApp(
                app.packageName.value,
                app.channels.entries.associate { (channel, group) -> channel.value to group.value },
                app.names.entries.associate { (group, name) -> group.value to name.value },
                app.addresses.map { it.value },
            )
    }

    fun toKnownApp(): KnownApp =
        KnownApp.of(
            PackageName(app),
            channels.entries.associate { (channel, group) -> ChannelId(channel) to ChannelGroupId(group) },
            names.entries
                .flatMap { (group, name) ->
                    when (val parse = GroupName.parse(name)) {
                        is GroupNameParse.Valid -> listOf(ChannelGroupId(group) to parse.name)
                        GroupNameParse.Blank -> emptyList()
                    }
                }.toMap(),
            addresses
                .flatMap { raw ->
                    when (val parse = AccountAddress.parse(raw)) {
                        is AccountAddressParse.Valid -> listOf(parse.address)
                        AccountAddressParse.NotAnAddress -> emptyList()
                    }
                },
        )
}
