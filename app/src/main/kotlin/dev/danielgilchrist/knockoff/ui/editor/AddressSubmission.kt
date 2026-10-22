package dev.danielgilchrist.knockoff.ui.editor

import dev.danielgilchrist.knockoff.domain.group.GroupName
import dev.danielgilchrist.knockoff.domain.notification.AccountAddress
import dev.danielgilchrist.knockoff.domain.rule.AccountId

sealed interface AddressSubmission {
    data class Added(
        val address: AccountAddress,
    ) : AddressSubmission

    data class TicksListed(
        val address: AccountAddress,
    ) : AddressSubmission

    data object Invalid : AddressSubmission

    data class Duplicate(
        val address: AccountAddress,
    ) : AddressSubmission

    data class MatchesGroup(
        val id: AccountId,
        val name: GroupName,
    ) : AddressSubmission
}
