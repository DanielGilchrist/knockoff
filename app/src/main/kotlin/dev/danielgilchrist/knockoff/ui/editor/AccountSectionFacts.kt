package dev.danielgilchrist.knockoff.ui.editor

import dev.danielgilchrist.knockoff.domain.group.GroupName
import dev.danielgilchrist.knockoff.domain.rule.AccountId
import dev.danielgilchrist.knockoff.ui.GroupNoun

data class AccountSectionFacts(
    val label: String,
    val kind: AppKind,
    val accounts: AccountChoices,
    val offer: AddressOffer,
) {
    val categories: Map<AccountId, GroupName>
        get() =
            when (val list = accounts.list) {
                is AccountList.Single -> {
                    when (list.noun) {
                        GroupNoun.Categories -> list.offered
                        GroupNoun.Accounts, GroupNoun.AccountsAndCategories -> emptyMap()
                    }
                }

                is AccountList.Split -> {
                    list.categories
                }
            }

    val accountItems: Map<AccountId, GroupName>
        get() = accounts.offered - categories.keys

    val listsAccounts: Boolean
        get() =
            accountItems.isNotEmpty() ||
                when (offer) {
                    AddressOffer.Shown -> true
                    AddressOffer.Hidden -> false
                }

    val listsAnything: Boolean
        get() = categories.isNotEmpty() || listsAccounts

    val headed: Boolean
        get() = kind.hasChats || (categories.isNotEmpty() && listsAccounts)
}
