package dev.danielgilchrist.knockoff.domain.rule

import dev.danielgilchrist.knockoff.domain.notification.AccountAddress
import dev.danielgilchrist.knockoff.domain.notification.ChannelGroupId

sealed interface AccountId {
    data class Group(
        val id: ChannelGroupId,
    ) : AccountId

    data class Address(
        val address: AccountAddress,
    ) : AccountId
}
