package dev.danielgilchrist.knockoff.domain.settings

import dev.danielgilchrist.knockoff.domain.group.GroupName
import dev.danielgilchrist.knockoff.domain.notification.AccountAddress
import dev.danielgilchrist.knockoff.domain.notification.AccountAddressParse
import dev.danielgilchrist.knockoff.domain.rule.AccountId
import dev.danielgilchrist.knockoff.domain.rule.AppScope
import dev.danielgilchrist.knockoff.domain.rule.ChatPick
import dev.danielgilchrist.knockoff.domain.rule.NarrowedParse
import dev.danielgilchrist.knockoff.domain.rule.NewChatsAsking
import dev.danielgilchrist.knockoff.domain.rule.PatternPick
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal sealed interface StoredAppScope {
    fun parse(): Parsed<AppScope>

    @Serializable
    @SerialName("everything")
    data object Everything : StoredAppScope {
        override fun parse(): Parsed<AppScope> = Parsed.Ok(AppScope.Everything)
    }

    @Serializable
    @SerialName("narrowed")
    data class Narrowed(
        private val accounts: List<StoredAccount>,
        private val addresses: List<String>,
        private val chats: List<StoredChatPick>,
        private val patterns: List<StoredPatternPick>,
        private val asking: Boolean,
    ) : StoredAppScope {
        override fun parse(): Parsed<AppScope> =
            accounts.parseEach("account") { it.parse() }.then { parsedAccounts ->
                addresses.parseEach("address", ::address).then { parsedAddresses ->
                    chats.parseEach("chat") { it.parse() }.then { parsedChats ->
                        patterns.parseEach("pattern") { it.parse() }.then { parsedPatterns ->
                            scopeOf(
                                parsedAccounts.map { (group, label) -> AccountId.Group(group) to label } +
                                    parsedAddresses.map { AccountId.Address(it) to GroupName.of(it) },
                                parsedChats,
                                parsedPatterns,
                            )
                        }
                    }
                }
            }

        private fun address(raw: String): Parsed<AccountAddress> =
            when (val parse = AccountAddress.parse(raw)) {
                is AccountAddressParse.Valid -> Parsed.Ok(parse.address)
                AccountAddressParse.NotAnAddress -> Parsed.Failed("\"$raw\" isn't an address")
            }

        private fun scopeOf(
            parsedAccounts: List<Pair<AccountId, GroupName>>,
            parsedChats: List<ChatPick>,
            parsedPatterns: List<PatternPick>,
        ): Parsed<AppScope> {
            val repeatedAccounts = parsedAccounts.groupBy { it.first }.filterValues { it.size > 1 }.keys
            val repeatedChats = parsedChats.groupBy { it.id }.filterValues { it.size > 1 }.keys

            return when {
                repeatedAccounts.isNotEmpty() -> {
                    Parsed.Failed("duplicate accounts ${repeatedAccounts.map(::describe)}")
                }

                repeatedChats.isNotEmpty() -> {
                    Parsed.Failed("duplicate chats ${repeatedChats.map { it.value }}")
                }

                else -> {
                    when (
                        val parse =
                            AppScope.Narrowed.parse(
                                parsedAccounts.toMap(),
                                parsedChats,
                                parsedPatterns.toSet(),
                                when {
                                    asking -> NewChatsAsking.Ask
                                    else -> NewChatsAsking.DontAsk
                                },
                            )
                    ) {
                        is NarrowedParse.Valid -> Parsed.Ok(parse.scope)
                        NarrowedParse.NothingChosen -> Parsed.Failed("narrowed to nothing")
                    }
                }
            }
        }

        private fun describe(id: AccountId): String =
            when (id) {
                is AccountId.Group -> id.id.value
                is AccountId.Address -> id.address.value
            }
    }

    companion object {
        private fun stored(accounts: Map<AccountId, GroupName>): Pair<List<StoredAccount>, List<String>> =
            accounts.entries.fold(emptyList<StoredAccount>() to emptyList()) { (groups, addresses), (id, label) ->
                when (id) {
                    is AccountId.Group -> (groups + StoredAccount(id.id.value, label.value)) to addresses
                    is AccountId.Address -> groups to (addresses + id.address.value)
                }
            }

        fun from(scope: AppScope): StoredAppScope =
            when (scope) {
                AppScope.Everything -> {
                    Everything
                }

                is AppScope.Narrowed -> {
                    val (groups, addresses) = stored(scope.accounts)

                    Narrowed(
                        groups,
                        addresses,
                        scope.chats.values.map(StoredChatPick::from),
                        scope.patterns.map(StoredPatternPick::from),
                        scope.asking == NewChatsAsking.Ask,
                    )
                }
            }
    }
}
