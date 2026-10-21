package dev.danielgilchrist.knockoff.ui

import dev.danielgilchrist.knockoff.domain.group.GroupName

enum class GroupNoun(
    val one: String,
    val many: String,
) {
    Accounts("account", "accounts"),
    Categories("category", "categories"),
    ;

    fun counted(count: Int): String = counted(count, one, many)

    fun names(name: GroupName): Boolean =
        when (this) {
            Accounts -> '@' in name.value
            Categories -> true
        }

    companion object {
        fun of(names: Collection<GroupName>): GroupNoun =
            when {
                names.any { Accounts.names(it) } -> Accounts
                else -> Categories
            }
    }
}
