package dev.danielgilchrist.knockoff.ui.editor

import dev.danielgilchrist.knockoff.domain.group.GroupName
import dev.danielgilchrist.knockoff.domain.rule.AccountId
import dev.danielgilchrist.knockoff.ui.GroupNoun
import dev.danielgilchrist.knockoff.ui.listed

sealed interface AccountList {
    val noun: GroupNoun
    val groupNoun: GroupNoun
    val offered: Map<AccountId, GroupName>

    fun counted(picked: Collection<AccountId>): List<String>

    val choosing: String get() = listed(counted(offered.keys), "and")

    data class Single(
        override val noun: GroupNoun,
        override val offered: Map<AccountId, GroupName>,
    ) : AccountList {
        override val groupNoun: GroupNoun get() = noun

        override fun counted(picked: Collection<AccountId>): List<String> = listOf(noun.counted(picked.size))
    }

    data class Split(
        val categories: Map<AccountId, GroupName>,
        val accounts: Map<AccountId, GroupName>,
    ) : AccountList {
        override val noun: GroupNoun get() = GroupNoun.AccountsAndCategories

        override val groupNoun: GroupNoun get() = GroupNoun.Categories

        override val offered: Map<AccountId, GroupName> get() = categories + accounts

        override fun counted(picked: Collection<AccountId>): List<String> {
            val addresses = picked.count { it is AccountId.Address }
            val groups = picked.size - addresses

            return listOf(GroupNoun.Accounts to addresses, GroupNoun.Categories to groups)
                .filter { (_, count) -> count > 0 }
                .map { (noun, count) -> noun.counted(count) }
        }
    }

    companion object {
        fun of(
            groupNoun: GroupNoun,
            groups: Map<AccountId, GroupName>,
            addresses: Map<AccountId, GroupName>,
        ): AccountList =
            when {
                addresses.isEmpty() -> Single(groupNoun, groups)
                groups.isEmpty() || groupNoun == GroupNoun.Accounts -> Single(GroupNoun.Accounts, groups + addresses)
                else -> Split(groups, addresses)
            }
    }
}
