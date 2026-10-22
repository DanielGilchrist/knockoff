package dev.danielgilchrist.knockoff.ui

import dev.danielgilchrist.knockoff.domain.group.GroupName
import dev.danielgilchrist.knockoff.domain.notification.AccountAddress
import dev.danielgilchrist.knockoff.domain.notification.AccountAddressParse

enum class GroupNoun(
    private val ones: List<String>,
    private val manys: List<String>,
) {
    Accounts(listOf("account"), listOf("accounts")),
    Categories(listOf("category"), listOf("categories")),
    AccountsAndCategories(listOf("account", "category"), listOf("accounts", "categories")),
    ;

    val one: String get() = listed(ones, "or")

    val many: String get() = listed(manys, "and")

    fun counted(count: Int): String = counted(count, one, many)

    fun oneOr(more: String): String = listed(ones + more, "or")

    fun manyAnd(more: String): String = listed(manys + more, "and")

    fun manyOr(vararg more: String): String = listed(manys + more, "or")

    fun names(name: GroupName): Boolean =
        when (this) {
            Accounts -> isAddress(name)
            Categories, AccountsAndCategories -> true
        }

    companion object {
        fun of(names: Collection<GroupName>): GroupNoun =
            when {
                names.any(::isAddress) -> Accounts
                else -> Categories
            }

        private fun isAddress(name: GroupName): Boolean =
            when (AccountAddress.parse(name.value)) {
                is AccountAddressParse.Valid -> true
                AccountAddressParse.NotAnAddress -> false
            }
    }
}
