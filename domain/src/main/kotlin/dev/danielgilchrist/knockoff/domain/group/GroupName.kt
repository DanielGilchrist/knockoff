package dev.danielgilchrist.knockoff.domain.group

import dev.danielgilchrist.knockoff.domain.notification.AccountAddress
import dev.danielgilchrist.knockoff.domain.notification.ChannelGroupId

@JvmInline
value class GroupName private constructor(
    val value: String,
) {
    companion object {
        fun of(id: ChannelGroupId): GroupName = GroupName(id.value)

        fun of(address: AccountAddress): GroupName = GroupName(address.value)

        fun parse(raw: String): GroupNameParse =
            when {
                raw.isBlank() -> GroupNameParse.Blank
                else -> GroupNameParse.Valid(GroupName(raw.trim()))
            }
    }

    fun names(address: AccountAddress): Boolean = value.equals(address.value, ignoreCase = true)
}
