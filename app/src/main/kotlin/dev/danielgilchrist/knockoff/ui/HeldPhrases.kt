package dev.danielgilchrist.knockoff.ui

import dev.danielgilchrist.knockoff.domain.notification.PackageName
import dev.danielgilchrist.knockoff.domain.rule.AppScope
import dev.danielgilchrist.knockoff.domain.rule.PatternPick
import dev.danielgilchrist.knockoff.domain.rule.Policy
import dev.danielgilchrist.knockoff.domain.rule.Rule

internal object HeldPhrases {
    private const val SHOWN = 3

    fun of(
        rule: Rule,
        labelOf: (PackageName) -> String,
    ): List<HeldPhrase> =
        rule.apps.scopes.flatMap { (app, scope) ->
            when (scope) {
                AppScope.Everything -> listOf(one(labelOf(app)))
                is AppScope.Narrowed -> narrowed(labelOf(app), scope)
            }
        }

    fun listed(
        first: String,
        rest: List<String>,
    ): String {
        val phrases = listOf(first) + rest

        return when {
            rest.isEmpty() -> first
            phrases.size <= SHOWN -> "${phrases.dropLast(1).joinToString()} and ${phrases.last()}"
            else -> "${phrases.take(SHOWN).joinToString()} and ${phrases.size - SHOWN} more"
        }
    }

    fun listed(
        first: HeldPhrase,
        rest: List<HeldPhrase>,
    ): HeldPhrase = first.followedBy(rest, ::listed)

    fun effect(
        held: HeldPhrase,
        policy: Policy,
        hours: String,
    ): String =
        when (policy) {
            Policy.DeliverOnlyDuring -> {
                when (held.number) {
                    GrammaticalNumber.One -> "${held.opening} only reaches you $hours."
                    GrammaticalNumber.Many -> "${held.opening} only reach you $hours."
                }
            }

            Policy.HoldDuring -> {
                when (held.number) {
                    GrammaticalNumber.One -> "${held.opening} goes quiet $hours."
                    GrammaticalNumber.Many -> "${held.opening} go quiet $hours."
                }
            }
        }

    private fun narrowed(
        app: String,
        scope: AppScope.Narrowed,
    ): List<HeldPhrase> {
        val accounts = scope.accounts.values
        val chats = scope.chats.values
        val noun = GroupNoun.of(accounts)

        return when {
            scope.patterns.isNotEmpty() -> {
                listOf(HeldPhrase.some("$app ${matched(scope)}"))
            }

            accounts.isEmpty() && chats.isEmpty() -> {
                emptyList()
            }

            chats.isEmpty() -> {
                listOf(
                    when (accounts.size) {
                        1 -> one("${accounts.single().value} in $app")
                        else -> many("${accounts.size} $app ${noun.many}")
                    },
                )
            }

            accounts.isEmpty() -> {
                listOf(
                    when (chats.size) {
                        1 -> one("${chats.single().name.value} on $app")
                        else -> many("${chats.size} $app chats")
                    },
                )
            }

            else -> {
                listOf(many("${noun.counted(accounts.size)} plus ${counted(chats.size, "chat", "chats")} in $app"))
            }
        }
    }

    private fun one(text: String): HeldPhrase = HeldPhrase.of(text, GrammaticalNumber.One)

    private fun many(text: String): HeldPhrase = HeldPhrase.of(text, GrammaticalNumber.Many)

    private fun matched(scope: AppScope.Narrowed): String =
        when {
            scope.accounts.isEmpty() && scope.patterns.all(::namesChats) -> "chats"
            else -> "notifications"
        }

    private fun namesChats(pick: PatternPick): Boolean =
        when (pick) {
            is PatternPick.ChatName -> true
            is PatternPick.Channel -> false
        }
}
