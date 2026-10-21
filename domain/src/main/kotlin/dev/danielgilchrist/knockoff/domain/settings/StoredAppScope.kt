package dev.danielgilchrist.knockoff.domain.settings

import dev.danielgilchrist.knockoff.domain.group.GroupName
import dev.danielgilchrist.knockoff.domain.notification.ChannelGroupId
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
        private val chats: List<StoredChatPick>,
        private val patterns: List<StoredPatternPick>,
        private val asking: Boolean,
    ) : StoredAppScope {
        override fun parse(): Parsed<AppScope> =
            accounts.parseEach("account") { it.parse() }.then { parsedAccounts ->
                chats.parseEach("chat") { it.parse() }.then { parsedChats ->
                    patterns.parseEach("pattern") { it.parse() }.then { parsedPatterns ->
                        scopeOf(parsedAccounts, parsedChats, parsedPatterns)
                    }
                }
            }

        private fun scopeOf(
            parsedAccounts: List<Pair<ChannelGroupId, GroupName>>,
            parsedChats: List<ChatPick>,
            parsedPatterns: List<PatternPick>,
        ): Parsed<AppScope> {
            val repeatedAccounts = parsedAccounts.groupBy { it.first }.filterValues { it.size > 1 }.keys
            val repeatedChats = parsedChats.groupBy { it.id }.filterValues { it.size > 1 }.keys

            return when {
                repeatedAccounts.isNotEmpty() -> {
                    Parsed.Failed("duplicate accounts ${repeatedAccounts.map { it.value }}")
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
    }

    companion object {
        fun from(scope: AppScope): StoredAppScope =
            when (scope) {
                AppScope.Everything -> {
                    Everything
                }

                is AppScope.Narrowed -> {
                    Narrowed(
                        scope.accounts.map { (group, label) -> StoredAccount(group.value, label.value) },
                        scope.chats.values.map(StoredChatPick::from),
                        scope.patterns.map(StoredPatternPick::from),
                        scope.asking == NewChatsAsking.Ask,
                    )
                }
            }
    }
}
