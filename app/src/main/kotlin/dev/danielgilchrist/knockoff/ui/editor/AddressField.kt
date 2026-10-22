package dev.danielgilchrist.knockoff.ui.editor

import dev.danielgilchrist.knockoff.domain.group.GroupName
import dev.danielgilchrist.knockoff.domain.notification.AccountAddress
import dev.danielgilchrist.knockoff.domain.notification.AccountAddressParse
import dev.danielgilchrist.knockoff.domain.rule.AccountId

sealed interface AddressField {
    data object Closed : AddressField

    data class Open(
        val text: String,
        val problem: AddressProblem,
    ) : AddressField {
        fun edited(text: String): Open = Open(text, AddressProblem.None)

        fun submitted(
            offered: Map<AccountId, GroupName>,
            picked: Set<AccountId>,
        ): AddressSubmission =
            when (val parse = AccountAddress.parse(text)) {
                is AccountAddressParse.Valid -> placed(parse.address, offered, picked)
                AccountAddressParse.NotAnAddress -> AddressSubmission.Invalid
            }

        fun answered(submission: AddressSubmission): AddressField =
            when (submission) {
                is AddressSubmission.Added,
                is AddressSubmission.TicksListed,
                is AddressSubmission.MatchesGroup,
                -> Closed

                AddressSubmission.Invalid -> copy(problem = AddressProblem.NotAnAddress)

                is AddressSubmission.Duplicate -> copy(problem = AddressProblem.AlreadyListed(submission.address))
            }

        private fun placed(
            address: AccountAddress,
            offered: Map<AccountId, GroupName>,
            picked: Set<AccountId>,
        ): AddressSubmission {
            val id = AccountId.Address(address)
            val group =
                offered.entries.firstOrNull { (offeredId, name) ->
                    offeredId is AccountId.Group &&
                        name.names(address)
                }

            return when {
                id in picked -> AddressSubmission.Duplicate(address)
                id in offered -> AddressSubmission.TicksListed(address)
                group != null -> AddressSubmission.MatchesGroup(group.key, group.value)
                else -> AddressSubmission.Added(address)
            }
        }
    }

    companion object {
        val EMPTY: AddressField get() = Open("", AddressProblem.None)
    }
}
