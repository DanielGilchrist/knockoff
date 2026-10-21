package dev.danielgilchrist.knockoff.domain.group

import dev.danielgilchrist.knockoff.domain.notification.AccountAddress

@JvmInline
value class GroupName private constructor(
    val value: String,
) {
    companion object {
        fun of(address: AccountAddress): GroupName = GroupName(address.value)

        fun parse(raw: String): GroupNameParse =
            when {
                raw.isBlank() -> GroupNameParse.Blank
                else -> GroupNameParse.Valid(GroupName(raw.trim()))
            }
    }
}
