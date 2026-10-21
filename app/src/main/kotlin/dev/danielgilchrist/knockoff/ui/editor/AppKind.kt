package dev.danielgilchrist.knockoff.ui.editor

import dev.danielgilchrist.knockoff.domain.notification.PackageName
import dev.danielgilchrist.knockoff.domain.rule.NewChatsAsking
import dev.danielgilchrist.knockoff.domain.rule.RuleApps
import dev.danielgilchrist.knockoff.domain.rule.Target
import dev.danielgilchrist.knockoff.ui.GroupNoun
import dev.danielgilchrist.knockoff.ui.SeenChatsState
import dev.danielgilchrist.knockoff.ui.TargetChoices

enum class AppKind {
    Plain,
    Accounts,
    Chats,
    AccountsAndChats,
    ;

    companion object {
        fun forApp(
            app: PackageName,
            draft: AppDraft,
            stored: RuleApps,
            choices: TargetChoices,
        ): AppChoices {
            val accounts = AccountChoices.forApp(app, choices.groups, stored, draft)

            return AppChoices(of(app, draft, accounts, choices.chats), accounts)
        }

        fun of(
            app: PackageName,
            draft: AppDraft,
            accounts: AccountChoices,
            seen: SeenChatsState,
        ): AppKind {
            val hasAccounts = accounts.offered.isNotEmpty()
            val chats =
                mayHaveChats(app, seen) ||
                    draft.chats.isNotEmpty() ||
                    draft.asking == NewChatsAsking.Ask

            return when {
                hasAccounts && chats -> AccountsAndChats
                hasAccounts -> Accounts
                chats -> Chats
                else -> Plain
            }
        }

        private fun mayHaveChats(
            app: PackageName,
            seen: SeenChatsState,
        ): Boolean =
            when (seen) {
                is SeenChatsState.Loaded -> seenChatsOf(app, seen).isNotEmpty()
                SeenChatsState.Loading -> false
                is SeenChatsState.Failed -> true
            }

        fun seenChatsOf(
            app: PackageName,
            seen: SeenChatsState,
        ): List<Target.Chat> =
            when (seen) {
                is SeenChatsState.Loaded -> seen.chats.filter { it.packageName == app }
                SeenChatsState.Loading, is SeenChatsState.Failed -> emptyList()
            }
    }

    val hasAccounts: Boolean get() = this == Accounts || this == AccountsAndChats

    val hasChats: Boolean get() = this == Chats || this == AccountsAndChats

    fun narrowed(noun: GroupNoun): String =
        when (this) {
            Plain -> "Only ones matching a pattern"
            Accounts -> "Only some ${noun.many}"
            Chats -> "Only some chats"
            AccountsAndChats -> "Only some ${noun.many} and chats"
        }

    fun more(noun: GroupNoun): String =
        when (this) {
            Plain -> "notifications"
            Accounts -> noun.many
            Chats -> "chats"
            AccountsAndChats -> "${noun.many} and chats"
        }

    fun pick(noun: GroupNoun): String =
        when (this) {
            Plain -> "pattern"
            Accounts -> noun.one
            Chats -> "chat"
            AccountsAndChats -> "${noun.one} or chat"
        }
}
