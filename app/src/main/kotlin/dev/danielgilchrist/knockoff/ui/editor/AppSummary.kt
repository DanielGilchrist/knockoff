package dev.danielgilchrist.knockoff.ui.editor

import dev.danielgilchrist.knockoff.domain.rule.NewChatsAsking
import dev.danielgilchrist.knockoff.ui.GroupNoun
import dev.danielgilchrist.knockoff.ui.counted

internal object AppSummary {
    private const val NAMED_CHATS = 2

    fun of(
        draft: AppDraft,
        accounts: AccountChoices,
    ): String =
        when (draft.mode) {
            AppMode.Everything -> "All notifications"
            AppMode.Narrowed -> narrowed(draft, accounts)
        }

    fun unpicked(
        kind: AppKind,
        noun: GroupNoun,
    ): String =
        when (kind) {
            AppKind.Plain -> "No patterns added"
            AppKind.Accounts -> "No ${noun.manyOr()} picked"
            AppKind.Chats -> "No chats picked"
            AppKind.AccountsAndChats -> "No ${noun.manyOr("chats")} picked"
        }

    private fun narrowed(
        draft: AppDraft,
        choices: AccountChoices,
    ): String {
        val accounts =
            when (draft.accounts.size) {
                0 -> {
                    emptyList()
                }

                1 -> {
                    val (id, label) = draft.accounts.entries.single()

                    listOf(choices.offered.getOrDefault(id, label).value)
                }

                else -> {
                    choices.list.counted(draft.accounts.keys)
                }
            }
        val chats =
            when (draft.chats.size) {
                0 -> {
                    emptyList()
                }

                in 1..NAMED_CHATS -> {
                    listOf(
                        draft.chats.values
                            .map { it.name.value }
                            .sorted()
                            .joinToString(),
                    )
                }

                else -> {
                    listOf(counted(draft.chats.size, "chat", "chats"))
                }
            }
        val patterns =
            when (draft.patterns.size) {
                0 -> emptyList()
                else -> listOf(counted(draft.patterns.size, "pattern", "patterns"))
            }
        val asking =
            when (draft.asking) {
                NewChatsAsking.DontAsk -> {
                    emptyList()
                }

                NewChatsAsking.Ask -> {
                    when {
                        draft.chats.isEmpty() -> listOf("no chats yet · asks about new ones")
                        else -> listOf("asks about new ones")
                    }
                }
            }

        return (accounts + chats + patterns + asking).joinToString(" · ").ifEmpty { "Nothing chosen" }
    }
}
