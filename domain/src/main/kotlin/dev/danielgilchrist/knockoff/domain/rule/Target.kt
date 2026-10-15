package dev.danielgilchrist.knockoff.domain.rule

import dev.danielgilchrist.knockoff.domain.notification.AccountAddress
import dev.danielgilchrist.knockoff.domain.notification.Channel
import dev.danielgilchrist.knockoff.domain.notification.Origin
import dev.danielgilchrist.knockoff.domain.notification.PackageName

sealed interface Target {
    val packageName: PackageName

    data class App(
        override val packageName: PackageName,
    ) : Target

    data class GmailInbox(
        val address: AccountAddress,
    ) : Target {
        override val packageName: PackageName get() = PackageName.GMAIL
    }

    fun matches(origin: Origin): Boolean =
        when (this) {
            is App -> {
                origin.packageName == packageName
            }

            is GmailInbox -> {
                origin.packageName == packageName &&
                    when (val channel = origin.channel) {
                        is Channel.Named -> channel.id.endsWithAccount(address)
                        Channel.Unnamed -> false
                    }
            }
        }
}
