package dev.danielgilchrist.knockoff.domain.settings

import dev.danielgilchrist.knockoff.domain.group.GroupName
import dev.danielgilchrist.knockoff.domain.group.GroupNameParse
import dev.danielgilchrist.knockoff.domain.notification.ChannelGroupId
import kotlinx.serialization.Serializable

@Serializable
internal data class StoredAccount(
    private val group: String,
    private val label: String,
) {
    fun parse(): Parsed<Pair<ChannelGroupId, GroupName>> =
        when {
            group.isBlank() -> {
                Parsed.Failed("blank account group")
            }

            else -> {
                when (val name = GroupName.parse(label)) {
                    is GroupNameParse.Valid -> Parsed.Ok(ChannelGroupId(group) to name.name)
                    GroupNameParse.Blank -> Parsed.Failed("blank account label")
                }
            }
        }
}
