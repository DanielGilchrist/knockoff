package dev.danielgilchrist.knockoff.domain.rule

import dev.danielgilchrist.knockoff.domain.group.GroupName
import dev.danielgilchrist.knockoff.domain.notification.AccountAddress
import dev.danielgilchrist.knockoff.domain.notification.ChannelGroupId
import dev.danielgilchrist.knockoff.domain.notification.PackageName

private const val GMAIL_ACCOUNT_GROUP = "^ncg_1_account_"

fun AccountAddress.gmailAccount(): Target.Account =
    Target.Account(
        PackageName.GMAIL,
        ChannelGroupId(GMAIL_ACCOUNT_GROUP + value),
        GroupName.of(this),
    )

fun Target.Account.gmailAddress(): String = group.value.removePrefix(GMAIL_ACCOUNT_GROUP)
